package dev.aether.manager.ui.tweak

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

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
    var minCpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxCpuFreq by rememberSaveable { mutableStateOf("2200") }
    var cpuFreqLocked by rememberSaveable { mutableStateOf(false) }

    var gpuProfile by rememberSaveable {
        mutableStateOf(if (tweaks.gpuThrottleOff) "Performance" else "Balanced")
    }
    var minGpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxGpuFreq by rememberSaveable { mutableStateOf("850") }
    var gpuLocked by rememberSaveable { mutableStateOf(false) }
    var renderer by rememberSaveable { mutableStateOf("OpenGL") }
    var rendererDialog by rememberSaveable { mutableStateOf(false) }

    var dnsProvider by rememberSaveable { mutableStateOf("Off") }
    var networkStable by rememberSaveable { mutableStateOf(false) }
    var tcpEnabled by rememberSaveable { mutableStateOf(tweaks.tcpBbr) }

    var swapEnabled by rememberSaveable { mutableStateOf(false) }
    var killBackgroundActive by rememberSaveable { mutableStateOf(false) }

    var ioScheduler by rememberSaveable {
        mutableStateOf(if (tweaks.ioScheduler.isBlank()) "Auto" else normalizeLabel(tweaks.ioScheduler))
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

    fun setTweakNow(key: String, value: Boolean) {
        vm.setTweak(key, value)
        vm.applyAll()
    }

    fun setProfileNow(profile: String) {
        vm.setProfile(profile)
        vm.applyAll()
    }

    if (rendererDialog) {
        RendererDialog(
            current = renderer,
            onDismiss = { rendererDialog = false },
            onSelect = {
                renderer = it
                rendererDialog = false
                setTweakNow("renderer", true)
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
            left = { modifier, expanded ->
                CpuCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = tweaks.cpuBoost || cpuFreqLocked,
                    governor = cpuGovernor,
                    minFreq = minCpuFreq,
                    maxFreq = maxCpuFreq,
                    locked = cpuFreqLocked,
                    onClick = { toggleExpand("cpu") },
                    onGovernorChange = {
                        cpuGovernor = it
                        setTweakNow("cpuBoost", it != "Battery")
                    },
                    onMinFreqChange = { minCpuFreq = it },
                    onMaxFreqChange = { maxCpuFreq = it },
                    onLockClick = {
                        cpuFreqLocked = !cpuFreqLocked
                        setTweakNow("cpuFreqLock", cpuFreqLocked)
                    }
                )
            },
            right = { modifier, expanded ->
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
                        setTweakNow("gpuThrottleOff", it == "Performance")
                    },
                    onMinFreqChange = { minGpuFreq = it },
                    onMaxFreqChange = { maxGpuFreq = it },
                    onLockClick = {
                        gpuLocked = !gpuLocked
                        setTweakNow("gpuThrottleOff", gpuLocked)
                    },
                    onRendererClick = { rendererDialog = true }
                )
            }
        )

        AppProfileCard(onClick = onOpenAppProfile)

        ExpandablePair(
            expandedKey = expandedCard,
            leftKey = "network",
            rightKey = "memory",
            left = { modifier, expanded ->
                NetworkCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = dnsProvider != "Off" || networkStable || tcpEnabled || tweaks.tcpBbr,
                    dnsProvider = dnsProvider,
                    networkStable = networkStable,
                    tcpEnabled = tcpEnabled || tweaks.tcpBbr,
                    onClick = { toggleExpand("network") },
                    onDnsSelect = {
                        dnsProvider = it
                        setTweakNow("privateDns", it != "Off")
                    },
                    onNetworkStableToggle = {
                        networkStable = !networkStable
                        setTweakNow("networkStable", networkStable)
                    },
                    onTcpToggle = {
                        tcpEnabled = !(tcpEnabled || tweaks.tcpBbr)
                        setTweakNow("tcpBbr", tcpEnabled)
                    }
                )
            },
            right = { modifier, expanded ->
                MemoryCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = tweaks.zram || tweaks.lmkAggressive || swapEnabled || killBackgroundActive,
                    zram = tweaks.zram,
                    lmk = tweaks.lmkAggressive,
                    swap = swapEnabled,
                    killBackground = killBackgroundActive,
                    onClick = { toggleExpand("memory") },
                    onZramToggle = { setTweakNow("zram", !tweaks.zram) },
                    onLmkToggle = { setTweakNow("lmkAggressive", !tweaks.lmkAggressive) },
                    onSwapToggle = {
                        swapEnabled = !swapEnabled
                        setTweakNow("swap", swapEnabled)
                    },
                    onKillBackgroundClick = {
                        killBackgroundActive = !killBackgroundActive
                        setTweakNow("killBackground", true)
                    }
                )
            }
        )

        ExpandablePair(
            expandedKey = expandedCard,
            leftKey = "io",
            rightKey = "sched",
            left = { modifier, expanded ->
                IoSchedulerCard(
                    modifier = modifier,
                    expanded = expanded,
                    selected = ioScheduler,
                    onClick = { toggleExpand("io") },
                    onSelect = {
                        ioScheduler = it
                        setTweakNow("ioScheduler", it != "Auto")
                    }
                )
            },
            right = { modifier, expanded ->
                SchedBoostCard(
                    modifier = modifier,
                    expanded = expanded,
                    active = schedBoostMode != "Off" || tweaks.schedboost,
                    mode = schedBoostMode,
                    onClick = { toggleExpand("sched") },
                    onSelect = {
                        schedBoostMode = it
                        setTweakNow("schedboost", it != "Off")
                    }
                )
            }
        )

        ThermalProfileCard(
            expanded = expandedCard == "thermal",
            current = tweaks.thermalProfile,
            onClick = { toggleExpand("thermal") },
            onSelect = { setProfileNow(it) }
        )
    }
}

