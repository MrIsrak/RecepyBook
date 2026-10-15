package app.receptnik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.data.Recipe

enum class SortMode(val title: String) {
    NEWEST("Сначала новые"), OLDEST("Сначала старые"), ALPHA("По алфавиту"),
    QUICK("Сначала быстрые"), COOKED("Недавно готовил"), CALORIES("Меньше калорий")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(nav: NavHostController) {
    val c = rememberContainer()
    val recipes by c.repo.recipes.collectAsState(initial = null)
    var query by rememberSaveable { mutableStateOf("") }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    var mealFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortMode.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }
    var addSheet by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Рецептник",
        tab = true,
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, "Сортировка") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text((if (m == sort) "✓ " else "") + m.title) },
                            onClick = { sort = m; sortMenu = false },
                        )
                    }
                }
            }
            IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Outlined.Settings, "Настройки") }
        },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { addSheet = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Добавить") },
            )
        },
    ) { mod ->
        val all = recipes
        if (all == null) return@AppScaffold
        if (all.isEmpty()) {
            EmptyState(
                "Пока нет рецептов",
                "Нажми «Поделиться» под постом в Instagram, TikTok или YouTube и выбери Рецептник — " +
                    "рецепт сохранится сам. Или нажми «Добавить» и вставь ссылку.",
                mod,
            )
            return@AppScaffold
        }
        val mealTypes = remember(all) { all.mapNotNull { it.mealType?.trim()?.lowercase()?.takeIf { t -> t.isNotBlank() } }.distinct().sorted() }
        val filtered = remember(all, query, onlyFav, mealFilter, sort) {
            filterAndSort(all, query, onlyFav, mealFilter, sort)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = mod.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Название, ингредиент, кухня…") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Очистить") }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        item {
                            FilterChip(
                                selected = onlyFav, onClick = { onlyFav = !onlyFav },
                                label = { Text("Избранное") },
                                leadingIcon = { Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp)) },
                            )
                        }
                        items(mealTypes) { t ->
                            FilterChip(
                                selected = mealFilter == t,
                                onClick = { mealFilter = if (mealFilter == t) null else t },
                                label = { Text(t.replaceFirstChar { it.uppercaseChar() }) },
                            )
                        }
                    }
                    Text(
                        "${filtered.size} из ${all.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            items(filtered, key = { it.id }) { r ->
                RecipeCard(r) { nav.navigate(Routes.recipe(r.id)) }
            }
        }
    }

    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { addSheet = false }) {
            Text(
                "Добавить рецепт", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("По ссылке") },
                supportingContent = { Text("Instagram, TikTok, YouTube, Facebook, Pinterest, любой сайт") },
                leadingContent = { Icon(Icons.Outlined.Link, null) },
                modifier = Modifier.clickable { addSheet = false; nav.navigate(Routes.importLink()) },
            )
            ListItem(
                headlineContent = { Text("С фото или скриншота") },
                supportingContent = { Text("Страница кулинарной книги, скриншот, заметка") },
                leadingContent = { Icon(Icons.Outlined.PhotoCamera, null) },
                modifier = Modifier.clickable { addSheet = false; nav.navigate(Routes.IMPORT_PICK_PHOTO) },
            )
            ListItem(
                headlineContent = { Text("Вручную") },
                supportingContent = { Text("Свой рецепт или вставить текст") },
                leadingContent = { Icon(Icons.Outlined.EditNote, null) },
                modifier = Modifier.clickable { addSheet = false; nav.navigate(Routes.edit(0)) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun filterAndSort(all: List<Recipe>, query: String, onlyFav: Boolean, meal: String?, sort: SortMode): List<Recipe> {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    return all.asSequence()
        .filter { !onlyFav || it.favorite }
        .filter { meal == null || it.mealType?.trim()?.lowercase() == meal }
        .filter { r ->
            if (words.isEmpty()) true else {
                val hay = buildString {
                    append(r.title).append(' ')
                    r.ingredients.forEach { append(it.name).append(' ') }
                    r.tags.forEach { append(it).append(' ') }
                    append(r.cuisine ?: "").append(' ').append(r.mealType ?: "").append(' ').append(r.author ?: "")
                }.lowercase()
                words.all { it in hay }
            }
        }
        .let { s ->
            when (sort) {
                SortMode.NEWEST -> s.sortedByDescending { it.createdAt }
                SortMode.OLDEST -> s.sortedBy { it.createdAt }
                SortMode.ALPHA -> s.sortedBy { it.title.lowercase() }
                SortMode.QUICK -> s.sortedBy { it.totalTimeMin ?: Int.MAX_VALUE }
                SortMode.COOKED -> s.sortedByDescending { it.lastCookedAt ?: 0L }
                SortMode.CALORIES -> s.sortedBy { it.kcalPerServing() ?: Int.MAX_VALUE }
            }
        }
        .toList()
}

@Composable
fun RecipeCard(r: Recipe, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
        Box {
            RecipeImage(r.image, Modifier.fillMaxWidth().aspectRatio(1f))
            if (r.favorite) {
                Surface(
                    shape = CircleShape, color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.padding(6.dp).size(16.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(formatMinutes(r.totalTimeMin), r.kcalPerServing()?.let { "$it ккал" }).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    meta, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun RecipeRow(r: Recipe, subtitle: String? = null, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeImage(r.image, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = subtitle ?: listOfNotNull(formatMinutes(r.totalTimeMin), r.kcalPerServing()?.let { "$it ккал/порц." }).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}
