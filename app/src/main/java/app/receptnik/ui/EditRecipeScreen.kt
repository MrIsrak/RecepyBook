package app.receptnik.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.receptnik.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import app.receptnik.AppContainer
import app.receptnik.core.CaptionParser
import app.receptnik.data.Recipe
import kotlinx.coroutines.launch

class EditViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    val editor = EditorState()
    var loaded by mutableStateOf(id == 0L)
    private var original: Recipe? = null

    init {
        if (id != 0L) viewModelScope.launch {
            c.repo.getRecipe(id)?.let { original = it; editor.load(it) }
            loaded = true
        }
    }

    fun parsePasted(text: String) {
        val d = CaptionParser.parse(text)
        if (editor.title.isBlank()) editor.title = d.title
        if (d.ingredients.isNotEmpty()) editor.ingredients = EditorState.ingredientsToText(d.ingredients)
        if (d.steps.isNotEmpty()) editor.steps = d.steps.joinToString("\n")
        if (editor.time.isBlank()) d.totalTimeMin?.let { editor.time = it.toString() }
        editor.servings = d.servings.toString()
    }

    fun save(untitled: String, onDone: (Long) -> Unit) = viewModelScope.launch {
        var draft = editor.toDraft()
        draft = c.links.withNutrition(draft)
        if (draft.nutrition == null && !editor.ingredientsChanged) {
            draft = draft.copy(nutrition = editor.nutrition, nutritionNote = editor.nutritionNote)
        }
        val o = original
        if (o == null) {
            val newId = c.repo.saveDraft(draft, untitled)
            c.repo.getRecipe(newId)?.let { c.repo.update(it.copy(notes = editor.notes.trim())) }
            onDone(newId)
        } else {
            if (o.image != draft.imageUrl) c.images.delete(o.image)
            c.repo.update(
                o.copy(
                    title = draft.title.ifBlank { untitled }, description = draft.description, image = draft.imageUrl,
                    servings = draft.servings, totalTimeMin = draft.totalTimeMin, ingredients = draft.ingredients,
                    steps = draft.steps, nutrition = draft.nutrition, nutritionNote = draft.nutritionNote,
                    tags = draft.tags, mealType = draft.mealType, cuisine = draft.cuisine, notes = editor.notes.trim(),
                )
            )
            onDone(o.id)
        }
    }
}

@Composable
fun EditRecipeScreen(id: Long, nav: NavHostController) {
    val c = rememberContainer()
    val vm: EditViewModel = viewModel { EditViewModel(c, id) }
    var pasted by remember { mutableStateOf("") }
    val untitled = stringResource(R.string.untitled)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.editor.image = uri.toString()
    }

    AppScaffold(
        title = stringResource(if (id == 0L) R.string.new_recipe else R.string.editing),
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(
                enabled = vm.editor.title.isNotBlank(),
                onClick = {
                    vm.save(untitled) { savedId ->
                        if (id == 0L) nav.navigate(Routes.recipe(savedId)) { popUpTo("recipes") }
                        else nav.popBackStack()
                    }
                },
            ) { Icon(Icons.Outlined.Check, stringResource(R.string.save)) }
        },
    ) { mod ->
        if (!vm.loaded) return@AppScaffold
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecipeImage(
                vm.editor.image,
                Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(24.dp)).clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            Text(stringResource(R.string.tap_to_pick_photo), style = MaterialTheme.typography.bodySmall, color = muted)
            if (id == 0L) {
                OutlinedTextField(
                    pasted, { pasted = it }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                    label = { Text(stringResource(R.string.paste_whole_recipe)) },
                    shape = RoundedCornerShape(18.dp), colors = softFieldColors(),
                )
                OutlinedButton(onClick = { vm.parsePasted(pasted) }, enabled = pasted.isNotBlank(), shape = androidx.compose.foundation.shape.CircleShape) {
                    Text(stringResource(R.string.parse_into_fields))
                }
            }
            RecipeEditorFields(vm.editor)
            Spacer(Modifier.height(40.dp))
        }
    }
}