@Composable
private fun ExpandablePair(
    expandedKey: String?,
    leftKey: String,
    rightKey: String,
    left: @Composable (Modifier, Boolean) -> Unit,
    right: @Composable (Modifier, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(smoothSpring()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (expandedKey) {
            leftKey -> {
                left(Modifier.fillMaxWidth(), true)
                right(Modifier.fillMaxWidth(), false)
            }
            rightKey -> {
                right(Modifier.fillMaxWidth(), true)
                left(Modifier.fillMaxWidth(), false)
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    left(Modifier.weight(1f), false)
                    right(Modifier.weight(1f), false)
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
    minFreq: String,
    maxFreq: String,
    locked: Boolean,
    onClick: () -> Unit,
    onGovernorChange: (String) -> Unit,
    onMinFreqChange: (String) -> Unit,
    onMaxFreqChange: (String) -> Unit,
    onLockClick: () -> Unit
) {
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.Memory,
        title = "CPU",
        subtitle = "Governor & frequency lock",
        badge = if (locked) "Locked" else governor,
        active = active,
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        DropdownAction(
            title = "CPU Governor",
            value = governor,
            options = listOf("Schedutil", "Performance", "Battery", "Ondemand"),
            onSelect = onGovernorChange
        )

        SubCard(title = "Lock Frequency CPU", subtitle = "Atur min dan max frequency") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (locked) "CPU Frequency Locked" else "Lock CPU Frequency")
            }
        }
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
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.GridView,
        title = "GPU",
        subtitle = "Profile, frequency, renderer",
        badge = if (locked) "Locked" else profile,
        active = active || locked,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        DropdownAction(
            title = "Governor Profile GPU",
            value = profile,
            options = listOf("Battery", "Balanced", "Performance"),
            onSelect = onProfileChange
        )

        SubCard(title = "GPU Frequency Lock", subtitle = "Kunci batas frekuensi GPU") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (locked) "GPU Frequency Locked" else "Lock GPU Frequency")
            }
        }

        CompactFeatureBlock(
            icon = Icons.Outlined.Tune,
            title = "Renderer",
            subtitle = "Select GPU Renderer",
            trailing = renderer,
            onClick = onRendererClick
        )
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
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.Wifi,
        title = "Network",
        subtitle = "DNS private, stabilizer, TCP",
        badge = if (active) "Tuned" else "Off",
        active = active,
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        DropdownAction(
            title = "DNS Private",
            value = dnsProvider,
            options = listOf("Off", "Cloudflare", "Google", "Quad9", "CleanBrowsing", "Control D", "NextDNS"),
            onSelect = onDnsSelect
        )
        ToggleOption(
            title = "Stabilkan Network",
            subtitle = "Prioritas koneksi stabil dan latency rendah",
            checked = networkStable,
            onClick = onNetworkStableToggle
        )
        ToggleOption(
            title = "TCP",
            subtitle = "Optimasi TCP stack",
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
    killBackground: Boolean,
    onClick: () -> Unit,
    onZramToggle: () -> Unit,
    onLmkToggle: () -> Unit,
    onSwapToggle: () -> Unit,
    onKillBackgroundClick: () -> Unit
) {
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.Memory,
        title = "Memory",
        subtitle = "ZRAM, LMK, swap, cleaner",
        badge = if (active) "Boost" else "Off",
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        ToggleOption("ZRAM", "Kompresi RAM virtual", zram, onZramToggle)
        ToggleOption("LMK", "Low memory killer agresif", lmk, onLmkToggle)
        ToggleOption("Swap", "Bantu paging saat RAM penuh", swap, onSwapToggle)
        ToggleOption("Kill Background All", "Bersihkan proses latar belakang", killBackground, onKillBackgroundClick)
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
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.Storage,
        title = "I/O Scheduler",
        subtitle = "Disk read/write queue",
        badge = selected,
        active = selected != "Auto",
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        DropdownAction(
            title = "I/O Scheduler",
            value = selected,
            options = listOf("Auto", "CFQ", "Deadline", "Noop", "BFQ", "Maple"),
            onSelect = onSelect
        )
    }
}

