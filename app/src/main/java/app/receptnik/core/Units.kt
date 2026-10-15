package app.receptnik.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class UnitKind { MASS, VOLUME, COUNT }

/**
 * Единица измерения. [factor] — граммы (MASS) или миллилитры (VOLUME) в одной единице.
 */
data class MeasureUnit(
    val key: String,
    val kind: UnitKind,
    val factor: Double,
    val display: String,
    val aliases: List<String>,
)

enum class UnitSystem { ORIGINAL, METRIC, US }

object Units {
    val all: List<MeasureUnit> = listOf(
        MeasureUnit("mg", UnitKind.MASS, 0.001, "мг", listOf("мг", "mg", "מ\"ג", "מג")),
        MeasureUnit("g", UnitKind.MASS, 1.0, "г", listOf("г", "гр", "грамм", "грамма", "граммов", "g", "gr", "gram", "grams", "gramm", "ג", "גרם", "גר", "ג'")),
        MeasureUnit("kg", UnitKind.MASS, 1000.0, "кг", listOf("кг", "килограмм", "килограмма", "kg", "kilo", "kilogram", "kilograms", "ק\"ג", "קג", "קילו")),
        MeasureUnit("oz", UnitKind.MASS, 28.3495, "унц.", listOf("унц", "унция", "унции", "oz", "ounce", "ounces")),
        MeasureUnit("lb", UnitKind.MASS, 453.592, "фунт", listOf("фунт", "фунта", "фунтов", "lb", "lbs", "pound", "pounds")),

        MeasureUnit("ml", UnitKind.VOLUME, 1.0, "мл", listOf("мл", "миллилитр", "миллилитров", "ml", "milliliter", "milliliters", "millilitre", "מ\"ל", "מל")),
        MeasureUnit("l", UnitKind.VOLUME, 1000.0, "л", listOf("л", "литр", "литра", "литров", "l", "liter", "liters", "litre", "litres", "ליטר")),
        MeasureUnit("tsp", UnitKind.VOLUME, 5.0, "ч. л.", listOf("ч.л", "ч. л", "чл", "ч/л", "чайная ложка", "чайной ложки", "чайные ложки", "чайных ложек", "чайн. л", "tsp", "teaspoon", "teaspoons", "כפית", "כפיות")),
        MeasureUnit("tbsp", UnitKind.VOLUME, 15.0, "ст. л.", listOf("ст.л", "ст. л", "стл", "ст/л", "столовая ложка", "столовой ложки", "столовые ложки", "столовых ложек", "tbsp", "tbs", "tablespoon", "tablespoons", "כף", "כפות")),
        MeasureUnit("glass", UnitKind.VOLUME, 200.0, "стак.", listOf("стакан", "стакана", "стаканов", "стак", "ст", "כוס", "כוסות")),
        MeasureUnit("cup", UnitKind.VOLUME, 240.0, "cup", listOf("cup", "cups", "c")),
        MeasureUnit("floz", UnitKind.VOLUME, 29.5735, "fl oz", listOf("fl oz", "fl. oz", "floz", "fluid ounce", "fluid ounces")),
        MeasureUnit("pinch", UnitKind.VOLUME, 0.3, "щеп.", listOf("щепотка", "щепотки", "щепоть", "щеп", "pinch", "pinches", "קורט")),

        MeasureUnit("piece", UnitKind.COUNT, 1.0, "шт.", listOf("шт", "штук", "штуки", "штука", "pc", "pcs", "piece", "pieces", "יח'", "יחידות", "יחידה")),
        MeasureUnit("clove", UnitKind.COUNT, 1.0, "зуб.", listOf("зубчик", "зубчика", "зубчиков", "зуб", "clove", "cloves", "שן", "שיניים")),
        MeasureUnit("can", UnitKind.COUNT, 1.0, "банка", listOf("банка", "банки", "банок", "can", "cans", "tin", "tins", "קופסה", "קופסת", "פחית")),
        MeasureUnit("pack", UnitKind.COUNT, 1.0, "уп.", listOf("упаковка", "упаковки", "упак", "уп", "пачка", "пачки", "пачек", "pack", "package", "packages", "packet", "חבילה", "חבילת")),
        MeasureUnit("slice", UnitKind.COUNT, 1.0, "ломт.", listOf("ломтик", "ломтика", "ломтиков", "ломт", "slice", "slices", "פרוסה", "פרוסות")),
        MeasureUnit("bunch", UnitKind.COUNT, 1.0, "пучок", listOf("пучок", "пучка", "пучков", "bunch", "bunches", "צרור")),
        MeasureUnit("handful", UnitKind.COUNT, 1.0, "горсть", listOf("горсть", "горсти", "handful", "handfuls", "חופן")),
        MeasureUnit("sprig", UnitKind.COUNT, 1.0, "веточка", listOf("веточка", "веточки", "веточек", "sprig", "sprigs", "ענף")),
    )

