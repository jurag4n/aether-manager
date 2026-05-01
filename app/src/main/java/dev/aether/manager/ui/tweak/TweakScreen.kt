package dev.aether.manager.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import dev.aether.manager.data.MainViewModel

private enum class ExpandSide { Left, Right, Center }

@Composable
fun TweakScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {}
) {
    val tweaks by vm.tweaks.collectAsState()
    val scroll = rememberScrollState()

    var expandedCard by rememberSaveable { mutableStateOf<String?>(null) }
    var cpuGovernor by rememberSaveable { mutableStateOf(if (tweaks.cpuBoost) "Performance" else "Schedutil") }
    var gpuProfile by rememberSaveable { mutableStateOf(if (tweaks.gpuThrottleOff) "Performance" else "Balanced") }
    var minGpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxGpuFreq by rememberSaveable { mutableStateOf("850") }
    var gpuLocked by rememberSaveable { mutableStateOf(false) }
    var renderer by rememberSaveable { mutableStateOf("OpenGL") }
    var showRendererDialog by rememberSaveable { mutableStateOf(false) }
    var privateDns by rememberSaveable { mutableStateOf(false) }
    var networkStable by rememberSaveable { mutableStateOf(false) }
    var swapBoost by rememberSaveable { mutableStateOf(false) }
    var killBackground by rememberSaveable { mutableStateOf(false) }
    var ioScheduler by rememberSaveable { mutableStateOf(if (tweaks.ioScheduler.isBlank()) "Auto" else tweaks.ioScheduler.uppercase()) }

    fun setExpanded(key: String) {
        expandedCard = if (expandedCard == key) null else key
    }

    fun applyTweak(key: String, value: Boolean) {
        vm.setTweak(key, value)
        vm.applyAll()
    }

    fun applyProfile(profile: String) {
        vm.setProfile(profile)
        vm.applyAll()
    }

    if (showRendererDialog) {
        RendererDialog(
            current = renderer,
            onDismiss = { showRendererDialog = false },
            onSelect = {
                renderer = it
                showRendererDialog = false
                vm.applyAll()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionLabel("Core Tweak")

        PrimaryTweakLayout(
            expandedCard = expandedCard,
            cpuGovernor = cpuGovernor,
            gpuProfile = gpuProfile,
            minGpuFreq = minGpuFreq,
            maxGpuFreq = maxGpuFreq,
            gpuLocked = gpuLocked,
            renderer = renderer,
            cpuActive = tweaks.cpuBoost,
            gpuActive = tweaks.gpuThrottleOff,
            onExpand = ::setExpanded,
            onCpuGovernorChange = {
                cpuGovernor = it
                applyTweak("cpuBoost", it == "Performance")
            },
            onGpuProfileChange = {
                gpuProfile = it
                applyTweak("gpuThrottleOff", it != "Battery")
            },
            onMinGpuFreqChange = { minGpuFreq = it },
            onMaxGpuFreqChange = { maxGpuFreq = it },
            onLockGpu = {
                gpuLocked = !gpuLocked
                applyTweak("gpuThrottleOff", gpuLocked)
            },
            onRendererClick = { showRendererDialog = true }
        )

        AppProfileShortcutCard(onClick = onOpenAppProfile)

        SectionLabel("Network & Memory")

        ExpandableFeatureGrid(
            expandedCard = expandedCard,
            onExpand = ::setExpanded,
            networkActive = tweaks.tcpBbr || privateDns || networkStable,
            memoryActive = tweaks.zram || tweaks.lmkAggressive || swapBoost || killBackground,
            ioScheduler = ioScheduler,
            schedBoostActive = tweaks.schedboost,
            thermalProfile = tweaks.thermalProfile,
            privateDns = privateDns,
            networkStable = networkStable,
            tcpBbr = tweaks.tcpBbr,
            zram = tweaks.zram,
            lmk = tweaks.lmkAggressive,
            swapBoost = swapBoost,
            killBackground = killBackground,
            onPrivateDnsToggle = {
                privateDns = !privateDns
                applyTweak("privateDns", privateDns)
            },
            onNetworkStableToggle = {
                networkStable = !networkStable
                applyTweak("networkStable", networkStable)
            },
            onTcpToggle = { applyTweak("tcpBbr", !tweaks.tcpBbr) },
            onZramToggle = { applyTweak("zram", !tweaks.zram) },
            onLmkToggle = { applyTweak("lmkAggressive", !tweaks.lmkAggressive) },
            onSwapToggle = {
                swapBoost = !swapBoost
                applyTweak("swapBoost", swapBoost)
            },
            onKillBackgroundToggle = {
                killBackground = !killBackground
                applyTweak("killBackground", killBackground)
            },
            onIoSchedulerSelect = {
                ioScheduler = it
                vm.applyAll()
            },
            onSchedBoostToggle = { applyTweak("schedboost", !tweaks.schedboost) },
            onThermalSelect = ::applyProfile
        )
    }
}

@Composable
private fun PrimaryTweakLayout(
    expandedCard: String?,
    cpuGovernor: String,
    gpuProfile: String,
    minGpuFreq: String,
    maxGpuFreq: String,
    gpuLocked: Boolean,
    renderer: String,
    cpuActive: Boolean,
    gpuActive: Boolean,
    onExpand: (String) -> Unit,
    onCpuGovernorChange: (String) -> Unit,
    onGpuProfileChange: (String) -> Unit,
    onMinGpuFreqChange: (String) -> Unit,
    onMaxGpuFreqChange: (String) -> Unit,
    onLockGpu: () -> Unit,
    onRendererClick: () -> Unit
) {
    Column(
        modifier = Modifier.animateContentSize(smoothSpring()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedContent(
            targetState = expandedCard,
            transitionSpec = {
                val fromRight = targetState == "gpu" || initialState == "gpu"
                val dir = if (fromRight) 1 else -1
                (slideInHorizontally(tween(260)) { dir * it / 3 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(180)) { -dir * it / 4 } + fadeOut(tween(150))) using
                    SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> smoothSpring() })
            },
            label = "primary_card_content"
        ) { target ->
            when (target) {
                "cpu" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CpuControlCard(
                        expanded = true,
                        active = cpuActive,
                        governor = cpuGovernor,
                        side = ExpandSide.Left,
                        onClick = { onExpand("cpu") },
                        onGovernorChange = onCpuGovernorChange
                    )
                    GpuControlCard(
                        expanded = false,
                        active = gpuActive,
                        profile = gpuProfile,
                        minFreq = minGpuFreq,
                        maxFreq = maxGpuFreq,
                        locked = gpuLocked,
                        renderer = renderer,
                        side = ExpandSide.Right,
                        onClick = { onExpand("gpu") },
                        onProfileChange = onGpuProfileChange,
                        onMinFreqChange = onMinGpuFreqChange,
                        onMaxFreqChange = onMaxGpuFreqChange,
                        onLockClick = onLockGpu,
                        onRendererClick = onRendererClick
                    )
                }
                "gpu" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GpuControlCard(
                        expanded = true,
                        active = gpuActive,
                        profile = gpuProfile,
                        minFreq = minGpuFreq,
                        maxFreq = maxGpuFreq,
                        locked = gpuLocked,
                        renderer = renderer,
                        side = ExpandSide.Right,
                        onClick = { onExpand("gpu") },
                        onProfileChange = onGpuProfileChange,
                        onMinFreqChange = onMinGpuFreqChange,
                        onMaxFreqChange = onMaxGpuFreqChange,
                        onLockClick = onLockGpu,
                        onRendererClick = onRendererClick
                    )
                    CpuControlCard(
                        expanded = false,
                        active = cpuActive,
                        governor = cpuGovernor,
                        side = ExpandSide.Left,
                        onClick = { onExpand("cpu") },
                        onGovernorChange = onCpuGovernorChange
                    )
                }
                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CpuControlCard(
                        modifier = Modifier.weight(1f),
                        expanded = false,
                        active = cpuActive,
                        governor = cpuGovernor,
                        side = ExpandSide.Left,
                        onClick = { onExpand("cpu") },
                        onGovernorChange = onCpuGovernorChange
                    )
                    GpuControlCard(
                        modifier = Modifier.weight(1f),
                        expanded = false,
                        active = gpuActive,
                        profile = gpuProfile,
                        minFreq = minGpuFreq,
                        maxFreq = maxGpuFreq,
                        locked = gpuLocked,
                        renderer = renderer,
                        side = ExpandSide.Right,
                        onClick = { onExpand("gpu") },
                        onProfileChange = onGpuProfileChange,
                        onMinFreqChange = onMinGpuFreqChange,
                        onMaxFreqChange = onMaxGpuFreqChange,
                        onLockClick = onLockGpu,
                        onRendererClick = onRendererClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuControlCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    governor: String,
    side: ExpandSide,
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
        side = side,
        onClick = onClick
    ) {
        SelectMenu(
            label = "CPU Governor",
            value = governor,
            options = listOf("Schedutil", "Performance", "Battery", "Ondemand"),
            onSelect = onGovernorChange
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniActionChip(
                text = "Schedutil",
                selected = governor == "Schedutil",
                modifier = Modifier.weight(1f),
                onClick = { onGovernorChange("Schedutil") }
            )
            MiniActionChip(
                text = "Performance",
                selected = governor == "Performance",
                modifier = Modifier.weight(1f),
                onClick = { onGovernorChange("Performance") }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MiniActionChip(
                text = "Battery",
                selected = governor == "Battery",
                modifier = Modifier.weight(1f),
                onClick = { onGovernorChange("Battery") }
            )
            MiniActionChip(
                text = "Ondemand",
                selected = governor == "Ondemand",
                modifier = Modifier.weight(1f),
                onClick = { onGovernorChange("Ondemand") }
            )
        }
    }
}

@Composable
private fun GpuControlCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    profile: String,
    minFreq: String,
    maxFreq: String,
    locked: Boolean,
    renderer: String,
    side: ExpandSide,
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
        subtitle = "Governor & renderer",
        badge = profile,
        active = active || locked,
        expanded = expanded,
        side = side,
        onClick = onClick
    ) {
        SubCard(title = "Governor Profile GPU", subtitle = "Pilih mode performa GPU") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Battery", "Balanced", "Performance").forEach { item ->
                    MiniActionChip(
                        text = item,
                        selected = profile == item,
                        modifier = Modifier.weight(1f),
                        onClick = { onProfileChange(item) }
                    )
                }
            }
        }

        SubCard(title = "GPU Frequency Lock", subtitle = "Kunci batas frekuensi GPU") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
                Text(if (locked) "Frequency Locked" else "Lock Frequency")
            }
        }

        SubCard(title = "Renderer", subtitle = "Select GPU Renderer") {
            Surface(
                onClick = onRendererClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        renderer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AppProfileShortcutCard(onClick: () -> Unit) {
    val scale by animateFloatAsState(1f, smoothSpring(), label = "app_profile_scale")

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "App Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Atur tweak khusus per aplikasi",
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
                modifier = Modifier.rotate(-90f).size(22.dp)
            )
        }
    }
}

