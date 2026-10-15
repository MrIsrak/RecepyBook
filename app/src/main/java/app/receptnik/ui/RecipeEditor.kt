package app.receptnik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import app.receptnik.R
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

/** Editable recipe fields. Ingredients and steps are one per line. */
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
                // Keep the original line when parsing did not change its meaning
                if (it.quantity == null) it.original else "$amount ${it.name}" + (it.note?.let { n -> " $n" } ?: "")
            }
        }

        fun textToIngredients(text: String): List<Ingredient> =
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { IngredientParser.parse(it) }
    }
}

@Composable
fun RecipeEditorFields(s: EditorState, modifier: Modifier = Modifier) {
    val cap = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)
    val shape = RoundedCornerShape(18.dp)
    val colors = softFieldColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(s.title, { s.title = it }, label = { Text(stringResource(R.string.f_title)) },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = cap, shape = shape, colors = colors)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(s.servings, { v -> s.servings = v.filter { it.isDigit() }.take(3) }, label = { Text(stringResource(R.string.servings)) },
                modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = num, shape = shape, colors = colors)
            OutlinedTextField(s.time, { v -> s.time = v.filter { it.isDigit() }.take(4) }, label = { Text(stringResource(R.string.f_time)) },
                modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = num, shape = shape, colors = colors)
        }
        OutlinedTextField(s.ingredients, { s.ingredients = it }, label = { Text(stringResource(R.string.f_ingredients)) },
            placeholder = { Text(stringResource(R.string.f_ingredients_hint)) },
            modifier = Modifier.fillMaxWidth(), minLines = 5, keyboardOptions = cap, shape = shape, colors = colors)
        OutlinedTextField(s.steps, { s.steps = it }, label = { Text(stringResource(R.string.f_steps)) },
            modifier = Modifier.fillMaxWidth(), minLines = 5, keyboardOptions = cap, shape = shape, colors = colors)
        OutlinedTextField(s.description, { s.description = it }, label = { Text(stringResource(R.string.f_description)) },
            modifier = Modifier.fillMaxWidth(), minLines = 2, keyboardOptions = cap, shape = shape, colors = colors)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(s.mealType, { s.mealType = it }, label = { Text(stringResource(R.string.f_meal_type)) },
                modifier = Modifier.weight(1f), singleLine = true, shape = shape, colors = colors)
            OutlinedTextField(s.cuisine, { s.cuisine = it }, label = { Text(stringResource(R.string.f_cuisine)) },
                modifier = Modifier.weight(1f), singleLine = true, shape = shape, colors = colors)
        }
        OutlinedTextField(s.tags, { s.tags = it }, label = { Text(stringResource(R.string.f_tags)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = shape, colors = colors)
        OutlinedTextField(s.notes, { s.notes = it }, label = { Text(stringResource(R.string.my_notes)) },
            modifier = Modifier.fillMaxWidth(), minLines = 2, shape = shape, colors = colors)
        Text(stringResource(R.string.calories_auto), style = MaterialTheme.typography.bodySmall, color = muted,
            modifier = Modifier.padding(bottom = 8.dp))
    }
}
