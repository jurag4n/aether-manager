package dev.aether.manager.ui.tweak

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel

private enum class ExpandSide {
    Left,
    Right,
    Center
}

@Composable
fun TweakScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {}
) {
    val tweaks by vm.tweaks.collectAsState()
    val scroll = rememberScrollState()

    var expandedCard by rememberSaveable { mutableStateOf<String?>(null) }

    var cpuGovernor by rememberSaveable {
        mutableStateOf(if (tweaks.cpuBoost) "Performance" else "Schedutil")
    }
    var gpuProfile by rememberSaveable {
        mutableStateOf(if (tweaks.gpuThrottleOff) "Performance" else "Balanced")
    }
    var minGpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxGpuFreq by rememberSaveable { mutableStateOf("850") }
    var gpuLocked by rememberSaveable { mutableStateOf(false) }
    var renderer by rememberSaveable { mutableStateOf("OpenGL") }
    var showRendererDialog by rememberSaveable { mutableStateOf(false) }

    var dnsProvider by rememberSaveable { mutableStateOf("Off") }
    var networkStable by rememberSaveable { mutableStateOf(false) }
    var tcpEnabled by rememberSaveable { mutableStateOf(tweaks.tcpBbr) }

    var swapEnabled by rememberSaveable { mutableStateOf(false) }
    var killBackgroundPulse by rememberSaveable { mutableStateOf(false) }

    var ioScheduler by rememberSaveable {
        mutableStateOf(if (tweaks.ioScheduler.isBlank()) "Auto" else tweaks.ioScheduler.uppercase())
    }
    var schedBoostMode by rememberSaveable {
        mutableStateOf(if (tweaks.schedboost) "Game" else "Off")
    }

    fun toggleExpand(key: String) {
        expandedCard = if (expandedCard == key) null else key
    }

    fun applyNow() {
        vm.applyAll()
    }

    fun applyTweak(key: String, value: Boolean) {
        vm.setTweak(key, value)
        vm.applyAll()
    }

    fun applyProfile(profile: String) {
        vm.setProfile(profile)
        vm.applyAll()
    }

    val dnsActive = dnsProvider != "Off"
    val schedActive = schedBoostMode != "Off" || tweaks.schedboost

    if (showRendererDialog) {
        RendererDialog(
            current = renderer,
            onDismiss = { showRendererDialog = false },
            onSelect = {
                renderer = it
                showRendererDialog = false
                applyNow()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ExpandablePair(
            expandedKey = expandedCard,
            leftKey = "cpu",
            rightKey = "gpu",
            leftCard = { modifier, expanded ->
                CpuCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = tweaks.cpuBoost,
                    governor = cpuGovernor,
                    onClick = { toggleExpand("cpu") },
                    onGovernorChange = {
                        cpuGovernor = it
                        applyTweak("cpuBoost", it != "Battery")
                    }
                )
            },
            rightCard = { modifier, expanded ->
                GpuCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = tweaks.gpuThrottleOff || gpuLocked,
                    profile = gpuProfile,
                    minFreq = minGpuFreq,
                    maxFreq = maxGpuFreq,
                    locked = gpuLocked,
                    renderer = renderer,
                    onClick = { toggleExpand("gpu") },
                    onProfileChange = {
                        gpuProfile = it
                        applyTweak("gpuThrottleOff", it == "Performance")
                    },
                    onMinFreqChange = { minGpuFreq = it },
                    onMaxFreqChange = { maxGpuFreq = it },
                    onLockClick = {
                        val nextLocked = !gpuLocked
                        gpuLocked = nextLocked
                        applyTweak("gpuThrottleOff", nextLocked)
                    },
                    onRendererClick = { showRendererDialog = true }
                )
            }
        )

        AppProfileCard(onClick = onOpenAppProfile)

        ExpandablePair(
            expandedKey = expandedCard,
            leftKey = "network",
            rightKey = "memory",
            leftCard = { modifier, expanded ->
                NetworkCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = dnsActive || networkStable || tcpEnabled || tweaks.tcpBbr,
                    dnsProvider = dnsProvider,
                    networkStable = networkStable,
                    tcpEnabled = tcpEnabled || tweaks.tcpBbr,
                    onClick = { toggleExpand("network") },
                    onDnsSelect = {
                        dnsProvider = it
                        applyTweak("privateDns", it != "Off")
                    },
                    onNetworkStableToggle = {
                        networkStable = !networkStable
                        applyTweak("networkStable", networkStable)
                    },
                    onTcpToggle = {
                        tcpEnabled = !(tcpEnabled || tweaks.tcpBbr)
                        applyTweak("tcpBbr", tcpEnabled)
                    }
                )
            },
            rightCard = { modifier, expanded ->
                MemoryCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = tweaks.zram || tweaks.lmkAggressive || swapEnabled || killBackgroundPulse,
                    zram = tweaks.zram,
                    lmk = tweaks.lmkAggressive,
                    swap = swapEnabled,
                    killPulse = killBackgroundPulse,
                    onClick = { toggleExpand("memory") },
                    onZramToggle = { applyTweak("zram", !tweaks.zram) },
                    onLmkToggle = { applyTweak("lmkAggressive", !tweaks.lmkAggressive) },
                    onSwapToggle = {
                        swapEnabled = !swapEnabled
                        applyTweak("swapBoost", swapEnabled)
                    },
                    onKillBackground = {
                        killBackgroundPulse = !killBackgroundPulse
                        applyTweak("killBackground", true)
                    }
                )
            }
        )

        ExpandablePair(
            expandedKey = expandedCard,
            leftKey = "io",
            rightKey = "sched",
            leftCard = { modifier, expanded ->
                IoSchedulerCard(
                    modifier = modifier,
                    expanded = expanded,
                    selected = ioScheduler,
                    onClick = { toggleExpand("io") },
                    onSelect = {
                        ioScheduler = it
                        applyTweak("ioScheduler", it != "Auto")
                    }
                )
            },
            rightCard = { modifier, expanded ->
                SchedBoostCard(
                    modifier = modifier,
                    expanded = expanded,
                    mode = schedBoostMode,
                    active = schedActive,
                    onClick = { toggleExpand("sched") },
                    onModeSelect = {
                        schedBoostMode = it
                        applyTweak("schedboost", it != "Off")
                    }
                )
            }
        )

        ThermalProfileCard(
            expanded = expandedCard == "thermal",
            current = tweaks.thermalProfile,
            onClick = { toggleExpand("thermal") },
            onSelect = { applyProfile(it) }
        )
    }
}

