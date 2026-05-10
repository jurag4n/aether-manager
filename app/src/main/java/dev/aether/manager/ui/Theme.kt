package dev.aether.manager.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme as CupertinoComposeTheme

/**
 * App-wide visual preset.
 * DEFAULT keeps Android Material 3 behavior; MIUI and IOS are Android-native inspired skins.
 */
enum class AetherThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
) {
    DEFAULT(
        id = "default",
        title = "Default Android",
        subtitle = "Material 3, dynamic color, clean Android look"
    ),
    MIUI(
        id = "miui",
        title = "MIUI",
        subtitle = "Miuix Compose core, glossy blur surfaces, HyperOS-style controls"
    ),
    IOS(
        id = "ios",
        title = "iOS",
        subtitle = "Compose Cupertino core, iOS-like controls, rounded panels"
    );

    companion object {
        fun fromId(id: String?): AetherThemePreset = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

data class AetherThemeStyle(
    val preset: AetherThemePreset,
    val cardCorner: Dp,
    val sheetCorner: Dp,
    val controlCorner: Dp,
    val iconCorner: Dp,
    val pillCorner: Dp,
    val cardAlpha: Float,
    val navAlpha: Float,
    val cardElevation: Dp,
)

private val DefaultAetherStyle = AetherThemeStyle(
    preset = AetherThemePreset.DEFAULT,
    cardCorner = 24.dp,
    sheetCorner = 28.dp,
    controlCorner = 16.dp,
    iconCorner = 14.dp,
    pillCorner = 50.dp,
    cardAlpha = 1.0f,
    navAlpha = 0.94f,
    cardElevation = 1.dp,
)

val LocalAetherThemeStyle = staticCompositionLocalOf { DefaultAetherStyle }

private val md_primary_light = Color(0xFF6750A4)
private val md_onPrimary_light = Color(0xFFFFFFFF)
private val md_primaryContainer_light = Color(0xFFEADDFF)
private val md_onPrimaryContainer_light = Color(0xFF4F378B)
private val md_secondary_light = Color(0xFF625B71)
private val md_onSecondary_light = Color(0xFFFFFFFF)
private val md_secondaryContainer_light = Color(0xFFE8DEF8)
private val md_onSecondaryContainer_light = Color(0xFF4A4458)
private val md_tertiary_light = Color(0xFF7D5260)
private val md_onTertiary_light = Color(0xFFFFFFFF)
private val md_tertiaryContainer_light = Color(0xFFFFD8E4)
private val md_onTertiaryContainer_light = Color(0xFF633B48)
private val md_error_light = Color(0xFFB3261E)
private val md_onError_light = Color(0xFFFFFFFF)
private val md_errorContainer_light = Color(0xFFF9DEDC)
private val md_onErrorContainer_light = Color(0xFF8C1D18)
private val md_surface_light = Color(0xFFFEF7FF)
private val md_onSurface_light = Color(0xFF1D1B20)
private val md_surfaceVariant_light = Color(0xFFE7E0EC)
private val md_onSurfaceVariant_light = Color(0xFF49454F)
private val md_outline_light = Color(0xFF79747E)
private val md_outlineVariant_light = Color(0xFFCAC4D0)
private val md_inverseSurface_light = Color(0xFF322F35)
private val md_inverseOnSurface_light = Color(0xFFF5EFF7)
private val md_surfaceContainer_light = Color(0xFFF3EDF7)
private val md_surfaceContainerLow_light = Color(0xFFF7F2FA)
private val md_surfaceContainerHigh_light = Color(0xFFECE6F0)
private val md_surfaceContainerHighest_light = Color(0xFFE6E0E9)

private val md_primary_dark = Color(0xFFD0BCFF)
private val md_onPrimary_dark = Color(0xFF381E72)
private val md_primaryContainer_dark = Color(0xFF4F378B)
private val md_onPrimaryContainer_dark = Color(0xFFEADDFF)
private val md_secondary_dark = Color(0xFFCCC2DC)
private val md_onSecondary_dark = Color(0xFF332D41)
private val md_secondaryContainer_dark = Color(0xFF4A4458)
private val md_onSecondaryContainer_dark = Color(0xFFE8DEF8)
private val md_tertiary_dark = Color(0xFFEFB8C8)
private val md_onTertiary_dark = Color(0xFF492532)
private val md_tertiaryContainer_dark = Color(0xFF633B48)
private val md_onTertiaryContainer_dark = Color(0xFFFFD8E4)
private val md_error_dark = Color(0xFFF2B8B5)
private val md_onError_dark = Color(0xFF601410)
private val md_errorContainer_dark = Color(0xFF8C1D18)
private val md_onErrorContainer_dark = Color(0xFFF9DEDC)
private val md_surface_dark = Color(0xFF141218)
private val md_onSurface_dark = Color(0xFFE6E0E9)
private val md_surfaceVariant_dark = Color(0xFF49454F)
private val md_onSurfaceVariant_dark = Color(0xFFCAC4D0)
private val md_outline_dark = Color(0xFF938F99)
private val md_outlineVariant_dark = Color(0xFF49454F)
private val md_inverseSurface_dark = Color(0xFFE6E0E9)
private val md_inverseOnSurface_dark = Color(0xFF322F35)
private val md_surfaceContainer_dark = Color(0xFF211F26)
private val md_surfaceContainerLow_dark = Color(0xFF1D1B20)
private val md_surfaceContainerHigh_dark = Color(0xFF2B2930)
private val md_surfaceContainerHighest_dark = Color(0xFF36343B)

private val LightColorScheme = lightColorScheme(
    primary = md_primary_light,
    onPrimary = md_onPrimary_light,
    primaryContainer = md_primaryContainer_light,
    onPrimaryContainer = md_onPrimaryContainer_light,
    secondary = md_secondary_light,
    onSecondary = md_onSecondary_light,
    secondaryContainer = md_secondaryContainer_light,
    onSecondaryContainer = md_onSecondaryContainer_light,
    tertiary = md_tertiary_light,
    onTertiary = md_onTertiary_light,
    tertiaryContainer = md_tertiaryContainer_light,
    onTertiaryContainer = md_onTertiaryContainer_light,
    error = md_error_light,
    onError = md_onError_light,
    errorContainer = md_errorContainer_light,
    onErrorContainer = md_onErrorContainer_light,
    surface = md_surface_light,
    onSurface = md_onSurface_light,
    surfaceVariant = md_surfaceVariant_light,
    onSurfaceVariant = md_onSurfaceVariant_light,
    outline = md_outline_light,
    outlineVariant = md_outlineVariant_light,
    inverseSurface = md_inverseSurface_light,
    inverseOnSurface = md_inverseOnSurface_light,
    surfaceContainer = md_surfaceContainer_light,
    surfaceContainerLow = md_surfaceContainerLow_light,
    surfaceContainerHigh = md_surfaceContainerHigh_light,
    surfaceContainerHighest = md_surfaceContainerHighest_light,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_primary_dark,
    onPrimary = md_onPrimary_dark,
    primaryContainer = md_primaryContainer_dark,
    onPrimaryContainer = md_onPrimaryContainer_dark,
    secondary = md_secondary_dark,
    onSecondary = md_onSecondary_dark,
    secondaryContainer = md_secondaryContainer_dark,
    onSecondaryContainer = md_onSecondaryContainer_dark,
    tertiary = md_tertiary_dark,
    onTertiary = md_onTertiary_dark,
    tertiaryContainer = md_tertiaryContainer_dark,
    onTertiaryContainer = md_onTertiaryContainer_dark,
    error = md_error_dark,
    onError = md_onError_dark,
    errorContainer = md_errorContainer_dark,
    onErrorContainer = md_onErrorContainer_dark,
    surface = md_surface_dark,
    onSurface = md_onSurface_dark,
    surfaceVariant = md_surfaceVariant_dark,
    onSurfaceVariant = md_onSurfaceVariant_dark,
    outline = md_outline_dark,
    outlineVariant = md_outlineVariant_dark,
    inverseSurface = md_inverseSurface_dark,
    inverseOnSurface = md_inverseOnSurface_dark,
    surfaceContainer = md_surfaceContainer_dark,
    surfaceContainerLow = md_surfaceContainerLow_dark,
    surfaceContainerHigh = md_surfaceContainerHigh_dark,
    surfaceContainerHighest = md_surfaceContainerHighest_dark,
)

private val MiuiLightColorScheme = lightColorScheme(
    primary = Color(0xFF1677FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = Color(0xFF064B9B),
    secondary = Color(0xFF5D6B82),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EEF7),
    onSecondaryContainer = Color(0xFF223149),
    tertiary = Color(0xFF00A8A8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF7F7),
    onTertiaryContainer = Color(0xFF003F42),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E7),
    onErrorContainer = Color(0xFF8F1016),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF101318),
    surfaceVariant = Color(0xFFE7EBF2),
    onSurfaceVariant = Color(0xFF596273),
    outline = Color(0xFF8A94A6),
    outlineVariant = Color(0xFFD7DEE8),
    inverseSurface = Color(0xFF1D232E),
    inverseOnSurface = Color(0xFFF5F7FA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEFF3FA),
    surfaceContainerHighest = Color(0xFFE8EEF7),
)

