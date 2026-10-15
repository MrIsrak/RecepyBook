package app.receptnik.data

import androidx.room.withTransaction
import app.receptnik.core.Aisles
import app.receptnik.core.NutritionDb
import app.receptnik.core.RecipeDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Backup(
    val version: Int = 1,
    val recipes: List<Recipe>,
    val cookbooks: List<Cookbook>,
    val links: List<RecipeCookbook>,
    val plan: List<MealPlanEntry>,
    val grocery: List<GroceryItem>,
    /** Картинки рецептов: id рецепта → JPEG в base64. */
    val images: Map<Long, String> = emptyMap(),
)

class Repository(private val db: AppDatabase, private val images: ImageStore) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    // ---------- Рецепты ----------
    val recipes: Flow<List<Recipe>> = db.recipes().all()
    fun recipe(id: Long): Flow<Recipe?> = db.recipes().byId(id)
    suspend fun getRecipe(id: Long): Recipe? = db.recipes().get(id)

    suspend fun saveDraft(d: RecipeDraft): Long {
        val image = images.persist(d.imageUrl)
        return db.recipes().insert(
            Recipe(
                title = d.title.ifBlank { "Без названия" },
                description = d.description,
                image = image,
                sourceUrl = d.sourceUrl,
                author = d.author,
                servings = d.servings,
                totalTimeMin = d.totalTimeMin,
                ingredients = d.ingredients,
                steps = d.steps,
                nutrition = d.nutrition,
                nutritionNote = d.nutritionNote,
                tags = d.tags,
                mealType = d.mealType,
                cuisine = d.cuisine,
            )
        )
    }

    suspend fun update(r: Recipe) {
        val image = if (r.image != null && !images.isLocal(r.image)) images.persist(r.image) else r.image
        db.recipes().update(r.copy(image = image, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(r: Recipe) {
        db.recipes().delete(r)
        images.delete(r.image)
    }

    suspend fun toggleFavorite(r: Recipe) = db.recipes().update(r.copy(favorite = !r.favorite))
    suspend fun markCooked(r: Recipe) = db.recipes().update(r.copy(lastCookedAt = System.currentTimeMillis()))

    // ---------- Кулинарные книги ----------
    val cookbooks: Flow<List<Cookbook>> = db.cookbooks().all()
    val cookbookLinks: Flow<List<RecipeCookbook>> = db.cookbooks().links()
    fun cookbook(id: Long) = db.cookbooks().byId(id)
    fun recipesIn(cookbookId: Long) = db.cookbooks().recipesIn(cookbookId)

    suspend fun createCookbook(name: String, emoji: String): Long = db.cookbooks().insert(Cookbook(name = name.trim(), emoji = emoji))
    suspend fun updateCookbook(c: Cookbook) = db.cookbooks().update(c)
    suspend fun deleteCookbook(c: Cookbook) = db.cookbooks().delete(c)

    suspend fun setInCookbook(recipeId: Long, cookbookId: Long, inBook: Boolean) {
        val link = RecipeCookbook(recipeId, cookbookId)
        if (inBook) db.cookbooks().link(link) else db.cookbooks().unlink(link)
    }

    // ---------- План питания ----------
    fun plan(fromDay: Long, toDay: Long): Flow<List<PlannedMeal>> =
        db.mealPlan().range(fromDay, toDay).combine(recipes) { entries, recipes ->
            val byId = recipes.associateBy { it.id }
            entries.mapNotNull { e -> byId[e.recipeId]?.let { PlannedMeal(e, it) } }
        }

    suspend fun addToPlan(recipe: Recipe, day: Long, slot: MealSlot, servings: Int = recipe.servings) {
        db.mealPlan().insert(MealPlanEntry(epochDay = day, slot = slot, recipeId = recipe.id, servings = servings))
    }

    suspend fun removeFromPlan(e: MealPlanEntry) = db.mealPlan().delete(e)

    // ---------- Список покупок ----------
    val grocery: Flow<List<GroceryItem>> = db.grocery().all()

    /** Добавляет ингредиенты рецепта; одинаковые продукты с одной единицей складываются. */
    suspend fun addRecipeToGrocery(recipe: Recipe, servings: Int = recipe.servings) {
        val factor = if (recipe.servings > 0) servings.toDouble() / recipe.servings else 1.0
        val existing = db.grocery().allOnce().filter { !it.checked }.toMutableList()
        val toInsert = mutableListOf<GroceryItem>()
        for (ing in recipe.ingredients) {
            if (ing.isHeader) continue
            val name = ing.name.ifBlank { ing.original }.trim().replaceFirstChar { it.uppercaseChar() }
            if (name.isBlank()) continue
            val q = ing.quantity?.let { it * factor }
            val same = (existing + toInsert).firstOrNull {
                it.name.equals(name, ignoreCase = true) && it.unit == ing.unit && (it.quantity == null) == (q == null)
            }
            if (same != null) {
                val merged = same.copy(quantity = q?.let { (same.quantity ?: 0.0) + it })
                if (same.id != 0L) {
                    db.grocery().update(merged)
                    existing[existing.indexOf(same)] = merged
                } else toInsert[toInsert.indexOf(same)] = merged
            } else {
                toInsert += GroceryItem(
                    name = name, quantity = q, unit = ing.unit,
                    aisle = NutritionDb.aisleFor(name),
                    recipeId = recipe.id, recipeTitle = recipe.title,
                )
            }
        }
        if (toInsert.isNotEmpty()) db.grocery().insertAll(toInsert)
    }

    suspend fun addPlanToGrocery(fromDay: Long, toDay: Long): Int {
        val entries = db.mealPlan().rangeOnce(fromDay, toDay)
        for (e in entries) {
            val r = db.recipes().get(e.recipeId) ?: continue
            addRecipeToGrocery(r, e.servings)
        }
        return entries.size
    }

    suspend fun addManualGrocery(text: String) {
        val ing = app.receptnik.core.IngredientParser.parse(text)
        val name = ing.name.ifBlank { text }.trim().replaceFirstChar { it.uppercaseChar() }
        db.grocery().insertAll(
            listOf(GroceryItem(name = name, quantity = ing.quantity, unit = ing.unit, aisle = NutritionDb.aisleFor(name)))
        )
    }

    suspend fun toggleGrocery(item: GroceryItem) = db.grocery().update(item.copy(checked = !item.checked))
    suspend fun deleteGrocery(item: GroceryItem) = db.grocery().delete(item)
    suspend fun clearCheckedGrocery() = db.grocery().clearChecked()
    suspend fun clearAllGrocery() = db.grocery().clearAll()

    // ---------- Резервная копия ----------
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val recipes = db.recipes().allOnce()
        val b = Backup(
            recipes = recipes,
            images = recipes.mapNotNull { r -> images.readBase64(r.image)?.let { r.id to it } }.toMap(),
            cookbooks = db.cookbooks().allOnce(),
            links = db.cookbooks().linksOnce(),
            plan = db.mealPlan().allOnce(),
            grocery = db.grocery().allOnce(),
        )
        json.encodeToString(Backup.serializer(), b)
    }

    /** Восстанавливает всё из копии (текущие данные заменяются). */
    suspend fun importJson(text: String): Int = withContext(Dispatchers.IO) {
        val parsed = json.decodeFromString(Backup.serializer(), text)
        val b = parsed.copy(recipes = parsed.recipes.map { r ->
            val img = parsed.images[r.id]
            if (img != null) r.copy(image = images.writeBase64(img) ?: r.image)
            else if (images.isLocal(r.image) && !images.exists(r.image)) r.copy(image = null) else r
        })
        db.withTransaction {
            val bk = db.backup()
            bk.wipeLinks(); bk.wipePlan(); bk.wipeGrocery(); bk.wipeCookbooks(); bk.wipeRecipes()
            db.recipes().upsertAll(b.recipes)
            db.cookbooks().upsertAll(b.cookbooks)
            db.cookbooks().linkAll(b.links)
            db.mealPlan().upsertAll(b.plan)
            db.grocery().upsertAll(b.grocery)
        }
        b.recipes.size
    }

    companion object {
        val aisleOrder = Aisles.order
    }
}
