package dev.aether.manager.ui

import android.app.Activity
import android.app.WallpaperManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =====================================================
// AETHER MATERIAL 3 COLOR PALETTE  –  v3.1
// Theme   : Azure Blue / Sky (Clean Blue-first)
// Spec    : Material Design 3 — Tone-based color system
//
// Dynamic color: Android 12+ gunakan seed dari Monet
// wallpaper via WallpaperColors, lalu tonal palette
// di-derive manual agar saturasi tetap sesuai M3 spec.
// Fallback: Static Azure Blue palette jika API < 31
//           atau WallpaperColors tidak tersedia.
// =====================================================

// ──────────────────────────────────────────────────────
//  STATIC FALLBACK PALETTE  (non-dynamic / API < 31)
//  Seed: #1A73E8  (Google Blue / Azure)
// ──────────────────────────────────────────────────────

private val PrimaryLight                 = Color(0xFF1A6DC4)
private val OnPrimaryLight               = Color(0xFFFFFFFF)
private val PrimaryContainerLight        = Color(0xFFD3E4FF)
private val OnPrimaryContainerLight      = Color(0xFF001D3D)
private val SecondaryLight               = Color(0xFF506B8A)
private val OnSecondaryLight             = Color(0xFFFFFFFF)
private val SecondaryContainerLight      = Color(0xFFD3E5F5)
private val OnSecondaryContainerLight    = Color(0xFF0C2032)
private val TertiaryLight                = Color(0xFF2D6B8A)
private val OnTertiaryLight              = Color(0xFFFFFFFF)
private val TertiaryContainerLight       = Color(0xFFBFE3F7)
private val OnTertiaryContainerLight     = Color(0xFF00202E)
private val ErrorLight                   = Color(0xFFBA1A1A)
private val OnErrorLight                 = Color(0xFFFFFFFF)
private val ErrorContainerLight          = Color(0xFFFFDAD6)
private val OnErrorContainerLight        = Color(0xFF410002)
private val BackgroundLight              = Color(0xFFF6F9FF)
private val OnBackgroundLight            = Color(0xFF101D2C)
private val SurfaceLight                 = Color(0xFFF6F9FF)
private val OnSurfaceLight               = Color(0xFF101D2C)
private val SurfaceVariantLight          = Color(0xFFDDE4EF)
private val OnSurfaceVariantLight        = Color(0xFF42474F)
private val SurfaceDimLight              = Color(0xFFD5DCE8)
private val SurfaceBrightLight           = Color(0xFFF6F9FF)
private val SurfaceContainerLowestLight  = Color(0xFFFFFFFF)
private val SurfaceContainerLowLight     = Color(0xFFEFF3FB)
private val SurfaceContainerLight        = Color(0xFFE9EDF5)
private val SurfaceContainerHighLight    = Color(0xFFE3E8F0)
private val SurfaceContainerHighestLight = Color(0xFFDDE2EA)
private val OutlineLight                 = Color(0xFF72787F)
private val OutlineVariantLight          = Color(0xFFC1C8D3)
private val InverseSurfaceLight          = Color(0xFF2B3240)
private val InverseOnSurfaceLight        = Color(0xFFEDF1FA)
private val InversePrimaryLight          = Color(0xFFA3C8FF)
private val SurfaceTintLight             = Color(0xFF1A6DC4)
private val ScrimLight                   = Color(0xFF000000)

