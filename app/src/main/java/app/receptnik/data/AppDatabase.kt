package app.receptnik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Recipe::class, Cookbook::class, RecipeCookbook::class, MealPlanEntry::class, GroceryItem::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipes(): RecipeDao
    abstract fun cookbooks(): CookbookDao
    abstract fun mealPlan(): MealPlanDao
    abstract fun grocery(): GroceryDao
    abstract fun backup(): BackupDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "receptnik.db").build()
    }
}
