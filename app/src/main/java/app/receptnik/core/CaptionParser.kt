package app.receptnik.core

/**
 * Turns free text (an Instagram/TikTok caption, a YouTube description,
 * OCR output) into a recipe: title, ingredients, steps, servings and time.
 */
object CaptionParser {
    private val ingredientHeaders = listOf(
        "ингредиенты", "ингридиенты", "ингредиенты на", "состав", "продукты", "вам понадобится", "нам понадобится",
        "понадобится", "что нужно", "нужно", "для теста", "ingredients", "ingredient", "you'll need", "you will need",
        "what you need", "shopping list", "מצרכים", "מרכיבים", "רכיבים", "חומרים",
    )
    private val stepHeaders = listOf(
        "приготовление", "способ приготовления", "как приготовить", "как готовить", "рецепт приготовления",
        "инструкция", "готовим", "пошагово", "шаги", "процесс", "method", "instructions", "directions",
        "steps", "preparation", "how to make", "how to", "to make", "אופן הכנה", "אופן ההכנה", "הוראות הכנה", "הכנה",
    )
    private val stopMarkers = listOf(
        "сохраняй", "сохрани", "подписывайся", "подпишись", "ставь лайк", "приятного аппетита", "пишите в комментариях",
        "save this", "save for later", "follow for", "follow me", "like and", "comment ", "link in bio",
        "בתיאבון", "שמרו", "תעקבו",
    )
    private val servingsRe = Regex("(\\d+)\\s*(?:-\\s*\\d+\\s*)?(?:порц|персон|человек|serving|servings|portion|people|מנות|סועדים)", RegexOption.IGNORE_CASE)
    private val servingsRe2 = Regex("(?:на|serves|for|ל-?)\\s*(\\d+)\\s*(?:порц|персон|человек|people|servings|מנות)?", RegexOption.IGNORE_CASE)
    private val timeRe = Regex("(\\d+)\\s*(?:-\\s*\\d+\\s*)?(мин|min|minutes|דקות|ч(?:ас|\\.)?|h(?:our|r)s?|שעות|שעה)", RegexOption.IGNORE_CASE)
    private val numberedStep = Regex("^(?:шаг\\s*|step\\s*)?(\\d{1,2})\\s*[.)\\-:]\\s*(.+)$", RegexOption.IGNORE_CASE)

    private fun norm(line: String): String =
        TextUtil.cleanLine(line).lowercase().trim(' ', ':', '-', '—', '.', '!', '*', '_', '|')

    private fun headerType(line: String): Pair<Char, String>? {
        val clean = TextUtil.cleanLine(line)
        val lower = clean.lowercase()
        for (h in ingredientHeaders + stepHeaders) {
            if (lower.startsWith(h)) {
                val after = clean.substring(h.length)
                // "Ingredients for 4 servings:" is a header too
                val rest = after.trimStart()
                val isHeader = rest.isEmpty() || rest.startsWith(":") || rest.startsWith("(") ||
                    rest.startsWith("-") || rest.startsWith("—") ||
                    (clean.length <= 40 && clean.endsWith(":"))
                if (!isHeader) continue
                val type = if (h in ingredientHeaders) 'I' else 'S'
                val inline = rest.substringAfter(":", "").trim()
                return type to inline
            }
        }
        return null
    }

    private fun isStop(line: String): Boolean {
        val l = norm(line)
        if (l.isEmpty()) return false
        if (line.trim().startsWith("#")) return true
        return stopMarkers.any { l.startsWith(it) }
    }

    /** Splits a long paragraph into sentence-sized steps. */
    private fun splitSteps(text: String): List<String> {
        val t = text.trim()
        val parts = t.split(Regex("(?<=[.!?])\\s+(?=[\\p{Lu}\\u0590-\\u05FF\\d])")).filterNot { isStop(it) }
        if (t.length < 100) return listOf(parts.joinToString(" ")).filter { it.isNotBlank() }
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (p in parts) {
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(p)
            if (sb.length > 60) { out += sb.toString(); sb.clear() }
        }
        if (sb.isNotEmpty()) out += sb.toString()
        return out
    }

    private fun splitInlineIngredients(s: String): List<String> =
        s.split(Regex("\\s*[,;]\\s*(?![0-9])|\\s*[,;]\\s*(?=[0-9]+\\s*\\p{L})")).map { it.trim() }.filter { it.isNotEmpty() }

