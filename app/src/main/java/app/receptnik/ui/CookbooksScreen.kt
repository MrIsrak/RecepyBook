package app.receptnik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.data.Cookbook
import kotlinx.coroutines.launch

private val bookEmojis = listOf("📖", "🍝", "🥗", "🍰", "🍲", "🥘", "🍳", "🥩", "🐟", "🌱", "🍕", "🥟", "🍪", "☕", "🎉", "❤️")

@Composable
fun CookbooksScreen(nav: NavHostController) {
    val c = rememberContainer()
    val books by c.repo.cookbooks.collectAsState(initial = null)
    val links by c.repo.cookbookLinks.collectAsState(initial = emptyList())
    val recipes by c.repo.recipes.collectAsState(initial = emptyList())
    var create by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AppScaffold(
        title = "Кулинарные книги",
        tab = true,
        fab = {
            ExtendedFloatingActionButton(onClick = { create = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Новая книга") })
        },
    ) { mod ->
        val list = books ?: return@AppScaffold
        if (list.isEmpty()) {
            EmptyState("Книг пока нет", "Собирай рецепты в книги: «Завтраки», «Быстрые ужины», «На праздник»…", mod)
            return@AppScaffold
        }
        val byId = recipes.associateBy { it.id }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = mod.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { b ->
                val inBook = links.filter { it.cookbookId == b.id }.mapNotNull { byId[it.recipeId] }
                Card(onClick = { nav.navigate(Routes.cookbook(b.id)) }, shape = RoundedCornerShape(18.dp)) {
                    Box {
                        RecipeImage(inBook.firstOrNull { it.image != null }?.image, Modifier.fillMaxWidth().aspectRatio(1.3f))
                    }
                    Column(Modifier.padding(12.dp)) {
                        Text("${b.emoji} ${b.name}", style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(recipesCount(inBook.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (create) {
        CookbookNameDialog(
            title = "Новая книга", initialName = "", initialEmoji = "📖",
            onDismiss = { create = false },
            onSave = { name, emoji -> create = false; scope.launch { c.repo.createCookbook(name, emoji) } },
        )
    }
}

fun recipesCount(n: Int): String {
    val mod10 = n % 10; val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "рецепт"
        mod10 in 2..4 && mod100 !in 12..14 -> "рецепта"
        else -> "рецептов"
    }
    return "$n $word"
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CookbookNameDialog(title: String, initialName: String, initialEmoji: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    bookEmojis.forEach { e -> FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), emoji) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
fun CookbookDetailScreen(id: Long, nav: NavHostController) {
    val c = rememberContainer()
    val book by c.repo.cookbook(id).collectAsState(initial = null)
    val inBook by c.repo.recipesIn(id).collectAsState(initial = emptyList())
    val all by c.repo.recipes.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    val b: Cookbook = book ?: run { AppScaffold("", onBack = { nav.popBackStack() }) {}; return }

    AppScaffold(
        title = "${b.emoji} ${b.name}",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { addDialog = true }) { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, "Добавить рецепты") }
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Ещё") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Переименовать") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { menu = false; rename = true })
                DropdownMenuItem(text = { Text("Удалить книгу") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                    onClick = { menu = false; confirmDelete = true })
            }
        },
    ) { mod ->
        if (inBook.isEmpty()) {
            EmptyState("Книга пуста", "Нажми ➕ вверху, чтобы добавить рецепты, или открой рецепт → ⋮ → «В кулинарную книгу».", mod)
            return@AppScaffold
        }
        LazyColumn(mod.fillMaxSize(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
            items(inBook, key = { it.id }) { r ->
                RecipeRow(r, onClick = { nav.navigate(Routes.recipe(r.id)) }) {
                    IconButton(onClick = { scope.launch { c.repo.setInCookbook(r.id, b.id, false) } }) {
                        Icon(Icons.Outlined.Close, "Убрать из книги", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (rename) {
        CookbookNameDialog("Переименовать", b.name, b.emoji, onDismiss = { rename = false }) { name, emoji ->
            rename = false
            scope.launch { c.repo.updateCookbook(b.copy(name = name, emoji = emoji)) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить книгу?") },
            text = { Text("Рецепты останутся в приложении, удалится только книга.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { c.repo.deleteCookbook(b); nav.popBackStack() } }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
    if (addDialog) {
        val ids = inBook.map { it.id }.toSet()
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("Рецепты в книге") },
            text = {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(all, key = { it.id }) { r ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = r.id in ids, onCheckedChange = { scope.launch { c.repo.setInCookbook(r.id, b.id, it) } })
                            Text(r.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addDialog = false }) { Text("Готово") } },
        )
    }
}
