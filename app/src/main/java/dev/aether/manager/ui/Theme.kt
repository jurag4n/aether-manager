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
// AETHER MATERIAL 3 COLOR PALETTE
// Modern Teal/Cyan theme with elegant accent colors
// =====================================================

// -------------------- LIGHT THEME COLORS --------------------
// Primary: Deep Teal - professional, modern, and fresh
private val PrimaryLight = Color(0xFF006A6A)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFF6FF7F6)
private val OnPrimaryContainerLight = Color(0xFF002020)

// Secondary: Warm Coral accent for contrast and visual interest
private val SecondaryLight = Color(0xFF4A6363)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFCCE8E7)
private val OnSecondaryContainerLight = Color(0xFF051F1F)

// Tertiary: Soft Blue for highlights and accents
private val TertiaryLight = Color(0xFF4B607C)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFD3E4FF)
private val OnTertiaryContainerLight = Color(0xFF041C35)

// Error: Vibrant but not harsh red
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

// Surface colors with subtle warmth
private val SurfaceLight = Color(0xFFF4FBFA)
private val OnSurfaceLight = Color(0xFF161D1D)
private val SurfaceVariantLight = Color(0xFFDAE5E4)
private val OnSurfaceVariantLight = Color(0xFF3F4948)
private val SurfaceDimLight = Color(0xFFD5DBDB)
private val SurfaceBrightLight = Color(0xFFF4FBFA)

// Container surfaces for depth hierarchy
private val SurfaceContainerLight = Color(0xFFEEF5F4)
private val SurfaceContainerLowLight = Color(0xFFF4FBFA)
private val SurfaceContainerHighLight = Color(0xFFE8EFEE)
private val SurfaceContainerHighestLight = Color(0xFFE2E9E8)

// Outline and borders
private val OutlineLight = Color(0xFF6F7978)
private val OutlineVariantLight = Color(0xFFBEC9C8)

// Inverse colors for contrast elements
private val InverseSurfaceLight = Color(0xFF2B3232)
private val InverseOnSurfaceLight = Color(0xFFECF2F1)
private val InversePrimaryLight = Color(0xFF4CDADA)

// Scrim for modals and overlays
private val ScrimLight = Color(0xFF000000)

// -------------------- DARK THEME COLORS --------------------
// Primary: Bright Teal/Cyan - vibrant on dark backgrounds
private val PrimaryDark = Color(0xFF4CDADA)
private val OnPrimaryDark = Color(0xFF003737)
private val PrimaryContainerDark = Color(0xFF004F4F)
private val OnPrimaryContainerDark = Color(0xFF6FF7F6)

// Secondary: Muted Teal for dark theme harmony
private val SecondaryDark = Color(0xFFB0CCCB)
private val OnSecondaryDark = Color(0xFF1B3535)
private val SecondaryContainerDark = Color(0xFF324B4B)
private val OnSecondaryContainerDark = Color(0xFFCCE8E7)

// Tertiary: Soft Blue for dark theme accents
private val TertiaryDark = Color(0xFFB3C8E8)
private val OnTertiaryDark = Color(0xFF1C314B)
private val TertiaryContainerDark = Color(0xFF334863)
private val OnTertiaryContainerDark = Color(0xFFD3E4FF)

// Error: Softer red for dark theme
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

// Surface colors with deep, rich backgrounds
private val SurfaceDark = Color(0xFF0E1515)
private val OnSurfaceDark = Color(0xFFDEE4E3)
private val SurfaceVariantDark = Color(0xFF3F4948)
private val OnSurfaceVariantDark = Color(0xFFBEC9C8)
private val SurfaceDimDark = Color(0xFF0E1515)
private val SurfaceBrightDark = Color(0xFF343B3B)

// Container surfaces for depth hierarchy in dark theme
private val SurfaceContainerDark = Color(0xFF1A2121)
private val SurfaceContainerLowDark = Color(0xFF161D1D)
private val SurfaceContainerHighDark = Color(0xFF242B2B)
private val SurfaceContainerHighestDark = Color(0xFF2F3636)

// Outline and borders
private val OutlineDark = Color(0xFF899392)
private val OutlineVariantDark = Color(0xFF3F4948)

// Inverse colors for contrast elements
private val InverseSurfaceDark = Color(0xFFDEE4E3)
private val InverseOnSurfaceDark = Color(0xFF2B3232)
private val InversePrimaryDark = Color(0xFF006A6A)

// Scrim for modals and overlays
private val ScrimDark = Color(0xFF000000)

// -------------------- COLOR SCHEMES --------------------

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    scrim = ScrimLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    scrim = ScrimDark
)

// -------------------- THEME COMPOSABLE --------------------

/**
 * Aether Material 3 Theme
 * 
 * Features:
 * - Dynamic Color (Material You/Monet): On Android 12+ (API 31+), colors automatically
 *   adapt to the user's wallpaper for a personalized experience.
 * - Custom Fallback: Beautiful Teal/Cyan color palette for devices without dynamic color support.
 * - Proper status bar styling with edge-to-edge support.
 * 
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use Material You dynamic colors (defaults to true on API 31+)
 * @param content The composable content to theme
 */
@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Determine color scheme priority:
    // 1. Dynamic Color (Material You) - if supported and enabled
    // 2. Custom fallback colors
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configure status bar and navigation bar appearance
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            // Set status bar icons to dark/light based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}