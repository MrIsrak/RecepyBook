package app.receptnik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.core.QtyFormat
import app.receptnik.core.UnitSystem
import app.receptnik.data.MealSlot
import app.receptnik.data.Recipe
import kotlinx.coroutines.launch
import java.net.URI
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(id: Long, nav: NavHostController) {
    val c = rememberContainer()
    val recipe by c.repo.recipe(id).collectAsState(initial = null)
    val settings by c.settings.collectAsState()
    val r = recipe ?: run {
        AppScaffold(title = "", onBack = { nav.popBackStack() }) {}
        return
    }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }

    var servings by rememberSaveable(r.id) { mutableIntStateOf(r.servings) }
    var units by rememberSaveable { mutableStateOf(settings.units) }
    val checked = remember(r.id) { mutableStateListOf<Int>() }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var planDialog by remember { mutableStateOf(false) }
    var bookDialog by remember { mutableStateOf(false) }
    val factor = if (r.servings > 0) servings.toDouble() / r.servings else 1.0

    AppScaffold(
        title = "",
        onBack = { nav.popBackStack() },
        snackbar = snackbar,
        actions = {
            IconButton(onClick = { scope.launch { c.repo.toggleFavorite(r) } }) {
                if (r.favorite) Icon(Icons.Filled.Favorite, "Убрать из избранного", tint = MaterialTheme.colorScheme.primary)
                else Icon(Icons.Outlined.FavoriteBorder, "В избранное")
            }
            IconButton(onClick = { shareText(ctx, recipeAsText(r, factor, units), r.title) }) { Icon(Icons.Outlined.Share, "Поделиться") }
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Ещё") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Редактировать") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { menu = false; nav.navigate(Routes.edit(r.id)) },
                )
                DropdownMenuItem(
                    text = { Text("В кулинарную книгу") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null) },
                    onClick = { menu = false; bookDialog = true },
                )
                DropdownMenuItem(
                    text = { Text("Удалить") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                    onClick = { menu = false; confirmDelete = true },
                )
            }
        },
    ) { mod ->
        LazyColumn(mod.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
            item {
                RecipeImage(
                    r.image,
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(24.dp)),
                )
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(r.title, style = MaterialTheme.typography.headlineSmall)
                    r.sourceUrl?.let { src ->
                        val host = runCatching { URI(src).host?.removePrefix("www.") }.getOrNull() ?: src
                        Row(
                            Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { runCatching { uri.openUri(src) } },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                listOfNotNull(r.author, host).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(Icons.Outlined.OpenInNew, null, Modifier.padding(start = 4.dp).size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (r.description.isNotBlank()) {
                        Text(r.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        formatMinutes(r.totalTimeMin)?.let { AssistChip(onClick = {}, label = { Text(it) }, leadingIcon = { Icon(Icons.Outlined.Timer, null, Modifier.size(16.dp)) }) }
                        r.kcalPerServing()?.let { AssistChip(onClick = {}, label = { Text("$it ккал / порция") }) }
                        r.cuisine?.takeIf { it.isNotBlank() }?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        r.mealType?.takeIf { it.isNotBlank() }?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate(Routes.cook(r.id)) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Готовить")
                        }
                        FilledTonalButton(onClick = { planDialog = true }) {
                            Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text("В план")
                        }
                    }
                }
            }

            // ---- Ингредиенты ----
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ингредиенты", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        FilledTonalIconButton(onClick = { if (servings > 1) servings-- }) { Icon(Icons.Outlined.Remove, "Меньше") }
                        Text(
                            "$servings порц.", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        FilledTonalIconButton(onClick = { servings++ }) { Icon(Icons.Outlined.Add, "Больше") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        listOf(UnitSystem.ORIGINAL to "Как в рецепте", UnitSystem.METRIC to "Граммы/мл", UnitSystem.US to "US").forEach { (u, t) ->
                            FilterChip(selected = units == u, onClick = { units = u }, label = { Text(t) })
                        }
                    }
                }
            }
            itemsIndexed(r.ingredients) { i, ing ->
                if (ing.isHeader) {
                    Text(
                        ing.original, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    val done = i in checked
                    Row(
                        Modifier.fillMaxWidth().clickable { if (done) checked.remove(i) else checked.add(i) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = done, onCheckedChange = { if (done) checked.remove(i) else checked.add(i) })
                        val amount = QtyFormat.formatAmount(ing, factor, units)
                        Text(
                            QtyFormat.displayName(ing),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            color = if (done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        )
                        if (amount.isNotBlank()) Text(
                            amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            color = if (done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            c.repo.addRecipeToGrocery(r, servings)
                            snackbar.showSnackbar("Добавлено в список покупок")
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, null); Spacer(Modifier.width(8.dp)); Text("В список покупок")
                }
            }

            // ---- Пищевая ценность ----
            r.nutrition?.takeIf { it.kcal > 0 }?.let { n ->
                item {
                    val per = n / r.servings.coerceAtLeast(1).toDouble()
                    Card(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("На одну порцию", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                NutrientCell("${per.kcal.roundToInt()}", "ккал")
                                NutrientCell("${per.protein.roundToInt()} г", "белки")
                                NutrientCell("${per.fat.roundToInt()} г", "жиры")
                                NutrientCell("${per.carbs.roundToInt()} г", "углеводы")
                            }
                            Text(
                                (r.nutritionNote ?: "Примерная оценка") + " · всего ${n.kcal.roundToInt()} ккал",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            // ---- Шаги ----
            if (r.steps.isNotEmpty()) {
                item {
                    Text("Приготовление", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
                }
                var n = 0
                val numbered = r.steps.map { s -> if (s.endsWith(":")) null to s else (++n) to s }
                itemsIndexed(numbered) { _, (num, text) ->
                    if (num == null) {
                        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    } else {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Text("$num", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp).weight(1f))
                        }
                    }
                }
            }

            if (r.notes.isNotBlank()) {
                item {
                    HorizontalDivider(Modifier.padding(16.dp))
                    Text("Мои заметки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    Text(r.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                }
            }
            if (r.tags.isNotEmpty()) {
                item {
                    FlowRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        r.tags.forEach { t -> AssistChip(onClick = {}, label = { Text("#$t") }) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить рецепт?") },
            text = { Text("«${r.title}» будет удалён вместе с записями в плане.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch { c.repo.delete(r); nav.popBackStack() }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
    if (planDialog) {
        AddToPlanDialog(
            recipe = r, initialServings = servings,
            onDismiss = { planDialog = false },
            onAdd = { day, slot, s ->
                planDialog = false
                scope.launch {
                    c.repo.addToPlan(r, day, slot, s)
                    snackbar.showSnackbar("Добавлено в план: ${slot.title.lowercase()}, ${dayLabel(day)}")
                }
            },
        )
    }
    if (bookDialog) CookbookPickDialog(r, onDismiss = { bookDialog = false })
}

@Composable
private fun NutrientCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

fun dayLabel(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (d) {
        today -> "сегодня"
        today.plusDays(1) -> "завтра"
        else -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")) + ", " + d.dayOfMonth + " " +
            monthGenitive(d.monthValue)
    }
}

fun monthGenitive(m: Int) = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)[m - 1]

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddToPlanDialog(recipe: Recipe, initialServings: Int, onDismiss: () -> Unit, onAdd: (Long, MealSlot, Int) -> Unit) {
    val today = LocalDate.now().toEpochDay()
    var day by remember { mutableStateOf(today) }
    var slot by remember { mutableStateOf(guessSlot(recipe.mealType)) }
    var servings by remember { mutableIntStateOf(initialServings) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить в план") },
        text = {
            Column {
                Text("День", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0L..6L).forEach { off ->
                        val d = today + off
                        FilterChip(selected = day == d, onClick = { day = d }, label = { Text(shortDay(d)) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Приём пищи", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealSlot.entries.forEach { s ->
                        FilterChip(selected = slot == s, onClick = { slot = s }, label = { Text(s.title) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Порций", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { if (servings > 1) servings-- }) { Icon(Icons.Outlined.Remove, null) }
                    Text("$servings", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
                    FilledTonalIconButton(onClick = { servings++ }) { Icon(Icons.Outlined.Add, null) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(day, slot, servings) }) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

fun shortDay(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return when (d) {
        LocalDate.now() -> "Сегодня"
        LocalDate.now().plusDays(1) -> "Завтра"
        else -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")).replaceFirstChar { it.uppercaseChar() } + " " + d.dayOfMonth
    }
}

fun guessSlot(mealType: String?): MealSlot {
    val t = mealType?.lowercase() ?: return MealSlot.DINNER
    return when {
        "завтрак" in t || "breakfast" in t || "brunch" in t -> MealSlot.BREAKFAST
        "обед" in t || "lunch" in t || "суп" in t -> MealSlot.LUNCH
        "перекус" in t || "десерт" in t || "snack" in t || "dessert" in t || "выпечк" in t -> MealSlot.SNACK
        else -> MealSlot.DINNER
    }
}

@Composable
fun CookbookPickDialog(recipe: Recipe, onDismiss: () -> Unit) {
    val c = rememberContainer()
    val scope = rememberCoroutineScope()
    val books by c.repo.cookbooks.collectAsState(initial = emptyList())
    val links by c.repo.cookbookLinks.collectAsState(initial = emptyList())
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Кулинарные книги") },
        text = {
            Column {
                if (books.isEmpty()) Text("Книг пока нет — создай первую:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                books.forEach { b ->
                    val inBook = links.any { it.recipeId == recipe.id && it.cookbookId == b.id }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { scope.launch { c.repo.setInCookbook(recipe.id, b.id, !inBook) } },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = inBook, onCheckedChange = { scope.launch { c.repo.setInCookbook(recipe.id, b.id, it) } })
                        Text("${b.emoji} ${b.name}")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        placeholder = { Text("Новая книга") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) scope.launch {
                            val id = c.repo.createCookbook(name, "📖")
                            c.repo.setInCookbook(recipe.id, id, true)
                            newName = ""
                        }
                    }) { Icon(Icons.Outlined.Add, "Создать") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}
