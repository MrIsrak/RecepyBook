package app.receptnik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.receptnik.R
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
    val lang = uiLang()
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val shareHeader = stringResource(R.string.grocery_share_header)
    val shareTitle = stringResource(R.string.send_list)
    val manual = stringResource(R.string.added_manually)

    fun amount(i: GroceryItem): String =
        QtyFormat.formatAmount(Ingredient(original = i.name, quantity = i.quantity, unit = i.unit, name = i.name), 1.0, settings.units, lang)

    fun addInput() {
        val t = input.trim()
        if (t.isEmpty()) return
        input = ""
        scope.launch { c.repo.addManualGrocery(t) }
    }

    AppScaffold(
        title = stringResource(R.string.tab_grocery),
        tab = true,
        actions = {
            IconButton(onClick = {
                val list = items.orEmpty().filter { !it.checked }
                if (list.isNotEmpty()) shareText(ctx, "$shareHeader\n\n" + list.joinToString("\n") { i ->
                    "☐ ${i.name}" + amount(i).let { a -> if (a.isBlank()) "" else " — $a" }
                }, shareTitle)
            }) { Icon(Icons.Outlined.Share, shareTitle) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.remove_checked)) },
                        onClick = { menu = false; scope.launch { c.repo.clearCheckedGrocery() } })
                    DropdownMenuItem(text = { Text(stringResource(R.string.clear_list)) }, onClick = { menu = false; confirmClear = true })
                }
            }
        },
    ) { mod ->
        val all = items ?: return@AppScaffold
        Column(mod.fillMaxSize()) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text(stringResource(R.string.grocery_add_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = CircleShape, colors = softFieldColors(),
                trailingIcon = { IconButton(onClick = { addInput() }) { Icon(Icons.Outlined.Add, stringResource(R.string.add)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { addInput() }),
            )
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.by_aisle), selected = settings.groupGroceryByAisle,
                    onClick = { scope.launch { c.settingsStore.update { it.copy(groupGroceryByAisle = true) } } })
                Pill(stringResource(R.string.by_recipe), selected = !settings.groupGroceryByAisle,
                    onClick = { scope.launch { c.settingsStore.update { it.copy(groupGroceryByAisle = false) } } })
                Pill(stringResource(R.string.units_metric), selected = settings.units == UnitSystem.METRIC, onClick = {
                    scope.launch { c.settingsStore.update { it.copy(units = if (it.units == UnitSystem.METRIC) UnitSystem.ORIGINAL else UnitSystem.METRIC) } }
                })
            }
            if (all.isEmpty()) {
                EmptyState(stringResource(R.string.empty_grocery_title), stringResource(R.string.empty_grocery_text))
                return@Column
            }
            val unchecked = all.filter { !it.checked }
            val checked = all.filter { it.checked }
            val byAisle = settings.groupGroceryByAisle
            val groups: List<Pair<String, List<GroceryItem>>> = if (byAisle) {
                unchecked.groupBy { Aisles.normalize(it.aisle) }.toList()
                    .sortedBy { (k, _) -> Aisles.order.indexOf(k).let { i -> if (i < 0) 99 else i } }
            } else unchecked.groupBy { it.recipeTitle ?: manual }.toList()

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
                groups.forEach { (key, list) ->
                    item(key = "h_$key") {
                        SectionLabel(if (byAisle) aisleLabel(key) else key, Modifier.padding(top = 20.dp, bottom = 4.dp))
                    }
                    items(list, key = { it.id }) { i ->
                        GroceryRow(i, amount(i), onToggle = { scope.launch { c.repo.toggleGrocery(i) } },
                            onDelete = { scope.launch { c.repo.deleteGrocery(i) } })
                    }
                }
                if (checked.isNotEmpty()) {
                    item(key = "h_checked") {
                        SectionLabel(stringResource(R.string.bought_n, checked.size), Modifier.padding(top = 28.dp, bottom = 4.dp))
                    }
                    items(checked, key = { it.id }) { i ->
                        GroceryRow(i, amount(i), onToggle = { scope.launch { c.repo.toggleGrocery(i) } },
                            onDelete = { scope.launch { c.repo.deleteGrocery(i) } })
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.clear_list_q)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; scope.launch { c.repo.clearAllGrocery() } }) {
                    Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun GroceryRow(i: GroceryItem, amount: String, onToggle: () -> Unit, onDelete: () -> Unit) {
    val color = if (i.checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckDot(i.checked)
            Spacer(Modifier.width(14.dp))
            Text(
                i.name, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f),
                textDecoration = if (i.checked) TextDecoration.LineThrough else null,
            )
            if (amount.isNotBlank()) Text(amount, style = MaterialTheme.typography.bodyLarge, color = if (i.checked) color else muted)
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Close, stringResource(R.string.delete), tint = muted, modifier = Modifier.size(18.dp)) }
        }
        Hairline()
    }
}
