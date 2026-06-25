package com.aether.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Blue = Color(0xFF2563EB)
private val BlueSoft = Color(0xFFDDE8FF)
private val Indigo = Color(0xFF7C3AED)
private val Green = Color(0xFF059669)
private val Red = Color(0xFFDC2626)

private val LightScheme = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = BlueSoft,
    onPrimaryContainer = Color(0xFF0B2E68),
    secondary = Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FF),
    onSecondaryContainer = Color(0xFF2A1263),
    tertiary = Green,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7FBE8),
    onTertiaryContainer = Color(0xFF052E1B),
    error = Red,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE7ECF4),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF7C8797),
    outlineVariant = Color(0xFFD3DAE5),
    inverseSurface = Color(0xFF111827),
    inverseOnSurface = Color(0xFFF9FAFB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F4FA),
    surfaceContainerHigh = Color(0xFFE9EEF7),
    surfaceContainerHighest = Color(0xFFE0E7F1),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CC2FF),
    onPrimary = Color(0xFF001E48),
    primaryContainer = Color(0xFF103B78),
    onPrimaryContainer = Color(0xFFDDE8FF),
    secondary = Color(0xFFD4C4FF),
    onSecondary = Color(0xFF2B125F),
    secondaryContainer = Color(0xFF43248B),
    onSecondaryContainer = Color(0xFFEDE7FF),
    tertiary = Color(0xFF7EE2B3),
    onTertiary = Color(0xFF003823),
    tertiaryContainer = Color(0xFF075B3C),
    onTertiaryContainer = Color(0xFFD7FBE8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF0B1120),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF243043),
    onSurfaceVariant = Color(0xFFC4CCDA),
    outline = Color(0xFF8B98AA),
    outlineVariant = Color(0xFF334155),
    inverseSurface = Color(0xFFE5E7EB),
    inverseOnSurface = Color(0xFF111827),
    surfaceContainerLowest = Color(0xFF060B14),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainer = Color(0xFF111C31),
    surfaceContainerHigh = Color(0xFF17223A),
    surfaceContainerHighest = Color(0xFF1D2A44),
)

private val BaseTypography = Typography()

private val AetherTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
)

private val AetherShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AetherTypography,
        shapes = AetherShapes,
        content = content
    )
}
