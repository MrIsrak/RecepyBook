package app.receptnik.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun all(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun byId(id: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: Long): Recipe?

    @Query("SELECT * FROM recipes")
    suspend fun allOnce(): List<Recipe>

    @Insert
    suspend fun insert(r: Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(r: List<Recipe>)

    @Update
    suspend fun update(r: Recipe)

    @Delete
    suspend fun delete(r: Recipe)
}

@Dao
interface CookbookDao {
    @Query("SELECT * FROM cookbooks ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Cookbook>>

    @Query("SELECT * FROM cookbooks")
    suspend fun allOnce(): List<Cookbook>

    @Query("SELECT * FROM cookbooks WHERE id = :id")
    fun byId(id: Long): Flow<Cookbook?>

    @Insert
    suspend fun insert(c: Cookbook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(c: List<Cookbook>)

    @Update
    suspend fun update(c: Cookbook)

    @Delete
    suspend fun delete(c: Cookbook)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(x: RecipeCookbook)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkAll(x: List<RecipeCookbook>)

    @Delete
    suspend fun unlink(x: RecipeCookbook)

    @Query("SELECT * FROM recipe_cookbook")
    fun links(): Flow<List<RecipeCookbook>>

    @Query("SELECT * FROM recipe_cookbook")
    suspend fun linksOnce(): List<RecipeCookbook>

    @Query("SELECT r.* FROM recipes r JOIN recipe_cookbook x ON x.recipeId = r.id WHERE x.cookbookId = :cookbookId ORDER BY r.title COLLATE NOCASE")
    fun recipesIn(cookbookId: Long): Flow<List<Recipe>>
}

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, slot")
    fun range(from: Long, to: Long): Flow<List<MealPlanEntry>>

    @Query("SELECT * FROM meal_plan WHERE epochDay BETWEEN :from AND :to")
    suspend fun rangeOnce(from: Long, to: Long): List<MealPlanEntry>

    @Query("SELECT * FROM meal_plan")
    suspend fun allOnce(): List<MealPlanEntry>

    @Insert
    suspend fun insert(e: MealPlanEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(e: List<MealPlanEntry>)

    @Delete
    suspend fun delete(e: MealPlanEntry)
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery ORDER BY checked, createdAt")
    fun all(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery")
    suspend fun allOnce(): List<GroceryItem>

    @Insert
    suspend fun insertAll(items: List<GroceryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GroceryItem>)

    @Update
    suspend fun update(item: GroceryItem)

    @Delete
    suspend fun delete(item: GroceryItem)

    @Query("DELETE FROM grocery WHERE checked = 1")
    suspend fun clearChecked()

    @Query("DELETE FROM grocery")
    suspend fun clearAll()

    @Query("DELETE FROM grocery WHERE recipeId = :recipeId")
    suspend fun removeRecipe(recipeId: Long)
}

@Dao
interface BackupDao {
    @Query("DELETE FROM recipes") suspend fun wipeRecipes()
    @Query("DELETE FROM cookbooks") suspend fun wipeCookbooks()
    @Query("DELETE FROM grocery") suspend fun wipeGrocery()
    @Query("DELETE FROM meal_plan") suspend fun wipePlan()
    @Query("DELETE FROM recipe_cookbook") suspend fun wipeLinks()
}
