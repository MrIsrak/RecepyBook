package app.receptnik.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.receptnik.AppContainer
import app.receptnik.container
import app.receptnik.data.Recipe
import coil.compose.SubcomposeAsyncImage
import kotlin.math.roundToInt

@Composable
fun rememberContainer(): AppContainer {
    val ctx = LocalContext.current
    return remember { ctx.container }
}

/** Общий каркас экрана. [tab] — экран с нижней панелью (не добавляем отступ навигации дважды). */
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
        contentWindowInsets = if (tab) WindowInsets.statusBars else WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = actions,
                windowInsets = WindowInsets.statusBars,
            )
        },
        floatingActionButton = fab,
        bottomBar = bottomBar,
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
    ) { pad -> content(Modifier.padding(pad)) }
}

@Composable
fun RecipeImage(path: String?, modifier: Modifier = Modifier) {
    val c = rememberContainer()
    val placeholder: @Composable () -> Unit = {
        Box(
            modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Restaurant, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp),
            )
        }
    }
    if (path.isNullOrBlank()) {
        placeholder()
    } else {
        SubcomposeAsyncImage(
            model = c.images.model(path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { placeholder() },
            error = { placeholder() },
        )
    }
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Restaurant, contentDescription = null,
            modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.padding(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.padding(4.dp))
        Text(
            text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatMinutes(m: Int?): String? {
    if (m == null || m <= 0) return null
    val h = m / 60
    val mm = m % 60
    return when {
        h == 0 -> "$mm мин"
        mm == 0 -> "$h ч"
        else -> "$h ч $mm мин"
    }
}

fun Recipe.kcalPerServing(): Int? =
    nutrition?.takeIf { it.kcal > 0 }?.let { (it.kcal / servings.coerceAtLeast(1)).roundToInt() }

fun shareText(context: Context, text: String, title: String = "Поделиться") {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}

fun recipeAsText(r: Recipe, factor: Double = 1.0, system: app.receptnik.core.UnitSystem = app.receptnik.core.UnitSystem.ORIGINAL): String = buildString {
    appendLine(r.title)
    val servings = (r.servings * factor).roundToInt()
    append("Порций: $servings")
    formatMinutes(r.totalTimeMin)?.let { append(" · $it") }
    appendLine()
    appendLine()
    appendLine("Ингредиенты:")
    r.ingredients.forEach {
        if (it.isHeader) appendLine("\n${it.original}:")
        else appendLine("• " + app.receptnik.core.QtyFormat.formatIngredient(it, factor, system))
    }
    if (r.steps.isNotEmpty()) {
        appendLine()
        appendLine("Приготовление:")
        var n = 1
        r.steps.forEach { s ->
            if (s.endsWith(":")) appendLine("\n$s") else appendLine("${n++}. $s")
        }
    }
    r.sourceUrl?.let { appendLine(); appendLine("Источник: $it") }
}

/** Выбор рецепта из списка с поиском. */
@Composable
fun RecipePickerDialog(
    recipes: List<Recipe>,
    title: String = "Выбери рецепт",
    onDismiss: () -> Unit,
    header: @Composable () -> Unit = {},
    onPick: (Recipe) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                header()
                OutlinedTextField(
                    value = q, onValueChange = { q = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Поиск") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(4.dp))
                val list = recipes.filter { q.isBlank() || it.title.contains(q.trim(), ignoreCase = true) }
                if (list.isEmpty()) Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list, key = { it.id }) { r ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(r) }.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RecipeImage(r.image, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.padding(6.dp))
                            Text(r.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