@Composable
private fun SchedBoostCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    mode: String,
    onClick: () -> Unit,
    onSelect: (String) -> Unit
) {
    ExpandableTweakCard(
        modifier = modifier,
        icon = Icons.Outlined.Speed,
        title = "Sched Boost",
        subtitle = "Game booster scheduler",
        badge = if (active) mode else "Off",
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        Speedometer(active = active, mode = mode)
        DropdownAction(
            title = "Sched Boost Mode",
            value = mode,
            options = listOf("Off", "Auto", "Balanced", "Game", "Extreme"),
            onSelect = onSelect
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
    ExpandableTweakCard(
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Outlined.Thermostat,
        title = "Thermal Profile",
        subtitle = "Fitur muncul saat expand",
        badge = thermalLabel(current),
        active = current != "default",
        expanded = expanded,
        side = ExpandSide.Center,
        baseHeight = 92.dp,
        onClick = onClick
    ) {
        ProfileSelector(current = current, onSelect = onSelect)
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
    side: ExpandSide,
    baseHeight: Dp = 132.dp,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(220),
        label = "card_container_$title"
    )
    val iconBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "icon_bg_$title"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "icon_tint_$title"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "badge_bg_$title"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "badge_fg_$title"
    )
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.985f,
        animationSpec = smoothSpring(),
        label = "card_scale_$title"
    )
    val detailAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 260 else 140),
        label = "detail_alpha_$title"
    )
    val detailOffset by animateFloatAsState(
        targetValue = if (expanded) 0f else when (side) {
            ExpandSide.Left -> -24f
            ExpandSide.Right -> 24f
            ExpandSide.Center -> 0f
        },
        animationSpec = smoothSpring(),
        label = "detail_offset_$title"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (expanded) 28.dp else 24.dp),
        color = container,
        tonalElevation = if (expanded) 3.dp else 0.dp,
        modifier = modifier
            .heightIn(min = baseHeight)
            .animateContentSize(smoothSpring())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Spacer(Modifier.weight(1f))
                StatusPill(text = badge, active = active || expanded, bg = badgeBg, fg = badgeFg)
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

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(smoothSpring()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(smoothSpring())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = detailAlpha
                                    translationX = detailOffset
                                    scaleX = scale
                                    scaleY = scale
                                },
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = content
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppProfileCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
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
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Apps, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "App Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Klik untuk masuk ke AppProfileScreen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = "Open",
                active = true,
                bg = MaterialTheme.colorScheme.onPrimaryContainer,
                fg = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun DropdownAction(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(text = "Pilih", active = false)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
private fun ToggleOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(180),
        label = "toggle_bg_$title"
    )
    val fg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180),
        label = "toggle_fg_$title"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = fg)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.68f))
            }
        }
    }
}

@Composable
private fun CompactFeatureBlock(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(text = trailing, active = true)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun ProfileSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "default" to "Balance",
            "performance" to "Performance",
            "extreme" to "Extreme"
        ).forEach { (key, label) ->
            val selected = current == key
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = tween(180),
                label = "thermal_bg_$key"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(180),
                label = "thermal_fg_$key"
            )

            Surface(
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(15.dp),
                color = bg,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = when (key) {
                            "performance" -> Icons.Outlined.FlashOn
                            "extreme" -> Icons.Outlined.Speed
                            else -> Icons.Outlined.Thermostat
                        },
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Speedometer(
    active: Boolean,
    mode: String
) {
    val progress by animateFloatAsState(
        targetValue = if (active) {
            when (mode) {
                "Extreme" -> 0.96f
                "Game" -> 0.86f
                "Balanced" -> 0.64f
                "Auto" -> 0.52f
                else -> 0.28f
            }
        } else {
            0.18f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "speedometer_progress"
    )

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val needle = MaterialTheme.colorScheme.onSurface
    val percent = (progress * 100).toInt().coerceIn(0, 100)

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = track,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = primary,
                        startAngle = 150f,
                        sweepAngle = 240f * progress,
                        useCenter = false,
                        style = stroke
                    )

                    val angle = Math.toRadians((150f + 240f * progress).toDouble())
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val end = Offset(
                        x = center.x + cos(angle).toFloat() * 38.dp.toPx(),
                        y = center.y + sin(angle).toFloat() * 38.dp.toPx()
                    )
                    drawLine(color = needle, start = center, end = end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(color = primary, radius = 6.dp.toPx(), center = center)
                }
            }

            Text(
                text = if (active) "$mode Boost • $percent%" else "Boost Off • $percent%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(8) { index ->
                    val filled = progress >= (index + 1) / 8f
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
    val renderers = listOf("OpenGL", "Vulkan", "ANGLE", "SkiaVulkan", "SkiaGL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select GPU Renderer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                renderers.forEach { item ->
                    ToggleOption(
                        title = item,
                        subtitle = if (item == current) "Sedang dipilih" else "Tap untuk memilih",
                        checked = item == current,
                        onClick = { onSelect(item) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
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

private fun thermalLabel(value: String): String {
    return when (value) {
        "performance" -> "Performance"
        "extreme" -> "Extreme"
        else -> "Balance"
    }
}

private fun normalizeLabel(value: String): String {
    return when (value.lowercase()) {
        "cfq" -> "CFQ"
        "deadline" -> "Deadline"
        "noop" -> "Noop"
        "bfq" -> "BFQ"
        "maple" -> "Maple"
        else -> "Auto"
    }
}

private fun <T> smoothSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
