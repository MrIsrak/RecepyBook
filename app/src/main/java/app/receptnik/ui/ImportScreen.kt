package app.receptnik.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import app.receptnik.AppContainer
import app.receptnik.R
import app.receptnik.core.CaptionParser
import app.receptnik.core.TextUtil
import app.receptnik.importer.ImportException
import app.receptnik.importer.ImportWarning
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Ошибка импорта: известная причина (локализуется) или текст как есть. */
sealed interface ImportError {
    data class Known(val reason: ImportException.Reason) : ImportError
    data object NoInternet : ImportError
    data object Timeout : ImportError
    data object PasteLink : ImportError
    data class Raw(val text: String) : ImportError
}

class ImportViewModel(private val c: AppContainer) : ViewModel() {
    var input by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<ImportError?>(null)
    var warnings by mutableStateOf<List<ImportWarning>>(emptyList())
    var aiError by mutableStateOf<String?>(null)
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

    private fun reset() { error = null; warnings = emptyList(); aiError = null }

    fun importLink() {
        val text = input.trim()
        reset()
        if (TextUtil.firstUrl(text) == null) {
            if (text.length > 20) {
                editor.load(c.links.withNutrition(CaptionParser.parse(text)))
                hasResult = true
            } else error = ImportError.PasteLink
            return
        }
        loading = true
        viewModelScope.launch {
            runCatching { c.links.import(text) }
                .onSuccess { res -> editor.load(res.draft); warnings = res.warnings; aiError = res.aiError; hasResult = true }
                .onFailure { error = classify(it) }
            loading = false
        }
    }

    fun importPhoto(uri: Uri) {
        reset()
        loading = true
        viewModelScope.launch {
            runCatching { c.photos.import(uri) }
                .onSuccess { res -> editor.load(res.draft); warnings = res.warnings; aiError = res.aiError; hasResult = true }
                .onFailure { error = classify(it) }
            loading = false
        }
    }

    fun save(untitled: String, onSaved: (Long) -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val draft = c.links.withNutrition(editor.toDraft()).let {
                if (it.nutrition == null && !editor.ingredientsChanged) it.copy(nutrition = editor.nutrition, nutritionNote = editor.nutritionNote) else it
            }
            val id = c.repo.saveDraft(draft, untitled)
            if (editor.notes.isNotBlank()) c.repo.getRecipe(id)?.let { c.repo.update(it.copy(notes = editor.notes.trim())) }
            saving = false
            onSaved(id)
        }
    }

    private fun classify(e: Throwable): ImportError = when {
        e is ImportException -> ImportError.Known(e.reason)
        e is java.net.UnknownHostException || e is java.net.ConnectException -> ImportError.NoInternet
        e is java.net.SocketTimeoutException || e is java.io.InterruptedIOException -> ImportError.Timeout
        e is kotlinx.serialization.SerializationException || e is IllegalArgumentException ->
            ImportError.Known(ImportException.Reason.BAD_RESPONSE)
        (e.message ?: "").startsWith("Gemini") -> ImportError.Raw(e.message!!)
        else -> ImportError.Known(ImportException.Reason.SITE_ERROR)
    }
}

@Composable
private fun errorText(e: ImportError): String = when (e) {
    is ImportError.Known -> stringResource(
        when (e.reason) {
            ImportException.Reason.NO_LINK -> R.string.err_paste_link
            ImportException.Reason.RATE_LIMITED -> R.string.err_rate_limited
            ImportException.Reason.NOT_FOUND -> R.string.err_not_found
            ImportException.Reason.BLOCKED -> R.string.err_blocked
            ImportException.Reason.SITE_ERROR -> R.string.err_site
            ImportException.Reason.BAD_RESPONSE -> R.string.err_bad_response
        }
    )
    ImportError.NoInternet -> stringResource(R.string.err_no_internet)
    ImportError.Timeout -> stringResource(R.string.err_timeout)
    ImportError.PasteLink -> stringResource(R.string.err_paste_link)
    is ImportError.Raw -> e.text
}

@Composable
private fun warningText(w: ImportWarning): String = stringResource(
    when (w) {
        ImportWarning.NO_INGREDIENTS -> R.string.warn_no_ingredients
        ImportWarning.NO_INGREDIENTS_WITH_AI -> R.string.warn_no_ingredients_ai
        ImportWarning.PHOTO_UNREADABLE -> R.string.warn_photo_unreadable
        ImportWarning.PHOTO_NO_RECIPE -> R.string.warn_photo_no_recipe
    }
)

