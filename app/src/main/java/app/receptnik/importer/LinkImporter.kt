package app.receptnik.importer

import app.receptnik.core.CaptionParser
import app.receptnik.core.NutritionDb
import app.receptnik.core.RecipeDraft
import app.receptnik.core.TextUtil
import app.receptnik.core.TikTokLinks
import app.receptnik.core.VideoLinks
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.net.URI
import java.net.URLEncoder

/** Import warnings; messages live in UI resources. */
enum class ImportWarning { NO_INGREDIENTS, NO_INGREDIENTS_WITH_AI, PHOTO_UNREADABLE, PHOTO_NO_RECIPE }

/** Codes describing where nutrition data came from (stored with the recipe). */
object NutritionNote {
    const val SITE = "site"
    const val AI = "ai"
    const val ALL = "calc:all"
    fun partial(matched: Int, total: Int) = "calc:$matched/$total"
}

data class ImportResult(
    val draft: RecipeDraft,
    val warnings: List<ImportWarning> = emptyList(),
    /** Why AI parsing failed, if it was attempted and failed. */
    val aiError: AiException? = null,
    /** True when the recipe was extracted from the video itself. */
    val fromVideo: Boolean = false,
)

/** Progress stages reported to the UI while importing. */
enum class ImportStage { FETCHING, READING, DOWNLOADING_VIDEO, WATCHING_VIDEO }

/** Raw material extracted from a page before recipe parsing. */
private data class Source(
    val url: String,
    val text: String = "",
    val title: String? = null,
    val image: String? = null,
    val author: String? = null,
    val structured: RecipeDraft? = null,
    /** Direct video file URL, when the page exposes one. */
    val videoUrl: String? = null,
)

/**
 * Imports a recipe from a link. [cacheDir] holds temporary video downloads for AI video understanding.
 */
class LinkImporter(private val gemini: () -> GeminiClient?, private val cacheDir: File) {
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

    suspend fun import(input: String, onStage: (ImportStage) -> Unit = {}): ImportResult {
        val url = TextUtil.firstUrl(input) ?: throw ImportException(ImportException.Reason.NO_LINK)
        val warnings = mutableListOf<ImportWarning>()
        var aiError: AiException? = null
        var fromVideo = false
        val platform = platformOf(url)
        onStage(ImportStage.FETCHING)
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
            // Last resort: the link preview social networks serve to messenger crawlers
            runCatching { ogPage(url, Http.UA_BOT) }.getOrNull()
                ?.takeIf { it.text.isNotBlank() && !(platform == Platform.TIKTOK && TikTokLinks.isGeneric(it.text)) }
                ?: throw if (e is ImportException) e else ImportException(ImportException.Reason.BAD_RESPONSE, e.message)
        }

        val ai = gemini()
        val material = listOfNotNull(src.title, src.text).joinToString("\n\n").trim()
        var parsed: RecipeDraft? = src.structured?.takeIf { it.ingredients.isNotEmpty() }

        // 1. AI on the caption text
        if (parsed == null && ai != null && material.isNotBlank()) {
            onStage(ImportStage.READING)
            try { parsed = ai.extract(material) } catch (e: AiException) { aiError = e }
        }
        // 2. Built-in parser on the caption text
        val captionDraft = CaptionParser.parse(src.text, fallbackTitle = src.title)
        if (parsed == null && captionDraft.ingredients.isNotEmpty()) parsed = captionDraft

        // 3. The recipe is only in the video: let AI watch it
        val canRetryAi = aiError == null || aiError.reason == AiException.Reason.OTHER
        if (parsed == null && ai != null && canRetryAi && platform != Platform.WEB && platform != Platform.PINTEREST) {
            var tmp: File? = null
            try {
                if (platform == Platform.YOUTUBE) {
                    onStage(ImportStage.WATCHING_VIDEO)
                    parsed = ai.extractFromVideo(null, src.url, material)
                } else {
                    val videoUrl = src.videoUrl ?: findVideoUrl(platform, src.url)
                    if (videoUrl != null) {
                        onStage(ImportStage.DOWNLOADING_VIDEO)
                        tmp = File.createTempFile("recipe", ".mp4", cacheDir)
                        val ok = Http.download(videoUrl, tmp, referer = refererFor(platform))
                        if (ok) {
                            onStage(ImportStage.WATCHING_VIDEO)
                            parsed = ai.extractFromVideo(tmp, null, material)
                        }
                    }
                }
                fromVideo = parsed != null
            } catch (e: AiException) {
                aiError = e
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Video is a best-effort enhancement; caption results still apply
            } finally {
                tmp?.delete()
            }
        }

