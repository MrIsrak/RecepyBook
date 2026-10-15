package app.receptnik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.data.MealSlot
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealPlanScreen(nav: NavHostController) {
    val c = rememberContainer()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val today = LocalDate.now()
    var weekStart by rememberSaveable { mutableStateOf(today.with(DayOfWeek.MONDAY).toEpochDay()) }
    val from = weekStart
    val to = weekStart + 6
    val planned by remember(from) { c.repo.plan(from, to) }.collectAsState(initial = emptyList())
    val recipes by c.repo.recipes.collectAsState(initial = emptyList())
    var addFor by remember { mutableStateOf<Long?>(null) }
    var addSlot by remember { mutableStateOf(MealSlot.DINNER) }

    AppScaffold(
        title = "План питания",
        tab = true,
        snackbar = snackbar,
        actions = {
            IconButton(onClick = {
                scope.launch {
                    val n = c.repo.addPlanToGrocery(from, to)
                    snackbar.showSnackbar(if (n == 0) "На этой неделе пока ничего не запланировано" else "Ингредиенты недели добавлены в покупки")
                }
            }) { Icon(Icons.Outlined.AddShoppingCart, "Всё в список покупок") }
        },
    ) { mod ->
        LazyColumn(mod.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { weekStart -= 7 }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Прошлая неделя") }
                    val a = LocalDate.ofEpochDay(from); val b = LocalDate.ofEpochDay(to)
                    Text(
                        "${a.dayOfMonth} ${monthGenitive(a.monthValue)} — ${b.dayOfMonth} ${monthGenitive(b.monthValue)}",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = { weekStart += 7 }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Следующая неделя") }
                }
                if (weekStart != today.with(DayOfWeek.MONDAY).toEpochDay()) {
                    TextButton(onClick = { weekStart = today.with(DayOfWeek.MONDAY).toEpochDay() }, modifier = Modifier.fillMaxWidth()) {
                        Text("К текущей неделе")
                    }
                }
            }
            items((from..to).toList(), key = { it }) { day ->
                val date = LocalDate.ofEpochDay(day)
                val meals = planned.filter { it.entry.epochDay == day }.sortedBy { it.entry.slot.ordinal }
                val kcal = meals.sumOf { pm ->
                    val r = pm.recipe
                    (r.nutrition?.kcal ?: 0.0) / r.servings.coerceAtLeast(1)
                }
                val isToday = date == today
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru")).replaceFirstChar { it.uppercaseChar() } +
                                        (if (isToday) " · сегодня" else ""),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "${date.dayOfMonth} ${monthGenitive(date.monthValue)}" +
                                        (if (kcal > 0) " · ≈${kcal.roundToInt()} ккал на человека" else ""),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { addFor = day }) { Icon(Icons.Outlined.Add, "Добавить блюдо") }
                        }
                        meals.forEach { pm ->
                            RecipeRow(
                                pm.recipe,
                                subtitle = "${pm.entry.slot.title} · ${pm.entry.servings} порц.",
                                onClick = { nav.navigate(Routes.recipe(pm.recipe.id)) },
                            ) {
                                IconButton(onClick = { scope.launch { c.repo.removeFromPlan(pm.entry) } }) {
                                    Icon(Icons.Outlined.Close, "Убрать", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    addFor?.let { day ->
        if (recipes.isEmpty()) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { addFor = null },
                title = { Text("Нет рецептов") },
                text = { Text("Сначала сохрани хотя бы один рецепт.") },
                confirmButton = { TextButton(onClick = { addFor = null }) { Text("Понятно") } },
            )
        } else {
            RecipePickerDialog(
                recipes = recipes,
                title = shortDay(day),
                onDismiss = { addFor = null },
                header = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealSlot.entries.forEach { s ->
                            FilterChip(selected = addSlot == s, onClick = { addSlot = s }, label = { Text(s.title) })
                        }
                    }
                    Spacer(Modifier.height(4.dp).width(1.dp))
                },
            ) { r ->
                addFor = null
                scope.launch { c.repo.addToPlan(r, day, addSlot) }
            }
        }
    }
}
