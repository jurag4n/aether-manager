package dev.aether.manager.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Pisah ke object supaya R8 tidak merge 58 Color val ke satu <clinit> ──────
// Root cause VerifyError: top-level private val di file yang sama semua masuk
// satu <clinit> → R8 inline Color() constructor → 77+ argument registers → crash

private object LightColors {
    val primary              = Color(0xFF6750A4)
    val onPrimary            = Color(0xFFFFFFFF)
    val primaryContainer     = Color(0xFFEADDFF)
    val onPrimaryContainer   = Color(0xFF4F378B)
    val secondary            = Color(0xFF625B71)
    val onSecondary          = Color(0xFFFFFFFF)
    val secondaryContainer   = Color(0xFFE8DEF8)
    val onSecondaryContainer = Color(0xFF4A4458)
    val tertiary             = Color(0xFF7D5260)
    val onTertiary           = Color(0xFFFFFFFF)
    val tertiaryContainer    = Color(0xFFFFD8E4)
    val onTertiaryContainer  = Color(0xFF633B48)
    val error                = Color(0xFFB3261E)
    val onError              = Color(0xFFFFFFFF)
    val errorContainer       = Color(0xFFF9DEDC)
    val onErrorContainer     = Color(0xFF8C1D18)
    val surface              = Color(0xFFFEF7FF)
    val onSurface            = Color(0xFF1D1B20)
    val surfaceVariant       = Color(0xFFE7E0EC)
    val onSurfaceVariant     = Color(0xFF49454F)
    val outline              = Color(0xFF79747E)
    val outlineVariant       = Color(0xFFCAC4D0)
    val inverseSurface       = Color(0xFF322F35)
    val inverseOnSurface     = Color(0xFFF5EFF7)
    val surfaceContainer        = Color(0xFFF3EDF7)
    val surfaceContainerLow     = Color(0xFFF7F2FA)
    val surfaceContainerHigh    = Color(0xFFECE6F0)
    val surfaceContainerHighest = Color(0xFFE6E0E9)
}

private object DarkColors {
    val primary              = Color(0xFFD0BCFF)
    val onPrimary            = Color(0xFF381E72)
    val primaryContainer     = Color(0xFF4F378B)
    val onPrimaryContainer   = Color(0xFFEADDFF)
    val secondary            = Color(0xFFCCC2DC)
    val onSecondary          = Color(0xFF332D41)
    val secondaryContainer   = Color(0xFF4A4458)
    val onSecondaryContainer = Color(0xFFE8DEF8)
    val tertiary             = Color(0xFFEFB8C8)
    val onTertiary           = Color(0xFF492532)
    val tertiaryContainer    = Color(0xFF633B48)
    val onTertiaryContainer  = Color(0xFFFFD8E4)
    val error                = Color(0xFFF2B8B5)
    val onError              = Color(0xFF601410)
    val errorContainer       = Color(0xFF8C1D18)
    val onErrorContainer     = Color(0xFFF9DEDC)
    val surface              = Color(0xFF141218)
    val onSurface            = Color(0xFFE6E0E9)
    val surfaceVariant       = Color(0xFF49454F)
    val onSurfaceVariant     = Color(0xFFCAC4D0)
    val outline              = Color(0xFF938F99)
    val outlineVariant       = Color(0xFF49454F)
    val inverseSurface       = Color(0xFFE6E0E9)
    val inverseOnSurface     = Color(0xFF322F35)
    val surfaceContainer        = Color(0xFF211F26)
    val surfaceContainerLow     = Color(0xFF1D1B20)
    val surfaceContainerHigh    = Color(0xFF2B2930)
    val surfaceContainerHighest = Color(0xFF36343B)
}

private val LightColorScheme = lightColorScheme(
    primary                 = LightColors.primary,
    onPrimary               = LightColors.onPrimary,
    primaryContainer        = LightColors.primaryContainer,
    onPrimaryContainer      = LightColors.onPrimaryContainer,
    secondary               = LightColors.secondary,
    onSecondary             = LightColors.onSecondary,
    secondaryContainer      = LightColors.secondaryContainer,
    onSecondaryContainer    = LightColors.onSecondaryContainer,
    tertiary                = LightColors.tertiary,
    onTertiary              = LightColors.onTertiary,
    tertiaryContainer       = LightColors.tertiaryContainer,
    onTertiaryContainer     = LightColors.onTertiaryContainer,
    error                   = LightColors.error,
    onError                 = LightColors.onError,
    errorContainer          = LightColors.errorContainer,
    onErrorContainer        = LightColors.onErrorContainer,
    surface                 = LightColors.surface,
    onSurface               = LightColors.onSurface,
    surfaceVariant          = LightColors.surfaceVariant,
    onSurfaceVariant        = LightColors.onSurfaceVariant,
    outline                 = LightColors.outline,
    outlineVariant          = LightColors.outlineVariant,
    inverseSurface          = LightColors.inverseSurface,
    inverseOnSurface        = LightColors.inverseOnSurface,
    surfaceContainer        = LightColors.surfaceContainer,
    surfaceContainerLow     = LightColors.surfaceContainerLow,
    surfaceContainerHigh    = LightColors.surfaceContainerHigh,
    surfaceContainerHighest = LightColors.surfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary                 = DarkColors.primary,
    onPrimary               = DarkColors.onPrimary,
    primaryContainer        = DarkColors.primaryContainer,
    onPrimaryContainer      = DarkColors.onPrimaryContainer,
    secondary               = DarkColors.secondary,
    onSecondary             = DarkColors.onSecondary,
    secondaryContainer      = DarkColors.secondaryContainer,
    onSecondaryContainer    = DarkColors.onSecondaryContainer,
    tertiary                = DarkColors.tertiary,
    onTertiary              = DarkColors.onTertiary,
    tertiaryContainer       = DarkColors.tertiaryContainer,
    onTertiaryContainer     = DarkColors.onTertiaryContainer,
    error                   = DarkColors.error,
    onError                 = DarkColors.onError,
    errorContainer          = DarkColors.errorContainer,
    onErrorContainer        = DarkColors.onErrorContainer,
    surface                 = DarkColors.surface,
    onSurface               = DarkColors.onSurface,
    surfaceVariant          = DarkColors.surfaceVariant,
    onSurfaceVariant        = DarkColors.onSurfaceVariant,
    outline                 = DarkColors.outline,
    outlineVariant          = DarkColors.outlineVariant,
    inverseSurface          = DarkColors.inverseSurface,
    inverseOnSurface        = DarkColors.inverseOnSurface,
    surfaceContainer        = DarkColors.surfaceContainer,
    surfaceContainerLow     = DarkColors.surfaceContainerLow,
    surfaceContainerHigh    = DarkColors.surfaceContainerHigh,
    surfaceContainerHighest = DarkColors.surfaceContainerHighest,
)

@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}
