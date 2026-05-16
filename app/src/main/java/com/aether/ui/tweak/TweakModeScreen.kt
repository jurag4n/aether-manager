package com.aether.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aether.data.AccessMode
import com.aether.data.MainViewModel

@Composable
fun TweakModeScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {},
) {
    val mode by vm.accessMode.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccessModeSelector(
            current = mode,
            onSelect = { vm.setAccessMode(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tweak_access_mode_content"
            ) { target ->
                when (target) {
                    AccessMode.ROOT -> TweakScreen(vm = vm, onOpenAppProfile = onOpenAppProfile)
                    AccessMode.NO_ROOT -> NoRootTweakScreen(vm = vm)
                }
            }
        }
    }
}

@Composable
private fun AccessModeSelector(
    current: AccessMode,
    onSelect: (AccessMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeChip(
                mode = AccessMode.ROOT,
                icon = Icons.Outlined.AdminPanelSettings,
                selected = current == AccessMode.ROOT,
                onClick = { onSelect(AccessMode.ROOT) },
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                mode = AccessMode.NO_ROOT,
                icon = Icons.Outlined.PhoneAndroid,
                selected = current == AccessMode.NO_ROOT,
                onClick = { onSelect(AccessMode.NO_ROOT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeChip(
    mode: AccessMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "mode_chip_bg_${mode.value}"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "mode_chip_fg_${mode.value}"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(19.dp))
        Text(
            text = mode.label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
