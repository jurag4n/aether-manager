package dev.aether.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.ui.AetherThemePreset
import dev.aether.manager.ui.LocalAetherThemeStyle

/**
 * MIUI/HyperOS-like glossy glass container.
 * Stable blur/glass feel without expensive realtime backdrop blur.
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
    val glass = preset == AetherThemePreset.MIUI || preset == AetherThemePreset.IOS
    val alpha = when (preset) {
        AetherThemePreset.MIUI -> 0.68f
        AetherThemePreset.IOS -> 0.72f
        AetherThemePreset.DEFAULT -> 1f
    }
    val effectiveBorder = border ?: if (glass) {
        BorderStroke(0.8.dp, Color.White.copy(alpha = if (preset == AetherThemePreset.MIUI) 0.34f else 0.24f))
    } else null
    val resolvedColor = if (glass) color.copy(alpha = alpha) else color

    val inner: @Composable () -> Unit = {
        if (glass) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (preset == AetherThemePreset.MIUI) 0.26f else 0.18f),
                                Color.White.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.primary.copy(alpha = if (preset == AetherThemePreset.MIUI) 0.05f else 0.03f),
                            )
                        )
                    )
            ) { content() }
        } else {
            content()
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = resolvedColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = effectiveBorder,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
            content = inner,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = resolvedColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = effectiveBorder,
            content = inner,
        )
    }
}

@Composable
fun glossyContainerColor(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color {
    val preset = LocalAetherThemeStyle.current.preset
    return when (preset) {
        AetherThemePreset.MIUI -> base.copy(alpha = 0.70f)
        AetherThemePreset.IOS -> base.copy(alpha = 0.76f)
        AetherThemePreset.DEFAULT -> base
    }
}