@Composable
private fun ExpandablePair(
    expandedKey: String?,
    leftKey: String,
    rightKey: String,
    leftCard: @Composable (Modifier, Boolean) -> Unit,
    rightCard: @Composable (Modifier, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = smoothSpring())
    ) {
        when (expandedKey) {
            leftKey -> leftCard(Modifier.fillMaxWidth(), true)
            rightKey -> rightCard(Modifier.fillMaxWidth(), true)
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    leftCard(Modifier.weight(1f), false)
                    rightCard(Modifier.weight(1f), false)
                }
            }
        }
    }
}

@Composable
private fun CpuCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    governor: String,
    onClick: () -> Unit,
    onGovernorChange: (String) -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Memory,
        title = "CPU",
        subtitle = "Governor profile",
        badge = governor,
        active = active,
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        SelectMenu(
            label = "CPU Governor",
            value = governor,
            options = listOf("Schedutil", "Performance", "Battery", "Ondemand"),
            onSelect = onGovernorChange
        )
        ChipGrid(
            options = listOf("Schedutil", "Performance", "Battery", "Ondemand"),
            selected = governor,
            onSelect = onGovernorChange
        )
    }
}

@Composable
private fun GpuCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    profile: String,
    minFreq: String,
    maxFreq: String,
    locked: Boolean,
    renderer: String,
    onClick: () -> Unit,
    onProfileChange: (String) -> Unit,
    onMinFreqChange: (String) -> Unit,
    onMaxFreqChange: (String) -> Unit,
    onLockClick: () -> Unit,
    onRendererClick: () -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.GridView,
        title = "GPU",
        subtitle = "Profile & renderer",
        badge = if (locked) "Locked" else profile,
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        SubCard(title = "Governor Profile GPU", subtitle = "Pilih mode performa GPU") {
            ChipGrid(
                options = listOf("Battery", "Balanced", "Performance"),
                selected = profile,
                onSelect = onProfileChange
            )
        }

        SubCard(title = "GPU Frequency Lock", subtitle = "Kunci batas frekuensi GPU") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = minFreq,
                    onValueChange = onMinFreqChange,
                    label = { Text("Min") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxFreq,
                    onValueChange = onMaxFreqChange,
                    label = { Text("Max") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = onLockClick,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (locked) "Frequency Locked" else "Lock Frequency")
            }
        }

        SubCard(title = "Renderer", subtitle = "Select GPU Renderer") {
            CompactActionRow(
                icon = Icons.Outlined.GridView,
                title = renderer,
                subtitle = "Open renderer dialog",
                trailing = "Pilih",
                onClick = onRendererClick
            )
        }
    }
}