@Composable
private fun ExpandableFeatureGrid(
    expandedCard: String?,
    onExpand: (String) -> Unit,
    networkActive: Boolean,
    memoryActive: Boolean,
    ioScheduler: String,
    schedBoostActive: Boolean,
    thermalProfile: String,
    privateDns: Boolean,
    networkStable: Boolean,
    tcpBbr: Boolean,
    zram: Boolean,
    lmk: Boolean,
    swapBoost: Boolean,
    killBackground: Boolean,
    onPrivateDnsToggle: () -> Unit,
    onNetworkStableToggle: () -> Unit,
    onTcpToggle: () -> Unit,
    onZramToggle: () -> Unit,
    onLmkToggle: () -> Unit,
    onSwapToggle: () -> Unit,
    onKillBackgroundToggle: () -> Unit,
    onIoSchedulerSelect: (String) -> Unit,
    onSchedBoostToggle: () -> Unit,
    onThermalSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.animateContentSize(smoothSpring()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (expandedCard == "network") {
            NetworkCard(
                expanded = true,
                active = networkActive,
                privateDns = privateDns,
                networkStable = networkStable,
                tcpBbr = tcpBbr,
                onClick = { onExpand("network") },
                onPrivateDnsToggle = onPrivateDnsToggle,
                onNetworkStableToggle = onNetworkStableToggle,
                onTcpToggle = onTcpToggle
            )
            MemoryCard(
                expanded = false,
                active = memoryActive,
                zram = zram,
                lmk = lmk,
                swapBoost = swapBoost,
                killBackground = killBackground,
                onClick = { onExpand("memory") },
                onZramToggle = onZramToggle,
                onLmkToggle = onLmkToggle,
                onSwapToggle = onSwapToggle,
                onKillBackgroundToggle = onKillBackgroundToggle
            )
        } else if (expandedCard == "memory") {
            MemoryCard(
                expanded = true,
                active = memoryActive,
                zram = zram,
                lmk = lmk,
                swapBoost = swapBoost,
                killBackground = killBackground,
                onClick = { onExpand("memory") },
                onZramToggle = onZramToggle,
                onLmkToggle = onLmkToggle,
                onSwapToggle = onSwapToggle,
                onKillBackgroundToggle = onKillBackgroundToggle
            )
            NetworkCard(
                expanded = false,
                active = networkActive,
                privateDns = privateDns,
                networkStable = networkStable,
                tcpBbr = tcpBbr,
                onClick = { onExpand("network") },
                onPrivateDnsToggle = onPrivateDnsToggle,
                onNetworkStableToggle = onNetworkStableToggle,
                onTcpToggle = onTcpToggle
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                NetworkCard(
                    modifier = Modifier.weight(1f),
                    expanded = false,
                    active = networkActive,
                    privateDns = privateDns,
                    networkStable = networkStable,
                    tcpBbr = tcpBbr,
                    onClick = { onExpand("network") },
                    onPrivateDnsToggle = onPrivateDnsToggle,
                    onNetworkStableToggle = onNetworkStableToggle,
                    onTcpToggle = onTcpToggle
                )
                MemoryCard(
                    modifier = Modifier.weight(1f),
                    expanded = false,
                    active = memoryActive,
                    zram = zram,
                    lmk = lmk,
                    swapBoost = swapBoost,
                    killBackground = killBackground,
                    onClick = { onExpand("memory") },
                    onZramToggle = onZramToggle,
                    onLmkToggle = onLmkToggle,
                    onSwapToggle = onSwapToggle,
                    onKillBackgroundToggle = onKillBackgroundToggle
                )
            }
        }

        if (expandedCard == "io") {
            IoSchedulerCard(
                expanded = true,
                selected = ioScheduler,
                onClick = { onExpand("io") },
                onSelect = onIoSchedulerSelect
            )
        } else if (expandedCard == "sched") {
            SchedBoostCard(
                expanded = true,
                active = schedBoostActive,
                onClick = { onExpand("sched") },
                onToggle = onSchedBoostToggle
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                IoSchedulerCard(
                    modifier = Modifier.weight(1f),
                    expanded = false,
                    selected = ioScheduler,
                    onClick = { onExpand("io") },
                    onSelect = onIoSchedulerSelect
                )
                SchedBoostCard(
                    modifier = Modifier.weight(1f),
                    expanded = false,
                    active = schedBoostActive,
                    onClick = { onExpand("sched") },
                    onToggle = onSchedBoostToggle
                )
            }
        }

        ThermalProfileCard(
            expanded = expandedCard == "thermal",
            current = thermalProfile,
            onClick = { onExpand("thermal") },
            onSelect = onThermalSelect
        )
    }
}

