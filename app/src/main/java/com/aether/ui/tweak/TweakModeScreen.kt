package com.aether.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.data.AccessMode
import com.aether.data.MainViewModel

@Composable
fun TweakModeScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {},
) {
    val mode by vm.accessMode.collectAsState()
    val rootGranted by vm.rootGranted.collectAsState()
    val applyStatus by vm.applyStatus.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TweakModeHeader(
            current = mode,
            rootGranted = rootGranted,
            checkingRoot = applyStatus.running && applyStatus.summary.contains("Root", ignoreCase = true),
            message = applyStatus.summary,
            lastOk = applyStatus.lastOk,
            onSelect = { vm.setAccessMode(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(220, easing = FastOutSlowInEasing)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.985f)) togetherWith
                        (fadeOut(tween(140, easing = FastOutSlowInEasing)) + scaleOut(tween(140, easing = FastOutSlowInEasing), targetScale = 0.99f))
                },
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
private fun TweakModeHeader(
    current: AccessMode,
    rootGranted: Boolean,
    checkingRoot: Boolean,
    message: String,
    lastOk: Boolean,
    onSelect: (AccessMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Tweak Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        text = if (current == AccessMode.ROOT) "Root mode aktif" else "No Root mode aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                ModeStatePill(current = current, rootGranted = rootGranted)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeButton(
                    title = "Root",
                    subtitle = if (rootGranted) "SU aktif" else "Butuh SU",
                    icon = Icons.Outlined.AdminPanelSettings,
                    selected = current == AccessMode.ROOT,
                    warning = !rootGranted,
                    loading = checkingRoot,
                    onClick = { onSelect(AccessMode.ROOT) },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    title = "No Root",
                    subtitle = "Aman",
                    icon = Icons.Outlined.PhoneAndroid,
                    selected = current == AccessMode.NO_ROOT,
                    warning = false,
                    loading = false,
                    onClick = { onSelect(AccessMode.NO_ROOT) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (message.isNotBlank() && !message.equals("Mode No Root aktif", ignoreCase = true)) {
                ModeMessage(text = message, ok = lastOk)
            }
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    warning: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
            warning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "tweak_mode_button_bg_$title"
    )
    val fg by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            warning -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "tweak_mode_button_fg_$title"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.012f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
        label = "tweak_mode_button_scale_$title"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = bg,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else fg.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(fg.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = fg)
                else Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = fg.copy(alpha = 0.80f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ModeStatePill(current: AccessMode, rootGranted: Boolean) {
    val color = when {
        current == AccessMode.ROOT && rootGranted -> MaterialTheme.colorScheme.primary
        current == AccessMode.ROOT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (current == AccessMode.ROOT && rootGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Security,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (current == AccessMode.ROOT) "ROOT" else "NO ROOT",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun ModeMessage(text: String, ok: Boolean) {
    val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
        }
    }
}
