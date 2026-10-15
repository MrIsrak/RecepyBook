package app.receptnik.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFFD9512C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF6B5D3F),
    secondaryContainer = Color(0xFFF4E2BB),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Color(0xFF3E6A47),
    tertiaryContainer = Color(0xFFC0F0C5),
    background = Color(0xFFFFFBF6),
    surface = Color(0xFFFFFBF6),
    surfaceVariant = Color(0xFFF3E6DD),
    surfaceContainer = Color(0xFFF8EEE7),
    surfaceContainerHigh = Color(0xFFF2E7E0),
    surfaceContainerHighest = Color(0xFFEDE1DA),
    surfaceContainerLow = Color(0xFFFCF4EE),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD8C2B9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB59E),
    onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF842400),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFD7C6A1),
    secondaryContainer = Color(0xFF524629),
    onSecondaryContainer = Color(0xFFF4E2BB),
    tertiary = Color(0xFFA5D4AA),
    tertiaryContainer = Color(0xFF26512F),
    background = Color(0xFF1A1110),
    surface = Color(0xFF1A1110),
    surfaceVariant = Color(0xFF53433D),
    surfaceContainer = Color(0xFF271D1A),
    surfaceContainerHigh = Color(0xFF322824),
    surfaceContainerHighest = Color(0xFF3D322E),
    surfaceContainerLow = Color(0xFF231917),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    )
}

@Composable
fun ReceptnikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