    private val byKey = all.associateBy { it.key }
    fun byKey(key: String?): MeasureUnit? = key?.let { byKey[it] }

    /** Псевдонимы, отсортированные от длинных к коротким, нормализованные (без точек). */
    val aliasIndex: List<Pair<String, MeasureUnit>> = all
        .flatMap { u -> u.aliases.map { normalizeAlias(it) to u } }
        .sortedByDescending { it.first.length }

    fun normalizeAlias(s: String): String =
        s.lowercase().replace(".", "").replace(Regex("\\s+"), " ").trim()

    private val labels: Map<String, Triple<String, String, String>> = mapOf(
        "mg" to Triple("мг", "mg", "מ\"ג"),
        "g" to Triple("г", "g", "גר׳"),
        "kg" to Triple("кг", "kg", "ק\"ג"),
        "oz" to Triple("унц.", "oz", "אונ׳"),
        "lb" to Triple("фунт", "lb", "ליברה"),
        "ml" to Triple("мл", "ml", "מ\"ל"),
        "l" to Triple("л", "l", "ליטר"),
        "tsp" to Triple("ч. л.", "tsp", "כפית"),
        "tbsp" to Triple("ст. л.", "tbsp", "כף"),
        "glass" to Triple("стак.", "glass", "כוס"),
        "cup" to Triple("чашк.", "cup", "כוס"),
        "floz" to Triple("fl oz", "fl oz", "fl oz"),
        "pinch" to Triple("щеп.", "pinch", "קורט"),
        "piece" to Triple("шт.", "pc", "יח׳"),
        "clove" to Triple("зуб.", "clove", "שן"),
        "can" to Triple("банка", "can", "קופסה"),
        "pack" to Triple("уп.", "pack", "חבילה"),
        "slice" to Triple("ломт.", "slice", "פרוסה"),
        "bunch" to Triple("пучок", "bunch", "צרור"),
        "handful" to Triple("горсть", "handful", "חופן"),
        "sprig" to Triple("веточка", "sprig", "ענף"),
    )

    /** Подпись единицы на языке интерфейса: lang = "ru" | "en" | "he". */
    fun display(key: String?, lang: String = "ru"): String {
        val t = key?.let { labels[it] } ?: return key ?: ""
        return when (Lang.of(lang)) { "en" -> t.second; "he" -> t.third; else -> t.first }
    }

