package app.receptnik.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.receptnik.R

/*
 * Дизайн: минимализм. Тёплый белый фон, графитовый текст, один акцент (коралл),
 * тонкие линии вместо теней, крупная типографика, фото — главный элемент.
 * Шрифт Rubik (OFL) покрывает латиницу, кириллицу и иврит.
 */

/** Единственный акцентный цвет. */
val Accent = Color(0xFFFF5B37)

private val LightColors = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECEBE6),
    onPrimaryContainer = Color(0xFF141414),
    secondary = Color(0xFF6E6C66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECEBE6),
    onSecondaryContainer = Color(0xFF141414),
    tertiary = Accent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6DF),
    onTertiaryContainer = Color(0xFF5C1A08),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFF7F6F2),
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFECEBE6),
    onSurfaceVariant = Color(0xFF6E6C66),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F0EB),
    surfaceContainerHigh = Color(0xFFECEBE6),
    surfaceContainerHighest = Color(0xFFE5E3DD),
    outline = Color(0xFFB9B6AE),
    outlineVariant = Color(0xFFE3E1DA),
    error = Color(0xFFC62828),
    errorContainer = Color(0xFFFDE7E4),
    onErrorContainer = Color(0xFF5F1111),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F1EC),
    onPrimary = Color(0xFF141414),
    primaryContainer = Color(0xFF242422),
    onPrimaryContainer = Color(0xFFF2F1EC),
    secondary = Color(0xFF9C9A93),
    onSecondary = Color(0xFF141414),
    secondaryContainer = Color(0xFF242422),
    onSecondaryContainer = Color(0xFFF2F1EC),
    tertiary = Accent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A1A0D),
    onTertiaryContainer = Color(0xFFFFD9CF),
    background = Color(0xFF0F0F0E),
    onBackground = Color(0xFFF2F1EC),
    surface = Color(0xFF0F0F0E),
    onSurface = Color(0xFFF2F1EC),
    surfaceVariant = Color(0xFF1C1C1A),
    onSurfaceVariant = Color(0xFF9C9A93),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF161615),
    surfaceContainer = Color(0xFF1A1A19),
    surfaceContainerHigh = Color(0xFF222220),
    surfaceContainerHighest = Color(0xFF2A2A28),
    outline = Color(0xFF55534E),
    outlineVariant = Color(0xFF2A2A28),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF3B1412),
    onErrorContainer = Color(0xFFFFDAD6),
)

@OptIn(ExperimentalTextApi::class)
private fun rubik(weight: FontWeight) = Font(
    R.font.rubik,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Rubik = FontFamily(
    rubik(FontWeight.Light),
    rubik(FontWeight.Normal),
    rubik(FontWeight.Medium),
    rubik(FontWeight.SemiBold),
    rubik(FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Rubik, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp,
)

private val AppTypography = Typography(
    displaySmall = style(34, 40, FontWeight.SemiBold, -0.8),
    headlineLarge = style(30, 36, FontWeight.SemiBold, -0.6),
    headlineMedium = style(26, 32, FontWeight.SemiBold, -0.5),
    headlineSmall = style(22, 28, FontWeight.SemiBold, -0.3),
    titleLarge = style(19, 26, FontWeight.Medium, -0.2),
    titleMedium = style(16, 22, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 21, FontWeight.Normal),
    bodySmall = style(12, 17, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium, 0.2),
    labelSmall = style(11, 14, FontWeight.Medium, 0.6),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ReceptnikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** Вторичный (приглушённый) цвет текста. */
val muted: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant

/** Цвет тонких разделителей. */
val hairline: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
