package app.receptnik.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import app.receptnik.core.Ingredient
import app.receptnik.core.Nutrition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** Локальный путь к файлу картинки или URL. */
    val image: String? = null,
    val sourceUrl: String? = null,
    val author: String? = null,
    val servings: Int = 2,
    val totalTimeMin: Int? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    /** КБЖУ на весь рецепт. */
    val nutrition: Nutrition? = null,
    val nutritionNote: String? = null,
    val tags: List<String> = emptyList(),
    val mealType: String? = null,
    val cuisine: String? = null,
    val notes: String = "",
    val favorite: Boolean = false,
    val rating: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastCookedAt: Long? = null,
)

@Serializable
@Entity(tableName = "cookbooks")
data class Cookbook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "📖",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "recipe_cookbook",
    primaryKeys = ["recipeId", "cookbookId"],
    foreignKeys = [
        ForeignKey(entity = Recipe::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Cookbook::class, parentColumns = ["id"], childColumns = ["cookbookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("cookbookId")],
)
data class RecipeCookbook(val recipeId: Long, val cookbookId: Long)

enum class MealSlot {
    BREAKFAST, LUNCH, DINNER, SNACK
}

@Serializable
@Entity(
    tableName = "meal_plan",
    foreignKeys = [ForeignKey(entity = Recipe::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId"), Index("epochDay")],
)
data class MealPlanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val slot: MealSlot,
    val recipeId: Long,
    val servings: Int,
)

@Serializable
@Entity(tableName = "grocery")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val aisle: String,
    val recipeId: Long? = null,
    val recipeTitle: String? = null,
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Строка плана + рецепт (для экрана планирования). */
data class PlannedMeal(val entry: MealPlanEntry, val recipe: Recipe)

/** Сериализация для конвертеров Room. Вынесено отдельно: KSP не видит типы, сгенерированные плагином сериализации. */
internal object JsonCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val ingList: KSerializer<List<Ingredient>> = ListSerializer(Ingredient.serializer())
    private val strList: KSerializer<List<String>> = ListSerializer(String.serializer())
    private val nutrition: KSerializer<Nutrition> = Nutrition.serializer()

    fun encodeIngredients(v: List<Ingredient>): String = json.encodeToString(ingList, v)
    fun decodeIngredients(v: String): List<Ingredient> = runCatching { json.decodeFromString(ingList, v) }.getOrDefault(emptyList())
    fun encodeStrings(v: List<String>): String = json.encodeToString(strList, v)
    fun decodeStrings(v: String): List<String> = runCatching { json.decodeFromString(strList, v) }.getOrDefault(emptyList())
    fun encodeNutrition(v: Nutrition): String = json.encodeToString(nutrition, v)
    fun decodeNutrition(v: String): Nutrition? = runCatching { json.decodeFromString(nutrition, v) }.getOrNull()
}

class Converters {
    @TypeConverter fun ingredientsToJson(v: List<Ingredient>): String = JsonCodec.encodeIngredients(v)
    @TypeConverter fun jsonToIngredients(v: String): List<Ingredient> = JsonCodec.decodeIngredients(v)
    @TypeConverter fun stringsToJson(v: List<String>): String = JsonCodec.encodeStrings(v)
    @TypeConverter fun jsonToStrings(v: String): List<String> = JsonCodec.decodeStrings(v)
    @TypeConverter fun nutritionToJson(v: Nutrition?): String? = v?.let { JsonCodec.encodeNutrition(it) }
    @TypeConverter fun jsonToNutrition(v: String?): Nutrition? = v?.let { JsonCodec.decodeNutrition(it) }
    @TypeConverter fun slotToString(v: MealSlot): String = v.name
    @TypeConverter fun stringToSlot(v: String): MealSlot = runCatching { MealSlot.valueOf(v) }.getOrDefault(MealSlot.DINNER)
}