@Composable
private fun NetworkCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    privateDns: Boolean,
    networkStable: Boolean,
    tcpBbr: Boolean,
    onClick: () -> Unit,
    onPrivateDnsToggle: () -> Unit,
    onNetworkStableToggle: () -> Unit,
    onTcpToggle: () -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Wifi,
        title = "Network",
        subtitle = "DNS, TCP, latency",
        badge = if (active) "Tuned" else "Off",
        active = active,
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        ToggleOptionRow("DNS Private", "Aktifkan mode private DNS", privateDns, onPrivateDnsToggle)
        ToggleOptionRow("Stabilkan Network", "Kurangi latency dan jitter", networkStable, onNetworkStableToggle)
        ToggleOptionRow("TCP", "Optimasi congestion control", tcpBbr, onTcpToggle)
    }
}

@Composable
private fun MemoryCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    zram: Boolean,
    lmk: Boolean,
    swapBoost: Boolean,
    killBackground: Boolean,
    onClick: () -> Unit,
    onZramToggle: () -> Unit,
    onLmkToggle: () -> Unit,
    onSwapToggle: () -> Unit,
    onKillBackgroundToggle: () -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Memory,
        title = "Memory",
        subtitle = "RAM cleaner & paging",
        badge = if (active) "Boost" else "Off",
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        ToggleOptionRow("ZRAM", "Kompresi RAM virtual", zram, onZramToggle)
        ToggleOptionRow("LMK", "Low memory killer agresif", lmk, onLmkToggle)
        ToggleOptionRow("Swap", "Bantu paging saat RAM penuh", swapBoost, onSwapToggle)
        ToggleOptionRow("Kill Background All", "Bersihkan proses latar belakang", killBackground, onKillBackgroundToggle)
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
        subtitle = "Disk read/write queue",
        badge = selected,
        active = selected != "Auto",
        expanded = expanded,
        side = ExpandSide.Left,
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Auto", "CFQ", "Deadline").forEach { item ->
                MiniActionChip(
                    text = item,
                    selected = selected == item,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(item) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Noop", "BFQ", "Maple").forEach { item ->
                MiniActionChip(
                    text = item,
                    selected = selected == item,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun SchedBoostCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    ExpandableCardShell(
        modifier = modifier,
        icon = Icons.Outlined.Speed,
        title = "Sched Boost",
        subtitle = "Game booster priority",
        badge = if (active) "Boost" else "Off",
        active = active,
        expanded = expanded,
        side = ExpandSide.Right,
        onClick = onClick
    ) {
        Speedometer(active = active)
        Button(
            onClick = onToggle,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Speed, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (active) "Disable Booster" else "Enable Booster")
        }
    }
}