private val PrimaryDark                  = Color(0xFFA3C8FF)
private val OnPrimaryDark                = Color(0xFF003667)
private val PrimaryContainerDark         = Color(0xFF0D52A0)
private val OnPrimaryContainerDark       = Color(0xFFD3E4FF)
private val SecondaryDark                = Color(0xFFB0CAE0)
private val OnSecondaryDark              = Color(0xFF203548)
private val SecondaryContainerDark       = Color(0xFF385265)
private val OnSecondaryContainerDark     = Color(0xFFD3E5F5)
private val TertiaryDark                 = Color(0xFF93CEE9)
private val OnTertiaryDark               = Color(0xFF00374D)
private val TertiaryContainerDark        = Color(0xFF155270)
private val OnTertiaryContainerDark      = Color(0xFFBFE3F7)
private val ErrorDark                    = Color(0xFFFFB4AB)
private val OnErrorDark                  = Color(0xFF690005)
private val ErrorContainerDark           = Color(0xFF93000A)
private val OnErrorContainerDark         = Color(0xFFFFDAD6)
private val BackgroundDark               = Color(0xFF0F1923)
private val OnBackgroundDark             = Color(0xFFDDE3EC)
private val SurfaceDark                  = Color(0xFF0F1923)
private val OnSurfaceDark                = Color(0xFFDDE3EC)
private val SurfaceVariantDark           = Color(0xFF42474F)
private val OnSurfaceVariantDark         = Color(0xFFC1C8D3)
private val SurfaceDimDark               = Color(0xFF0F1923)
private val SurfaceBrightDark            = Color(0xFF35404F)
private val SurfaceContainerLowestDark   = Color(0xFF09131C)
private val SurfaceContainerLowDark      = Color(0xFF171F2B)
private val SurfaceContainerDark         = Color(0xFF1B232F)
private val SurfaceContainerHighDark     = Color(0xFF252E3A)
private val SurfaceContainerHighestDark  = Color(0xFF303945)
private val OutlineDark                  = Color(0xFF8B9199)
private val OutlineVariantDark           = Color(0xFF42474F)
private val InverseSurfaceDark           = Color(0xFFDDE3EC)
private val InverseOnSurfaceDark         = Color(0xFF2B3240)
private val InversePrimaryDark           = Color(0xFF1A6DC4)
private val SurfaceTintDark              = Color(0xFFA3C8FF)
private val ScrimDark                    = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight, onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight, onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight, onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight, onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight, onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight, onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight, onError = OnErrorLight,
    errorContainer = ErrorContainerLight, onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight, onBackground = OnBackgroundLight,
    surface = SurfaceLight, onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight, onSurfaceVariant = OnSurfaceVariantLight,
    surfaceDim = SurfaceDimLight, surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight, outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight, inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight, surfaceTint = SurfaceTintLight, scrim = ScrimLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark, onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark, onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark, onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark, onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark, onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark, onError = OnErrorDark,
    errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark, onBackground = OnBackgroundDark,
    surface = SurfaceDark, onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
    surfaceDim = SurfaceDimDark, surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark, outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark, inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark, surfaceTint = SurfaceTintDark, scrim = ScrimDark,
)

// ──────────────────────────────────────────────────────
//  MONET ENGINE
//
//  Cara kerja:
//  1. Android 12+: ambil WallpaperColors dari OS
//     (sudah di-compute, zero overhead / tidak decode bitmap)
//  2. Extract seed ARGB dari primaryColor
//  3. Convert ke HSV, clamp saturation ≤ 55%
//     (raw Monet bisa sampai 100% — ini yang bikin "gore")
//  4. Derive secondary (+30° hue), tertiary (+60° hue)
//  5. Neutral surface: saturation 4% (tinted, bukan pure grey)
//  6. Neutral-variant: saturation 12% (outline, surfaceVariant)
//  7. Build light/dark ColorScheme dari semua tone tersebut
//
//  Error colors selalu pakai static value — tidak ikut seed
//  agar tetap identifiable sebagai error/bahaya.
// ──────────────────────────────────────────────────────

/**
 * Extract primary seed ARGB dari wallpaper sistem Android 12+.
 * Menggunakan WallpaperColors yang sudah di-compute oleh OS —
 * tidak perlu decode bitmap sehingga tidak ada overhead memori/CPU.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun extractWallpaperSeed(context: android.content.Context): Int? = try {
    WallpaperManager.getInstance(context)
        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        ?.primaryColor
        ?.toArgb()
} catch (_: SecurityException) { null }
  catch (_: Exception)          { null }

/**
 * Dari seed ARGB, generate ColorScheme M3-compliant yang mengikuti
 * hue wallpaper tapi tidak over-saturated.
 *
 * Tone lightness mapping (mengacu M3 tonal palette spec):
 *   Light: primary=tone40 (38%), container=tone90, onContainer=tone10
 *   Dark : primary=tone80, container=tone30, onContainer=tone90
 */
