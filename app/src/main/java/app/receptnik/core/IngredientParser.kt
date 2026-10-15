package app.receptnik.core

/**
 * Splits an ingredient line into quantity, unit and name.
 * Handles: "200 г муки", "2-3 ст. л. сахара", "1½ cups flour", "Мука — 200 г",
 * "Яйца 2 шт.", "Соль по вкусу", "200г сыра", "3 כוסות קמח".
 */
object IngredientParser {
    private const val NUM = "\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?"
    private val leadingQty = Regex("^($NUM)(?:\\s*(?:-|–|—|до|to|או)\\s*($NUM))?\\s*")
    private val trailingQty = Regex("^(.+)\\s*(?:[-–—:=]\\s*|\\(\\s*|\\s)(?<![\\d][-–—\\s/])(?<![\\d.,])($NUM)(?:\\s*(?:-|–|—)\\s*($NUM))?\\s*(.*?)\\)?\\.?$")
    private val numberedPrefix = Regex("^\\d{1,2}[.)]\\s+(?=\\D)")
    private val toTaste = Regex("(по вкусу|для подачи|по желанию|to taste|optional|as needed|for serving|לפי הטעם|להגשה)", RegexOption.IGNORE_CASE)

    private val unitRegexes: List<Pair<Regex, MeasureUnit>> = Units.aliasIndex.map { (alias, unit) ->
        val body = alias.split(" ").joinToString("\\.?\\s*") { Regex.escape(it) }
        Regex("^$body\\.?(?![\\p{L}])\\s*", RegexOption.IGNORE_CASE) to unit
    }

    /** If the string starts with a unit, returns the unit and the match length. */
    fun matchUnit(s: String): Pair<MeasureUnit, Int>? {
        for ((re, unit) in unitRegexes) {
            val m = re.find(s) ?: continue
            return unit to m.value.length
        }
        return null
    }

    private fun cleanName(s: String): String = s.trim()
        .removePrefix("of ").removePrefix("של ")
        .trim(' ', ',', '-', '–', '—', ':', ';', '.')
        .replace(Regex("\\s+"), " ")

    private fun extractNote(name: String): Pair<String, String?> {
        val m = toTaste.find(name) ?: return name to null
        val cleaned = cleanName(name.removeRange(m.range))
        return (if (cleaned.isBlank()) name else cleaned) to m.value.lowercase()
    }

    fun parse(raw: String): Ingredient {
        var line = TextUtil.cleanLine(raw)
        line = line.replace(numberedPrefix, "")
        line = TextUtil.expandFractions(line).replace(Regex("\\s+"), " ").trim()
        if (line.isEmpty()) return Ingredient(original = raw.trim(), name = raw.trim())

        // Group header: "Для соуса:", "Тесто:", "For the dough:"
        if (line.endsWith(":") && line.length < 40 && line.none { it.isDigit() }) {
            return Ingredient(original = line.trimEnd(':'), name = line.trimEnd(':'), isHeader = true)
        }

        // 1) Quantity first
        leadingQty.find(line)?.let { m ->
            val q = TextUtil.parseNumber(m.groupValues[1])
            val qMax = m.groupValues[2].takeIf { it.isNotEmpty() }?.let { TextUtil.parseNumber(it) }
            var rest = line.substring(m.value.length)
            var unitKey: String? = null
            matchUnit(rest)?.let { (u, len) ->
                unitKey = u.key
                rest = rest.substring(len)
            }
            // "2 eggs (about 120 g)" is kept as is
            val (name, note) = extractNote(cleanName(rest))
            if (q != null && (name.isNotBlank() || unitKey != null)) {
                return Ingredient(original = line, quantity = q, quantityMax = qMax, unit = unitKey, name = name, note = note)
            }
        }

        // 2) Quantity last: "Flour — 200 g", "Eggs 2 pcs"
        trailingQty.find(line)?.let { m ->
            val namePart = m.groupValues[1]
            val q = TextUtil.parseNumber(m.groupValues[2])
            val qMax = m.groupValues[3].takeIf { it.isNotEmpty() }?.let { TextUtil.parseNumber(it) }
            val tail = m.groupValues[4].trim()
            val unitMatch = if (tail.isEmpty()) null else matchUnit(tail)
            val tailIsUnitOnly = tail.isEmpty() || (unitMatch != null && unitMatch.second >= tail.trimEnd('.', ')').length)
            if (q != null && tailIsUnitOnly && namePart.any { it.isLetter() }) {
                val (name, note) = extractNote(cleanName(namePart))
                return Ingredient(original = line, quantity = q, quantityMax = qMax, unit = unitMatch?.first?.key, name = name, note = note)
            }
        }

        // 3) No quantity
        val (name, note) = extractNote(cleanName(line))
        return Ingredient(original = line, name = name, note = note)
    }

    /** Whether a line looks like an ingredient (used for captions without headers). */
    fun looksLikeIngredient(raw: String): Boolean {
        val line = TextUtil.expandFractions(TextUtil.cleanLine(raw))
        if (line.isEmpty() || line.length > 90) return false
        if (Regex("(порц|serves|servings|минут|minutes|דקות|מנות)", RegexOption.IGNORE_CASE).containsMatchIn(line)) return false
        val ing = parse(raw)
        if (ing.isHeader) return false
        val words = ing.name.split(" ").count { it.isNotBlank() }
        val leading = line.replace(numberedPrefix, "").firstOrNull()?.isDigit() == true
        if (ing.quantity != null) {
            return if (leading) ing.unit != null || words <= 5
            else (ing.unit != null || Regex("[-–—:]").containsMatchIn(line)) && words <= 4
        }
        if (ing.note != null && line.length < 50) return true
        return false
    }
}
