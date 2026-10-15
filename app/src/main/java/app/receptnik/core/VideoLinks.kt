package app.receptnik.core

/** Extracts a direct video file URL from page HTML (og: tags or embedded JSON). */
object VideoLinks {
    private val ogVideo = Regex(
        """<meta[^>]+(?:property|name)=["']og:video(?::secure_url|:url)?["'][^>]+content=["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val ogVideoReversed = Regex(
        """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']og:video(?::secure_url|:url)?["']""",
        RegexOption.IGNORE_CASE,
    )
    private val jsonVideo = Regex(""""(?:video_url|playable_url_quality_hd|playable_url|contentUrl)"\s*:\s*"(https?:[^"]+)"""")

    fun fromHtml(html: String): String? {
        val raw = ogVideo.find(html)?.groupValues?.get(1)
            ?: ogVideoReversed.find(html)?.groupValues?.get(1)
            ?: jsonVideo.find(html)?.groupValues?.get(1)
            ?: return null
        return unescape(raw).takeIf { it.startsWith("http") }
    }

    /** Undoes JSON and HTML escaping found in embedded URLs. */
    fun unescape(s: String): String = s
        .replace("\\/", "/")
        .replace(Regex("""\\u([0-9a-fA-F]{4})""")) { it.groupValues[1].toInt(16).toChar().toString() }
        .replace("&amp;", "&")
}
