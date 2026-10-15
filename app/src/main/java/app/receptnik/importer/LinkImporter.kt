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

data class ImportResult(val draft: RecipeDraft, val warnings: List<String> = emptyList())

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
        val url = TextUtil.firstUrl(input) ?: throw IllegalArgumentException("В тексте нет ссылки")
        val warnings = mutableListOf<String>()
        val src = when (platformOf(url)) {
            Platform.TIKTOK -> tiktok(url)
            Platform.INSTAGRAM -> instagram(url)
            Platform.YOUTUBE -> youtube(url)
            Platform.FACEBOOK -> ogPage(url, Http.UA_BOT)
            Platform.PINTEREST -> pinterest(url)
            Platform.WEB -> web(url)
        }

        val ai = gemini()
        var parsed: RecipeDraft? = src.structured?.takeIf { it.ingredients.isNotEmpty() }
        if (parsed == null) {
            val material = listOfNotNull(src.title, src.text).joinToString("\n\n").trim()
            if (ai != null && material.isNotBlank()) {
                parsed = runCatching { ai.extract(material) }
                    .onFailure { warnings += it.message ?: "Ошибка ИИ" }
                    .getOrNull()
            }
        }
        val base: RecipeDraft = parsed ?: CaptionParser.parse(src.text, fallbackTitle = src.title)

        val draft = base.copy(
            title = base.title.ifBlank { src.title?.let { CaptionParser.cleanTitle(it) } ?: "" }
                .ifBlank { "Рецепт" + (src.author?.let { " от $it" } ?: "") },
            imageUrl = base.imageUrl ?: src.image,
            sourceUrl = src.url,
            author = base.author ?: src.author,
        )

        if (draft.ingredients.isEmpty()) {
            warnings += if (ai == null)
                "В описании не нашлось списка ингредиентов. Впиши их вручную или добавь бесплатный ключ Gemini в настройках — тогда ИИ разберёт даже сложные подписи."
            else "В посте нет списка ингредиентов — возможно, он только в самом видео. Впиши вручную."
        }
        return ImportResult(withNutrition(draft, ai != null), warnings)
    }

    /** Считает КБЖУ встроенной таблицей; если покрытие плохое — оставляет оценку сайта/ИИ. */
    fun withNutrition(draft: RecipeDraft, aiAvailable: Boolean = false): RecipeDraft {
        val r = NutritionDb.calculate(draft.ingredients)
        if (r.considered == 0) return draft
        val local = r.total
        val ownBetter = draft.nutrition == null || r.coverage >= 0.8
        return if (ownBetter && r.matched > 0) draft.copy(
            nutrition = local,
            nutritionNote = if (r.coverage >= 1.0) "Рассчитано по всем ингредиентам"
            else "Рассчитано по ${r.matched} из ${r.considered} ингредиентов",
        ) else draft
    }

    // ---------- Платформы ----------

    private suspend fun tiktok(url: String): Source {
        val resolved = runCatching { Http.get(url, Http.UA_DESKTOP).finalUrl }.getOrDefault(url).substringBefore('?')
        val oembed = Http.get("https://www.tiktok.com/oembed?url=" + enc(resolved), Http.UA_DESKTOP)
        val o = json.parseToJsonElement(oembed.body).jsonObject
        return Source(
            url = resolved,
            text = o["title"]?.jsonPrimitive?.content ?: "",
            image = o["thumbnail_url"]?.jsonPrimitive?.content,
            author = o["author_name"]?.jsonPrimitive?.content,
        )
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
        val page = Http.get(url, Http.UA_DESKTOP)
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
        val page = Http.get(url, Http.UA_MOBILE)
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
        val page = Http.get(url, ua)
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
