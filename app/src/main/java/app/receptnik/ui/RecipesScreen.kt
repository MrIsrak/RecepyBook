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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.R
import app.receptnik.data.Recipe

enum class SortMode(val title: Int) {
    NEWEST(R.string.sort_newest), OLDEST(R.string.sort_oldest), ALPHA(R.string.sort_alpha),
    QUICK(R.string.sort_quick), COOKED(R.string.sort_cooked), CALORIES(R.string.sort_calories)
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
        title = stringResource(R.string.tab_recipes),
        tab = true,
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(R.string.sort)) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text((if (m == sort) "✓  " else "") + stringResource(m.title)) },
                            onClick = { sort = m; sortMenu = false },
                        )
                    }
                }
            }
            IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
        },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { addSheet = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add)) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { mod ->
        val all = recipes ?: return@AppScaffold
        if (all.isEmpty()) {
            EmptyState(stringResource(R.string.empty_recipes_title), stringResource(R.string.empty_recipes_text), mod)
            return@AppScaffold
        }
        val mealTypes = remember(all) {
            all.mapNotNull { it.mealType?.trim()?.lowercase()?.takeIf { t -> t.isNotBlank() } }.distinct().sorted()
        }
        val filtered = remember(all, query, onlyFav, mealFilter, sort) { filterAndSort(all, query, onlyFav, mealFilter, sort) }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(158.dp),
            modifier = mod.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.clear)) }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = softFieldColors(),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        item {
                            Pill(stringResource(R.string.all), selected = !onlyFav && mealFilter == null,
                                onClick = { onlyFav = false; mealFilter = null })
                        }
                        item {
                            Pill(stringResource(R.string.favorites), selected = onlyFav, onClick = { onlyFav = !onlyFav },
                                icon = Icons.Filled.Favorite)
                        }
                        items(mealTypes) { t ->
                            Pill(t.replaceFirstChar { it.uppercaseChar() }, selected = mealFilter == t,
                                onClick = { mealFilter = if (mealFilter == t) null else t })
                        }
                    }
                    Text(
                        stringResource(R.string.count_of, filtered.size, all.size),
                        style = MaterialTheme.typography.labelMedium, color = muted,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            items(filtered, key = { it.id }) { r -> RecipeCard(r) { nav.navigate(Routes.recipe(r.id)) } }
        }
    }

    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { addSheet = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
                Text(stringResource(R.string.add_recipe), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                AddOption(Icons.Outlined.Link, stringResource(R.string.add_by_link), stringResource(R.string.add_by_link_sub)) {
                    addSheet = false; nav.navigate(Routes.importLink())
                }
                Hairline()
                AddOption(Icons.Outlined.PhotoCamera, stringResource(R.string.add_from_photo), stringResource(R.string.add_from_photo_sub)) {
                    addSheet = false; nav.navigate(Routes.IMPORT_PICK_PHOTO)
                }
                Hairline()
                AddOption(Icons.Outlined.EditNote, stringResource(R.string.add_manually), stringResource(R.string.add_manually_sub)) {
                    addSheet = false; nav.navigate(Routes.edit(0))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = muted)
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

/** Карточка: фото — главное, под ним название и тихая мета-строка. */
@Composable
fun RecipeCard(r: Recipe, onClick: () -> Unit) {
    val time = minutesText(r.totalTimeMin)
    val kcal = r.kcalPerServing()?.let { stringResource(R.string.kcal_short, it) }
    Column(Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick)) {
        Box {
            RecipeImage(r.image, Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(22.dp)))
            if (r.favorite) {
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, null, tint = Accent, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        Text(
            r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
        )
        val meta = listOfNotNull(time, kcal).joinToString(" · ")
        if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(horizontal = 2.dp))
    }
}
