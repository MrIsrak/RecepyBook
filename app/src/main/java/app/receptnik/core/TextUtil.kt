package app.receptnik.core

object TextUtil {
    private val fractionMap = mapOf(
        '½' to "1/2", '⅓' to "1/3", '⅔' to "2/3", '¼' to "1/4", '¾' to "3/4",
        '⅕' to "1/5", '⅖' to "2/5", '⅗' to "3/5", '⅘' to "4/5", '⅙' to "1/6",
        '⅚' to "5/6", '⅛' to "1/8", '⅜' to "3/8", '⅝' to "5/8", '⅞' to "7/8",
    )

    /** Expands "1½" to "1 1/2" and "½" to "1/2". */
    fun expandFractions(s: String): String {
        if (s.none { it in fractionMap }) return s
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            val f = fractionMap[c]
            if (f != null) {
                if (i > 0 && s[i - 1].isDigit()) sb.append(' ')
                sb.append(f)
            } else sb.append(c)
        }
        return sb.toString()
    }

    /** Converts "1️⃣ Mix" to "1. Mix". */
    fun keycapsToNumbers(s: String): String =
        s.replace(Regex("([0-9])\\uFE0F?\\u20E3"), "$1. ")
            .replace("🔟", "10. ") // 🔟

    private fun isEmojiCodePoint(cp: Int): Boolean =
        cp in 0x1F000..0x1FAFF || cp in 0x2600..0x27BF || cp in 0x2B00..0x2BFF ||
            cp in 0x1F900..0x1F9FF || cp == 0xFE0F || cp == 0x200D || cp == 0x20E3 ||
            cp in 0x2190..0x21FF || cp in 0x2300..0x23FF || cp in 0xE0020..0xE007F ||
            cp == 0x2122 || cp == 0x00A9 || cp == 0x00AE

    fun stripEmoji(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            if (!isEmojiCodePoint(cp)) sb.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return sb.toString()
    }

    /** Cleans a line: strips emoji, extra whitespace and leading list markers. */
    fun cleanLine(raw: String): String {
        var s = keycapsToNumbers(raw)
        s = stripEmoji(s).replace(' ', ' ').replace(Regex("[\\t ]+"), " ").trim()
        s = s.replace(Regex("^[•·▪▫◦●○■□►▶➤➡→✓✔✅☑*\\-–—+~>|]+\\s*"), "")
        return s.trim()
    }

    fun firstUrl(text: String): String? =
        Regex("https?://[^\\s<>\"']+").find(text)?.value?.trimEnd('.', ',', ')', '!', '?')

    /** Removes hashtags from a caption. */
    fun stripHashtags(s: String): String =
        s.replace(Regex("(^|\\s)#[\\p{L}\\p{N}_]+"), " ").replace(Regex("[ \\t]+"), " ").trim()

    fun parseNumber(s: String): Double? {
        val t = s.trim().replace(',', '.')
        val mixed = Regex("^(\\d+)\\s+(\\d+)/(\\d+)$").find(t)
        if (mixed != null) {
            val (w, n, d) = mixed.destructured
            val dd = d.toDouble()
            return if (dd == 0.0) null else w.toDouble() + n.toDouble() / dd
        }
        val frac = Regex("^(\\d+)/(\\d+)$").find(t)
        if (frac != null) {
            val (n, d) = frac.destructured
            val dd = d.toDouble()
            return if (dd == 0.0) null else n.toDouble() / dd
        }
        return t.toDoubleOrNull()
    }

    /**
     * Removes an author prefix that link previews add before the caption, such as
     * "@chef_name - …" or "Chef Name (@chef_name): …".
     */
    fun stripAuthorPrefix(s: String): String = s.trim()
        .replace(Regex("""^@[\w.]+\s*[-–—:]\s*"""), "")
        .replace(Regex("""^[^\n]{0,60}\(@[\w.]+\)\s*[:\-–—]\s*"""), "")
        .trim()
}
