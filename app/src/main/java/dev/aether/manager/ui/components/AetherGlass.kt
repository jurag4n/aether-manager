package dev.aether.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.ui.AetherThemePreset
import dev.aether.manager.ui.LocalAetherThemeStyle

/**
 * Stable themed surface.
 *
 * Previous glass implementation used a white gradient overlay inside every Surface.
 * On MIUI/iOS that overlay looked like broken white blocks on cards. This version
 * keeps the glossy feel through solid translucent-safe colors, border, and elevation
 * without drawing extra rectangles above the content.
 */
@Composable
fun AetherGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalAetherThemeStyle.current.cardCorner),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val preset = LocalAetherThemeStyle.current.preset
    val isStyledOs = preset == AetherThemePreset.MIUI || preset == AetherThemePreset.IOS

    val resolvedColor = when (preset) {
        AetherThemePreset.DEFAULT -> color
        AetherThemePreset.MIUI -> color.copy(alpha = 0.98f)
        AetherThemePreset.IOS -> color.copy(alpha = 0.99f)
    }

    val resolvedBorder = border ?: when (preset) {
        AetherThemePreset.DEFAULT -> null
        AetherThemePreset.MIUI -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
        AetherThemePreset.IOS -> BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    }

    val resolvedTonalElevation = if (isStyledOs && tonalElevation == 0.dp) 1.dp else tonalElevation
    val resolvedShadowElevation = if (isStyledOs && shadowElevation == 0.dp) 2.dp else shadowElevation

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = resolvedColor,
            contentColor = contentColor,
            tonalElevation = resolvedTonalElevation,
            shadowElevation = resolvedShadowElevation,
            border = resolvedBorder,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = resolvedColor,
            contentColor = contentColor,
            tonalElevation = resolvedTonalElevation,
            shadowElevation = resolvedShadowElevation,
            border = resolvedBorder,
            content = content,
        )
    }
}

@Composable
fun glossyContainerColor(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color {
    val preset = LocalAetherThemeStyle.current.preset
    return when (preset) {
        AetherThemePreset.MIUI -> base.copy(alpha = 0.98f)
        AetherThemePreset.IOS -> base.copy(alpha = 0.99f)
        AetherThemePreset.DEFAULT -> base
    }
}
