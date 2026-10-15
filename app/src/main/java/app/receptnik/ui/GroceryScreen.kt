package app.receptnik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.receptnik.core.Aisles
import app.receptnik.core.Ingredient
import app.receptnik.core.QtyFormat
import app.receptnik.core.UnitSystem
import app.receptnik.data.GroceryItem
import kotlinx.coroutines.launch

@Composable
fun GroceryScreen() {
    val c = rememberContainer()
    val items by c.repo.grocery.collectAsState(initial = null)
    val settings by c.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    fun amount(i: GroceryItem): String =
        QtyFormat.formatAmount(Ingredient(original = i.name, quantity = i.quantity, unit = i.unit, name = i.name), 1.0, settings.units)

    fun addInput() {
        val t = input.trim()
        if (t.isEmpty()) return
        input = ""
        scope.launch { c.repo.addManualGrocery(t) }
    }

    AppScaffold(
        title = "Список покупок",
        tab = true,
        actions = {
            IconButton(onClick = {
                val list = items.orEmpty().filter { !it.checked }
                if (list.isNotEmpty()) shareText(ctx, "🛒 Список покупок\n\n" + list.joinToString("\n") { i ->
                    "☐ ${i.name}" + amount(i).let { a -> if (a.isBlank()) "" else " — $a" }
                }, "Отправить список")
            }) { Icon(Icons.Outlined.Share, "Поделиться списком") }
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Ещё") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Убрать отмеченные") }, onClick = { menu = false; scope.launch { c.repo.clearCheckedGrocery() } })
                DropdownMenuItem(text = { Text("Очистить список") }, onClick = { menu = false; confirmClear = true })
            }
        },
    ) { mod ->
        val all = items ?: return@AppScaffold
        Column(mod.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("Добавить: 2 л молока, хлеб…") },
                    singleLine = true, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions(onDone = { addInput() }),
                )
                IconButton(onClick = { addInput() }) { Icon(Icons.Outlined.Add, "Добавить") }
            }
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = settings.groupGroceryByAisle, onClick = { scope.launch { c.settingsStore.update { it.copy(groupGroceryByAisle = true) } } },
                    label = { Text("По отделам") })
                FilterChip(selected = !settings.groupGroceryByAisle, onClick = { scope.launch { c.settingsStore.update { it.copy(groupGroceryByAisle = false) } } },
                    label = { Text("По рецептам") })
                FilterChip(
                    selected = settings.units == UnitSystem.METRIC,
                    onClick = {
                        scope.launch {
                            c.settingsStore.update {
                                it.copy(units = if (it.units == UnitSystem.METRIC) UnitSystem.ORIGINAL else UnitSystem.METRIC)
                            }
                        }
                    },
                    label = { Text("г / мл") },
                )
            }
            if (all.isEmpty()) {
                EmptyState("Список пуст", "Добавь продукты сверху, или открой рецепт и нажми «В список покупок». Из плана можно добавить сразу всю неделю.")
                return@Column
            }
            val unchecked = all.filter { !it.checked }
            val checked = all.filter { it.checked }
            val groups: List<Pair<String, List<GroceryItem>>> = if (settings.groupGroceryByAisle) {
                unchecked.groupBy { it.aisle }.toList().sortedBy { (k, _) -> Aisles.order.indexOf(k).let { i -> if (i < 0) 99 else i } }
            } else {
                unchecked.groupBy { it.recipeTitle ?: "Добавлено вручную" }.toList()
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                groups.forEach { (title, list) ->
                    item(key = "h_$title") {
                        Text(
                            title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
                        )
                    }
                    items(list, key = { it.id }) { i -> GroceryRow(i, amount(i), onToggle = { scope.launch { c.repo.toggleGrocery(i) } },
                        onDelete = { scope.launch { c.repo.deleteGrocery(i) } }) }
                }
                if (checked.isNotEmpty()) {
                    item(key = "h_checked") {
                        Text(
                            "Куплено (${checked.size})", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 2.dp),
                        )
                    }
                    items(checked, key = { it.id }) { i -> GroceryRow(i, amount(i), onToggle = { scope.launch { c.repo.toggleGrocery(i) } },
                        onDelete = { scope.launch { c.repo.deleteGrocery(i) } }) }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить список?") },
            confirmButton = { TextButton(onClick = { confirmClear = false; scope.launch { c.repo.clearAllGrocery() } }) { Text("Очистить") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun GroceryRow(i: GroceryItem, amount: String, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = i.checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                i.name, style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (i.checked) TextDecoration.LineThrough else null,
                color = if (i.checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (amount.isNotBlank()) Text(
            amount, fontWeight = FontWeight.SemiBold,
            color = if (i.checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        )
        Box { IconButton(onClick = onDelete) { Icon(Icons.Outlined.Close, "Удалить", tint = MaterialTheme.colorScheme.outline) } }
    }
}