@Composable
fun ImportScreen(initialText: String, initialImage: String, nav: NavHostController) {
    val c = rememberContainer()
    val vm: ImportViewModel = viewModel { ImportViewModel(c) }
    val clipboard = LocalClipboardManager.current
    val settings by c.settings.collectAsState()
    val untitled = stringResource(R.string.untitled)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }
    LaunchedEffect(Unit) {
        vm.startOnce(initialText, initialImage) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }

    AppScaffold(
        title = stringResource(if (vm.hasResult) R.string.check_and_save else R.string.add_recipe),
        onBack = { nav.popBackStack() },
        bottomBar = {
            if (vm.hasResult) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { vm.hasResult = false }, modifier = Modifier.height(54.dp),
                        shape = CircleShape, border = BorderStroke(1.dp, hairline),
                    ) { Icon(Icons.Outlined.Refresh, stringResource(R.string.start_over)) }
                    Button(
                        onClick = { vm.save(untitled) { id -> nav.navigate(Routes.recipe(id)) { popUpTo("recipes") } } },
                        enabled = !vm.saving, modifier = Modifier.weight(1f).height(54.dp), shape = CircleShape,
                    ) { Text(stringResource(R.string.save_recipe)) }
                }
            }
        },
    ) { mod ->
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!vm.hasResult) {
                Text(stringResource(R.string.import_intro), style = MaterialTheme.typography.bodyLarge, color = muted)
                OutlinedTextField(
                    value = vm.input, onValueChange = { vm.input = it },
                    placeholder = { Text(stringResource(R.string.link_or_text)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 8,
                    shape = RoundedCornerShape(20.dp), colors = softFieldColors(),
                    trailingIcon = {
                        IconButton(onClick = { clipboard.getText()?.text?.let { vm.input = it } }) {
                            Icon(Icons.Outlined.ContentPaste, stringResource(R.string.paste))
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.importLink() }, enabled = !vm.loading && vm.input.isNotBlank(),
                        modifier = Modifier.weight(1f).height(52.dp), shape = CircleShape,
                    ) { Text(stringResource(R.string.load)) }
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !vm.loading, modifier = Modifier.height(52.dp), shape = CircleShape, border = BorderStroke(1.dp, hairline),
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.from_photo))
                    }
                }
                if (settings.geminiKey.isBlank() && !vm.loading) {
                    Notice(stringResource(R.string.tip_gemini), action = stringResource(R.string.open_settings)) { nav.navigate("settings") }
                }
            }

            if (vm.loading) {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.fetching), style = MaterialTheme.typography.bodyMedium, color = muted)
                }
            }
            vm.error?.let { e ->
                val retry = e is ImportError.Known && e.reason == ImportException.Reason.RATE_LIMITED || e is ImportError.Timeout
                Notice(errorText(e), error = true, action = if (retry) stringResource(R.string.retry) else null) { vm.importLink() }
            }

            if (vm.hasResult && !vm.loading) {
                RecipeImage(vm.editor.image, Modifier.fillMaxWidth().aspectRatio(16f / 11f).clip(RoundedCornerShape(24.dp)))
                PreviewSummary(vm)
                vm.warnings.forEach { Notice(warningText(it)) }
                vm.aiError?.let { Notice(stringResource(R.string.ai_failed, it)) }
                Hairline()
                RecipeEditorFields(vm.editor)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

/** Тихое уведомление: серый блок (или красноватый для ошибок), без иконок. */
@Composable
fun Notice(text: String, error: Boolean = false, action: String? = null, onAction: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (error) cs.errorContainer else cs.surfaceContainer,
        contentColor = if (error) cs.onErrorContainer else cs.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (action != null) TextButton(onClick = onAction, modifier = Modifier.padding(top = 2.dp)) {
                Text(action, color = if (error) cs.onErrorContainer else Accent)
            }
        }
    }
}

@Composable
private fun PreviewSummary(vm: ImportViewModel) {
    val c = rememberContainer()
    val d = vm.editor.toDraft()
    val n = c.links.withNutrition(d).nutrition ?: vm.editor.nutrition
    val servings = d.servings.coerceAtLeast(1)
    Column {
        Text(d.title.ifBlank { stringResource(R.string.untitled) }, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            listOfNotNull(
                pluralStringResource(R.plurals.ingredients_count, d.ingredients.count { !it.isHeader }, d.ingredients.count { !it.isHeader }),
                pluralStringResource(R.plurals.steps_count, d.steps.size, d.steps.size),
                minutesText(d.totalTimeMin),
                pluralStringResource(R.plurals.servings_count, servings, servings),
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyMedium, color = muted,
        )
        if (n != null && n.kcal > 0) {
            val k = 1.0 / servings
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${(n.kcal * k).roundToInt()}", style = MaterialTheme.typography.displaySmall, color = Accent)
                Text(" " + stringResource(R.string.kcal_per_serving_short), style = MaterialTheme.typography.bodyMedium, color = muted,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(
                stringResource(R.string.macros_line, (n.protein * k).roundToInt(), (n.fat * k).roundToInt(), (n.carbs * k).roundToInt()),
                style = MaterialTheme.typography.bodySmall, color = muted,
            )
        }
    }
}
