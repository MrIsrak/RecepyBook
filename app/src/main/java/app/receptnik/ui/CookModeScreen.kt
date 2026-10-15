package app.receptnik.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.receptnik.R
import app.receptnik.core.QtyFormat
import kotlinx.coroutines.launch

/** Режим готовки: один шаг на экран, крупный текст, экран не гаснет. */
@Composable
fun CookModeScreen(id: Long, onBack: () -> Unit) {
    val c = rememberContainer()
    val recipe by c.repo.recipe(id).collectAsState(initial = null)
    val settings by c.settings.collectAsState()
    val view = LocalView.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = uiLang()

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val r = recipe ?: run { AppScaffold("", onBack = onBack) {}; return }
    val steps = r.steps.filterNot { it.endsWith(":") }
    val pages = 1 + steps.size + 1
    val pager = rememberPagerState { pages }
    val noClock = stringResource(R.string.no_clock_app)

    AppScaffold(title = r.title, onBack = onBack) { mod ->
        Column(mod.fillMaxSize()) {
            LinearProgressIndicator(
                progress = { (pager.currentPage + 1).toFloat() / pages },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(3.dp),
                color = Accent, trackColor = hairline, strokeCap = StrokeCap.Round, gapSize = 0.dp,
                drawStopIndicator = {},
            )
            HorizontalPager(pager, modifier = Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 32.dp)) {
                    when {
                        page == 0 -> {
                            SectionLabel(stringResource(R.string.prepare))
                            Spacer(Modifier.height(20.dp))
                            r.ingredients.forEach { ing ->
                                if (ing.isHeader) SectionLabel(ing.original, Modifier.padding(top = 16.dp, bottom = 4.dp))
                                else {
                                    Row(Modifier.padding(vertical = 10.dp)) {
                                        Text(QtyFormat.displayName(ing), fontSize = 21.sp, lineHeight = 28.sp, modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge)
                                        Text(QtyFormat.formatAmount(ing, 1.0, settings.units, lang), fontSize = 21.sp, color = muted,
                                            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                                    }
                                    Hairline()
                                }
                            }
                        }
                        page <= steps.size -> {
                            val text = steps[page - 1]
                            Text(
                                page.toString().padStart(2, '0'), style = MaterialTheme.typography.displaySmall, color = Accent,
                            )
                            Text(stringResource(R.string.step_of, page, steps.size), style = MaterialTheme.typography.labelMedium, color = muted)
                            Spacer(Modifier.height(24.dp))
                            Text(text, fontSize = 27.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyLarge)
                            timerMinutes(text)?.let { min ->
                                Spacer(Modifier.height(32.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                                            putExtra(AlarmClock.EXTRA_LENGTH, min * 60)
                                            putExtra(AlarmClock.EXTRA_MESSAGE, r.title)
                                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                                        }
                                        try { ctx.startActivity(intent) } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(ctx, noClock, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = CircleShape, border = BorderStroke(1.dp, hairline),
                                ) {
                                    Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp), tint = Accent)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.timer_for, min))
                                }
                            }
                        }
                        else -> {
                            Spacer(Modifier.height(80.dp))
                            Text(stringResource(R.string.bon_appetit), style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { scope.launch { c.repo.markCooked(r); onBack() } }, shape = CircleShape,
                                modifier = Modifier.height(52.dp)) {
                                Text(stringResource(R.string.done))
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0, modifier = Modifier.weight(1f).height(56.dp),
                    shape = CircleShape, border = BorderStroke(1.dp, hairline),
                ) { Text(stringResource(R.string.prev)) }
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = pager.currentPage < pages - 1, modifier = Modifier.weight(1f).height(56.dp), shape = CircleShape,
                ) { Text(stringResource(R.string.next)) }
            }
        }
    }
}

/** «варить 15 минут» → 15; «1 час» → 60; «1,5 часа» → 90; «20 דקות» → 20. */
fun timerMinutes(text: String): Int? {
    val m = Regex(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:-|–)?\\s*(?:\\d+\\s*)?(мин|min|דק|час|ч\\b|hour|hr|שע)",
        RegexOption.IGNORE_CASE,
    ).find(text) ?: return null
    val n = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val u = m.groupValues[2].lowercase()
    val minutes = if (u.startsWith("ч") || u.startsWith("h") || u.startsWith("שע")) n * 60 else n
    return minutes.toInt().takeIf { it in 1..600 }
}
