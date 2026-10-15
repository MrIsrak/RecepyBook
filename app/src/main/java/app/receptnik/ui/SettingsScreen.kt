package app.receptnik.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.receptnik.BuildConfig
import app.receptnik.R
import app.receptnik.core.UnitSystem
import app.receptnik.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val s by c.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    var key by remember { mutableStateOf(s.geminiKey) }
    var model by remember { mutableStateOf(s.geminiModel) }
    var showKey by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.geminiKey, s.geminiModel) { key = s.geminiKey; model = s.geminiModel }

    val msgSaved = stringResource(R.string.saved)
    val msgBackupSaved = stringResource(R.string.backup_saved)
    val msgReadFail = stringResource(R.string.cannot_read_file)
    val msgErrorTpl = stringResource(R.string.error_with)
    val msgRestoredTpl = stringResource(R.string.restored_n)
    val msgBadFileTpl = stringResource(R.string.bad_backup)

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { dest ->
        if (dest != null) scope.launch {
            runCatching {
                val json = c.repo.exportJson()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(dest)?.use { it.write(json.toByteArray()) } }
            }.onSuccess { Toast.makeText(ctx, msgBackupSaved, Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, String.format(msgErrorTpl, it.message), Toast.LENGTH_LONG).show() }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { src ->
        if (src != null) scope.launch {
            pendingRestore = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(src)?.use { String(it.readBytes()) } }.getOrNull()
            }
            if (pendingRestore == null) Toast.makeText(ctx, msgReadFail, Toast.LENGTH_SHORT).show()
        }
    }

    AppScaffold(title = stringResource(R.string.settings), onBack = onBack) { mod ->
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Язык ──
            SectionLabel(stringResource(R.string.language), Modifier.padding(top = 8.dp))
            val current = remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().take(2)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("" to stringResource(R.string.lang_system), "en" to "English", "ru" to "Русский", "iw" to "עברית").forEach { (tag, label) ->
                    val selected = current.value == tag || (tag == "iw" && current.value == "he")
                    Pill(label, selected = selected, onClick = {
                        current.value = tag
                        AppCompatDelegate.setApplicationLocales(
                            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                        )
                    })
                }
            }

            Hairline(Modifier.padding(vertical = 10.dp))

            // ── Единицы ──
            SectionLabel(stringResource(R.string.default_units))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(UnitSystem.ORIGINAL to R.string.units_original, UnitSystem.METRIC to R.string.units_metric, UnitSystem.US to R.string.units_us)
                    .forEach { (u, t) -> Pill(stringResource(t), selected = s.units == u, onClick = { scope.launch { c.settingsStore.update { it.copy(units = u) } } }) }
            }

            Hairline(Modifier.padding(vertical = 10.dp))

            // ── ИИ ──
            SectionLabel(stringResource(R.string.smart_parsing))
            Text(stringResource(R.string.smart_parsing_text), style = MaterialTheme.typography.bodyMedium, color = muted)
            Notice(stringResource(R.string.how_to_get_key), action = stringResource(R.string.open_site)) {
                runCatching { uri.openUri("https://aistudio.google.com/apikey") }
            }
            OutlinedTextField(
                key, { key = it.trim() }, label = { Text(stringResource(R.string.gemini_key)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = softFieldColors(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, stringResource(R.string.show))
                    }
                },
            )
            OutlinedTextField(
                model, { model = it.trim() }, label = { Text(stringResource(R.string.model)) }, singleLine = true,
                supportingText = { Text(stringResource(R.string.model_hint, AppSettings.DEFAULT_MODEL)) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = softFieldColors(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.translate_recipes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = s.translateRecipes,
                    onCheckedChange = { v -> scope.launch { c.settingsStore.update { it.copy(translateRecipes = v) } } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        c.settingsStore.update { it.copy(geminiKey = key, geminiModel = model.ifBlank { AppSettings.DEFAULT_MODEL }) }
                        Toast.makeText(ctx, msgSaved, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = key != s.geminiKey || model != s.geminiModel, shape = CircleShape, modifier = Modifier.height(48.dp),
            ) { Text(stringResource(R.string.save_key)) }

            Hairline(Modifier.padding(vertical = 10.dp))

            // ── Резервная копия ──
            SectionLabel(stringResource(R.string.backup))
            Text(stringResource(R.string.backup_text), style = MaterialTheme.typography.bodyMedium, color = muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { exporter.launch("recepybook-${LocalDate.now()}.json") }, shape = CircleShape, modifier = Modifier.height(48.dp)) {
                    Text(stringResource(R.string.save_backup))
                }
                OutlinedButton(
                    onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    shape = CircleShape, border = BorderStroke(1.dp, hairline), modifier = Modifier.height(48.dp),
                ) { Text(stringResource(R.string.restore)) }
            }

            Hairline(Modifier.padding(vertical = 10.dp))
            Text(
                stringResource(R.string.about_line, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall, color = muted,
            )
            TextButton(onClick = { runCatching { uri.openUri("https://github.com/MrIsrak/RecepyBook") } }) {
                Text("github.com/MrIsrak/RecepyBook", color = Accent)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    pendingRestore?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.restore_q)) },
            text = { Text(stringResource(R.string.restore_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        runCatching { c.repo.importJson(text) }
                            .onSuccess { Toast.makeText(ctx, String.format(msgRestoredTpl, it), Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(ctx, String.format(msgBadFileTpl, it.message), Toast.LENGTH_LONG).show() }
                    }
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
