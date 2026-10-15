package app.receptnik.importer

import app.receptnik.core.CaptionParser
import app.receptnik.core.NutritionDb
import app.receptnik.core.RecipeDraft
import app.receptnik.core.TextUtil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.net.URI
import java.net.URLEncoder

/** Предупреждения импорта; тексты — в ресурсах интерфейса. */
enum class ImportWarning { NO_INGREDIENTS, NO_INGREDIENTS_WITH_AI, PHOTO_UNREADABLE, PHOTO_NO_RECIPE }

/** Коды пометки о происхождении КБЖУ (сохраняются в рецепте). */
object NutritionNote {
    const val SITE = "site"
    const val AI = "ai"
    const val ALL = "calc:all"
    fun partial(matched: Int, total: Int) = "calc:$matched/$total"
}

data class ImportResult(
    val draft: RecipeDraft,
    val warnings: List<ImportWarning> = emptyList(),
    /** Текст ошибки Gemini, если ИИ не сработал (показывается как есть). */
    val aiError: String? = null,
)

/** Что удалось вытащить со страницы до разбора на рецепт. */
private data class Source(
    val url: String,
    val text: String = "",
    val title: String? = null,
    val image: String? = null,
    val author: String? = null,
    val structured: RecipeDraft? = null,
)

class LinkImporter(private val gemini: () -> GeminiClient?) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    enum class Platform { TIKTOK, INSTAGRAM, YOUTUBE, FACEBOOK, PINTEREST, WEB }

    fun platformOf(url: String): Platform {
        val host = runCatching { URI(url).host?.lowercase() ?: "" }.getOrDefault("")
        return when {
            "tiktok.com" in host -> Platform.TIKTOK
            "instagram.com" in host || "instagr.am" in host -> Platform.INSTAGRAM
            "youtube.com" in host || "youtu.be" in host -> Platform.YOUTUBE
            "facebook.com" in host || "fb.watch" in host || "fb.com" in host -> Platform.FACEBOOK
            "pinterest." in host || "pin.it" in host -> Platform.PINTEREST
            else -> Platform.WEB
        }
    }

    suspend fun import(input: String): ImportResult {
        val url = TextUtil.firstUrl(input) ?: throw ImportException(ImportException.Reason.NO_LINK)
        val warnings = mutableListOf<ImportWarning>()
        var aiError: String? = null
        val platform = platformOf(url)
        val src = try {
            when (platform) {
                Platform.TIKTOK -> tiktok(url)
                Platform.INSTAGRAM -> instagram(url)
                Platform.YOUTUBE -> youtube(url)
                Platform.FACEBOOK -> ogPage(url, Http.UA_BOT)
                Platform.PINTEREST -> pinterest(url)
                Platform.WEB -> web(url)
            }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Последний шанс: превью ссылки, которое соцсети отдают роботам мессенджеров
            runCatching { ogPage(url, Http.UA_BOT) }.getOrNull()?.takeIf { it.text.isNotBlank() }
                ?: throw if (e is ImportException) e else ImportException(ImportException.Reason.BAD_RESPONSE, e.message)
        }

        val ai = gemini()
        var parsed: RecipeDraft? = src.structured?.takeIf { it.ingredients.isNotEmpty() }
        if (parsed == null) {
            val material = listOfNotNull(src.title, src.text).joinToString("\n\n").trim()
            if (ai != null && material.isNotBlank()) {
                parsed = runCatching { ai.extract(material) }
                    .onFailure { aiError = it.message ?: "Gemini error" }
                    .getOrNull()
            }
        }
        val base: RecipeDraft = parsed ?: CaptionParser.parse(src.text, fallbackTitle = src.title)

        val draft = base.copy(
            title = base.title.ifBlank { src.title?.let { CaptionParser.cleanTitle(it) } ?: "" }
                .ifBlank { src.author?.let { "@$it" } ?: "" },
            imageUrl = base.imageUrl ?: src.image,
            sourceUrl = src.url,
            author = base.author ?: src.author,
        )

        if (draft.ingredients.isEmpty()) {
            warnings += if (ai == null) ImportWarning.NO_INGREDIENTS else ImportWarning.NO_INGREDIENTS_WITH_AI
        }
        return ImportResult(withNutrition(draft), warnings, aiError)
    }

    /** Считает КБЖУ встроенной таблицей; если покрытие плохое — оставляет оценку сайта/ИИ. */
    fun withNutrition(draft: RecipeDraft): RecipeDraft {
        val r = NutritionDb.calculate(draft.ingredients)
        if (r.considered == 0) return draft
        val local = r.total
        val ownBetter = draft.nutrition == null || r.coverage >= 0.8
        return if (ownBetter && r.matched > 0) draft.copy(
            nutrition = local,
            nutritionNote = if (r.coverage >= 1.0) NutritionNote.ALL else NutritionNote.partial(r.matched, r.considered),
        ) else draft
    }

    // ---------- Платформы ----------

    /**
     * TikTok: 1) раскрываем короткую ссылку; 2) oEmbed (подпись целиком);
     * 3) если oEmbed ограничен (429) — данные со страницы видео; 4) og-теги.
     */
    private suspend fun tiktok(url: String): Source {
        val full = runCatching { Http.resolve(url) }.getOrDefault(url)
        val canonical = full.substringBefore('?')
        var firstError: Exception? = null

        // 1. oEmbed
        try {
            val page = Http.getWithRetry("https://www.tiktok.com/oembed?url=" + enc(canonical), Http.UA_DESKTOP, attempts = 2)
            val o = json.parseToJsonElement(page.body).jsonObject
            val text = o["title"]?.jsonPrimitive?.content.orEmpty()
            if (text.isNotBlank()) return Source(
                url = canonical,
                text = text,
                image = o["thumbnail_url"]?.jsonPrimitive?.content,
                author = o["author_name"]?.jsonPrimitive?.content,
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            firstError = e
        }

        // 2. Страница видео: встроенные данные (__UNIVERSAL_DATA_FOR_REHYDRATION__ / SIGI_STATE) и og-теги
        for (ua in listOf(Http.UA_DESKTOP, Http.UA_BOT)) {
            val page = runCatching { Http.getWithRetry(canonical, ua, attempts = 2) }.getOrNull() ?: continue
            val doc = Jsoup.parse(page.body, page.finalUrl)
            tiktokEmbedded(doc)?.let { return it.copy(url = canonical) }
            val og = fromDoc(canonical, doc)
            if (og.text.isNotBlank()) return og.copy(url = canonical)
        }
        throw (firstError as? ImportException) ?: ImportException(ImportException.Reason.BAD_RESPONSE, firstError?.message)
    }

    private fun tiktokEmbedded(doc: Document): Source? {
        val raw = doc.selectFirst("script#__UNIVERSAL_DATA_FOR_REHYDRATION__")?.data()
            ?: doc.selectFirst("script#SIGI_STATE")?.data()
            ?: return null
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return null
        val item = findItemStruct(root) ?: return null
        val desc = (item["desc"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
        if (desc.isBlank()) return null
        val author = (item["author"] as? kotlinx.serialization.json.JsonObject)?.get("uniqueId")
            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            ?: (item["author"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val cover = (item["video"] as? kotlinx.serialization.json.JsonObject)?.let { v ->
            listOf("cover", "originCover", "dynamicCover").firstNotNullOfOrNull { k ->
                (v[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.startsWith("http") }
            }
        }
        return Source(url = "", text = desc, image = cover, author = author)
    }

    /** Ищет объект видео (с полями desc и id) в любом месте JSON. */
    private fun findItemStruct(el: kotlinx.serialization.json.JsonElement, depth: Int = 0): kotlinx.serialization.json.JsonObject? {
        if (depth > 8) return null
        return when (el) {
            is kotlinx.serialization.json.JsonObject -> {
                if ("desc" in el && "id" in el) el
                else el.values.firstNotNullOfOrNull { findItemStruct(it, depth + 1) }
            }
            is kotlinx.serialization.json.JsonArray -> el.firstNotNullOfOrNull { findItemStruct(it, depth + 1) }
            else -> null
        }
    }

    private suspend fun instagram(url: String): Source {
        val code = Regex("instagram\\.com/(?:[^/]+/)?(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)
        var text = ""
        var image: String? = null
        var author: String? = null
        if (code != null) {
            runCatching {
                val page = Http.get("https://www.instagram.com/p/$code/embed/captioned/", Http.UA_DESKTOP)
                val doc = Jsoup.parse(page.body, page.finalUrl)
                doc.selectFirst(".Caption")?.let { cap ->
                    author = cap.selectFirst(".CaptionUsername")?.text()
                    cap.select(".CaptionUsername, .CaptionComments").remove()
                    text = textWithBreaks(cap)
                }
                image = doc.selectFirst("img.EmbeddedMediaImage")?.absUrl("src")?.takeIf { it.isNotBlank() }
            }
        }
        val og = runCatching { ogPage(url, Http.UA_BOT) }.getOrNull()
        val ogCaption = og?.let { cleanInstagramOg(it.text, it.title) } ?: ""
        if (ogCaption.length > text.length) text = ogCaption
        return Source(
            url = code?.let { "https://www.instagram.com/p/$it/" } ?: url,
            text = text,
            image = image ?: og?.image,
            author = author ?: og?.author,
        )
    }

    /** «1 234 likes, 56 comments - user on May 5, 2026: "Подпись..."» → «Подпись...». */
    private fun cleanInstagramOg(desc: String, title: String?): String {
        fun strip(s: String): String {
            val idx = s.indexOf(": \"").takeIf { it in 0..200 } ?: s.indexOf(": “").takeIf { it in 0..200 }
            val body = if (idx != null) s.substring(idx + 3) else s
            return body.trim().trimEnd('"', '”', '.').trim()
        }
        val a = strip(desc)
        val b = title?.let { strip(it) } ?: ""
        return if (b.length > a.length) b else a
    }

    private suspend fun youtube(url: String): Source {
        val id = Regex("(?:v=|youtu\\.be/|shorts/|embed/|live/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
        val watch = if (id != null) "https://www.youtube.com/watch?v=$id" else url
        var title: String? = null
        var image: String? = null
        var author: String? = null
        runCatching {
            val o = json.parseToJsonElement(
                Http.get("https://www.youtube.com/oembed?format=json&url=" + enc(watch), Http.UA_DESKTOP).body
            ).jsonObject
            title = o["title"]?.jsonPrimitive?.content
            image = o["thumbnail_url"]?.jsonPrimitive?.content
            author = o["author_name"]?.jsonPrimitive?.content
        }
        var description = ""
        runCatching {
            val html = Http.get(watch, Http.UA_DESKTOP).body
            Regex("\"shortDescription\":\"((?:\\\\.|[^\"\\\\])*)\"").find(html)?.groupValues?.get(1)?.let { raw ->
                description = json.parseToJsonElement("\"$raw\"").jsonPrimitive.content
            }
        }
        if (id != null && image == null) image = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        return Source(url = watch, text = description, title = title, image = image, author = author)
    }

    private suspend fun pinterest(url: String): Source {
        val page = Http.getWithRetry(url, Http.UA_DESKTOP)
        val doc = Jsoup.parse(page.body, page.finalUrl)
        // Пин обычно ведёт на сайт с рецептом — импортируем оттуда
        val external = doc.selectFirst("meta[property=og:see_also]")?.attr("content")
            ?: Regex("\"link\":\"(https?:[^\"]+)\"").findAll(page.body).map { it.groupValues[1].replace("\\/", "/") }
                .firstOrNull { "pinterest" !in it }
        if (external != null) {
            val web = runCatching { web(external) }.getOrNull()
            if (web?.structured != null) return web
        }
        return fromDoc(page.finalUrl, doc)
    }

    private suspend fun web(url: String): Source {
        val page = Http.getWithRetry(url, Http.UA_MOBILE)
        val doc = Jsoup.parse(page.body, page.finalUrl)
        JsonLd.findRecipe(doc)?.let { obj ->
            val d = JsonLd.toDraft(obj, page.finalUrl)
            if (d.ingredients.isNotEmpty()) {
                return Source(url = page.finalUrl, structured = d, image = d.imageUrl ?: og(doc, "og:image"), title = d.title)
            }
        }
        val src = fromDoc(page.finalUrl, doc)
        // Текст статьи — для разбора без разметки
        val main = doc.selectFirst("article, main, [itemprop=recipeInstructions], .recipe, #recipe") ?: doc.body()
        main.select("script, style, nav, header, footer, aside, form, noscript, iframe").remove()
        val body = textWithBreaks(main).lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n").take(20000)
        return src.copy(text = body.ifBlank { src.text })
    }

    private suspend fun ogPage(url: String, ua: String): Source {
        val page = Http.getWithRetry(url, ua)
        return fromDoc(page.finalUrl, Jsoup.parse(page.body, page.finalUrl))
    }

    private fun fromDoc(url: String, doc: Document): Source = Source(
        url = og(doc, "og:url")?.takeIf { it.startsWith("http") } ?: url,
        text = og(doc, "og:description") ?: doc.selectFirst("meta[name=description]")?.attr("content") ?: "",
        title = og(doc, "og:title") ?: doc.title(),
        image = og(doc, "og:image"),
        author = doc.selectFirst("meta[name=author]")?.attr("content"),
    )

    private fun og(doc: Document, prop: String): String? =
        (doc.selectFirst("meta[property=$prop]") ?: doc.selectFirst("meta[name=$prop]"))
            ?.attr("content")?.takeIf { it.isNotBlank() }

    private fun textWithBreaks(el: Element): String {
        val copy = el.clone()
        copy.select("br").forEach { it.after(TextNode("\n")) }
        copy.select("p, div, li, h1, h2, h3, h4, tr").forEach {
            it.prependChild(TextNode("\n"))
            it.appendChild(TextNode("\n"))
        }
        return copy.wholeText().replace(Regex("[ \\t\\u00A0]+"), " ").replace(Regex("\\n\\s*\\n+"), "\n").trim()
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
