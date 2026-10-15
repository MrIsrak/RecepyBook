package app.receptnik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.receptnik.R
import app.receptnik.data.MealSlot
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealPlanScreen(nav: NavHostController) {
    val c = rememberContainer()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val locale = uiLocale()
    val firstDay: DayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val today = LocalDate.now()
    val thisWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDay)).toEpochDay()
    var weekStart by rememberSaveable { mutableStateOf(thisWeek) }
    val from = weekStart
    val to = weekStart + 6
    val planned by remember(from) { c.repo.plan(from, to) }.collectAsState(initial = emptyList())
    val recipes by c.repo.recipes.collectAsState(initial = emptyList())
    var addFor by remember { mutableStateOf<Long?>(null) }
    var addSlot by remember { mutableStateOf(MealSlot.DINNER) }
    val msgEmpty = stringResource(R.string.plan_week_empty)
    val msgAdded = stringResource(R.string.plan_week_added)

    AppScaffold(
        title = stringResource(R.string.tab_plan),
        tab = true,
        snackbar = snackbar,
        actions = {
            IconButton(onClick = {
                scope.launch {
                    val n = c.repo.addPlanToGrocery(from, to)
                    snackbar.showSnackbar(if (n == 0) msgEmpty else msgAdded)
                }
            }) { Icon(Icons.Outlined.AddShoppingCart, stringResource(R.string.week_to_grocery)) }
        },
    ) { mod ->
        LazyColumn(mod.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            item {
                val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    IconButton(onClick = { weekStart -= 7 }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.prev_week)) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${LocalDate.ofEpochDay(from).format(fmt)} — ${LocalDate.ofEpochDay(to).format(fmt)}",
                            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                        )
                        if (weekStart != thisWeek) {
                            TextButton(onClick = { weekStart = thisWeek }) { Text(stringResource(R.string.this_week), color = Accent) }
                        }
                    }
                    IconButton(onClick = { weekStart += 7 }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.next_week)) }
                }
            }
            items((from..to).toList(), key = { it }) { day ->
                val date = LocalDate.ofEpochDay(day)
                val meals = planned.filter { it.entry.epochDay == day }.sortedBy { it.entry.slot.ordinal }
                val kcal = meals.sumOf { pm -> (pm.recipe.nutrition?.kcal ?: 0.0) / pm.recipe.servings.coerceAtLeast(1) }
                val isToday = date == today
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Large day number; today is marked with an accent dot
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineMedium,
                            color = if (isToday) Accent else MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(48.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(date.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercaseChar() },
                                    style = MaterialTheme.typography.titleMedium)
                                if (isToday) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(Accent))
                                }
                            }
                            if (kcal > 0) Text(stringResource(R.string.kcal_per_person, kcal.roundToInt()),
                                style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                        IconButton(onClick = { addFor = day }) { Icon(Icons.Outlined.Add, stringResource(R.string.add_dish)) }
                    }
                    meals.forEach { pm ->
                        Box(Modifier.padding(start = 40.dp)) {
                            RecipeRow(
                                pm.recipe,
                                subtitle = slotLabel(pm.entry.slot) + " · " + stringResource(R.string.servings_short, pm.entry.servings),
                                onClick = { nav.navigate(Routes.recipe(pm.recipe.id)) },
                            ) {
                                IconButton(onClick = { scope.launch { c.repo.removeFromPlan(pm.entry) } }) {
                                    Icon(Icons.Outlined.Close, stringResource(R.string.remove), tint = muted, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Hairline()
                }
            }
        }
    }

    addFor?.let { day ->
        if (recipes.isEmpty()) {
            AlertDialog(
                onDismissRequest = { addFor = null },
                containerColor = MaterialTheme.colorScheme.background,
                title = { Text(stringResource(R.string.no_recipes)) },
                text = { Text(stringResource(R.string.save_one_first)) },
                confirmButton = { TextButton(onClick = { addFor = null }) { Text(stringResource(R.string.ok)) } },
            )
        } else {
            RecipePickerDialog(
                recipes = recipes,
                title = dayTitle(day),
                onDismiss = { addFor = null },
                header = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealSlot.entries.forEach { s -> Pill(slotLabel(s), selected = addSlot == s, onClick = { addSlot = s }) }
                    }
                    Spacer(Modifier.height(10.dp))
                },
            ) { r ->
                addFor = null
                scope.launch { c.repo.addToPlan(r, day, addSlot) }
            }
        }
    }
}
