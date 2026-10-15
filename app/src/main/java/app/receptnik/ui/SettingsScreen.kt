package app.receptnik.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.receptnik.core.UnitSystem
import app.receptnik.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { dest ->
        if (dest != null) scope.launch {
            runCatching {
                val json = c.repo.exportJson()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(dest)?.use { it.write(json.toByteArray()) } }
            }.onSuccess { Toast.makeText(ctx, "Копия сохранена", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { src ->
        if (src != null) scope.launch {
            pendingRestore = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(src)?.use { String(it.readBytes()) } }.getOrNull()
            }
            if (pendingRestore == null) Toast.makeText(ctx, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
        }
    }

    AppScaffold(title = "Настройки", onBack = onBack) { mod ->
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Умный разбор (ИИ)", style = MaterialTheme.typography.titleLarge)
            Text(
                "Без ключа рецепты разбираются встроенным парсером — он понимает подписи со списком ингредиентов. " +
                    "С бесплатным ключом Google Gemini приложение разберёт любой пост, видео-описание или фото, " +
                    "переведёт рецепт и оценит калорийность. Ключ хранится только на телефоне.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Как получить ключ (2 минуты, бесплатно):", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "1. Открой aistudio.google.com/apikey и войди Google-аккаунтом\n" +
                            "2. Нажми «Create API key»\n3. Скопируй ключ и вставь ниже",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { runCatching { uri.openUri("https://aistudio.google.com/apikey") } }) { Text("Открыть сайт") }
                }
            }
            OutlinedTextField(
                key, { key = it.trim() }, label = { Text("Ключ Gemini API") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Показать")
                    }
                },
            )
            OutlinedTextField(
                model, { model = it.trim() }, label = { Text("Модель") }, singleLine = true,
                supportingText = { Text("По умолчанию ${AppSettings.DEFAULT_MODEL}. Если Google переименует модель — впиши новое имя.") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Переводить рецепты на русский", modifier = Modifier.weight(1f))
                Switch(checked = s.translateToRussian, onCheckedChange = { v -> scope.launch { c.settingsStore.update { it.copy(translateToRussian = v) } } })
            }
            Button(
                onClick = {
                    scope.launch {
                        c.settingsStore.update { it.copy(geminiKey = key, geminiModel = model.ifBlank { AppSettings.DEFAULT_MODEL }) }
                        Toast.makeText(ctx, "Сохранено", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = key != s.geminiKey || model != s.geminiModel,
            ) { Text("Сохранить ключ") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Единицы измерения по умолчанию", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(UnitSystem.ORIGINAL to "Как в рецепте", UnitSystem.METRIC to "Граммы/мл", UnitSystem.US to "US").forEach { (u, t) ->
                    FilterChip(selected = s.units == u, onClick = { scope.launch { c.settingsStore.update { it.copy(units = u) } } }, label = { Text(t) })
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Резервная копия", style = MaterialTheme.typography.titleLarge)
            Text(
                "Все рецепты, книги, план и покупки хранятся только на телефоне. Сохрани копию в файл (например, на Google Диск), " +
                    "чтобы перенести на другой телефон.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { exporter.launch("receptnik-${LocalDate.now()}.json") }) { Text("Сохранить копию") }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Восстановить") }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "Рецептник ${app.receptnik.BuildConfig.VERSION_NAME} — бесплатно, без рекламы и подписок.\ngithub.com/MrIsrak/RecepyBook",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingRestore?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Восстановить из копии?") },
            text = { Text("Текущие рецепты, книги, план и список покупок будут заменены данными из файла.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        runCatching { c.repo.importJson(text) }
                            .onSuccess { Toast.makeText(ctx, "Восстановлено рецептов: $it", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(ctx, "Файл не подходит: ${it.message}", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Восстановить") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Отмена") } },
        )
    }
}
