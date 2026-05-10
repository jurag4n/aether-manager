package dev.aether.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.rememberInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.ui.LocalAetherThemeStyle

@Composable
fun AetherGlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalAetherThemeStyle.current.cardCorner),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = LocalAetherThemeStyle.current.cardElevation,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val resolvedModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick
        )
    } else {
        modifier
    }

    Surface(
        modifier = resolvedModifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
    ) {
        Box(content = content)
    }
}

@Composable
fun glossyContainerColor(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color = base