private val MiuiDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5EA2FF),
    onPrimary = Color(0xFF001B3D),
    primaryContainer = Color(0xFF073A78),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFFB9C4D8),
    onSecondary = Color(0xFF233044),
    secondaryContainer = Color(0xFF334057),
    onSecondaryContainer = Color(0xFFE5ECF7),
    tertiary = Color(0xFF5ADDDD),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF004F52),
    onTertiaryContainer = Color(0xFFD6FFFF),
    error = Color(0xFFFFB3B5),
    onError = Color(0xFF680009),
    errorContainer = Color(0xFF9E121A),
    onErrorContainer = Color(0xFFFFDADC),
    surface = Color(0xFF0E1117),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF343B48),
    onSurfaceVariant = Color(0xFFC3CCD8),
    outline = Color(0xFF8993A1),
    outlineVariant = Color(0xFF3E4655),
    inverseSurface = Color(0xFFE8EDF5),
    inverseOnSurface = Color(0xFF151A22),
    surfaceContainer = Color(0xFF171C24),
    surfaceContainerLow = Color(0xFF141820),
    surfaceContainerHigh = Color(0xFF202733),
    surfaceContainerHighest = Color(0xFF2A3342),
)

private val IosLightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9ECFF),
    onPrimaryContainer = Color(0xFF003A7A),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E7FF),
    onSecondaryContainer = Color(0xFF25236B),
    tertiary = Color(0xFF34C759),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF8E4),
    onTertiaryContainer = Color(0xFF0D5620),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1DE),
    onErrorContainer = Color(0xFF8A0C04),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF5F5F67),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD1D1D6),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEAEAEE),
    surfaceContainerHighest = Color(0xFFE0E0E6),
)