    fun parse(rawText: String, fallbackTitle: String? = null): RecipeDraft {
        var text = TextUtil.keycapsToNumbers(rawText).replace("\r", "")
        // Move headers found mid-line ("...in 15 minutes! Ingredients: ...") onto their own line
        for (h in (ingredientHeaders + stepHeaders).sortedByDescending { it.length }) {
            text = text.replace(Regex("(?<=[.!?…)])[ \\t]+(" + Regex.escape(h) + ")\\s*:", RegexOption.IGNORE_CASE), "\n$1:")
        }
        val lines = text.split("\n").map { it.trimEnd() }

        val ingredients = mutableListOf<String>()
        val steps = mutableListOf<String>()
        val intro = mutableListOf<String>()
        var mode = '0' // 0 = intro, I = ingredients, S = steps, X = trailing hashtags
        var sawHeaders = false

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (mode != '0' && isStop(line)) { mode = 'X'; continue }
            if (mode == 'X') continue
            val h = headerType(line)
            if (h != null) {
                sawHeaders = true
                mode = h.first
                if (h.second.isNotBlank()) {
                    if (mode == 'I') ingredients += splitInlineIngredients(h.second) else steps += splitSteps(h.second)
                }
                continue
            }
            when (mode) {
                'I' -> {
                    val c = TextUtil.cleanLine(line)
                    // long numbered lines after the ingredients are steps
                    if (numberedStep.matches(c) && c.length > 45 && !IngredientParser.looksLikeIngredient(c)) {
                        mode = 'S'; steps += cleanStep(c)
                    } else if (c.contains(',') && c.length > 60 && !c.contains('.') && ingredients.isEmpty()) {
                        ingredients += splitInlineIngredients(c)
                    } else ingredients += c
                }
                'S' -> steps += splitSteps(cleanStep(TextUtil.cleanLine(line)))
                else -> intro += line
            }
        }

        // No section headers: fall back to heuristics
        if (!sawHeaders) {
            intro.clear()
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (isStop(line) && (ingredients.isNotEmpty() || steps.isNotEmpty())) break
                if (line.startsWith("#")) continue
                val c = TextUtil.cleanLine(line)
                when {
                    IngredientParser.looksLikeIngredient(line) && steps.isEmpty() -> ingredients += c
                    numberedStep.matches(c) || (ingredients.isNotEmpty() && c.length > 25) -> steps += splitSteps(cleanStep(c))
                    else -> intro += line
                }
            }
        }

        val introClean = intro.map { TextUtil.stripHashtags(TextUtil.stripEmoji(it)).trim() }.filter { it.isNotBlank() }
        val title = introClean.firstOrNull()?.let { cleanTitle(it) }?.takeIf { it.isNotBlank() }
            ?: fallbackTitle?.let { cleanTitle(it) } ?: ""
        val description = introClean.drop(1).joinToString("\n").take(500)

        val servings = servingsRe.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: servingsRe2.find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..20 }
        val time = timeRe.find((listOf(fallbackTitle ?: "") + intro).joinToString("\n"))?.let {
            val n = it.groupValues[1].toIntOrNull() ?: 0
            val u = it.groupValues[2].lowercase()
            if (u.startsWith("ч") || u.startsWith("h") || u.startsWith("שע")) n * 60 else n
        }

        return RecipeDraft(
            title = title,
            description = description,
            servings = servings?.coerceIn(1, 50) ?: 2,
            totalTimeMin = time?.takeIf { it in 1..2000 },
            ingredients = ingredients.map { IngredientParser.parse(it) }.filter { it.name.isNotBlank() || it.isHeader },
            steps = steps.map { it.trim() }.filter { it.length > 2 },
        )
    }

    private fun cleanStep(s: String): String {
        val m = numberedStep.find(s)
        return (m?.groupValues?.get(2) ?: s).trim()
    }

    fun cleanTitle(s: String): String {
        var t = TextUtil.stripHashtags(TextUtil.stripEmoji(s)).replace(Regex("\\s+"), " ").trim()
        t = t.trim(' ', '"', '«', '»', '-', '—', ':', '!', '.', '|')
        // If the first line is a whole paragraph, keep the first sentence
        if (t.length > 70) t = t.split(Regex("(?<=[.!?])\\s")).first().take(70).trim()
        return t
    }
}
