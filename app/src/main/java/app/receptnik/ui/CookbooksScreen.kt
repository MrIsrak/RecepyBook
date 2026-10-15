package app.receptnik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.R
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
        title = stringResource(R.string.tab_cookbooks),
        tab = true,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { create = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.new_cookbook)) },
                shape = CircleShape, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { mod ->
        val list = books ?: return@AppScaffold
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.empty_books_title), stringResource(R.string.empty_books_text), mod)
            return@AppScaffold
        }
        val byId = recipes.associateBy { it.id }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(158.dp),
            modifier = mod.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(list, key = { it.id }) { b ->
                val inBook = links.filter { it.cookbookId == b.id }.mapNotNull { byId[it.recipeId] }
                Column(Modifier.clip(RoundedCornerShape(22.dp)).clickable { nav.navigate(Routes.cookbook(b.id)) }) {
                    // Cover: mosaic of the first two photos
                    val covers = inBook.mapNotNull { it.image }.take(2)
                    Row(Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(22.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (covers.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxSize()) {
                                    Box(contentAlignment = Alignment.Center) { Text(b.emoji, style = MaterialTheme.typography.displaySmall) }
                                }
                            }
                        } else covers.forEach { RecipeImage(it, Modifier.weight(1f).fillMaxSize()) }
                    }
                    Text(
                        "${b.emoji}  ${b.name}", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
                    )
                    Text(recipesCount(inBook.size), style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(horizontal = 2.dp))
                }
            }
        }
    }

    if (create) {
        CookbookNameDialog(
            title = stringResource(R.string.new_cookbook), initialName = "", initialEmoji = "📖",
            onDismiss = { create = false },
            onSave = { name, emoji -> create = false; scope.launch { c.repo.createCookbook(name, emoji) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookbookNameDialog(title: String, initialName: String, initialEmoji: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    name, { name = it }, placeholder = { Text(stringResource(R.string.f_title)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = CircleShape, colors = softFieldColors(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    bookEmojis.forEach { e -> Pill(e, selected = emoji == e, onClick = { emoji = e }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), emoji) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        title = "${b.emoji}  ${b.name}",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { addDialog = true }) { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, stringResource(R.string.add_recipes)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menu = false; rename = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete_cookbook)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menu = false; confirmDelete = true })
                }
            }
        },
    ) { mod ->
        if (inBook.isEmpty()) {
            EmptyState(stringResource(R.string.empty_book_title), stringResource(R.string.empty_book_text), mod)
            return@AppScaffold
        }
        LazyColumn(mod.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(inBook, key = { it.id }) { r ->
                RecipeRow(r, onClick = { nav.navigate(Routes.recipe(r.id)) }) {
                    IconButton(onClick = { scope.launch { c.repo.setInCookbook(r.id, b.id, false) } }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.remove_from_book), tint = muted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (rename) {
        CookbookNameDialog(stringResource(R.string.rename), b.name, b.emoji, onDismiss = { rename = false }) { name, emoji ->
            rename = false
            scope.launch { c.repo.updateCookbook(b.copy(name = name, emoji = emoji)) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.delete_cookbook_q)) },
            text = { Text(stringResource(R.string.delete_cookbook_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { c.repo.deleteCookbook(b); nav.popBackStack() } }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (addDialog) {
        val ids = inBook.map { it.id }.toSet()
        AlertDialog(
            onDismissRequest = { addDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.recipes_in_book)) },
            text = {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(all, key = { it.id }) { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = r.id in ids, onCheckedChange = { scope.launch { c.repo.setInCookbook(r.id, b.id, it) } })
                            Text(r.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addDialog = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}