private val IosDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF063C75),
    onPrimaryContainer = Color(0xFFD7EAFF),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2F2E75),
    onSecondaryContainer = Color(0xFFE6E6FF),
    tertiary = Color(0xFF30D158),
    onTertiary = Color(0xFF002F0B),
    tertiaryContainer = Color(0xFF0E5820),
    onTertiaryContainer = Color(0xFFDFF8E4),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF7A100B),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFC7C7CC),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF3A3A3C),
    inverseSurface = Color(0xFFF5F5F7),
    inverseOnSurface = Color(0xFF111114),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF111113),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
)

private fun shapesFor(preset: AetherThemePreset): Shapes = when (preset) {
    AetherThemePreset.DEFAULT -> Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )
    AetherThemePreset.MIUI -> Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    )
    AetherThemePreset.IOS -> Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
    )
}

private fun styleFor(preset: AetherThemePreset): AetherThemeStyle = when (preset) {
    AetherThemePreset.DEFAULT -> DefaultAetherStyle
    AetherThemePreset.MIUI -> AetherThemeStyle(
        preset = preset,
        cardCorner = 26.dp,
        sheetCorner = 32.dp,
        controlCorner = 18.dp,
        iconCorner = 16.dp,
        pillCorner = 50.dp,
        cardAlpha = 0.98f,
        navAlpha = 0.96f,
        cardElevation = 2.dp,
    )
    AetherThemePreset.IOS -> AetherThemeStyle(
        preset = preset,
        cardCorner = 22.dp,
        sheetCorner = 30.dp,
        controlCorner = 14.dp,
        iconCorner = 13.dp,
        pillCorner = 50.dp,
        cardAlpha = 0.99f,
        navAlpha = 0.97f,
        cardElevation = 1.dp,
    )
}

private fun typographyFor(preset: AetherThemePreset): Typography {
    val base = Typography()
    return when (preset) {
        AetherThemePreset.DEFAULT -> base
        AetherThemePreset.MIUI -> base.copy(
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        AetherThemePreset.IOS -> base.copy(
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}

private fun staticColorScheme(preset: AetherThemePreset, darkTheme: Boolean): ColorScheme = when (preset) {
    AetherThemePreset.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
    AetherThemePreset.MIUI -> if (darkTheme) MiuiDarkColorScheme else MiuiLightColorScheme
    AetherThemePreset.IOS -> if (darkTheme) IosDarkColorScheme else IosLightColorScheme
}

@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themePreset: AetherThemePreset = AetherThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themePreset == AetherThemePreset.DEFAULT && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        else -> staticColorScheme(themePreset, darkTheme)
    }

    val style = styleFor(themePreset)

    CompositionLocalProvider(LocalAetherThemeStyle provides style) {
        val materialContent: @Composable () -> Unit = {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typographyFor(themePreset),
                shapes = shapesFor(themePreset),
                content = content
            )
        }

        if (themePreset == AetherThemePreset.IOS) {
            CupertinoComposeTheme {
                materialContent()
            }
        } else {
            materialContent()
        }
    }
}
