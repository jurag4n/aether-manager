package dev.aether.manager.ui.tweak

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.aether.manager.data.UiState
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.RootManager
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun TweakScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {},
    onOpenDeviceInfo: () -> Unit = {}
) {
    val tweaks by vm.tweaks.collectAsState()
    val scroll = rememberScrollState()

    var expandedCard by rememberSaveable { mutableStateOf<String?>(null) }
    var activeProfile by rememberSaveable { mutableStateOf(tweaks.profile) }
    var thermalProfile by rememberSaveable {
        mutableStateOf(if (tweaks.thermalProfile.isBlank()) "default" else tweaks.thermalProfile)
    }

    var cpuGovernor by rememberSaveable {
        mutableStateOf(if (tweaks.cpuBoost) "Performance" else "Schedutil")
    }
    var minCpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxCpuFreq by rememberSaveable { mutableStateOf("2200") }
    var cpuFreqLocked by rememberSaveable { mutableStateOf(tweaks.cpuFreqLock) }

    var gpuProfile by rememberSaveable {
        mutableStateOf(if (tweaks.gpuThrottleOff) "Performance" else "Balanced")
    }
    var minGpuFreq by rememberSaveable { mutableStateOf("300") }
    var maxGpuFreq by rememberSaveable { mutableStateOf("850") }
    var gpuLocked by rememberSaveable { mutableStateOf(tweaks.gpuFreqLock) }
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

    LaunchedEffect(tweaks) {
        activeProfile = tweaks.profile.ifBlank { "balance" }
        thermalProfile = tweaks.thermalProfile.ifBlank { "default" }
        cpuFreqLocked = tweaks.cpuFreqLock
        gpuLocked = tweaks.gpuFreqLock
        networkStable = tweaks.networkStable
        tcpEnabled = tweaks.tcpBbr
        swapEnabled = tweaks.swap
        killBackgroundActive = tweaks.killBackground
        dnsProvider = tweaks.dnsProvider.ifBlank { "Off" }
        ioScheduler = if (tweaks.ioScheduler.isBlank()) "Auto" else normalizeLabel(tweaks.ioScheduler)
        schedBoostMode = if (tweaks.schedboost) "Game" else "Off"
        cpuGovernor = when {
            tweaks.cpuGovernor.contains("performance", ignoreCase = true) -> "Performance"
            tweaks.cpuGovernor.contains("powersave", ignoreCase = true) -> "Battery"
            else -> "Schedutil"
        }
        gpuProfile = if (tweaks.gpuThrottleOff) "Performance" else "Balanced"
    }

    fun toggleExpand(key: String) {
        expandedCard = if (expandedCard == key) null else key
    }

    fun applyNow() {
        vm.applyAll()
    }

    fun setTweakNow(key: String, value: Boolean) {
        // vm.setTweak sudah masuk apply worker conflated. Jangan panggil applyAll lagi
        // karena double-apply bikin root shell rebutan dan fitur terasa tidak stabil.
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
        vm.setTweakStr("thermal_profile", profile)
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
        DeviceInfoCard(onClick = onOpenDeviceInfo)

        ActiveProfileCard(
            expanded = expandedCard == "active_profile",
            current = activeProfile,
            onClick = { toggleExpand("active_profile") },
            onSelect = { setActiveProfileNow(it) }
        )

        AdaptiveTweakGridRow(
            expandedKey = expandedCard,
            leftKey = "cpu",
            rightKey = "gpu",
            left = { itemModifier ->
                CpuCard(
                    modifier = itemModifier,
                    expanded = expandedCard == "cpu",
                    active = tweaks.cpuBoost || cpuFreqLocked,
                    governor = cpuGovernor,
                    minFreq = minCpuFreq,
                    maxFreq = maxCpuFreq,
                    locked = cpuFreqLocked,
                    onClick = { toggleExpand("cpu") },
                    onGovernorChange = {
                        cpuGovernor = it
                        vm.setTweakStr("cpu_governor", when (it) {
                            "Battery" -> "powersave"
                            "Performance" -> "performance"
                            "Ondemand" -> "ondemand"
                            else -> "schedutil"
                        })
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
            right = { itemModifier ->
                GpuCard(
                    modifier = itemModifier,
                    expanded = expandedCard == "gpu",
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
                        vm.setTweakStr("gpu_freq_max", maxGpuFreq.filter { ch -> ch.isDigit() }.toLongOrNull()?.let { mhz -> (mhz * 1000000L).toString() } ?: "")
                        setTweakNow("gpuFreqLock", gpuLocked)
                    },
                    onRendererClick = { rendererDialog = true }
                )
            }
        )

        AppProfileCard(onClick = onOpenAppProfile)

        AdaptiveTweakGridRow(
            expandedKey = expandedCard,
            leftKey = "network",
            rightKey = "memory",
            left = { itemModifier ->
                NetworkCard(
                    modifier = itemModifier,
                    expanded = expandedCard == "network",
                    active = dnsProvider != "Off" || networkStable || tcpEnabled || tweaks.tcpBbr,
                    dnsProvider = dnsProvider,
                    networkStable = networkStable,
                    tcpEnabled = tcpEnabled || tweaks.tcpBbr,
                    onClick = { toggleExpand("network") },
                    onDnsSelect = { provider ->
                        dnsProvider = provider
                        vm.setTweakStr("dnsProvider", provider)
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
            right = { itemModifier ->
                MemoryCard(
                    modifier = itemModifier,
                    expanded = expandedCard == "memory",
                    active = tweaks.zram || tweaks.lmkAggressive || swapEnabled || killBackgroundActive,
                    zram = tweaks.zram,
                    zramSize = tweaks.zramSize,
                    zramAlgo = tweaks.zramAlgo,
                    lmk = tweaks.lmkAggressive,
                    swap = swapEnabled,
                    killBackground = killBackgroundActive,
                    onClick = { toggleExpand("memory") },
                    onZramToggle = { setTweakNow("zram", !tweaks.zram) },
                    onZramSizeSelect = { vm.setTweakStr("zram_size", it) },
                    onZramAlgoSelect = { vm.setTweakStr("zram_algo", it) },
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

        AdaptiveTweakGridRow(
            expandedKey = expandedCard,
            leftKey = "io",
            rightKey = "sched",
            left = { itemModifier ->
                IoSchedulerCard(
                    modifier = itemModifier,
                    expanded = expandedCard == "io",
                    selected = ioScheduler,
                    onClick = { toggleExpand("io") },
                    onSelect = {
                        ioScheduler = it
                        val schedulerValue = when (it) {
                            "CFQ" -> "cfq"
                            "Deadline" -> "deadline"
                            "Noop" -> "noop"
                            "BFQ" -> "bfq"
                            "Maple" -> "maple"
                            else -> ""
                        }
                        vm.setTweakStr("io_scheduler", schedulerValue)
                        vm.setTweak("io_latency_opt", it != "Auto")
                    }
                )
            },
            right = { itemModifier ->
                SchedBoostCard(
                    modifier = itemModifier,
                    expanded = expandedCard == "sched",
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
private fun AdaptiveTweakGridRow(
    expandedKey: String?,
    leftKey: String,
    rightKey: String,
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit
) {
    val expandedLeft = expandedKey == leftKey
    val expandedRight = expandedKey == rightKey

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = !expandedRight,
            enter = fadeIn(tween(150, delayMillis = 20, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { it / 6 } +
                expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            exit = fadeOut(tween(90, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    animationSpec = tween(125, easing = FastOutSlowInEasing)
                ) { it / 8 } +
                shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(170, easing = FastOutSlowInEasing)
                )
        ) {
            if (expandedLeft) {
                TweakGridSlot(expanded = true) { left(Modifier.fillMaxWidth()) }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TweakGridSlot(modifier = Modifier.weight(1f), expanded = false) {
                        left(Modifier.fillMaxWidth())
                    }
                    if (!expandedRight) {
                        TweakGridSlot(modifier = Modifier.weight(1f), expanded = false) {
                            right(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expandedRight,
            enter = fadeIn(tween(150, delayMillis = 20, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { it / 6 } +
                expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            exit = fadeOut(tween(90, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    animationSpec = tween(125, easing = FastOutSlowInEasing)
                ) { it / 8 } +
                shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(170, easing = FastOutSlowInEasing)
                )
        ) {
            TweakGridSlot(expanded = true) { right(Modifier.fillMaxWidth()) }
        }

        if (expandedLeft || expandedRight) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (expandedLeft) {
                    TweakGridSlot(modifier = Modifier.weight(1f), expanded = false) {
                        right(Modifier.fillMaxWidth())
                    }
                } else {
                    TweakGridSlot(modifier = Modifier.weight(1f), expanded = false) {
                        left(Modifier.fillMaxWidth())
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TweakGridSlot(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1.01f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tweak_grid_slot_scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        content()
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
        icon = Icons.Outlined.Terminal,
        title = "CPU",
        subtitle = "Governor & frequency lock",
        badge = if (locked) "Locked" else governor,
        active = active,
        expanded = expanded,
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
        icon = Icons.Outlined.Layers,
        title = "GPU",
        subtitle = "Profile, frequency, renderer",
        badge = if (locked) "Locked" else profile,
        active = active || locked,
        expanded = expanded,
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
        icon = Icons.Outlined.NetworkCheck,
        title = "Network",
        subtitle = "DNS private, stabilizer, TCP",
        badge = if (active) "Tuned" else "Off",
        active = active,
        expanded = expanded,
        onClick = onClick
    ) {
        DropdownAction(
            title = "DNS Private",
            value = dnsProvider,
            options = listOf("Off", "AdGuard", "Cloudflare", "Google", "CleanBrowsing"),
            onSelect = onDnsSelect
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToggleOption(
                modifier = Modifier.weight(1f),
                title = "Stabilkan Network",
                subtitle = "Prioritas koneksi stabil dan latency rendah",
                checked = networkStable,
                onClick = onNetworkStableToggle
            )
            ToggleOption(
                modifier = Modifier.weight(1f),
                title = "TCP",
                subtitle = "Optimasi TCP stack",
                checked = tcpEnabled,
                onClick = onTcpToggle
            )
        }
    }
}

@Composable
private fun MemoryCard(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    active: Boolean,
    zram: Boolean,
    zramSize: String,
    zramAlgo: String,
    lmk: Boolean,
    swap: Boolean,
    killBackground: Boolean,
    onClick: () -> Unit,
    onZramToggle: () -> Unit,
    onZramSizeSelect: (String) -> Unit,
    onZramAlgoSelect: (String) -> Unit,
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
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToggleOption(
                modifier = Modifier.weight(1f),
                title = "ZRAM",
                subtitle = "Kompresi RAM virtual",
                checked = zram,
                onClick = onZramToggle
            )
            ToggleOption(
                modifier = Modifier.weight(1f),
                title = "LMK",
                subtitle = "Low memory killer agresif",
                checked = lmk,
                onClick = onLmkToggle
            )
        }
        if (zram) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DropdownAction(
                    modifier = Modifier.weight(1f),
                    title = "ZRAM Size",
                    value = when (zramSize) {
                        "536870912" -> "512 MB"
                        "1073741824" -> "1 GB"
                        "1610612736" -> "1.5 GB"
                        "2147483648" -> "2 GB"
                        "3221225472" -> "3 GB"
                        "4294967296" -> "4 GB"
                        else -> "1 GB"
                    },
                    options = listOf("512 MB", "1 GB", "1.5 GB", "2 GB", "3 GB", "4 GB"),
                    onSelect = { label ->
                        onZramSizeSelect(
                            when (label) {
                                "512 MB" -> "536870912"
                                "1 GB" -> "1073741824"
                                "1.5 GB" -> "1610612736"
                                "2 GB" -> "2147483648"
                                "3 GB" -> "3221225472"
                                "4 GB" -> "4294967296"
                                else -> "1073741824"
                            }
                        )
                    }
                )
                DropdownAction(
                    modifier = Modifier.weight(1f),
                    title = "ZRAM Algo",
                    value = zramAlgo.uppercase(),
                    options = listOf("LZ4", "LZO", "ZSTD", "DEFLATE"),
                    onSelect = { onZramAlgoSelect(it.lowercase()) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToggleOption(
                modifier = Modifier.weight(1f),
                title = "Swap",
                subtitle = "Bantu paging saat RAM penuh",
                checked = swap,
                onClick = onSwapToggle
            )
            ToggleOption(
                modifier = Modifier.weight(1f),
                title = "Kill Background",
                subtitle = "Bersihkan proses latar",
                checked = killBackground,
                onClick = onKillBackgroundClick
            )
        }
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
        icon = Icons.Outlined.RocketLaunch,
        title = "Sched Boost",
        subtitle = "Game booster scheduler",
        badge = if (active) mode else "Off",
        active = active,
        expanded = expanded,
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
    baseHeight: Dp = 132.dp,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.985f
            expanded -> 1.01f
            else -> 1f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "tweak_card_scale_$title"
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) 28.dp else 24.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "tweak_card_corner_$title"
    )
    val detailAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 180 else 100, easing = FastOutSlowInEasing),
        label = "tweak_detail_alpha_$title"
    )
    val detailOffset by animateFloatAsState(
        targetValue = if (expanded) 0f else 16f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "tweak_detail_offset_$title"
    )
    val iconBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tweak_icon_bg_$title"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tweak_icon_tint_$title"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tweak_badge_bg_$title"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tweak_badge_fg_$title"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (expanded) 3.dp else 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (expanded) 0.32f else 0.22f)
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .heightIn(min = baseHeight)
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
                        .background(iconBg, RoundedCornerShape(14.dp)),
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

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(durationMillis = 150, delayMillis = 25, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) { it / 5 } +
                    expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                exit = fadeOut(tween(durationMillis = 95, easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        animationSpec = tween(125, easing = FastOutSlowInEasing)
                    ) { it / 7 } +
                    shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(175, easing = FastOutSlowInEasing)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = detailAlpha
                            translationY = detailOffset
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(onClick: () -> Unit) {
    val deviceName = rememberDeviceName()
    val androidVersion = "Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}"
    val bootloader = remember { resolveBootloaderLabel() }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeveloperMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$androidVersion • Bootloader $bootloader",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = "Detail",
                active = true,
                bg = MaterialTheme.colorScheme.primary,
                fg = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun DeviceInfoScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val infoState by vm.deviceInfo.collectAsState()
    val bootloader = remember { resolveBootloaderLabel() }
    val buildId = remember { valueOrUnknown(Build.ID) }
    val fingerprint = remember { valueOrUnknown(Build.FINGERPRINT) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Device Info",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Device, root, SELinux, build, kernel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            when (val state = infoState) {
                is UiState.Loading -> {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Loading device info", fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                is UiState.Error -> {
                    DeviceInfoSection(
                        title = "Status",
                        rows = listOf("Error" to state.msg)
                    )
                }
                is UiState.Success -> {
                    val info = state.data
                    DeviceInfoHero(info, bootloader)
                    DeviceInfoSection(
                        title = "Device",
                        rows = listOf(
                            "Device ID" to buildId,
                            "Model" to valueOrUnknown(info.model),
                            "Brand" to valueOrUnknown(Build.BRAND),
                            "Manufacturer" to valueOrUnknown(Build.MANUFACTURER),
                            "Device" to valueOrUnknown(Build.DEVICE),
                            "Product" to valueOrUnknown(Build.PRODUCT),
                            "Board" to valueOrUnknown(Build.BOARD),
                            "Hardware" to valueOrUnknown(Build.HARDWARE)
                        )
                    )
                    DeviceInfoSection(
                        title = "Build",
                        rows = listOf(
                            "Android" to "${valueOrUnknown(info.android)} / API ${Build.VERSION.SDK_INT}",
                            "Fingerprint" to fingerprint,
                            "Build Type" to valueOrUnknown(Build.TYPE),
                            "Build Tags" to valueOrUnknown(Build.TAGS),
                            "Bootloader" to bootloader,
                            "Host" to valueOrUnknown(Build.HOST)
                        )
                    )
                    DeviceInfoSection(
                        title = "Root & Kernel",
                        rows = listOf(
                            "Root Status" to if (RootManager.isRootGranted) "Granted" else "Not granted",
                            "Root Type" to valueOrUnknown(info.rootType),
                            "SELinux" to valueOrUnknown(info.selinux),
                            "Kernel" to valueOrUnknown(info.kernel),
                            "Chipset" to info.soc.label,
                            "Raw SoC" to valueOrUnknown(info.socRaw),
                            "Profile" to valueOrUnknown(info.profile),
                            "Safe Mode" to if (info.safeMode) "On" else "Off",
                            "Boot Count" to info.bootCount.toString()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoHero(info: DeviceInfo, bootloader: String) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.DeveloperMode,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(27.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = valueOrUnknown(info.model),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${info.soc.label} • Android ${valueOrUnknown(info.android)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = valueOrUnknown(info.rootType),
                    active = RootManager.isRootGranted,
                    bg = if (RootManager.isRootGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    fg = if (RootManager.isRootGranted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(
                    text = valueOrUnknown(info.selinux),
                    active = info.selinux.equals("Enforcing", true),
                    bg = MaterialTheme.colorScheme.surfaceContainerHigh,
                    fg = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(
                    text = "BL $bootloader",
                    active = bootloader != "Unavailable",
                    bg = MaterialTheme.colorScheme.surfaceContainerHigh,
                    fg = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoSection(
    title: String,
    rows: List<Pair<String, String>>
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                    )
                }
                DeviceInfoLine(label = label, value = value)
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
        verticalAlignment = Alignment.Top,
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.14f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun valueOrUnknown(value: String?): String {
    val cleaned = value.orEmpty().trim()
    return if (cleaned.isBlank() || cleaned.equals("unknown", true) || cleaned == "?") "Unknown" else cleaned
}

private fun resolveBootloaderLabel(): String {
    val candidates = listOf(
        Build.BOOTLOADER,
        readSystemProperty("ro.bootloader"),
        readSystemProperty("ro.boot.bootloader"),
        readSystemProperty("ro.boot.bootloader_version"),
        readSystemProperty("ro.product.bootloader"),
        readSystemProperty("gsm.version.baseband")
    )
    return candidates.firstOrNull { raw ->
        val v = raw.orEmpty().trim()
        v.isNotBlank() && !v.equals("unknown", true) && v != "?"
    } ?: "Unavailable"
}

private fun readSystemProperty(name: String): String {
    return runCatching {
        val process = ProcessBuilder("getprop", name).redirectErrorStream(true).start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.destroy()
        result
    }.getOrDefault("")
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
                icon = Icons.Outlined.Balance,
                selected = current == "balance",
                onSelect = onSelect
            )
            ProfileChip(
                modifier = Modifier.weight(1f),
                key = "performance",
                label = "Performance",
                icon = Icons.Outlined.Bolt,
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
                icon = Icons.Outlined.BatteryChargingFull,
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
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .height(96.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "App Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Kelola profil aplikasi",
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
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxWidth()
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
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxWidth()
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
                icon = Icons.Outlined.WbSunny,
                selected = current == "default" || current == "stock",
                onSelect = onSelect
            )
            ThermalChip(
                modifier = Modifier.weight(1f),
                key = "cool",
                label = "Cool",
                icon = Icons.Outlined.AcUnit,
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
                icon = Icons.Outlined.SportsEsports,
                selected = current == "gaming",
                onSelect = onSelect
            )
            ThermalChip(
                modifier = Modifier.weight(1f),
                key = "throttle_off",
                label = "Throttle Off",
                icon = Icons.Outlined.PowerSettingsNew,
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
        "performance" -> Icons.Outlined.Bolt
        "extreme" -> Icons.Outlined.Speed
        "battery", "powersave" -> Icons.Outlined.BatteryChargingFull
        else -> Icons.Outlined.Balance
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
    stiffness = Spring.StiffnessMedium
)

private fun <T> snappySpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh
)
