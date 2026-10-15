package app.receptnik.importer

import app.receptnik.core.IngredientParser
import app.receptnik.core.Nutrition
import app.receptnik.core.RecipeDraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Разбор разметки schema.org/Recipe (её используют почти все сайты рецептов). */
object JsonLd {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    fun findRecipe(doc: Document): JsonObject? {
        for (script in doc.select("script[type=application/ld+json]")) {
            val el = runCatching { json.parseToJsonElement(script.data().trim()) }.getOrNull() ?: continue
            find(el)?.let { return it }
        }
        return null
    }

    private fun isRecipe(o: JsonObject): Boolean = when (val t = o["@type"]) {
        is JsonPrimitive -> t.contentOrNull.equals("Recipe", ignoreCase = true)
        is JsonArray -> t.any { (it as? JsonPrimitive)?.contentOrNull.equals("Recipe", ignoreCase = true) }
        else -> false
    }

    private fun find(el: JsonElement): JsonObject? = when (el) {
        is JsonObject -> when {
            isRecipe(el) -> el
            else -> el["@graph"]?.let { find(it) } ?: el["mainEntity"]?.let { find(it) }
        }
        is JsonArray -> el.firstNotNullOfOrNull { find(it) }
        else -> null
    }

    private fun str(el: JsonElement?): String? = when (el) {
        null, JsonNull -> null
        is JsonPrimitive -> el.contentOrNull?.let { unescape(it) }
        is JsonArray -> el.firstNotNullOfOrNull { str(it) }
        is JsonObject -> str(el["name"]) ?: str(el["text"]) ?: str(el["url"]) ?: str(el["@id"])
    }

    private fun unescape(s: String): String = if (s.contains('&') || s.contains('<')) Jsoup.parse(s).text() else s.trim()

    private fun image(el: JsonElement?): String? = when (el) {
        is JsonPrimitive -> el.contentOrNull
        is JsonArray -> el.firstNotNullOfOrNull { image(it) }
        is JsonObject -> str(el["url"]) ?: str(el["contentUrl"])
        else -> null
    }

    private fun list(el: JsonElement?): List<String> = when (el) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> el.contentOrNull?.split("\n")?.map { unescape(it) }?.filter { it.isNotBlank() } ?: emptyList()
        is JsonArray -> el.flatMap { list(it) }
        is JsonObject -> listOfNotNull(str(el))
    }

    private fun instructions(el: JsonElement?): List<String> = when (el) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> list(el).flatMap { line ->
            if (line.length > 300) line.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() } else listOf(line)
        }
        is JsonArray -> el.flatMap { instructions(it) }
        is JsonObject -> {
            val items = el["itemListElement"]
            if (items != null) {
                val name = str(el["name"])
                (if (name != null) listOf("$name:") else emptyList()) + instructions(items)
            } else listOfNotNull(str(el["text"]) ?: str(el["name"]))
        }
    }

    /** «PT1H30M» → 90. */
    fun isoMinutes(s: String?): Int? {
        if (s.isNullOrBlank()) return null
        val m = Regex("P(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?").find(s.uppercase()) ?: return null
        val d = m.groupValues[1].toIntOrNull() ?: 0
        val h = m.groupValues[2].toIntOrNull() ?: 0
        val mi = m.groupValues[3].toIntOrNull() ?: 0
        val total = d * 1440 + h * 60 + mi
        return total.takeIf { it > 0 }
    }

    private fun number(s: String?): Double? =
        s?.let { Regex("\\d+(?:[.,]\\d+)?").find(it)?.value?.replace(',', '.')?.toDoubleOrNull() }

    fun toDraft(o: JsonObject, sourceUrl: String): RecipeDraft {
        val servings = number(str(o["recipeYield"]))?.toInt()?.takeIf { it in 1..100 } ?: 2
        val total = isoMinutes(str(o["totalTime"]))
            ?: listOfNotNull(isoMinutes(str(o["prepTime"])), isoMinutes(str(o["cookTime"]))).sum().takeIf { it > 0 }
        val ingredientsRaw = list(o["recipeIngredient"]).ifEmpty { list(o["ingredients"]) }
        val nutrition = (o["nutrition"] as? JsonObject)?.let { n ->
            val kcal = number(str(n["calories"])) ?: return@let null
            Nutrition(
                kcal = kcal,
                protein = number(str(n["proteinContent"])) ?: 0.0,
                fat = number(str(n["fatContent"])) ?: 0.0,
                carbs = number(str(n["carbohydrateContent"])) ?: 0.0,
            ) * servings.toDouble() // на сайтах — на порцию
        }
        val keywords = str(o["keywords"])?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }?.take(8) ?: emptyList()
        return RecipeDraft(
            title = str(o["name"]) ?: "",
            description = str(o["description"])?.take(600) ?: "",
            imageUrl = image(o["image"]),
            sourceUrl = sourceUrl,
            author = str(o["author"]),
            servings = servings,
            totalTimeMin = total,
            ingredients = ingredientsRaw.map { IngredientParser.parse(it) },
            steps = instructions(o["recipeInstructions"]),
            nutrition = nutrition,
            nutritionNote = if (nutrition != null) NutritionNote.SITE else null,
            tags = keywords,
            cuisine = str(o["recipeCuisine"]),
            mealType = str(o["recipeCategory"]),
        )
    }
}
