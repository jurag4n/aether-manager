package dev.aether.manager.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =====================================================
// AETHER MATERIAL 3 COLOR PALETTE  –  v2.0
// Theme   : Deep Violet / Indigo (Premium Dark-first)
// Spec    : Material Design 3 — Tone-based color system
// Generator reference: m3.material.io/theme-builder
// Key hue : Primary 270° (Violet-Indigo)
//           Secondary 230° (Cool Blue-Grey)
//           Tertiary  320° (Soft Mauve/Pink)
// =====================================================

// ──────────────────────────────────────────────────────
//  LIGHT THEME
// ──────────────────────────────────────────────────────

// Primary — Violet Indigo (tone 40)
private val PrimaryLight                 = Color(0xFF5B4CDB)
private val OnPrimaryLight               = Color(0xFFFFFFFF)
private val PrimaryContainerLight        = Color(0xFFE6E0FF)
private val OnPrimaryContainerLight      = Color(0xFF160065)

// Secondary — Cool Slate Blue (tone 40)
private val SecondaryLight               = Color(0xFF5C5C8A)
private val OnSecondaryLight             = Color(0xFFFFFFFF)
private val SecondaryContainerLight      = Color(0xFFE2E0FF)
private val OnSecondaryContainerLight    = Color(0xFF181842)

// Tertiary — Soft Mauve (tone 40)
private val TertiaryLight                = Color(0xFF7B4E82)
private val OnTertiaryLight              = Color(0xFFFFFFFF)
private val TertiaryContainerLight       = Color(0xFFF8D8FF)
private val OnTertiaryContainerLight     = Color(0xFF300839)

// Error
private val ErrorLight                   = Color(0xFFBA1A1A)
private val OnErrorLight                 = Color(0xFFFFFFFF)
private val ErrorContainerLight          = Color(0xFFFFDAD6)
private val OnErrorContainerLight        = Color(0xFF410002)

// Background & Surface (neutral tone 99 / 98)
private val BackgroundLight              = Color(0xFFFBF8FF)
private val OnBackgroundLight            = Color(0xFF1B1B21)
private val SurfaceLight                 = Color(0xFFFBF8FF)
private val OnSurfaceLight               = Color(0xFF1B1B21)

// Surface Variant (neutral-variant tone 90)
private val SurfaceVariantLight          = Color(0xFFE5E0F3)
private val OnSurfaceVariantLight        = Color(0xFF47464F)

// Surface tonal hierarchy (light)
private val SurfaceDimLight              = Color(0xFFDDD8E9)
private val SurfaceBrightLight           = Color(0xFFFBF8FF)
private val SurfaceContainerLowestLight  = Color(0xFFFFFFFF)
private val SurfaceContainerLowLight     = Color(0xFFF5F1FF)
private val SurfaceContainerLight        = Color(0xFFEFEBF9)
private val SurfaceContainerHighLight    = Color(0xFFE9E5F3)
private val SurfaceContainerHighestLight = Color(0xFFE4DFEE)

// Outline
private val OutlineLight                 = Color(0xFF787680)
private val OutlineVariantLight          = Color(0xFFC9C5D4)

// Inverse
private val InverseSurfaceLight          = Color(0xFF303037)
private val InverseOnSurfaceLight        = Color(0xFFF3EFF9)
private val InversePrimaryLight          = Color(0xFFC7BFFF)

// Surface tint (same as primary)
private val SurfaceTintLight             = Color(0xFF5B4CDB)

// Scrim
private val ScrimLight                   = Color(0xFF000000)


// ──────────────────────────────────────────────────────
//  DARK THEME
// ──────────────────────────────────────────────────────

// Primary — Luminous Periwinkle (tone 80)
private val PrimaryDark                  = Color(0xFFC7BFFF)
private val OnPrimaryDark                = Color(0xFF2C1E7F)
private val PrimaryContainerDark         = Color(0xFF4336C0)
private val OnPrimaryContainerDark       = Color(0xFFE6E0FF)

// Secondary — Muted Indigo (tone 80)
private val SecondaryDark                = Color(0xFFC5C3F5)
private val OnSecondaryDark              = Color(0xFF2D2D5B)
private val SecondaryContainerDark       = Color(0xFF444473)
private val OnSecondaryContainerDark     = Color(0xFFE2E0FF)

// Tertiary — Soft Orchid (tone 80)
private val TertiaryDark                 = Color(0xFFEDB6F5)
private val OnTertiaryDark               = Color(0xFF4B1F52)
private val TertiaryContainerDark        = Color(0xFF62376A)
private val OnTertiaryContainerDark      = Color(0xFFF8D8FF)

// Error
private val ErrorDark                    = Color(0xFFFFB4AB)
private val OnErrorDark                  = Color(0xFF690005)
private val ErrorContainerDark           = Color(0xFF93000A)
private val OnErrorContainerDark         = Color(0xFFFFDAD6)

// Background & Surface (neutral tone 6 / 6)
private val BackgroundDark               = Color(0xFF131318)
private val OnBackgroundDark             = Color(0xFFE4E1EC)
private val SurfaceDark                  = Color(0xFF131318)
private val OnSurfaceDark                = Color(0xFFE4E1EC)

// Surface Variant (neutral-variant tone 30)
private val SurfaceVariantDark           = Color(0xFF47464F)
private val OnSurfaceVariantDark         = Color(0xFFC9C5D4)

