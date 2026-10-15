package app.receptnik.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.receptnik.core.QtyFormat
import kotlinx.coroutines.launch

/** Пошаговый режим готовки: крупный текст, экран не гаснет, таймеры из шагов. */
@Composable
fun CookModeScreen(id: Long, onBack: () -> Unit) {
    val c = rememberContainer()
    val recipe by c.repo.recipe(id).collectAsState(initial = null)
    val settings by c.settings.collectAsState()
    val view = LocalView.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val r = recipe ?: run { AppScaffold("Готовим", onBack = onBack) {}; return }
    val steps = r.steps.filterNot { it.endsWith(":") }
    val pages = 1 + steps.size + 1 // ингредиенты, шаги, финал
    val pager = rememberPagerState { pages }

    AppScaffold(title = r.title, onBack = onBack) { mod ->
        Column(mod.fillMaxSize()) {
            LinearProgressIndicator(
                progress = { (pager.currentPage + 1).toFloat() / pages },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            HorizontalPager(pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                ) {
                    when {
                        page == 0 -> {
                            Text("Подготовь", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            r.ingredients.forEach { ing ->
                                if (ing.isHeader) {
                                    Text(ing.original, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                                } else Text(
                                    "• " + QtyFormat.formatIngredient(ing, 1.0, settings.units),
                                    fontSize = 22.sp, lineHeight = 30.sp, modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                        page <= steps.size -> {
                            val text = steps[page - 1]
                            Text(
                                "Шаг $page из ${steps.size}", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(text, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium)
                            timerMinutes(text)?.let { min ->
                                Spacer(Modifier.height(24.dp))
                                FilledTonalButton(onClick = {
                                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                                        putExtra(AlarmClock.EXTRA_LENGTH, min * 60)
                                        putExtra(AlarmClock.EXTRA_MESSAGE, r.title)
                                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                                    }
                                    try { ctx.startActivity(intent) } catch (e: ActivityNotFoundException) {
                                        Toast.makeText(ctx, "Нет приложения часов", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Outlined.Timer, null); Spacer(Modifier.width(8.dp)); Text("Таймер на $min мин")
                                }
                            }
                        }
                        else -> {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(48.dp))
                                Text("Приятного аппетита!", style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { scope.launch { c.repo.markCooked(r); onBack() } }) {
                                    Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(8.dp)); Text("Готово")
                                }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0, modifier = Modifier.weight(1f).height(56.dp),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Назад") }
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = pager.currentPage < pages - 1, modifier = Modifier.weight(1f).height(56.dp),
                ) { Text("Дальше"); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
            }
        }
    }
}

/** «варить 15 минут» → 15; «1 час» → 60; «1,5 часа» → 90. */
fun timerMinutes(text: String): Int? {
    val m = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:-|–)?\\s*(?:\\d+\\s*)?(мин|min|דק|час|ч\\b|hour|hr|שע)", RegexOption.IGNORE_CASE).find(text) ?: return null
    val n = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val u = m.groupValues[2].lowercase()
    val minutes = if (u.startsWith("ч") || u.startsWith("h") || u.startsWith("שע")) n * 60 else n
    return minutes.toInt().takeIf { it in 1..600 }
}
