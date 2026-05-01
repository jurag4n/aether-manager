package dev.aether.manager.ui.tweak

import android.os.Build
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    // Track expansion state of the Device Info card separately from other cards
    var deviceInfoExpanded by rememberSaveable { mutableStateOf(false) }
    var activeProfile by rememberSaveable { mutableStateOf("balance") }
    var thermalProfile by rememberSaveable {
        mutableStateOf(if (tweaks.thermalProfile.isBlank()) "default" else tweaks.thermalProfile)
    }

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

    var dnsProvider by rememberSaveable {
        mutableStateOf(
            if (tweaks.dnsProvider.isBlank()) "Off" else tweaks.dnsProvider
        )
    }
    var networkStable by rememberSaveable { mutableStateOf(tweaks.networkStable) }
    var tcpEnabled by rememberSaveable { mutableStateOf(tweaks.tcpBbr) }

    var swapEnabled by rememberSaveable { mutableStateOf(tweaks.swap) }
    var killBackgroundActive by rememberSaveable { mutableStateOf(tweaks.killBackground) }

    var ioScheduler by rememberSaveable {
        mutableStateOf(if (tweaks.ioScheduler.isBlank()) "Auto" else normalizeLabel(tweaks.ioScheduler))
    }
    var schedBoostMode by rememberSaveable {
        mutableStateOf(if (tweaks.schedboost) "Game" else "Off")
    }

    LaunchedEffect(tweaks.thermalProfile, tweaks.dnsProvider, tweaks.networkStable, tweaks.tcpBbr, tweaks.swap, tweaks.killBackground) {
        thermalProfile = tweaks.thermalProfile.ifBlank { "default" }
        dnsProvider = tweaks.dnsProvider.ifBlank { "Off" }
        networkStable = tweaks.networkStable
        tcpEnabled = tweaks.tcpBbr
        swapEnabled = tweaks.swap
        killBackgroundActive = tweaks.killBackground
    }

    fun normalizeGovernor(value: String): String = when (value) {
        "Performance" -> "performance"
        "Battery" -> "powersave"
        "Ondemand" -> "ondemand"
        else -> "schedutil"
    }

    fun normalizeIoScheduler(value: String): String = when (value) {
        "CFQ" -> "cfq"
        "Deadline" -> "deadline"
        "Noop" -> "noop"
        "BFQ" -> "bfq"
        "Maple" -> "maple"
        else -> ""
    }

    fun normalizeGpuFreqHz(value: String): String {
        val n = value.filter { it.isDigit() }.toLongOrNull() ?: return ""
        return when {
            n <= 0L -> ""
            n < 10_000L -> (n * 1_000_000L).toString()
            n < 10_000_000L -> (n * 1_000L).toString()
            else -> n.toString()
        }
    }

    fun toggleExpand(key: String) {
        expandedCard = if (expandedCard == key) null else key
    }

    fun applyNow() {
        vm.applyAll()
    }

    fun setTweakNow(key: String, value: Boolean) {
        vm.setTweak(key, value)
    }

    fun setActiveProfileNow(profile: String) {
        activeProfile = profile

        when (profile) {
            "performance" -> {
                cpuGovernor = "Performance"
                gpuProfile = "Performance"
                schedBoostMode = "Game"
                vm.setTweak("cpuBoost", true)
                vm.setTweak("gpuThrottleOff", true)
                vm.setTweak("schedboost", true)
            }
            "extreme" -> {
                cpuGovernor = "Performance"
                gpuProfile = "Performance"
                schedBoostMode = "Extreme"
                networkStable = true
                tcpEnabled = true
                vm.setTweak("cpuBoost", true)
                vm.setTweak("gpuThrottleOff", true)
                vm.setTweak("schedboost", true)
                vm.setTweak("networkStable", true)
                vm.setTweak("tcpBbr", true)
            }
            "battery" -> {
                cpuGovernor = "Battery"
                gpuProfile = "Battery"
                schedBoostMode = "Off"
                vm.setTweak("cpuBoost", false)
                vm.setTweak("gpuThrottleOff", false)
                vm.setTweak("schedboost", false)
            }
            else -> {
                cpuGovernor = "Schedutil"
                gpuProfile = "Balanced"
                schedBoostMode = "Off"
                vm.setTweak("cpuBoost", false)
                vm.setTweak("gpuThrottleOff", false)
                vm.setTweak("schedboost", false)
            }
        }

        vm.setProfile(profile)
    }

    fun setThermalProfileNow(profile: String) {
        thermalProfile = profile
        vm.setThermalProfile(profile)
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
        // Device info is collapsed by default and expands when clicked
        DeviceInfoCard(
            expanded = deviceInfoExpanded,
            onClick = { deviceInfoExpanded = !deviceInfoExpanded }
        )

        ActiveProfileCard(
            expanded = expandedCard == "active_profile",
            current = activeProfile,
            onClick = { toggleExpand("active_profile") },
            onSelect = { setActiveProfileNow(it) }
        )

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
                        vm.setTweakStr("cpu_governor", normalizeGovernor(it))
                        setTweakNow("cpuBoost", it == "Performance")
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
                        vm.setTweakStr("gpu_freq_max", normalizeGpuFreqHz(maxGpuFreq))
                        setTweakNow("gpuFreqLock", gpuLocked)
                        setTweakNow("gpuThrottleOff", gpuLocked || gpuProfile == "Performance")
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
                    onDnsSelect = { provider ->
                        dnsProvider = provider
                        vm.setTweakStr("dns_provider", if (provider == "Off") "" else provider)
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
                        setTweakNow("killBackground", killBackgroundActive)
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
                        vm.setTweakStr("io_scheduler", normalizeIoScheduler(it))
                        setTweakNow("ioLatencyOpt", it != "Auto")
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
            current = thermalProfile,
            onClick = { toggleExpand("thermal") },
            onSelect = { setThermalProfileNow(it) }
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
        subtitle = "Thermal tweak terpisah",
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
        animationSpec = tween(300),
        label = "card_container_$title"
    )
    val iconBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(260),
        label = "icon_bg_$title"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(260),
        label = "icon_tint_$title"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(260),
        label = "badge_bg_$title"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(260),
        label = "badge_fg_$title"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.99f,
        animationSpec = smoothSpring(),
        label = "card_scale_$title"
    )
    val detailAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 320 else 220),
        label = "detail_alpha_$title"
    )
    val detailScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.965f,
        animationSpec = smoothSpring(),
        label = "detail_scale_$title"
    )
    val detailOffset by animateFloatAsState(
        targetValue = if (expanded) 0f else when (side) {
            ExpandSide.Left -> -18f
            ExpandSide.Right -> 18f
            ExpandSide.Center -> 0f
        },
        animationSpec = smoothSpring(),
        label = "detail_offset_$title"
    )
    val showDetails = expanded || detailAlpha > 0.02f

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (expanded) 30.dp else 24.dp),
        color = container,
        tonalElevation = if (expanded) 4.dp else 0.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
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

            if (showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = detailAlpha
                            translationX = detailOffset
                            scaleX = detailScale
                            scaleY = detailScale
                        }
                        .animateContentSize(smoothSpring()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    expanded: Boolean,
    onClick: () -> Unit
) {
    // Gather device info values
    val deviceName = rememberDeviceName()
    val codeName = Build.DEVICE ?: "Unknown"
    val androidVersion = "Android ${Build.VERSION.RELEASE}"
    val kernel = System.getProperty("os.version") ?: "Unknown"

    // Use a Surface with an onClick to provide default Material ripple
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            // Animate height changes smoothly
            .animateContentSize(smoothSpring())
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row: icon and title; removed the "Live" status chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Device Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ringkas dan clean",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Details section: only visible when expanded
            if (expanded) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Fade and scale the details for a subtle appearance animation
                            alpha = 1f
                            scaleX = 1f
                            scaleY = 1f
                        }
                        .animateContentSize(smoothSpring())
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DeviceInfoLine(label = "Nama Perangkat", value = deviceName)
                        DeviceInfoLine(label = "Android", value = "$androidVersion / API ${Build.VERSION.SDK_INT}")
                        DeviceInfoLine(label = "CodeName", value = codeName)
                        DeviceInfoLine(label = "Kernel", value = kernel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.14f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActiveProfileCard(
    expanded: Boolean,
    current: String,
    onClick: () -> Unit,
    onSelect: (String) -> Unit
) {
    val label = activeProfileLabel(current)

    ExpandableTweakCard(
        modifier = Modifier.fillMaxWidth(),
        icon = activeProfileIcon(current),
        title = label,
        subtitle = "Active Profile • CPU/GPU",
        badge = "Active",
        active = true,
        expanded = expanded,
        side = ExpandSide.Center,
        baseHeight = 96.dp,
        onClick = onClick
    ) {
        ProfileModeSelector(current = current, onSelect = onSelect)
    }
}

@Composable
private fun ProfileModeSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileChip(
                modifier = Modifier.weight(1f),
                key = "balance",
                label = "Balance",
                icon = Icons.Outlined.Tune,
                selected = current == "balance",
                onSelect = onSelect
            )
            ProfileChip(
                modifier = Modifier.weight(1f),
                key = "performance",
                label = "Performance",
                icon = Icons.Outlined.FlashOn,
                selected = current == "performance",
                onSelect = onSelect
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileChip(
                modifier = Modifier.weight(1f),
                key = "extreme",
                label = "Extreme",
                icon = Icons.Outlined.Speed,
                selected = current == "extreme",
                onSelect = onSelect
            )
            ProfileChip(
                modifier = Modifier.weight(1f),
                key = "battery",
                label = "Battery",
                icon = Icons.Outlined.Memory,
                selected = current == "battery" || current == "powersave",
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun ProfileChip(
    modifier: Modifier = Modifier,
    key: String,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "profile_chip_bg_$key"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "profile_chip_fg_$key"
    )

    Surface(
        onClick = { onSelect(key) },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun rememberDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val model = Build.MODEL.orEmpty()
    return when {
        model.isBlank() -> manufacturer.ifBlank { "Unknown" }
        manufacturer.isBlank() -> model
        model.lowercase().startsWith(manufacturer.lowercase()) -> model
        else -> "$manufacturer $model"
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

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { item ->
                        ModernDropdownItem(
                            text = item,
                            selected = item == value,
                            onClick = {
                                expanded = false
                                onSelect(item)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(text = "Select", active = true)
        }
    }
}

@Composable
private fun ModernDropdownItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(200),
        label = "modern_dropdown_bg_$text"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "modern_dropdown_fg_$text"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThermalChip(
                modifier = Modifier.weight(1f),
                key = "default",
                label = "Stock",
                icon = Icons.Outlined.Thermostat,
                selected = current == "default" || current == "stock",
                onSelect = onSelect
            )
            ThermalChip(
                modifier = Modifier.weight(1f),
                key = "cool",
                label = "Cool",
                icon = Icons.Outlined.Thermostat,
                selected = current == "cool",
                onSelect = onSelect
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThermalChip(
                modifier = Modifier.weight(1f),
                key = "gaming",
                label = "Gaming",
                icon = Icons.Outlined.Speed,
                selected = current == "gaming",
                onSelect = onSelect
            )
            ThermalChip(
                modifier = Modifier.weight(1f),
                key = "throttle_off",
                label = "Throttle Off",
                icon = Icons.Outlined.FlashOn,
                selected = current == "throttle_off",
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun ThermalChip(
    modifier: Modifier = Modifier,
    key: String,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(180),
        label = "thermal_chip_bg_$key"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "thermal_chip_fg_$key"
    )

    Surface(
        onClick = { onSelect(key) },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = modifier.height(62.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    return when (value.lowercase()) {
        "cool" -> "Cool"
        "gaming" -> "Gaming"
        "throttle_off" -> "Throttle Off"
        else -> "Stock"
    }
}

private fun activeProfileLabel(value: String): String {
    return when (value.lowercase()) {
        "performance" -> "Performance"
        "extreme" -> "Extreme"
        "battery", "powersave" -> "Battery"
        else -> "Balance"
    }
}

private fun activeProfileIcon(value: String): ImageVector {
    return when (value.lowercase()) {
        "performance" -> Icons.Outlined.FlashOn
        "extreme" -> Icons.Outlined.Speed
        "battery", "powersave" -> Icons.Outlined.Memory
        else -> Icons.Outlined.Tune
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
    // A softer spring for smoother, gentler animations
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessVeryLow
)