private fun buildMonetScheme(seedArgb: Int, dark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedArgb, hsv)

    // Clamp chroma: Monet bisa hasilkan S=100%, kita cap di 55%
    hsv[1] = hsv[1].coerceAtMost(0.55f)

    /** Buat warna dari HSV dengan lightness dan saturation multiplier tertentu */
    fun tone(lightness: Float, satMul: Float = 1f): Color {
        val v = floatArrayOf(hsv[0], (hsv[1] * satMul).coerceIn(0f, 1f), lightness)
        return Color(android.graphics.Color.HSVToColor(v))
    }

    /** Secondary: geser hue +30°, saturation 65% dari primer */
    fun sec(lightness: Float): Color {
        val v = floatArrayOf((hsv[0] + 30f) % 360f, (hsv[1] * 0.65f).coerceIn(0f, 1f), lightness)
        return Color(android.graphics.Color.HSVToColor(v))
    }

    /** Tertiary: geser hue +60°, saturation 50% dari primer */
    fun ter(lightness: Float): Color {
        val v = floatArrayOf((hsv[0] + 60f) % 360f, (hsv[1] * 0.50f).coerceIn(0f, 1f), lightness)
        return Color(android.graphics.Color.HSVToColor(v))
    }

    /** Neutral: hue sama, saturation 4% — permukaan tinted halus */
    fun neu(lightness: Float): Color {
        val v = floatArrayOf(hsv[0], 0.04f, lightness)
        return Color(android.graphics.Color.HSVToColor(v))
    }

    /** Neutral-variant: hue sama, saturation 12% — outline, surfaceVariant */
    fun neuVar(lightness: Float): Color {
        val v = floatArrayOf(hsv[0], 0.12f, lightness)
        return Color(android.graphics.Color.HSVToColor(v))
    }

    return if (dark) darkColorScheme(
        primary                 = tone(0.80f),
        onPrimary               = tone(0.20f),
        primaryContainer        = tone(0.30f),
        onPrimaryContainer      = tone(0.90f),
        secondary               = sec(0.80f),
        onSecondary             = sec(0.20f),
        secondaryContainer      = sec(0.30f),
        onSecondaryContainer    = sec(0.90f),
        tertiary                = ter(0.80f),
        onTertiary              = ter(0.20f),
        tertiaryContainer       = ter(0.30f),
        onTertiaryContainer     = ter(0.90f),
        error                   = ErrorDark,
        onError                 = OnErrorDark,
        errorContainer          = ErrorContainerDark,
        onErrorContainer        = OnErrorContainerDark,
        background              = neu(0.08f),
        onBackground            = neu(0.90f),
        surface                 = neu(0.08f),
        onSurface               = neu(0.90f),
        surfaceVariant          = neuVar(0.28f),
        onSurfaceVariant        = neuVar(0.80f),
        surfaceDim              = neu(0.08f),
        surfaceBright           = neu(0.22f),
        surfaceContainerLowest  = neu(0.05f),
        surfaceContainerLow     = neu(0.11f),
        surfaceContainer        = neu(0.13f),
        surfaceContainerHigh    = neu(0.17f),
        surfaceContainerHighest = neu(0.22f),
        outline                 = neuVar(0.58f),
        outlineVariant          = neuVar(0.28f),
        inverseSurface          = neu(0.90f),
        inverseOnSurface        = neu(0.20f),
        inversePrimary          = tone(0.40f),
        surfaceTint             = tone(0.80f),
        scrim                   = Color.Black,
    ) else lightColorScheme(
        primary                 = tone(0.38f),
        onPrimary               = Color.White,
        primaryContainer        = tone(0.90f, satMul = 0.6f),
        onPrimaryContainer      = tone(0.10f),
        secondary               = sec(0.38f),
        onSecondary             = Color.White,
        secondaryContainer      = sec(0.90f),
        onSecondaryContainer    = sec(0.10f),
        tertiary                = ter(0.38f),
        onTertiary              = Color.White,
        tertiaryContainer       = ter(0.90f),
        onTertiaryContainer     = ter(0.10f),
        error                   = ErrorLight,
        onError                 = OnErrorLight,
        errorContainer          = ErrorContainerLight,
        onErrorContainer        = OnErrorContainerLight,
        background              = neu(0.99f),
        onBackground            = neu(0.10f),
        surface                 = neu(0.99f),
        onSurface               = neu(0.10f),
        surfaceVariant          = neuVar(0.90f),
        onSurfaceVariant        = neuVar(0.30f),
        surfaceDim              = neu(0.87f),
        surfaceBright           = neu(0.99f),
        surfaceContainerLowest  = Color.White,
        surfaceContainerLow     = neu(0.96f),
        surfaceContainer        = neu(0.94f),
        surfaceContainerHigh    = neu(0.92f),
        surfaceContainerHighest = neu(0.90f),
        outline                 = neuVar(0.50f),
        outlineVariant          = neuVar(0.80f),
        inverseSurface          = neu(0.20f),
        inverseOnSurface        = neu(0.95f),
        inversePrimary          = tone(0.80f),
        surfaceTint             = tone(0.38f),
        scrim                   = Color.Black,
    )
}

// ──────────────────────────────────────────────────────
//  THEME COMPOSABLE
// ──────────────────────────────────────────────────────

/**
 * Aether Material 3 Theme — v3.1
 *
 * Ketika [dynamicColor] = true dan Android 12+:
 *   • Seed diambil dari WallpaperColors sistem (bukan decode bitmap)
 *   • Palette di-generate manual: hue ikut wallpaper, saturation di-clamp 55%
 *   • Secondary/Tertiary: hue dirotasi +30°/+60°, chroma diturunkan
 *   • Surface/neutral: saturation hanya 4–12% (tinted, tidak noisy)
 *   → Hasilnya serasa Google app: harmonis dengan wallpaper, tidak mencolok
 *
 * Fallback ke static Azure Blue palette jika:
 *   • [dynamicColor] = false
 *   • API < 31 (Android < 12)
 *   • WallpaperColors tidak tersedia
 */
@Composable
fun AetherTheme(
    darkTheme    : Boolean = isSystemInDarkTheme(),
    dynamicColor : Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content      : @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(dynamicColor, darkTheme) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val seed = extractWallpaperSeed(context)
                if (seed != null) buildMonetScheme(seed, darkTheme)
                else if (darkTheme) DarkColorScheme else LightColorScheme
            }
            darkTheme -> DarkColorScheme
            else      -> LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
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
        content     = content,
    )
}