@Composable
private fun ThermalProfileCard(
    expanded: Boolean,
    current: String,
    onClick: () -> Unit,
    onSelect: (String) -> Unit
) {
    val label = when (current) {
        "performance" -> "Performance"
        "extreme" -> "Extreme"
        else -> "Balance"
    }

    ExpandableCardShell(
        icon = Icons.Outlined.Thermostat,
        title = "Thermal Profile",
        subtitle = "Muncul saat card dibuka",
        badge = label,
        active = current != "default",
        expanded = expanded,
        side = ExpandSide.Center,
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileChip("Balance", current == "default", Modifier.weight(1f)) { onSelect("default") }
            ProfileChip("Performance", current == "performance", Modifier.weight(1f)) { onSelect("performance") }
            ProfileChip("Extreme", current == "extreme", Modifier.weight(1f)) { onSelect("extreme") }
        }
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
    content: @Composable () -> Unit
) {
    val cardColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(220),
        label = "card_color_$title"
    )
    val iconBg by animateColorAsState(
        if (active || expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(220),
        label = "icon_bg_$title"
    )
    val iconTint by animateColorAsState(
        if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220),
        label = "icon_tint_$title"
    )
    val badgeBg by animateColorAsState(
        if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(220),
        label = "badge_bg_$title"
    )
    val badgeFg by animateColorAsState(
        if (active || expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220),
        label = "badge_fg_$title"
    )
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, smoothSpring(), label = "arrow_$title")
    val pressScale by animateFloatAsState(if (expanded) 1f else 0.985f, smoothSpring(), label = "scale_$title")

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (expanded) 28.dp else 22.dp),
        color = cardColor,
        tonalElevation = if (expanded) 3.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .animateContentSize(smoothSpring())
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
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                        maxLines = 1
                    )
                }
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation).size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandEnter(side),
                exit = expandExit(side),
                label = "expand_$title"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SelectMenu(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
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
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
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
                        if (item == value) Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
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
    content: @Composable () -> Unit
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
private fun ToggleOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200),
        label = "toggle_bg_$title"
    )
    val fg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        tween(200),
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
                AnimatedVisibility(visible = checked, enter = scaleIn(tween(160)) + fadeIn(), exit = scaleOut(tween(120)) + fadeOut()) {
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
private fun MiniActionChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200),
        label = "chip_bg_$text"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200),
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
private fun ProfileChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MiniActionChip(text = text, selected = selected, modifier = modifier, onClick = onClick)
}

