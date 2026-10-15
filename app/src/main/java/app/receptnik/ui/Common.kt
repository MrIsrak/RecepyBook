package app.receptnik.ui

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.receptnik.AppContainer
import app.receptnik.R
import app.receptnik.container
import app.receptnik.core.Aisles
import app.receptnik.core.Lang
import app.receptnik.core.QtyFormat
import app.receptnik.core.UnitSystem
import app.receptnik.data.MealSlot
import app.receptnik.data.Recipe
import app.receptnik.importer.NutritionNote
import coil.compose.SubcomposeAsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun rememberContainer(): AppContainer {
    val ctx = LocalContext.current
    return remember { ctx.container }
}

// ───────────────────────── Язык и форматирование ─────────────────────────

/** Язык интерфейса для ядра: "ru" | "en" | "he". */
@Composable
fun uiLang(): String = Lang.of(LocalConfiguration.current.locales[0].language)

@Composable
fun uiLocale(): Locale = LocalConfiguration.current.locales[0]

fun formatMinutes(res: Resources, m: Int?): String? {
    if (m == null || m <= 0) return null
    val h = m / 60
    val mm = m % 60
    return when {
        h == 0 -> res.getString(R.string.time_min, mm)
        mm == 0 -> res.getString(R.string.time_h, h)
        else -> res.getString(R.string.time_h_min, h, mm)
    }
}

@Composable
fun minutesText(m: Int?): String? = formatMinutes(LocalContext.current.resources, m)

@Composable
fun recipesCount(n: Int): String = pluralStringResource(R.plurals.recipes_count, n, n)

@Composable
fun aisleLabel(key: String): String = stringResource(
    when (Aisles.normalize(key)) {
        Aisles.VEG -> R.string.aisle_veg
        Aisles.MEAT -> R.string.aisle_meat
        Aisles.DAIRY -> R.string.aisle_dairy
        Aisles.GROCERY -> R.string.aisle_grocery
        Aisles.BAKERY -> R.string.aisle_bakery
        Aisles.SPICES -> R.string.aisle_spices
        Aisles.NUTS -> R.string.aisle_nuts
        Aisles.SWEETS -> R.string.aisle_sweets
        Aisles.DRINKS -> R.string.aisle_drinks
        else -> R.string.aisle_other
    }
)

@Composable
fun slotLabel(slot: MealSlot): String = stringResource(
    when (slot) {
        MealSlot.BREAKFAST -> R.string.slot_breakfast
        MealSlot.LUNCH -> R.string.slot_lunch
        MealSlot.DINNER -> R.string.slot_dinner
        MealSlot.SNACK -> R.string.slot_snack
    }
)

/** Пояснение к КБЖУ: коды из импорта или текст из версии 1.0. */
@Composable
fun nutritionNoteText(note: String?): String = when {
    note == null -> stringResource(R.string.nutrition_estimate)
    note == NutritionNote.SITE -> stringResource(R.string.nutrition_site)
    note == NutritionNote.AI -> stringResource(R.string.nutrition_ai)
    note == NutritionNote.ALL -> stringResource(R.string.nutrition_all)
    note.startsWith("calc:") -> {
        val (a, b) = note.removePrefix("calc:").split("/").map { it.toIntOrNull() ?: 0 }.let { it.getOrElse(0) { 0 } to it.getOrElse(1) { 0 } }
        stringResource(R.string.nutrition_partial, a, b)
    }
    else -> note
}

fun Recipe.kcalPerServing(): Int? =
    nutrition?.takeIf { it.kcal > 0 }?.let { (it.kcal / servings.coerceAtLeast(1)).roundToInt() }

@Composable
fun dayTitle(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (d) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM", uiLocale())).replaceFirstChar { it.uppercaseChar() }
    }
}

fun shareText(context: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}

fun recipeAsText(res: Resources, r: Recipe, factor: Double, system: UnitSystem, lang: String): String = buildString {
    appendLine(r.title)
    val servings = (r.servings * factor).roundToInt()
    append(res.getQuantityString(R.plurals.servings_count, servings, servings))
    formatMinutes(res, r.totalTimeMin)?.let { append(" · $it") }
    appendLine()
    appendLine()
    appendLine(res.getString(R.string.ingredients))
    r.ingredients.forEach {
        if (it.isHeader) appendLine("\n${it.original}:")
        else appendLine("• " + QtyFormat.formatIngredient(it, factor, system, lang))
    }
    if (r.steps.isNotEmpty()) {
        appendLine()
        appendLine(res.getString(R.string.method))
        var n = 1
        r.steps.forEach { s -> if (s.endsWith(":")) appendLine("\n$s") else appendLine("${n++}. $s") }
    }
    r.sourceUrl?.let { appendLine(); appendLine(res.getString(R.string.source_label, it)) }
}

// ───────────────────────── Компоненты ─────────────────────────

/** Каркас экрана: прозрачная шапка, крупный заголовок на вкладках. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    tab: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbar: SnackbarHostState? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (tab) WindowInsets.statusBars else WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = if (tab) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = actions,
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = fab,
        bottomBar = bottomBar,
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
    ) { pad -> content(Modifier.padding(pad)) }
}

/** Капсула-фильтр: выбранная — залитая графитом, остальные — тонкая обводка. */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) cs.primary else cs.background,
        contentColor = if (selected) cs.onPrimary else cs.onSurface,
        border = if (selected) null else BorderStroke(1.dp, cs.outlineVariant),
        modifier = modifier.heightIn(min = 36.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Небольшой подзаголовок раздела капслоком. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(uiLocale()),
        style = MaterialTheme.typography.labelSmall,
        color = muted,
        modifier = modifier,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 1.dp, color = hairline)

/** Поле ввода в стиле приложения: мягкая заливка без рамки. */
@Composable
fun softFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

@Composable
fun RecipeImage(path: String?, modifier: Modifier = Modifier) {
    val c = rememberContainer()
    val placeholder: @Composable () -> Unit = {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Restaurant, null, tint = muted.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
        }
    }
    if (path.isNullOrBlank()) placeholder()
    else SubcomposeAsyncImage(
        model = c.images.model(path),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { placeholder() },
        error = { placeholder() },
    )
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Accent))
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = muted)
    }
}

/** Строка рецепта для списков: миниатюра, название, мета. */
@Composable
fun RecipeRow(r: Recipe, subtitle: String? = null, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val time = minutesText(r.totalTimeMin)
    val kcal = r.kcalPerServing()?.let { stringResource(R.string.kcal_short, it) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeImage(r.image, Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = subtitle ?: listOfNotNull(time, kcal).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = muted)
        }
        trailing()
    }
}

@Composable
fun RecipePickerDialog(
    recipes: List<Recipe>,
    title: String,
    onDismiss: () -> Unit,
    header: @Composable () -> Unit = {},
    onPick: (Recipe) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(title) },
        text = {
            Column {
                header()
                OutlinedTextField(
                    value = q, onValueChange = { q = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text(stringResource(R.string.search)) }, singleLine = true,
                    shape = CircleShape, colors = softFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                val list = recipes.filter { q.isBlank() || it.title.contains(q.trim(), ignoreCase = true) }
                if (list.isEmpty()) Text(stringResource(R.string.nothing_found), color = muted)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list, key = { it.id }) { r -> RecipeRow(r, onClick = { onPick(r) }) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Круглая полупрозрачная кнопка поверх фото. */
@Composable
fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color? = null, size: Dp = 44.dp) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}
