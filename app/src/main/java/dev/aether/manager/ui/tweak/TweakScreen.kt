package dev.aether.manager.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.ApplyStatus
import dev.aether.manager.data.MainViewModel

@Composable
fun TweakScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {}
) {
    val tweaks by vm.tweaks.collectAsState()
    val status by vm.applyStatus.collectAsState()
    val scroll = rememberScrollState()

    var expandedCard by rememberSaveable { mutableStateOf<String?>(null) }
    var cpuGovernor by rememberSaveable { mutableStateOf("Schedutil") }
    var gpuProfile by rememberSaveable { mutableStateOf("Balanced") }
    var minGpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxGpuFreq by rememberSaveable { mutableStateOf("850") }
    var gpuLocked by rememberSaveable { mutableStateOf(false) }
    var renderer by rememberSaveable { mutableStateOf("OpenGL") }
    var showRendererDialog by rememberSaveable { mutableStateOf(false) }

    if (showRendererDialog) {
        RendererDialog(
            current = renderer,
            onDismiss = { showRendererDialog = false },
            onSelect = {
                renderer = it
                showRendererDialog = false
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderBlock()

            ActiveProfileBanner(
                current = tweaks.thermalProfile,
                onSelect = { vm.setProfile(it) }
            )

            SectionLabel("Core Tweak")

            Column(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (expandedCard) {
                    "cpu" -> {
                        CpuControlCard(
                            modifier = Modifier.fillMaxWidth(),
                            expanded = true,
                            governor = cpuGovernor,
                            onGovernorChange = {
                                cpuGovernor = it
                                vm.setTweak("cpuBoost", it == "Performance")
                            },
                            onClick = { expandedCard = null }
                        )
                        GpuControlCard(
                            modifier = Modifier.fillMaxWidth(),
                            expanded = false,
                            profile = gpuProfile,
                            minFreq = minGpuFreq,
                            maxFreq = maxGpuFreq,
                            locked = gpuLocked,
                            renderer = renderer,
                            onProfileChange = { gpuProfile = it },
                            onMinFreqChange = { minGpuFreq = it },
                            onMaxFreqChange = { maxGpuFreq = it },
                            onLockClick = {
                                gpuLocked = !gpuLocked
                                vm.setTweak("gpuThrottleOff", true)
                            },
                            onRendererClick = { showRendererDialog = true },
                            onClick = { expandedCard = "gpu" }
                        )
                    }
                    "gpu" -> {
                        GpuControlCard(
                            modifier = Modifier.fillMaxWidth(),
                            expanded = true,
                            profile = gpuProfile,
                            minFreq = minGpuFreq,
                            maxFreq = maxGpuFreq,
                            locked = gpuLocked,
                            renderer = renderer,
                            onProfileChange = { gpuProfile = it },
                            onMinFreqChange = { minGpuFreq = it },
                            onMaxFreqChange = { maxGpuFreq = it },
                            onLockClick = {
                                gpuLocked = !gpuLocked
                                vm.setTweak("gpuThrottleOff", true)
                            },
                            onRendererClick = { showRendererDialog = true },
                            onClick = { expandedCard = null }
                        )
                        CpuControlCard(
                            modifier = Modifier.fillMaxWidth(),
                            expanded = false,
                            governor = cpuGovernor,
                            onGovernorChange = {
                                cpuGovernor = it
                                vm.setTweak("cpuBoost", it == "Performance")
                            },
                            onClick = { expandedCard = "cpu" }
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            CpuControlCard(
                                modifier = Modifier.weight(1f),
                                expanded = false,
                                governor = cpuGovernor,
                                onGovernorChange = {
                                    cpuGovernor = it
                                    vm.setTweak("cpuBoost", it == "Performance")
                                },
                                onClick = { expandedCard = "cpu" }
                            )
                            GpuControlCard(
                                modifier = Modifier.weight(1f),
                                expanded = false,
                                profile = gpuProfile,
                                minFreq = minGpuFreq,
                                maxFreq = maxGpuFreq,
                                locked = gpuLocked,
                                renderer = renderer,
                                onProfileChange = { gpuProfile = it },
                                onMinFreqChange = { minGpuFreq = it },
                                onMaxFreqChange = { maxGpuFreq = it },
                                onLockClick = {
                                    gpuLocked = !gpuLocked
                                    vm.setTweak("gpuThrottleOff", true)
                                },
                                onRendererClick = { showRendererDialog = true },
                                onClick = { expandedCard = "gpu" }
                            )
                        }
                    }
                }
            }

            AppProfileShortcutCard(onClick = onOpenAppProfile)

            SectionLabel("Network & Memory")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SimpleTweakCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Wifi,
                    title = "Network",
                    subtitle = "TCP & latency",
                    badge = if (tweaks.tcpBbr) "BBR" else "Off",
                    active = tweaks.tcpBbr,
                    onClick = { vm.setTweak("tcpBbr", !tweaks.tcpBbr) }
                )
                SimpleTweakCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Dns,
                    title = "Memory",
                    subtitle = "ZRAM & LMK",
                    badge = if (tweaks.zram) "ZRAM" else "Off",
                    active = tweaks.zram || tweaks.lmkAggressive,
                    onClick = { vm.setTweak("zram", !tweaks.zram) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SimpleTweakCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Storage,
                    title = "I/O",
                    subtitle = "Scheduler",
                    badge = if (tweaks.ioScheduler.isBlank()) "Auto" else tweaks.ioScheduler.uppercase(),
                    active = tweaks.ioScheduler.isNotBlank(),
                    onClick = { }
                )
                SimpleTweakCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Speed,
                    title = "Sched",
                    subtitle = "Boost task",
                    badge = if (tweaks.schedboost) "Aktif" else "Off",
                    active = tweaks.schedboost,
                    onClick = { vm.setTweak("schedboost", !tweaks.schedboost) }
                )
            }

            ProfileModeCard(
                current = tweaks.thermalProfile,
                onSelect = { vm.setProfile(it) }
            )

            if (tweaks.mtkBoost != null) {
                WideFeatureCard(
                    icon = Icons.Filled.Bolt,
                    label = "MTK Boost",
                    badge = if (tweaks.mtkBoost == true) "Aktif" else "Off",
                    badgeActive = tweaks.mtkBoost == true,
                    onClick = { vm.setTweak("mtkBoost", !(tweaks.mtkBoost ?: false)) }
                )
            }
        }

        ApplyBar(
            status = status,
            onApply = { vm.applyAll() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HeaderBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Tweak Center",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tap card untuk membuka fitur tersembunyi dengan transisi halus.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun ActiveProfileBanner(
    current: String,
    onSelect: (String) -> Unit
) {
    val label = when (current) {
        "performance" -> "Performance"
        "extreme" -> "Extreme"
        else -> "Balance"
    }
    val icon = when (current) {
        "performance" -> Icons.Outlined.FlashOn
        "extreme" -> Icons.Filled.Bolt
        else -> Icons.Outlined.Balance
    }

    Surface(
        onClick = {
            val next = when (current) {
                "default" -> "performance"
                "performance" -> "extreme"
                else -> "default"
            }
            onSelect(next)
        },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Profil aktif • tap untuk ganti mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                )
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun CpuControlCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    governor: String,
    onGovernorChange: (String) -> Unit,
    onClick: () -> Unit
) {
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.Memory,
        title = "CPU",
        subtitle = if (expanded) "Governor scheduler" else "Governor",
        badge = governor,
        active = governor == "Performance",
        expanded = expanded,
        onClick = onClick
    ) {
        ControlGroupCard(
            title = "CPU Governor",
            subtitle = "Pilih mode scheduler sesuai kebutuhan performa.",
            icon = Icons.Outlined.Tune
        ) {
            OptionDropdown(
                label = "Governor",
                value = governor,
                options = listOf("Schedutil", "Performance", "Battery", "Ondemand"),
                onSelected = onGovernorChange
            )
        }
    }
}

