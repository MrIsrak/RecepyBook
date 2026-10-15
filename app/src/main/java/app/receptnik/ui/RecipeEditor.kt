package app.receptnik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.receptnik.core.IngredientParser
import app.receptnik.core.Ingredient
import app.receptnik.core.Nutrition
import app.receptnik.core.QtyFormat
import app.receptnik.core.RecipeDraft
import app.receptnik.core.UnitSystem
import app.receptnik.data.Recipe

/** Редактируемые поля рецепта. Ингредиенты и шаги — по одному на строку. */
class EditorState {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var image by mutableStateOf<String?>(null)
    var sourceUrl by mutableStateOf<String?>(null)
    var author by mutableStateOf<String?>(null)
    var servings by mutableStateOf("2")
    var time by mutableStateOf("")
    var ingredients by mutableStateOf("")
    var steps by mutableStateOf("")
    var tags by mutableStateOf("")
    var mealType by mutableStateOf("")
    var cuisine by mutableStateOf("")
    var notes by mutableStateOf("")
    var nutrition: Nutrition? = null
    var nutritionNote: String? = null
    private var originalIngredients = ""

    val ingredientsChanged get() = ingredients != originalIngredients

    fun load(d: RecipeDraft) {
        title = d.title; description = d.description; image = d.imageUrl; sourceUrl = d.sourceUrl; author = d.author
        servings = d.servings.toString(); time = d.totalTimeMin?.toString() ?: ""
        ingredients = ingredientsToText(d.ingredients); originalIngredients = ingredients
        steps = d.steps.joinToString("\n")
        tags = d.tags.joinToString(", "); mealType = d.mealType ?: ""; cuisine = d.cuisine ?: ""
        nutrition = d.nutrition; nutritionNote = d.nutritionNote
    }

    fun load(r: Recipe) {
        load(
            RecipeDraft(
                title = r.title, description = r.description, imageUrl = r.image, sourceUrl = r.sourceUrl, author = r.author,
                servings = r.servings, totalTimeMin = r.totalTimeMin, ingredients = r.ingredients, steps = r.steps,
                nutrition = r.nutrition, nutritionNote = r.nutritionNote, tags = r.tags, mealType = r.mealType, cuisine = r.cuisine,
            )
        )
        notes = r.notes
    }

    fun toDraft(): RecipeDraft = RecipeDraft(
        title = title.trim(),
        description = description.trim(),
        imageUrl = image,
        sourceUrl = sourceUrl,
        author = author,
        servings = servings.toIntOrNull()?.coerceIn(1, 100) ?: 2,
        totalTimeMin = time.toIntOrNull()?.takeIf { it > 0 },
        ingredients = textToIngredients(ingredients),
        steps = steps.lines().map { it.trim() }.filter { it.isNotEmpty() },
        nutrition = if (ingredientsChanged) null else nutrition,
        nutritionNote = if (ingredientsChanged) null else nutritionNote,
        tags = tags.split(',').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() },
        mealType = mealType.trim().ifBlank { null },
        cuisine = cuisine.trim().ifBlank { null },
    )

    companion object {
        fun ingredientsToText(list: List<Ingredient>): String = list.joinToString("\n") {
            if (it.isHeader) it.original + ":" else {
                val amount = QtyFormat.formatAmount(it, 1.0, UnitSystem.ORIGINAL)
                // Сохраняем исходную строку, если разбор её не изменил по смыслу
                if (it.quantity == null) it.original else "$amount ${it.name}" + (it.note?.let { n -> " $n" } ?: "")
            }
        }

        fun textToIngredients(text: String): List<Ingredient> =
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { IngredientParser.parse(it) }
    }
}

@Composable
fun RecipeEditorFields(s: EditorState, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            s.title, { s.title = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                s.servings, { v -> s.servings = v.filter { it.isDigit() }.take(3) }, label = { Text("Порций") },
                modifier = Modifier.weight(1f), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                s.time, { v -> s.time = v.filter { it.isDigit() }.take(4) }, label = { Text("Время, мин") },
                modifier = Modifier.weight(1f), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        OutlinedTextField(
            s.ingredients, { s.ingredients = it }, label = { Text("Ингредиенты — по одному на строку") },
            placeholder = { Text("200 г муки\n2 яйца\nСоль по вкусу\nДля соуса:") },
            modifier = Modifier.fillMaxWidth(), minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        OutlinedTextField(
            s.steps, { s.steps = it }, label = { Text("Шаги — по одному на строку") },
            modifier = Modifier.fillMaxWidth(), minLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        OutlinedTextField(
            s.description, { s.description = it }, label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth(), minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(s.mealType, { s.mealType = it }, label = { Text("Тип блюда") },
                placeholder = { Text("ужин") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(s.cuisine, { s.cuisine = it }, label = { Text("Кухня") },
                placeholder = { Text("итальянская") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        OutlinedTextField(s.tags, { s.tags = it }, label = { Text("Теги через запятую") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(s.notes, { s.notes = it }, label = { Text("Мои заметки") },
            modifier = Modifier.fillMaxWidth(), minLines = 2)
        Text(
            "Калории пересчитаются автоматически по ингредиентам.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}