        val base: RecipeDraft = parsed ?: captionDraft
        val draft = base.copy(
            title = base.title.ifBlank { src.title?.let { CaptionParser.cleanTitle(TextUtil.stripAuthorPrefix(it)) } ?: "" }
                .ifBlank { src.author?.let { "@$it" } ?: "" },
            imageUrl = base.imageUrl ?: src.image,
            sourceUrl = src.url,
            author = base.author ?: src.author,
        )

        if (draft.ingredients.isEmpty()) {
            warnings += if (ai == null) ImportWarning.NO_INGREDIENTS else ImportWarning.NO_INGREDIENTS_WITH_AI
        }
        return ImportResult(withNutrition(draft), warnings, aiError, fromVideo)
    }

    private fun refererFor(p: Platform) = when (p) {
        Platform.TIKTOK -> "https://www.tiktok.com/"
        Platform.INSTAGRAM -> "https://www.instagram.com/"
        Platform.FACEBOOK -> "https://www.facebook.com/"
        else -> null
    }

    /** Looks up a direct video URL for platforms that do not expose it in the first response. */
    private suspend fun findVideoUrl(platform: Platform, url: String): String? = when (platform) {
        Platform.TIKTOK -> {
            // Loading the page also sets the cookies the video CDN requires
            val page = runCatching { Http.getWithRetry(url, Http.UA_DESKTOP, attempts = 2) }.getOrNull()
            page?.let { tiktokEmbedded(Jsoup.parse(it.body, it.finalUrl), TikTokLinks.videoId(url))?.videoUrl }
                ?: runCatching { tikwm(url)?.videoUrl }.getOrNull()
        }
        Platform.INSTAGRAM, Platform.FACEBOOK -> {
            val page = runCatching { Http.get(url, Http.UA_BOT) }.getOrNull()
            page?.let { VideoLinks.fromHtml(it.body) }
        }
        else -> null
    }

    /** Computes nutrition from the built-in table; keeps the site/AI estimate when coverage is poor. */
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

    // ---------- Platforms ----------

    /**
     * TikTok, in order:
     * 1) expand the short link, stopping at the first URL that contains the video ID
     *    (TikTok may redirect further, to its home page);
     * 2) oEmbed; 3) the video page's embedded JSON; 4) the embed player page;
     * 5) the public tikwm.com API; 6) og: tags of the video page.
     * Generic site-wide previews ("TikTok | Make Your Day") are rejected at every step.
     */
    private suspend fun tiktok(url: String): Source {
        val full = runCatching { Http.resolve(url, stopWhen = { TikTokLinks.videoId(it) != null }) }.getOrDefault(url)
        val id = TikTokLinks.videoId(full) ?: TikTokLinks.videoId(url)
        val canonical = TikTokLinks.canonical(full) ?: full.substringBefore('?')
        var firstError: Exception? = null
        fun good(s: Source?) = s != null && !TikTokLinks.isGeneric(s.text)

        suspend fun attempt(block: suspend () -> Source?): Source? = try {
            block()?.takeIf { good(it) }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (firstError == null) firstError = e
            null
        }

        // 2. oEmbed
        attempt {
            val page = Http.getWithRetry("https://www.tiktok.com/oembed?url=" + enc(canonical), Http.UA_DESKTOP, attempts = 2)
            val o = json.parseToJsonElement(page.body).jsonObject
            Source(
                url = canonical,
                text = o["title"]?.jsonPrimitive?.content.orEmpty(),
                image = o["thumbnail_url"]?.jsonPrimitive?.content,
                author = o["author_name"]?.jsonPrimitive?.content,
            )
        }?.let { return it }

        // 3. Video page: embedded JSON
        var ogFallback: Source? = null
        for (ua in listOf(Http.UA_DESKTOP, Http.UA_MOBILE)) {
            val found = attempt {
                val page = Http.getWithRetry(canonical, ua, attempts = 1)
                val doc = Jsoup.parse(page.body, page.finalUrl)
                if (ogFallback == null && TikTokLinks.videoId(page.finalUrl) != null) {
                    ogFallback = fromDoc(canonical, doc).takeIf { good(it) }
                }
                tiktokEmbedded(doc, id)
            }
            if (found != null) return found.copy(url = canonical)
        }

        if (id != null) {
            // 4. Embed player page
            attempt {
                val page = Http.getWithRetry("https://www.tiktok.com/embed/v2/$id", Http.UA_DESKTOP, attempts = 1)
                tiktokEmbedded(Jsoup.parse(page.body, page.finalUrl), id)
            }?.let { return it.copy(url = canonical) }

            // 5. Public API (third party; see PRIVACY.md)
            attempt { tikwm(canonical) }?.let { return it.copy(url = canonical) }
        }

        // 6. og: tags of the actual video page
        ogFallback?.let { return it }
        throw (firstError as? ImportException) ?: ImportException(ImportException.Reason.BAD_RESPONSE, firstError?.message)
    }

    /** tikwm.com public API: returns the caption, cover and a direct video URL. */
    private suspend fun tikwm(videoUrl: String): Source? {
        val page = Http.getWithRetry("https://www.tikwm.com/api/?url=" + enc(videoUrl), Http.UA_DESKTOP, attempts = 2)
        val root = json.parseToJsonElement(page.body).jsonObject
        if (root["code"]?.jsonPrimitive?.content != "0") return null
        val d = root["data"] as? kotlinx.serialization.json.JsonObject ?: return null
        fun str(k: String) = (d[k] as? kotlinx.serialization.json.JsonPrimitive)?.content
        fun abs(u: String?) = u?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it }
        return Source(
            url = videoUrl,
            text = str("title").orEmpty(),
            image = abs(str("cover") ?: str("origin_cover")),
            author = ((d["author"] as? kotlinx.serialization.json.JsonObject)?.get("unique_id") as? kotlinx.serialization.json.JsonPrimitive)?.content,
            videoUrl = abs(str("play")),
        )
    }

    private fun tiktokEmbedded(doc: Document, id: String? = null): Source? {
        val scripts = listOf("script#__UNIVERSAL_DATA_FOR_REHYDRATION__", "script#SIGI_STATE", "script#__FRONTITY_CONNECT_STATE__", "script#__NEXT_DATA__")
            .mapNotNull { doc.selectFirst(it)?.data() }
        val item = scripts.firstNotNullOfOrNull { raw ->
            runCatching { json.parseToJsonElement(raw) }.getOrNull()?.let { findItemStruct(it, id) }
        } ?: return null
        val desc = listOf("desc", "text").firstNotNullOfOrNull { k ->
            (item[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        }.orEmpty()
        if (desc.isBlank() && item["video"] == null) return null
        val author = (item["author"] as? kotlinx.serialization.json.JsonObject)?.get("uniqueId")
            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            ?: (item["author"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val cover = (item["video"] as? kotlinx.serialization.json.JsonObject)?.let { v ->
            listOf("cover", "originCover", "dynamicCover").firstNotNullOfOrNull { k ->
                (v[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.startsWith("http") }
            }
        }
        val video = (item["video"] as? kotlinx.serialization.json.JsonObject)?.let { v ->
            listOf("playAddr", "downloadAddr").firstNotNullOfOrNull { k ->
                (v[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.startsWith("http") }
            }
        }
        return Source(url = "", text = desc, image = cover, author = author, videoUrl = video)
    }

    /** Finds the video object (with desc and id fields) anywhere in the JSON. */
    private fun findItemStruct(
        el: kotlinx.serialization.json.JsonElement,
        wantedId: String? = null,
        depth: Int = 0,
    ): kotlinx.serialization.json.JsonObject? {
        if (depth > 10) return null
        return when (el) {
            is kotlinx.serialization.json.JsonObject -> {
                val idValue = (el["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                val hasText = "desc" in el || ("text" in el && "video" in el)
                if (hasText && idValue != null && (wantedId == null || idValue == wantedId)) el
                else el.values.firstNotNullOfOrNull { findItemStruct(it, wantedId, depth + 1) }
            }
            is kotlinx.serialization.json.JsonArray -> el.firstNotNullOfOrNull { findItemStruct(it, wantedId, depth + 1) }
            else -> null
        }
    }

    private suspend fun instagram(url: String): Source {
        val code = Regex("instagram\\.com/(?:[^/]+/)?(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)
        var text = ""
        var image: String? = null
        var author: String? = null
        var video: String? = null
        if (code != null) {
            runCatching {
                val page = Http.get("https://www.instagram.com/p/$code/embed/captioned/", Http.UA_DESKTOP)
                video = VideoLinks.fromHtml(page.body)
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
            videoUrl = video ?: og?.videoUrl,
        )
    }

    /** "1,234 likes, 56 comments - user on May 5, 2026: \"Caption...\"" → "Caption...". */
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
        // A pin usually links to the recipe website; import from there
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
        // Article text, for pages without structured markup
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
        text = TextUtil.stripAuthorPrefix(og(doc, "og:description") ?: doc.selectFirst("meta[name=description]")?.attr("content") ?: ""),
        title = og(doc, "og:title") ?: doc.title(),
        image = og(doc, "og:image"),
        author = doc.selectFirst("meta[name=author]")?.attr("content"),
        videoUrl = VideoLinks.fromHtml(doc.outerHtml()),
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