@Composable
private fun GpuControlCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    profile: String,
    minFreq: String,
    maxFreq: String,
    locked: Boolean,
    renderer: String,
    onProfileChange: (String) -> Unit,
    onMinFreqChange: (String) -> Unit,
    onMaxFreqChange: (String) -> Unit,
    onLockClick: () -> Unit,
    onRendererClick: () -> Unit,
    onClick: () -> Unit
) {
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.GridView,
        title = "GPU",
        subtitle = if (expanded) "Profile, lock, renderer" else "Renderer",
        badge = if (locked) "Locked" else renderer,
        active = locked || profile == "Performance" || profile == "Gaming",
        expanded = expanded,
        onClick = onClick
    ) {
        ControlGroupCard(
            title = "GPU Governor Profile",
            subtitle = "Preset 4x1 untuk balancing, gaming, atau hemat daya.",
            icon = Icons.Outlined.Speed
        ) {
            ProfileSelector(
                selected = profile,
                options = listOf("Balanced", "Performance", "Gaming", "Battery"),
                onSelected = onProfileChange
            )
        }

        ControlGroupCard(
            title = "GPU Frequency Lock",
            subtitle = "Atur batas minimum dan maksimum sebelum dikunci.",
            icon = Icons.Outlined.Lock
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minFreq,
                    onValueChange = onMinFreqChange,
                    label = { Text("Min Frequency") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxFreq,
                    onValueChange = onMaxFreqChange,
                    label = { Text("Max Frequency") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = onLockClick,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (locked) Icons.Filled.Check else Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (locked) "Frequency Locked" else "Lock Frequency")
            }
        }

        ControlGroupCard(
            title = "Renderer",
            subtitle = "Pilih backend render untuk aplikasi dan UI.",
            icon = Icons.Outlined.Tune
        ) {
            Surface(
                onClick = onRendererClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Select GPU Renderer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = renderer,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ExpandableTweakCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    active: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(260),
        label = "expand_container"
    )
    val iconBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "expand_icon_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "expand_icon_tint"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "expand_badge_bg"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "expand_badge_fg"
    )
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.98f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expand_scale"
    )

    Surface(
        shape = RoundedCornerShape(if (expanded) 28.dp else 24.dp),
        color = container,
        tonalElevation = if (expanded) 3.dp else 0.dp,
        modifier = modifier
            .scale(scale)
            .heightIn(min = if (expanded) 152.dp else 132.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(Modifier.fillMaxWidth()) {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(if (expanded) 28.dp else 24.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (expanded) 44.dp else 38.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(if (expanded) 22.dp else 19.dp)
                            )
                        }
                        StatusPill(text = badge, active = active || expanded, bg = badgeBg, fg = badgeFg)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = title,
                            style = if (expanded) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = shrinkVertically(tween(170)) + fadeOut(tween(140))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun ControlGroupCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun OptionDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "dropdown_arrow"
    )

    Box(Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (option == value) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Spacer(Modifier.size(18.dp))
                            }
                            Text(option)
                        }
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileSelector(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val active = selected == option
            val bg by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                animationSpec = tween(180),
                label = "profile_chip_bg"
            )
            val fg by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(180),
                label = "profile_chip_fg"
            )
            Surface(
                onClick = { onSelected(option) },
                shape = RoundedCornerShape(14.dp),
                color = bg,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RendererDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        title = {
            Text(text = "Select GPU Renderer", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("OpenGL", "Vulkan", "ANGLE", "SkiaVulkan", "SkiaGL").forEach { option ->
                    val active = option == current
                    Surface(
                        onClick = { onSelect(option) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Filled.Check else Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AppProfileShortcutCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Apps, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "App Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "4x2 shortcut card • klik untuk masuk ke menu profile aplikasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = "Buka",
                active = true,
                bg = MaterialTheme.colorScheme.onSecondaryContainer,
                fg = MaterialTheme.colorScheme.secondaryContainer
            )
        }
    }
}

@Composable
private fun SimpleTweakCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val iconBg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(200),
        label = "simple_icon_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "simple_icon_tint"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(200),
        label = "simple_badge_bg"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "simple_badge_fg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.height(132.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
                }
                StatusPill(text = badge, active = active, bg = badgeBg, fg = badgeFg)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileModeCard(
    current: String,
    onSelect: (String) -> Unit
) {
    val profiles = listOf(
        Triple("default", "Balance", Icons.Outlined.Balance),
        Triple("performance", "Performance", Icons.Outlined.FlashOn),
        Triple("extreme", "Extreme", Icons.Filled.Bolt)
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Thermal Profile",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { (key, label, icon) ->
                    val selected = current == key
                    val bg by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        tween(200),
                        label = "profile_bg"
                    )
                    val fg by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        tween(200),
                        label = "profile_fg"
                    )
                    Surface(
                        onClick = { onSelect(key) },
                        shape = RoundedCornerShape(15.dp),
                        color = bg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WideFeatureCard(
    icon: ImageVector,
    label: String,
    badge: String,
    badgeActive: Boolean,
    onClick: () -> Unit
) {
    val iconBg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200),
        label = "wide_icon_bg"
    )
    val iconTint by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200),
        label = "wide_icon_tint"
    )
    val badgeBg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200),
        label = "wide_badge_bg"
    )
    val badgeFg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200),
        label = "wide_badge_fg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
            }

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            StatusPill(text = badge, active = badgeActive, bg = badgeBg, fg = badgeFg)
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    active: Boolean,
    bg: Color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
    fg: Color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(shape = RoundedCornerShape(50.dp), color = bg) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ApplyBar(
    status: ApplyStatus,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        (fadeIn(tween(200)) + scaleIn(initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(150)) + scaleOut(targetScale = 0.96f))
                    },
                    label = "apply_status"
                ) { s ->
                    if (s.running) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Menerapkan…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (s.summary.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (s.lastOk) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                null,
                                tint = if (s.lastOk) Color(0xFF2D7D46) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                s.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            "Tweaks siap diterapkan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onApply,
                enabled = !status.running,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                if (status.running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Apply",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