// Surface tonal hierarchy (dark)
private val SurfaceDimDark               = Color(0xFF131318)
private val SurfaceBrightDark            = Color(0xFF39383F)
private val SurfaceContainerLowestDark   = Color(0xFF0D0D13)
private val SurfaceContainerLowDark      = Color(0xFF1B1B21)
private val SurfaceContainerDark         = Color(0xFF1F1F25)
private val SurfaceContainerHighDark     = Color(0xFF29292F)
private val SurfaceContainerHighestDark  = Color(0xFF34343A)

// Outline
private val OutlineDark                  = Color(0xFF928F9E)
private val OutlineVariantDark           = Color(0xFF47464F)

// Inverse
private val InverseSurfaceDark           = Color(0xFFE4E1EC)
private val InverseOnSurfaceDark         = Color(0xFF303037)
private val InversePrimaryDark           = Color(0xFF5B4CDB)

// Surface tint (same as primary)
private val SurfaceTintDark              = Color(0xFFC7BFFF)

// Scrim
private val ScrimDark                    = Color(0xFF000000)


// ──────────────────────────────────────────────────────
//  COLOR SCHEMES
// ──────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary                  = PrimaryLight,
    onPrimary                = OnPrimaryLight,
    primaryContainer         = PrimaryContainerLight,
    onPrimaryContainer       = OnPrimaryContainerLight,
    secondary                = SecondaryLight,
    onSecondary              = OnSecondaryLight,
    secondaryContainer       = SecondaryContainerLight,
    onSecondaryContainer     = OnSecondaryContainerLight,
    tertiary                 = TertiaryLight,
    onTertiary               = OnTertiaryLight,
    tertiaryContainer        = TertiaryContainerLight,
    onTertiaryContainer      = OnTertiaryContainerLight,
    error                    = ErrorLight,
    onError                  = OnErrorLight,
    errorContainer           = ErrorContainerLight,
    onErrorContainer         = OnErrorContainerLight,
    background               = BackgroundLight,
    onBackground             = OnBackgroundLight,
    surface                  = SurfaceLight,
    onSurface                = OnSurfaceLight,
    surfaceVariant           = SurfaceVariantLight,
    onSurfaceVariant         = OnSurfaceVariantLight,
    surfaceDim               = SurfaceDimLight,
    surfaceBright            = SurfaceBrightLight,
    surfaceContainerLowest   = SurfaceContainerLowestLight,
    surfaceContainerLow      = SurfaceContainerLowLight,
    surfaceContainer         = SurfaceContainerLight,
    surfaceContainerHigh     = SurfaceContainerHighLight,
    surfaceContainerHighest  = SurfaceContainerHighestLight,
    outline                  = OutlineLight,
    outlineVariant           = OutlineVariantLight,
    inverseSurface           = InverseSurfaceLight,
    inverseOnSurface         = InverseOnSurfaceLight,
    inversePrimary           = InversePrimaryLight,
    surfaceTint              = SurfaceTintLight,
    scrim                    = ScrimLight
)

private val DarkColorScheme = darkColorScheme(
    primary                  = PrimaryDark,
    onPrimary                = OnPrimaryDark,
    primaryContainer         = PrimaryContainerDark,
    onPrimaryContainer       = OnPrimaryContainerDark,
    secondary                = SecondaryDark,
    onSecondary              = OnSecondaryDark,
    secondaryContainer       = SecondaryContainerDark,
    onSecondaryContainer     = OnSecondaryContainerDark,
    tertiary                 = TertiaryDark,
    onTertiary               = OnTertiaryDark,
    tertiaryContainer        = TertiaryContainerDark,
    onTertiaryContainer      = OnTertiaryContainerDark,
    error                    = ErrorDark,
    onError                  = OnErrorDark,
    errorContainer           = ErrorContainerDark,
    onErrorContainer         = OnErrorContainerDark,
    background               = BackgroundDark,
    onBackground             = OnBackgroundDark,
    surface                  = SurfaceDark,
    onSurface                = OnSurfaceDark,
    surfaceVariant           = SurfaceVariantDark,
    onSurfaceVariant         = OnSurfaceVariantDark,
    surfaceDim               = SurfaceDimDark,
    surfaceBright            = SurfaceBrightDark,
    surfaceContainerLowest   = SurfaceContainerLowestDark,
    surfaceContainerLow      = SurfaceContainerLowDark,
    surfaceContainer         = SurfaceContainerDark,
    surfaceContainerHigh     = SurfaceContainerHighDark,
    surfaceContainerHighest  = SurfaceContainerHighestDark,
    outline                  = OutlineDark,
    outlineVariant           = OutlineVariantDark,
    inverseSurface           = InverseSurfaceDark,
    inverseOnSurface         = InverseOnSurfaceDark,
    inversePrimary           = InversePrimaryDark,
    surfaceTint              = SurfaceTintDark,
    scrim                    = ScrimDark
)


// ──────────────────────────────────────────────────────
//  THEME COMPOSABLE
// ──────────────────────────────────────────────────────

/**
 * Aether Material 3 Theme — v2.0
 *
 * Features:
 * • Dynamic Color (Material You / Monet): Android 12+ (API 31+) — auto-adapts
 *   to wallpaper. Falls back to Deep Violet / Indigo custom palette.
 * • Full M3 token coverage including background, surfaceTint, and
 *   all five surfaceContainer tiers (Lowest → Highest).
 * • Edge-to-edge status bar + nav bar with proper light/dark icon tint.
 *
 * @param darkTheme      Use dark theme (default: system setting)
 * @param dynamicColor   Use Material You dynamic colors (default: true on API 31+)
 * @param content        Composable content to theme
 */
@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent bars — content draws edge-to-edge
            window.statusBarColor     = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}
