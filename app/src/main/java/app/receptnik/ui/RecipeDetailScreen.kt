package app.receptnik.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.R
import app.receptnik.core.QtyFormat
import app.receptnik.core.UnitSystem
import app.receptnik.data.MealSlot
import app.receptnik.data.Recipe
import kotlinx.coroutines.launch
import java.net.URI
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(id: Long, nav: NavHostController) {
    val c = rememberContainer()
    val recipe by c.repo.recipe(id).collectAsState(initial = null)
    val settings by c.settings.collectAsState()
    val r = recipe ?: run { AppScaffold(title = "", onBack = { nav.popBackStack() }) {}; return }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val lang = uiLang()
    val snackbar = remember { SnackbarHostState() }

    var servings by rememberSaveable(r.id) { mutableIntStateOf(r.servings) }
    var units by rememberSaveable { mutableStateOf(settings.units) }
    val checked = remember(r.id) { mutableStateListOf<Int>() }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var planDialog by remember { mutableStateOf(false) }
    var bookDialog by remember { mutableStateOf(false) }
    val factor = if (r.servings > 0) servings.toDouble() / r.servings else 1.0

    val msgAddedGrocery = stringResource(R.string.added_to_grocery)
    val shareTitle = stringResource(R.string.share)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = pad.calculateBottomPadding() + 40.dp)) {
            // ── Обложка с кнопками поверх ──
            item {
                Box {
                    RecipeImage(
                        r.image,
                        Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                    )
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GlassIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), { nav.popBackStack() })
                        Spacer(Modifier.weight(1f))
                        GlassIconButton(
                            if (r.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            stringResource(R.string.favorite),
                            { scope.launch { c.repo.toggleFavorite(r) } },
                            tint = if (r.favorite) Accent else null,
                        )
                        GlassIconButton(Icons.Outlined.Share, shareTitle, {
                            shareText(ctx, recipeAsText(ctx.resources, r, factor, units, lang), shareTitle)
                        })
                        Box {
                            GlassIconButton(Icons.Outlined.MoreVert, stringResource(R.string.more), { menu = true })
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                    onClick = { menu = false; nav.navigate(Routes.edit(r.id)) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.to_cookbook)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null) },
                                    onClick = { menu = false; bookDialog = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { menu = false; confirmDelete = true },
                                )
                            }
                        }
                    }
                }
            }

            // ── Заголовок ──
            item {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                    r.sourceUrl?.let { src ->
                        val host = runCatching { URI(src).host?.removePrefix("www.") }.getOrNull() ?: src
                        Text(
                            listOfNotNull(r.author, host).joinToString(" · ").uppercase(),
                            style = MaterialTheme.typography.labelSmall, color = muted,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { runCatching { uri.openUri(src) } }
                                .padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(r.title, style = MaterialTheme.typography.headlineLarge)
                    val meta = listOfNotNull(
                        minutesText(r.totalTimeMin),
                        r.kcalPerServing()?.let { stringResource(R.string.kcal_per_serving, it) },
                        r.cuisine?.takeIf { it.isNotBlank() },
                    ).joinToString("  ·  ")
                    if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = muted, modifier = Modifier.padding(top = 8.dp))
                    if (r.description.isNotBlank()) {
                        Text(r.description, style = MaterialTheme.typography.bodyLarge, color = muted, modifier = Modifier.padding(top = 14.dp))
                    }
                    Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { nav.navigate(Routes.cook(r.id)) },
                            modifier = Modifier.weight(1f).height(52.dp), shape = CircleShape,
                        ) { Text(stringResource(R.string.start_cooking)) }
                        OutlinedButton(
                            onClick = { planDialog = true }, modifier = Modifier.height(52.dp), shape = CircleShape,
                            border = BorderStroke(1.dp, hairline),
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.to_plan))
                        }
                    }
                }
            }

            // ── Ингредиенты ──
            item {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.ingredients), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        Stepper(
                            value = pluralStringResource(R.plurals.servings_count, servings, servings),
                            onMinus = { if (servings > 1) servings-- }, onPlus = { servings++ },
                        )
                    }
                    Row(Modifier.padding(top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            UnitSystem.ORIGINAL to R.string.units_original,
                            UnitSystem.METRIC to R.string.units_metric,
                            UnitSystem.US to R.string.units_us,
                        ).forEach { (u, t) -> Pill(stringResource(t), selected = units == u, onClick = { units = u }) }
                    }
                }
            }
            itemsIndexed(r.ingredients) { i, ing ->
                if (ing.isHeader) {
                    SectionLabel(ing.original, Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp))
                } else {
                    val done = i in checked
                    val color = if (done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { if (done) checked.remove(i) else checked.add(i) }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckDot(done)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                QtyFormat.displayName(ing), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge, color = color,
                                textDecoration = if (done) TextDecoration.LineThrough else null,
                            )
                            val amount = QtyFormat.formatAmount(ing, factor, units, lang)
                            if (amount.isNotBlank()) Text(
                                amount, style = MaterialTheme.typography.bodyLarge, color = if (done) color else muted,
                                modifier = Modifier.padding(start = 12.dp).widthIn(max = 160.dp),
                            )
                        }
                        Hairline()
                    }
                }
            }
            item {
                TextButton(
                    onClick = { scope.launch { c.repo.addRecipeToGrocery(r, servings); snackbar.showSnackbar(msgAddedGrocery) } },
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_grocery))
                }
            }

            // ── Пищевая ценность ──
            r.nutrition?.takeIf { it.kcal > 0 }?.let { n ->
                item {
                    val per = n / r.servings.coerceAtLeast(1).toDouble()
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
                        SectionLabel(stringResource(R.string.per_serving))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Nutrient("${per.kcal.roundToInt()}", stringResource(R.string.kcal), Modifier.weight(1f), accent = true)
                            Nutrient("${per.protein.roundToInt()}", stringResource(R.string.protein_g), Modifier.weight(1f))
                            Nutrient("${per.fat.roundToInt()}", stringResource(R.string.fat_g), Modifier.weight(1f))
                            Nutrient("${per.carbs.roundToInt()}", stringResource(R.string.carbs_g), Modifier.weight(1f))
                        }
                        Text(
                            nutritionNoteText(r.nutritionNote) + " · " + stringResource(R.string.kcal_total, n.kcal.roundToInt()),
                            style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            // ── Шаги ──
            if (r.steps.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.method), style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 8.dp),
                    )
                }
                var n = 0
                val numbered = r.steps.map { s -> if (s.endsWith(":")) null to s else (++n) to s }
                itemsIndexed(numbered) { _, (num, text) ->
                    if (num == null) {
                        SectionLabel(text, Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
                    } else {
                        Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                            Text(
                                num.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge, color = Accent,
                                modifier = Modifier.width(40.dp),
                            )
                            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (r.notes.isNotBlank()) {
                item {
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
                        SectionLabel(stringResource(R.string.my_notes))
                        Text(r.notes, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            if (r.tags.isNotEmpty()) {
                item {
                    FlowRow(
                        Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) { r.tags.forEach { t -> Text("#$t", style = MaterialTheme.typography.labelLarge, color = muted) } }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text(stringResource(R.string.delete_recipe_q)) },
            text = { Text(stringResource(R.string.delete_recipe_text, r.title)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { c.repo.delete(r); nav.popBackStack() } }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (planDialog) {
        val addedTpl = stringResource(R.string.added_to_plan)
        val res = ctx.resources
        AddToPlanDialog(
            recipe = r, initialServings = servings, onDismiss = { planDialog = false },
            onAdd = { day, slot, s, slotName, dayName ->
                planDialog = false
                scope.launch { c.repo.addToPlan(r, day, slot, s); snackbar.showSnackbar(String.format(addedTpl, slotName, dayName)) }
            },
        )
    }
    if (bookDialog) CookbookPickDialog(r, onDismiss = { bookDialog = false })
}

@Composable
private fun Nutrient(value: String, label: String, modifier: Modifier, accent: Boolean = false) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = if (accent) Accent else MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

/** Отметка «уже положил»: пустой кружок → залитый с галочкой. */
@Composable
fun CheckDot(done: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(22.dp).clip(CircleShape)
            .background(if (done) cs.primary else cs.background)
            .border(1.5.dp, if (done) cs.primary else cs.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (done) Icon(Icons.Filled.Check, null, tint = cs.onPrimary, modifier = Modifier.size(14.dp)) }
}

@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.Outlined.Remove, onMinus)
        Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 10.dp))
        RoundIcon(Icons.Outlined.Add, onPlus)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, border = BorderStroke(1.dp, hairline), color = MaterialTheme.colorScheme.background,
        modifier = Modifier.size(34.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddToPlanDialog(
    recipe: Recipe,
    initialServings: Int,
    onDismiss: () -> Unit,
    onAdd: (day: Long, slot: MealSlot, servings: Int, slotName: String, dayName: String) -> Unit,
) {
    val today = LocalDate.now().toEpochDay()
    var day by remember { mutableStateOf(today) }
    var slot by remember { mutableStateOf(guessSlot(recipe.mealType)) }
    var servings by remember { mutableIntStateOf(initialServings) }
    val slotName = slotLabel(slot)
    val dayName = dayTitle(day)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(stringResource(R.string.add_to_plan)) },
        text = {
            Column {
                SectionLabel(stringResource(R.string.day))
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0L..6L).forEach { off -> val d = today + off; Pill(dayTitle(d), selected = day == d, onClick = { day = d }) }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.meal))
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealSlot.entries.forEach { s -> Pill(slotLabel(s), selected = slot == s, onClick = { slot = s }) }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.servings), Modifier.weight(1f))
                    Stepper(servings.toString(), onMinus = { if (servings > 1) servings-- }, onPlus = { servings++ })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(day, slot, servings, slotName.lowercase(), dayName.lowercase()) }) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