@Composable
private fun AppProfileCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(25.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "App Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Atur profile performa per aplikasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(-90f)
            )
        }
    }
}

@Composable
private fun NetworkCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    dnsProvider: String,
    networkStable: Boolean,
    tcpEnabled: Boolean,
    onClick: () -> Unit,
    onDnsSelect: (String) -> Unit,
    onNetworkStableToggle: () -> Unit,
    onTcpToggle: () -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Wifi,
        title = "Network",
        subtitle = "DNS, Stabil, TCP",
        badge = when {
            dnsProvider != "Off" -> "DNS"
            tcpEnabled -> "TCP"
            networkStable -> "Stable"
            else -> "Off"
        },
        active = active,
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        SelectMenu(
            label = "DNS Private",
            value = dnsProvider,
            options = listOf(
                "Off",
                "Cloudflare",
                "Google",
                "Quad9",
                "CleanBrowsing",
                "Control D",
                "NextDNS"
            ),
            onSelect = onDnsSelect
        )
        ToggleOptionRow(
            title = "Stabilkan Network",
            subtitle = "Kurangi latency dan jitter",
            checked = networkStable,
            onClick = onNetworkStableToggle
        )
        ToggleOptionRow(
            title = "TCP",
            subtitle = "Optimasi congestion control",
            checked = tcpEnabled,
            onClick = onTcpToggle
        )
    }
}

@Composable
private fun MemoryCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    zram: Boolean,
    lmk: Boolean,
    swap: Boolean,
    killPulse: Boolean,
    onClick: () -> Unit,
    onZramToggle: () -> Unit,
    onLmkToggle: () -> Unit,
    onSwapToggle: () -> Unit,
    onKillBackground: () -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Memory,
        title = "Memory",
        subtitle = "ZRAM, LMK, Swap",
        badge = when {
            zram -> "ZRAM"
            lmk -> "LMK"
            swap -> "Swap"
            else -> "Off"
        },
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        ToggleOptionRow("ZRAM", "Kompresi RAM virtual", zram, onZramToggle)
        ToggleOptionRow("LMK", "Low memory killer agresif", lmk, onLmkToggle)
        ToggleOptionRow("Swap", "Bantu paging saat RAM penuh", swap, onSwapToggle)
        CompactActionRow(
            icon = Icons.Outlined.Memory,
            title = "Kill Background All",
            subtitle = "Bersihkan proses latar belakang",
            trailing = if (killPulse) "Done" else "Run",
            onClick = onKillBackground
        )
    }
}

@Composable
private fun IoSchedulerCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    selected: String,
    onClick: () -> Unit,
    onSelect: (String) -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Storage,
        title = "I/O Scheduler",
        subtitle = "Disk latency",
        badge = selected,
        active = selected != "Auto",
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        SelectMenu(
            label = "Pilih Scheduler",
            value = selected,
            options = listOf("Auto", "CFQ", "Deadline", "Noop", "BFQ", "Maple"),
            onSelect = onSelect
        )
        ChipGrid(
            options = listOf("Auto", "CFQ", "Deadline", "Noop", "BFQ", "Maple"),
            selected = selected,
            onSelect = onSelect
        )
    }
}