@Composable
private fun Speedometer(active: Boolean) {
    val power by animateFloatAsState(if (active) 1f else 0.34f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "speedometer_power")
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
                    modifier = Modifier.size(108.dp).scale(0.9f + power * 0.13f)
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
                    Text("Boost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(8) { index ->
                    val filled = power >= (index + 1) / 8f
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.weight(1f).height(8.dp)
                    ) {}
                }
            }
        }
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
private fun RendererDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf("OpenGL", "Vulkan", "ANGLE", "SkiaVulkan", "SkiaGL")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
        title = { Text("Select GPU Renderer", fontWeight = FontWeight.Bold) },
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

private fun <T> smoothSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private fun expandEnter(side: ExpandSide) =
    fadeIn(tween(180)) +
        expandVertically(animationSpec = smoothSpring()) +
        slideInHorizontally(animationSpec = smoothSpring()) { width ->
            when (side) {
                ExpandSide.Left -> -width / 4
                ExpandSide.Right -> width / 4
                ExpandSide.Center -> 0
            }
        }

private fun expandExit(side: ExpandSide) =
    fadeOut(tween(120)) +
        shrinkVertically(animationSpec = smoothSpring()) +
        slideOutHorizontally(animationSpec = tween(160)) { width ->
            when (side) {
                ExpandSide.Left -> -width / 5
                ExpandSide.Right -> width / 5
                ExpandSide.Center -> 0
            }
        }