    /** Пересчёт количества в выбранную систему единиц. */
    fun convert(quantity: Double?, unitKey: String?, system: UnitSystem): Pair<Double?, String?> {
        val unit = byKey(unitKey) ?: return quantity to unitKey
        if (quantity == null || system == UnitSystem.ORIGINAL) return quantity to unitKey
        return when (system) {
            UnitSystem.METRIC -> when (unit.kind) {
                UnitKind.MASS -> {
                    val g = quantity * unit.factor
                    if (g >= 1000) g / 1000 to "kg" else g to "g"
                }
                UnitKind.VOLUME -> {
                    // ложки и щепотки — универсальные, их оставляем
                    if (unit.key in setOf("tsp", "tbsp", "pinch")) quantity to unit.key
                    else {
                        val ml = quantity * unit.factor
                        if (ml >= 1000) ml / 1000 to "l" else ml to "ml"
                    }
                }
                UnitKind.COUNT -> quantity to unit.key
            }
            UnitSystem.US -> when (unit.kind) {
                UnitKind.MASS -> {
                    val g = quantity * unit.factor
                    if (g >= 453.592) g / 453.592 to "lb" else g / 28.3495 to "oz"
                }
                UnitKind.VOLUME -> {
                    if (unit.key in setOf("tsp", "tbsp", "pinch")) quantity to unit.key
                    else {
                        val ml = quantity * unit.factor
                        when {
                            ml >= 60 -> ml / 240.0 to "cup"
                            ml >= 15 -> ml / 15.0 to "tbsp"
                            else -> ml / 5.0 to "tsp"
                        }
                    }
                }
                UnitKind.COUNT -> quantity to unit.key
            }
            UnitSystem.ORIGINAL -> quantity to unitKey
        }
    }
}

object QtyFormat {
    private val nice = listOf(
        0.125 to "⅛", 0.25 to "¼", 1.0 / 3 to "⅓", 0.5 to "½", 2.0 / 3 to "⅔", 0.75 to "¾",
    )

    /** 1.5 → "1½", 0.333 → "⅓", 212.7 → "213", 2.25 → "2¼", 1.15 → "1,15". */
    fun format(q: Double, lang: String = "ru"): String {
        if (q <= 0) return "0"
        if (q >= 20) return q.roundToInt().toString()
        val whole = floor(q)
        val frac = q - whole
        if (frac < 0.03) return whole.toInt().toString()
        if (frac > 0.97) return (whole + 1).toInt().toString()
        for ((v, s) in nice) {
            if (abs(frac - v) < 0.015) return if (whole > 0) "${whole.toInt()}$s" else s
        }
        val r = (q * 10).roundToInt() / 10.0
        val str = if (r == floor(r)) r.toInt().toString() else r.toString()
        return if (Lang.of(lang) == "ru") str.replace('.', ',') else str
    }

    /** Количество с единицей: «2–3 ст. л.», «400 г», "" если количества нет. */
    fun formatAmount(ing: Ingredient, factor: Double, system: UnitSystem, lang: String = "ru"): String {
        if (ing.isHeader || ing.quantity == null) return ing.note ?: ""
        val (q, u) = Units.convert(ing.quantity * factor, ing.unit, system)
        val (qMax, _) = Units.convert(ing.quantityMax?.let { it * factor }, ing.unit, system)
        val qs = buildString {
            append(format(q ?: 0.0, lang))
            if (qMax != null) append("–").append(format(qMax, lang))
        }
        return listOf(qs, Units.display(u, lang)).filter { it.isNotBlank() }.joinToString(" ")
    }

    fun displayName(ing: Ingredient): String =
        ing.name.ifBlank { ing.original }.replaceFirstChar { it.uppercaseChar() }

    /** «Творог — 400 г» — для списков и «Поделиться». */
    fun formatIngredient(ing: Ingredient, factor: Double, system: UnitSystem, lang: String = "ru"): String {
        if (ing.isHeader) return ing.original
        val amount = formatAmount(ing, factor, system, lang)
        return if (amount.isBlank()) displayName(ing) else "${displayName(ing)} — $amount"
    }
}

/** Нормализация кода языка: iw → he, всё неизвестное → en. */
object Lang {
    fun of(code: String?): String = when (code?.lowercase()?.take(2)) {
        "ru", "uk", "be" -> "ru"
        "he", "iw" -> "he"
        else -> "en"
    }
}