@Composable
private fun SchedBoostCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    mode: String,
    active: Boolean,
    onClick: () -> Unit,
    onModeSelect: (String) -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Speed,
        title = "Sched Boost",
        subtitle = "Game booster",
        badge = mode,
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick,
        minHeight = if (expanded) 166.dp else 144.dp
    ) {
        Speedometer(mode = mode, active = active)
        SelectMenu(
            label = "Mode Sched Boost",
            value = mode,
            options = listOf("Off", "Auto", "Balanced", "Game", "Extreme"),
            onSelect = onModeSelect
        )
        Text(
            text = when (mode) {
                "Extreme" -> "Mode extreme untuk performa maksimal."
                "Game" -> "Mode game untuk respon lebih cepat."
                "Balanced" -> "Mode balance untuk performa stabil."
                "Auto" -> "Mode auto menyesuaikan beban sistem."
                else -> "Pilih mode boost untuk mengaktifkan."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThermalProfileCard(
    expanded: Boolean,
    current: String,
    onClick: () -> Unit,
    onSelect: (String) -> Unit
) {
    ExpandableCardShell(
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Outlined.Thermostat,
        title = "Thermal Profile",
        subtitle = "Fitur muncul saat expand",
        badge = thermalLabel(current),
        active = current != "default",
        expanded = expanded,
        side = ExpandSide.Center,
        onClick = onClick,
        minHeight = 92.dp
    ) {
        ChipGrid(
            options = listOf("Balance", "Performance", "Extreme"),
            selected = thermalLabel(current),
            onSelect = {
                onSelect(
                    when (it) {
                        "Performance" -> "performance"
                        "Extreme" -> "extreme"
                        else -> "default"
                    }
                )
            }
        )
    }
}

@Composable
private fun ExpandableCardShell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    active: Boolean,
    expanded: Boolean,
    side: ExpandSide,
    onClick: () -> Unit,
    minHeight: Dp = 144.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (expanded) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "card_color_$title"
    )
    val iconBg by animateColorAsState(
        targetValue = if (active || expanded) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(220),
        label = "icon_bg_$title"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active || expanded) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "icon_tint_$title"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active || expanded) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(220),
        label = "badge_bg_$title"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active || expanded) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "badge_fg_$title"
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = smoothSpring(),
        label = "arrow_$title"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.985f,
        animationSpec = smoothSpring(),
        label = "scale_$title"
    )
    val detailAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 240 else 120),
        label = "detail_alpha_$title"
    )
    val detailOffset by animateFloatAsState(
        targetValue = if (expanded) {
            0f
        } else {
            when (side) {
                ExpandSide.Left -> -28f
                ExpandSide.Right -> 28f
                ExpandSide.Center -> 0f
            }
        },
        animationSpec = smoothSpring(),
        label = "detail_offset_$title"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (expanded) 28.dp else 22.dp),
        color = cardColor,
        tonalElevation = if (expanded) 3.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .scale(cardScale)
            .animateContentSize(animationSpec = smoothSpring())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(21.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(shape = RoundedCornerShape(50.dp), color = badgeBg) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation)
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayerCompat(alpha = detailAlpha, translationX = detailOffset),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

private fun Modifier.graphicsLayerCompat(
    alpha: Float,
    translationX: Float
): Modifier = this.then(
    Modifier.graphicsLayer {
        this.alpha = alpha
        this.translationX = translationX
    }
)

@Composable
private fun SelectMenu(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        onSelect(item)
                    },
                    leadingIcon = {
                        if (item == value) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SubCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                content()
            }
        )
    }
}

@Composable
private fun ToggleOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(200),
        label = "toggle_bg_$title"
    )
    val fg by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "toggle_fg_$title"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = fg
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.68f)
                )
            }
        }
    }
}

@Composable
private fun ChipGrid(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { item ->
                    MiniActionChip(
                        text = item,
                        selected = selected == item,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(item) }
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniActionChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(200),
        label = "chip_bg_$text"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "chip_fg_$text"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primary) {
                Text(
                    trailing,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun Speedometer(
    mode: String,
    active: Boolean
) {
    val target = when (mode) {
        "Extreme" -> 1f
        "Game" -> 0.86f
        "Balanced" -> 0.62f
        "Auto" -> 0.48f
        else -> 0.28f
    }
    val power by animateFloatAsState(
        targetValue = if (active) target else 0.28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "speedometer_power"
    )
    val percent = (power * 100).toInt().coerceIn(0, 100)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(118.dp)) {
                Icon(
                    Icons.Outlined.Speed,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f + power * 0.34f),
                    modifier = Modifier
                        .size(108.dp)
                        .scale(0.9f + power * 0.13f)
                )
                Icon(
                    Icons.Outlined.FlashOn,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(34.dp)
                        .rotate(-28f + power * 56f)
                        .scale(0.9f + power * 0.18f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$percent%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(mode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(8) { index ->
                    val filled = power >= (index + 1) / 8f
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    ) {}
                }
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
    val options = listOf("OpenGL", "Vulkan", "ANGLE", "SkiaVulkan", "SkiaGL")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        title = {
            Text("Select GPU Renderer", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { item ->
                    MiniActionChip(
                        text = item,
                        selected = item == current,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(item) }
                    )
                }
            }
        }
    )
}

private fun thermalLabel(value: String): String {
    return when (value) {
        "performance" -> "Performance"
        "extreme" -> "Extreme"
        else -> "Balance"
    }
}

private fun <T> smoothSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
