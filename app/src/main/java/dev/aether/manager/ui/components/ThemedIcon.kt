package dev.aether.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.ui.AetherThemePreset
import dev.aether.manager.ui.LocalAetherThemeStyle

/**
 * Theme-aware icon container used across the app.
 * Default keeps Material 3 icon behavior, MIUI/iOS use OS-like colorful squircle tiles.
 */
@Composable
fun AetherIconTile(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = tint.copy(alpha = 0.14f),
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    selected: Boolean = true,
) {
    val style = LocalAetherThemeStyle.current
    val preset = style.preset
    val accent = themedIconAccent(icon, tint, preset)
    val shape = when (preset) {
        AetherThemePreset.DEFAULT -> RoundedCornerShape(style.iconCorner)
        AetherThemePreset.MIUI -> RoundedCornerShape(16.dp)
        AetherThemePreset.IOS -> RoundedCornerShape(14.dp)
    }
    val bg = when (preset) {
        AetherThemePreset.DEFAULT -> containerColor
        AetherThemePreset.MIUI -> if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHighest
        AetherThemePreset.IOS -> if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when (preset) {
        AetherThemePreset.DEFAULT -> tint
        AetherThemePreset.MIUI -> if (selected) Color.White else accent
        AetherThemePreset.IOS -> if (selected) Color.White else accent
    }
    val border = when (preset) {
        AetherThemePreset.DEFAULT -> null
        AetherThemePreset.MIUI -> BorderStroke(0.6.dp, Color.White.copy(alpha = if (selected) 0.18f else 0.0f))
        AetherThemePreset.IOS -> BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
    }

    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = bg,
        tonalElevation = when (preset) {
            AetherThemePreset.DEFAULT -> 0.dp
            AetherThemePreset.MIUI -> if (selected) 2.dp else 0.dp
            AetherThemePreset.IOS -> 0.dp
        },
        shadowElevation = when (preset) {
            AetherThemePreset.MIUI -> if (selected) 2.dp else 0.dp
            else -> 0.dp
        },
        border = border,
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = fg,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun AetherSmallIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp,
) {
    val preset = LocalAetherThemeStyle.current.preset
    if (preset == AetherThemePreset.DEFAULT) {
        Icon(icon, contentDescription, tint = tint, modifier = modifier.size(iconSize))
    } else {
        AetherIconTile(
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
            size = size,
            iconSize = iconSize,
            selected = true
        )
    }
}

@Composable
fun AetherIconDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

private fun themedIconAccent(
    icon: ImageVector,
    fallback: Color,
    preset: AetherThemePreset,
): Color {
    if (preset == AetherThemePreset.DEFAULT) return fallback
    val name = icon.name.lowercase()
    return when (preset) {
        AetherThemePreset.MIUI -> miuiAccent(name, fallback)
        AetherThemePreset.IOS -> iosAccent(name, fallback)
        AetherThemePreset.DEFAULT -> fallback
    }
}

private fun miuiAccent(name: String, fallback: Color): Color = when {
    name.hasAny("memory", "storage", "archive", "save", "folder") -> Color(0xFF00C2A8)
    name.hasAny("battery", "power", "flash") -> Color(0xFF39C75B)
    name.hasAny("network", "dns", "language", "cloud") -> Color(0xFF1677FF)
    name.hasAny("sports", "rocket", "speed", "terminal", "developer", "tune") -> Color(0xFFFF7A1A)
    name.hasAny("thermal", "thermostat", "acunit", "sunny") -> Color(0xFFFF4D6D)
    name.hasAny("settings", "palette", "style", "color", "dark") -> Color(0xFF7C5CFF)
    name.hasAny("warning", "error", "delete", "bad", "report") -> Color(0xFFFF3B30)
    name.hasAny("check", "verified") -> Color(0xFF26C65A)
    name.hasAny("apps", "widgets", "grid", "layers") -> Color(0xFF1EA7FF)
    name.hasAny("lock", "vpn") -> Color(0xFFFFB020)
    name.hasAny("info", "monitor", "query") -> Color(0xFF00A8E8)
    else -> fallback
}

private fun iosAccent(name: String, fallback: Color): Color = when {
    name.hasAny("memory", "storage", "archive", "save", "folder") -> Color(0xFF34C759)
    name.hasAny("battery", "power", "flash") -> Color(0xFF30D158)
    name.hasAny("network", "dns", "language", "cloud") -> Color(0xFF007AFF)
    name.hasAny("sports", "rocket", "speed", "terminal", "developer", "tune") -> Color(0xFFFF9500)
    name.hasAny("thermal", "thermostat", "acunit", "sunny") -> Color(0xFFFF2D55)
    name.hasAny("settings", "palette", "style", "color", "dark") -> Color(0xFF5856D6)
    name.hasAny("warning", "error", "delete", "bad", "report") -> Color(0xFFFF3B30)
    name.hasAny("check", "verified") -> Color(0xFF34C759)
    name.hasAny("apps", "widgets", "grid", "layers") -> Color(0xFF5AC8FA)
    name.hasAny("lock", "vpn") -> Color(0xFFFFCC00)
    name.hasAny("info", "monitor", "query") -> Color(0xFF32ADE6)
    else -> fallback
}

private fun String.hasAny(vararg needles: String): Boolean = needles.any { contains(it, ignoreCase = true) }
