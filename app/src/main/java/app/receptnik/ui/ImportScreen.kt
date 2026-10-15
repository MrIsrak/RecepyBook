package app.receptnik.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import app.receptnik.AppContainer
import app.receptnik.core.TextUtil
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ImportViewModel(private val c: AppContainer) : ViewModel() {
    var input by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var warnings by mutableStateOf<List<String>>(emptyList())
    var hasResult by mutableStateOf(false)
    var saving by mutableStateOf(false)
    val editor = EditorState()
    private var started = false

    fun startOnce(text: String, image: String, onPick: () -> Unit) {
        if (started) return
        started = true
        when {
            image == "pick" -> onPick()
            image.isNotBlank() -> importPhoto(Uri.parse(image))
            text.isNotBlank() -> { input = text; importLink() }
        }
    }

    fun importLink() {
        val text = input.trim()
        if (TextUtil.firstUrl(text) == null) {
            // Не ссылка — разбираем как текст рецепта
            if (text.length > 20) {
                editor.load(c.links.withNutrition(app.receptnik.core.CaptionParser.parse(text)))
                hasResult = true; error = null; warnings = emptyList()
            } else error = "Вставь ссылку на пост или страницу с рецептом"
            return
        }
        loading = true; error = null; warnings = emptyList()
        viewModelScope.launch {
            runCatching { c.links.import(text) }
                .onSuccess { res ->
                    editor.load(res.draft)
                    warnings = res.warnings
                    hasResult = true
                }
                .onFailure { error = friendly(it) }
            loading = false
        }
    }

    fun importPhoto(uri: Uri) {
        loading = true; error = null; warnings = emptyList()
        viewModelScope.launch {
            runCatching { c.photos.import(uri) }
                .onSuccess { res ->
                    editor.load(res.draft)
                    warnings = res.warnings
                    hasResult = true
                }
                .onFailure { error = friendly(it) }
            loading = false
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val draft = c.links.withNutrition(editor.toDraft()).let {
                // если пересчитать не удалось, оставляем оценку ИИ/сайта
                if (it.nutrition == null && !editor.ingredientsChanged) it.copy(nutrition = editor.nutrition, nutritionNote = editor.nutritionNote) else it
            }
            val id = c.repo.saveDraft(draft)
            if (editor.notes.isNotBlank()) c.repo.getRecipe(id)?.let { c.repo.update(it.copy(notes = editor.notes.trim())) }
            saving = false
            onSaved(id)
        }
    }

    private fun friendly(e: Throwable): String {
        val m = e.message ?: ""
        return when {
            e is java.net.UnknownHostException -> "Нет интернета или сайт недоступен"
            e is java.net.SocketTimeoutException -> "Сайт слишком долго отвечает, попробуй ещё раз"
            m.startsWith("Gemini") -> m
            m.isNotBlank() -> "Не получилось загрузить: $m"
            else -> "Не получилось загрузить рецепт"
        }
    }
}

@Composable
fun ImportScreen(initialText: String, initialImage: String, nav: NavHostController) {
    val c = rememberContainer()
    val vm: ImportViewModel = viewModel { ImportViewModel(c) }
    val clipboard = LocalClipboardManager.current
    val settings by c.settings.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }
    LaunchedEffect(Unit) {
        vm.startOnce(initialText, initialImage) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    AppScaffold(
        title = if (vm.hasResult) "Проверь и сохрани" else "Добавить рецепт",
        onBack = { nav.popBackStack() },
        bottomBar = {
            if (vm.hasResult) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { vm.hasResult = false }, modifier = Modifier.height(52.dp)) {
                        Icon(Icons.Outlined.Refresh, null)
                    }
                    Button(
                        onClick = {
                            vm.save { id ->
                                nav.navigate(Routes.recipe(id)) {
                                    popUpTo("recipes")
                                }
                            }
                        },
                        enabled = !vm.saving && vm.editor.title.isNotBlank(),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("Сохранить рецепт")
                    }
                }
            }
        },
    ) { mod ->
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!vm.hasResult) {
                Text(
                    "Вставь ссылку из Instagram, TikTok, YouTube, Facebook, Pinterest или с любого сайта. " +
                        "Ещё проще — нажми «Поделиться» прямо в соцсети и выбери Рецептник.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = vm.input, onValueChange = { vm.input = it },
                    label = { Text("Ссылка или текст рецепта") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 8,
                    trailingIcon = {
                        IconButton(onClick = { clipboard.getText()?.text?.let { vm.input = it } }) {
                            Icon(Icons.Outlined.ContentPaste, "Вставить")
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { vm.importLink() }, enabled = !vm.loading && vm.input.isNotBlank(), modifier = Modifier.weight(1f)) {
                        Text("Загрузить")
                    }
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !vm.loading,
                    ) { Icon(Icons.Outlined.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("С фото") }
                }
                if (settings.geminiKey.isBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Row(Modifier.padding(12.dp)) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(
                                    "Совет: добавь бесплатный ключ Gemini в настройках — ИИ будет разбирать даже подписи без списка ингредиентов и переводить рецепты на русский.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                TextButton(onClick = { nav.navigate("settings") }) { Text("Открыть настройки") }
                            }
                        }
                    }
                }
            }

            if (vm.loading) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Достаю рецепт…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            vm.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            if (vm.hasResult && !vm.loading) {
                val draft = vm.editor
                RecipeImage(draft.image, Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(20.dp)))
                PreviewSummary(vm)
                vm.warnings.forEach { w ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(w, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                RecipeEditorFields(draft)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PreviewSummary(vm: ImportViewModel) {
    val c = rememberContainer()
    val d = vm.editor.toDraft()
    val n = c.links.withNutrition(d).nutrition ?: vm.editor.nutrition
    val perServing = n?.let { (it.kcal / d.servings.coerceAtLeast(1)).roundToInt() }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(d.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull(
                    "${d.ingredients.count { !it.isHeader }} ингредиентов",
                    "${d.steps.size} шагов",
                    formatMinutes(d.totalTimeMin),
                    "${d.servings} порц.",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (perServing != null && perServing > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "≈ $perServing ккал на порцию", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                n?.let {
                    val k = 1.0 / d.servings.coerceAtLeast(1)
                    Text(
                        "Б ${(it.protein * k).roundToInt()} г · Ж ${(it.fat * k).roundToInt()} г · У ${(it.carbs * k).roundToInt()} г",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
