package app.receptnik.core

import kotlinx.serialization.Serializable

/** A single ingredient. [original] is the source line; the other fields are its parsed parts. */
@Serializable
data class Ingredient(
    val original: String,
    val quantity: Double? = null,
    val quantityMax: Double? = null,
    val unit: String? = null,
    val name: String = original,
    val note: String? = null,
    /** Group header ("For the sauce"); such a line is not an ingredient. */
    val isHeader: Boolean = false,
)

/** Nutrition facts for the whole recipe. */
@Serializable
data class Nutrition(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
) {
    operator fun plus(o: Nutrition) = Nutrition(kcal + o.kcal, protein + o.protein, fat + o.fat, carbs + o.carbs)
    operator fun times(k: Double) = Nutrition(kcal * k, protein * k, fat * k, carbs * k)
    operator fun div(k: Double) = if (k == 0.0) this else times(1.0 / k)
}

/** Recipe draft: the import result before it is saved to the database. */
data class RecipeDraft(
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val author: String? = null,
    val servings: Int = 2,
    val totalTimeMin: Int? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    val nutritionNote: String? = null,
    val tags: List<String> = emptyList(),
    val mealType: String? = null,
    val cuisine: String? = null,
)