fun guessSlot(mealType: String?): MealSlot {
    val t = mealType?.lowercase() ?: return MealSlot.DINNER
    return when {
        listOf("завтрак", "breakfast", "brunch", "בוקר").any { it in t } -> MealSlot.BREAKFAST
        listOf("обед", "lunch", "суп", "soup", "צהריים", "מרק").any { it in t } -> MealSlot.LUNCH
        listOf("перекус", "десерт", "выпечк", "snack", "dessert", "קינוח", "נשנוש", "חטיף").any { it in t } -> MealSlot.SNACK
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
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(stringResource(R.string.cookbooks)) },
        text = {
            Column {
                if (books.isEmpty()) Text(stringResource(R.string.no_books_create_first), color = muted)
                books.forEach { b ->
                    val inBook = links.any { it.recipeId == recipe.id && it.cookbookId == b.id }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable { scope.launch { c.repo.setInCookbook(recipe.id, b.id, !inBook) } },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = inBook, onCheckedChange = { scope.launch { c.repo.setInCookbook(recipe.id, b.id, it) } })
                        Text("${b.emoji}  ${b.name}")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        placeholder = { Text(stringResource(R.string.new_cookbook)) }, singleLine = true,
                        shape = CircleShape, colors = softFieldColors(), modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) scope.launch {
                            val id = c.repo.createCookbook(name, "📖")
                            c.repo.setInCookbook(recipe.id, id, true)
                            newName = ""
                        }
                    }) { Icon(Icons.Outlined.Add, stringResource(R.string.create)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}
