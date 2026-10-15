package app.receptnik.core

/** Pure helpers for TikTok URLs and captions (unit-tested). */
object TikTokLinks {
    private val idInPath = Regex("""/(?:video|photo|v)/(\d{8,})""")
    private val idInQuery = Regex("""[?&](?:item_id|share_item_id|aweme_id)=(\d{8,})""")
    private val user = Regex("""tiktok\.com/@([\w.\-]+)""")

    fun videoId(url: String): String? =
        idInPath.find(url)?.groupValues?.get(1) ?: idInQuery.find(url)?.groupValues?.get(1)

    fun username(url: String): String? = user.find(url)?.groupValues?.get(1)

    /** `https://www.tiktok.com/@user/video/ID`; "_" is accepted by TikTok when the user is unknown. */
    fun canonical(url: String): String? {
        val id = videoId(url) ?: return null
        val kind = if (url.contains("/photo/")) "photo" else "video"
        return "https://www.tiktok.com/@${username(url) ?: "_"}/$kind/$id"
    }

    /**
     * True for TikTok's generic site-wide preview texts (served instead of the video's data
     * when TikTok redirects to its home page or shows a bot check).
     */
    fun isGeneric(text: String?): Boolean {
        val t = text?.trim()?.lowercase() ?: return true
        if (t.isEmpty()) return true
        val generic = listOf(
            "make your day", "делает будни ярче", "tiktok - make your day", "tiktok | делает будни ярче",
            "watch more trending videos", "смотрите больше популярных", "discover videos",
            "tiktok is the leading destination", "tiktok — ведущая платформа",
        )
        return generic.any { t == it || t.startsWith(it) || (t.startsWith("tiktok") && t.contains(it)) } ||
            t == "tiktok"
    }
}
