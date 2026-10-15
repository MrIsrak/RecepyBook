package app.receptnik.importer

import android.util.Base64
import app.receptnik.core.IngredientParser
import app.receptnik.core.Nutrition
import app.receptnik.core.RecipeDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.URLEncoder

/**
 * Необязательный «умный» разбор через Google Gemini (у Google есть бесплатный тариф:
 * ключ берётся на aistudio.google.com). Без ключа приложение работает на встроенном парсере.
 */
class GeminiClient(private val apiKey: String, private val model: String, private val translateToRussian: Boolean) {

    @Serializable
    private data class GNutrition(val kcal: Double = 0.0, val protein: Double = 0.0, val fat: Double = 0.0, val carbs: Double = 0.0)

    @Serializable
    private data class GRecipe(
        val isRecipe: Boolean = true,
        val title: String = "",
        val description: String = "",
        val servings: Int? = null,
        val totalTimeMin: Int? = null,
        val ingredients: List<String> = emptyList(),
        val steps: List<String> = emptyList(),
        val nutritionPerServing: GNutrition? = null,
        val cuisine: String? = null,
        val mealType: String? = null,
        val tags: List<String> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun prompt(): String = buildString {
        append("Ты помощник кулинарного приложения. Извлеки рецепт из присланного материала ")
        append("(подпись к посту из соцсети, описание видео, текст страницы или фото рецепта). ")
        if (translateToRussian) append("Пиши всё на русском языке (переведи, если рецепт на другом языке). ")
        else append("Сохраняй язык оригинала. ")
        append("Верни ТОЛЬКО JSON такого вида: ")
        append("{\"isRecipe\":true,\"title\":\"\",\"description\":\"1-2 предложения\",\"servings\":2,\"totalTimeMin\":30,")
        append("\"ingredients\":[\"200 г муки\",\"2 шт. яйца\",\"соль по вкусу\"],")
        append("\"steps\":[\"Шаг 1...\",\"Шаг 2...\"],")
        append("\"nutritionPerServing\":{\"kcal\":0,\"protein\":0,\"fat\":0,\"carbs\":0},")
        append("\"cuisine\":\"итальянская\",\"mealType\":\"завтрак|обед|ужин|перекус|десерт|напиток\",\"tags\":[\"быстро\"]}. ")
        append("Каждый ингредиент — строкой в формате «количество единица продукт» с метрическими единицами где возможно. ")
        append("Если количества не указаны — оцени разумные. Шаги — короткие и понятные. ")
        append("Калорийность и БЖУ на одну порцию — оцени по ингредиентам. ")
        append("Если это не рецепт — верни {\"isRecipe\":false}.")
    }

    suspend fun extract(text: String?, imageBytes: ByteArray? = null, imageMime: String = "image/jpeg"): RecipeDraft? {
        val body = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt()) })
                        if (!text.isNullOrBlank()) add(buildJsonObject { put("text", text.take(15000)) })
                        if (imageBytes != null) add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", imageMime)
                                put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                            }
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            }
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            URLEncoder.encode(model, "UTF-8") + ":generateContent?key=" + URLEncoder.encode(apiKey, "UTF-8")
        val (code, resp) = Http.postJson(url, body.toString())
        if (code != 200) {
            val msg = runCatching {
                json.parseToJsonElement(resp).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
            }.getOrNull()
            throw IOException("Gemini: ${msg ?: "ошибка $code"}")
        }
        val root = json.parseToJsonElement(resp).jsonObject
        val textOut = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.joinToString("") { (it as? JsonObject)?.get("text")?.jsonPrimitive?.content ?: "" }
            ?: return null
        val cleaned = textOut.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val g = json.decodeFromString(GRecipe.serializer(), cleaned)
        if (!g.isRecipe || g.ingredients.isEmpty()) return null
        val servings = g.servings?.takeIf { it in 1..100 } ?: 2
        return RecipeDraft(
            title = g.title,
            description = g.description,
            servings = servings,
            totalTimeMin = g.totalTimeMin?.takeIf { it in 1..3000 },
            ingredients = g.ingredients.map { IngredientParser.parse(it) },
            steps = g.steps.filter { it.isNotBlank() },
            nutrition = g.nutritionPerServing?.takeIf { it.kcal > 0 }?.let {
                Nutrition(it.kcal, it.protein, it.fat, it.carbs) * servings.toDouble()
            },
            nutritionNote = "Оценка ИИ",
            tags = g.tags.take(8),
            cuisine = g.cuisine,
            mealType = g.mealType?.substringBefore('|'),
        )
    }
}
