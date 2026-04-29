package dev.aether.manager.ui.tweak

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.UiState
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.components.*
import dev.aether.manager.ui.home.TabSectionTitle

@Composable
fun TweakScreen(vm: MainViewModel) {
    val s           = LocalStrings.current
    val tweaks      by vm.tweaks.collectAsState()
    val deviceState by vm.deviceInfo.collectAsState()
    val applying    by vm.applyingTweak.collectAsState()
    val scrollState = rememberScrollState()
    val context     = LocalContext.current
    val activity    = context as? android.app.Activity

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Performance Profile ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabSectionTitle(icon = Icons.Outlined.Tune, title = s.tweakPerformanceProfile)
                val current = (deviceState as? UiState.Success)?.data?.profile ?: "balance"
                ProfileGrid(current = current, onSelect = { vm.setProfile(it) })
            }

            // ── CPU & Kernel ──────────────────────────────────
            TweakSection(s.tweakSectionCpu) {
                TweakRow(Icons.Outlined.DateRange, s.tweakSchedBoost, s.tweakSchedBoostDesc, tweaks.schedboost) { vm.setTweak("schedboost", it) }
                ItemDivider()
                TweakRow(Icons.AutoMirrored.Outlined.TrendingUp, s.tweakCpuBoost, s.tweakCpuBoostDesc, tweaks.cpuBoost) { vm.setTweak("cpu_boost", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.GridView, s.tweakGpuThrottle, s.tweakGpuThrottleDesc, tweaks.gpuThrottleOff) { vm.setTweak("gpu_throttle_off", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Memory, s.tweakCpusetOpt, s.tweakCpusetOptDesc, tweaks.cpusetOpt) { vm.setTweak("cpuset_opt", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Speed, s.tweakMtkBoost, s.tweakMtkBoostDesc, tweaks.mtkBoost) { vm.setTweak("obb_noop", it) }
            }

            // ── CPU Freq Limiter ──────────────────────────────
            TweakSection(s.tweakSectionCpuFreq) {
                TweakRow(
                    Icons.Outlined.Tune,
                    s.tweakCpuFreqEnable,
                    s.tweakCpuFreqEnableDesc,
                    tweaks.cpuFreqEnable
                ) { vm.setTweak("cpu_freq_enable", it) }
                AnimatedVisibility(visible = tweaks.cpuFreqEnable) {
                    Column {
                        ItemDivider()
                        CpuClusterRow(
                            clusterName = s.tweakCpuClusterPrime,
                            icon        = Icons.Outlined.ElectricBolt,
                            accentColor = MaterialTheme.colorScheme.error,
                            minVal      = tweaks.cpuFreqPrimeMin,
                            maxVal      = tweaks.cpuFreqPrimeMax,
                            onMinSelect = { vm.setTweakStr("cpu_freq_prime_min", it) },
                            onMaxSelect = { vm.setTweakStr("cpu_freq_prime_max", it) }
                        )
                        ItemDivider()
                        CpuClusterRow(
                            clusterName = s.tweakCpuClusterGold,
                            icon        = Icons.Outlined.Star,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            minVal      = tweaks.cpuFreqGoldMin,
                            maxVal      = tweaks.cpuFreqGoldMax,
                            onMinSelect = { vm.setTweakStr("cpu_freq_gold_min", it) },
                            onMaxSelect = { vm.setTweakStr("cpu_freq_gold_max", it) }
                        )
                        ItemDivider()
                        CpuClusterRow(
                            clusterName = s.tweakCpuClusterSilver,
                            icon        = Icons.Outlined.Memory,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            minVal      = tweaks.cpuFreqSilverMin,
                            maxVal      = tweaks.cpuFreqSilverMax,
                            onMinSelect = { vm.setTweakStr("cpu_freq_silver_min", it) },
                            onMaxSelect = { vm.setTweakStr("cpu_freq_silver_max", it) }
                        )
                    }
                }
            }

            // ── Thermal Profile ───────────────────────────────
            TweakSection(s.tweakThermalProfile) {
                ThermalProfileRow(
                    desc    = s.tweakThermalDesc,
                    current = tweaks.thermalProfile,
                    labelDefault     = s.tweakThermalDefault,
                    labelPerformance = s.tweakThermalPerformance,
                    labelExtreme     = s.tweakThermalExtreme,
                    onSelect = { vm.setTweakStr("thermal_profile", it) }
                )
            }

            // ── GPU Freq Lock ─────────────────────────────────
            TweakSection("GPU") {
                TweakRow(Icons.Outlined.LockOpen, s.tweakGpuFreqLock, s.tweakGpuFreqLockDesc, tweaks.gpuFreqLock) { vm.setTweak("gpu_freq_lock", it) }
                AnimatedVisibility(visible = tweaks.gpuFreqLock) {
                    Column {
                        ItemDivider()
                        GpuFreqRow(
                            current  = tweaks.gpuFreqMax,
                            label    = s.tweakGpuFreqMax,
                            onSelect = { vm.setTweakStr("gpu_freq_max", it) }
                        )
                    }
                }
            }

            // ── Memory ────────────────────────────────────────
            TweakSection(s.tweakSectionMemory) {
                TweakRow(Icons.AutoMirrored.Outlined.Article, s.tweakLmk, s.tweakLmkDesc, tweaks.lmkAggressive) { vm.setTweak("lmk_aggressive", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Memory, s.tweakZram, s.tweakZramDesc, tweaks.zram) { vm.setTweak("zram", it) }
                ItemDivider()
                ZramSizeRow(tweaks.zramSize) { vm.setTweakStr("zram_size", it) }
                ItemDivider()
                ZramAlgoRow(tweaks.zramAlgo) { vm.setTweakStr("zram_algo", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Info, s.tweakVmDirty, s.tweakVmDirtyDesc, tweaks.vmDirtyOpt) { vm.setTweak("vm_dirty_opt", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.ContentCopy, s.tweakKsm, s.tweakKsmDesc, tweaks.ksm) { vm.setTweak("ksm", it) }
                AnimatedVisibility(visible = tweaks.ksm) {
                    Column {
                        ItemDivider()
                        TweakRow(Icons.Outlined.FastForward, s.tweakKsmAggressive, s.tweakKsmAggressiveDesc, tweaks.ksmAggressive) { vm.setTweak("ksm_aggressive", it) }
                    }
                }
            }

            // ── I/O ───────────────────────────────────────────
            TweakSection(s.tweakSectionIo) {
                IoSchedulerRow(tweaks.ioScheduler) { vm.setTweakStr("io_scheduler", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Add, s.tweakIoLatency, s.tweakIoLatencyDesc, tweaks.ioLatencyOpt) { vm.setTweak("io_latency_opt", it) }
            }

            // ── Network ───────────────────────────────────────
            TweakSection(s.tweakSectionNetwork) {
                TweakRow(Icons.Outlined.Language, s.tweakTcpBbr, s.tweakTcpBbrDesc, tweaks.tcpBbr) { vm.setTweak("tcp_bbr", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Lock, s.tweakDoh, s.tweakDohDesc, tweaks.doh) { vm.setTweak("doh", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.SwapHoriz, s.tweakNetBuffer, s.tweakNetBufferDesc, tweaks.netBuffer) { vm.setTweak("net_buffer", it) }
            }

            // ── Battery & Daily ───────────────────────────────
            TweakSection(s.tweakSectionBattery) {
                TweakRow(Icons.Outlined.TouchApp, s.tweakTouchBoost, s.tweakTouchBoostDesc, tweaks.touchBoost) { vm.setTweak("touch_boost", it) }
                AnimatedVisibility(visible = tweaks.touchBoost) {
                    Column {
                        ItemDivider()
                        TouchSampleRateRow(
                            current  = tweaks.touchSampleRate,
                            label    = s.tweakTouchSampleRate,
                            onSelect = { vm.setTweakStr("touch_sample_rate", it) }
                        )
                    }
                }
                ItemDivider()
                TweakRow(Icons.Outlined.NightsStay, s.tweakDoze, s.tweakDozeDesc, tweaks.doze) { vm.setTweak("doze", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Delete, s.tweakClearCache, s.tweakClearCacheDesc, tweaks.clearCache) { vm.setTweak("clear_cache", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Animation, s.tweakFastAnim, s.tweakFastAnimDesc, tweaks.fastAnim) { vm.setTweak("fast_anim", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Shuffle, s.tweakEntropy, s.tweakEntropyDesc, tweaks.entropyBoost) { vm.setTweak("entropy_boost", it) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabSectionTitle(icon = Icons.Outlined.Settings, title = title)
        Surface(
            shape          = RoundedCornerShape(20.dp),
            color          = MaterialTheme.colorScheme.surfaceContainerLow,
            border         = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            ),
            tonalElevation = 0.dp
        ) {
            Column(content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    CardItemRow(
        icon               = icon,
        title              = title,
        subtitle           = subtitle,
        iconContainerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerHighest,
        iconTint           = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                             else MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContent    = { AetherSwitch(checked = checked, onCheckedChange = onToggle) }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileGrid(current: String, onSelect: (String) -> Unit) {
    val profiles = listOf(
        Triple("balance",     "Balance",      "schedutil")        to Pair(Icons.Outlined.Balance,             MaterialTheme.colorScheme.primary),
        Triple("performance", "Performance",  "performance gov")  to Pair(Icons.Outlined.BatteryChargingFull, MaterialTheme.colorScheme.error),
        Triple("gaming",      "Gaming",       "schedutil+boost")  to Pair(Icons.Outlined.SportsEsports,       MaterialTheme.colorScheme.tertiary),
        Triple("battery",     "Battery Saver","powersave gov")    to Pair(Icons.Outlined.BatteryFull,         Color(0xFF2D7D46)),
    )

    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column {
            profiles.forEachIndexed { idx, (triple, iconColor) ->
                val (key, label, desc) = triple
                val (icon, accentColor) = iconColor
                val isActive = current == key
                Surface(
                    onClick = { onSelect(key) },
                    color   = if (isActive)
                                  accentColor.copy(alpha = 0.08f)
                              else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .run {
                                    if (isActive) this.background(
                                        accentColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(11.dp)
                                    ) else this.background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(11.dp)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null,
                                tint     = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(label,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface)
                            Text(desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isActive) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (idx < profiles.lastIndex)
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 68.dp, end = 16.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

private val CPU_FREQ_OPTIONS = listOf(
    "" to "Auto",
    "300000"  to "300 MHz",
    "480000"  to "480 MHz",
    "576000"  to "576 MHz",
    "672000"  to "672 MHz",
    "768000"  to "768 MHz",
    "864000"  to "864 MHz",
    "960000"  to "960 MHz",
    "1056000" to "1.05 GHz",
    "1152000" to "1.15 GHz",
    "1248000" to "1.25 GHz",
    "1344000" to "1.34 GHz",
    "1440000" to "1.44 GHz",
    "1536000" to "1.54 GHz",
    "1632000" to "1.63 GHz",
    "1728000" to "1.73 GHz",
    "1824000" to "1.82 GHz",
    "1920000" to "1.92 GHz",
    "2016000" to "2.02 GHz",
    "2112000" to "2.11 GHz",
    "2208000" to "2.21 GHz",
    "2304000" to "2.30 GHz",
    "2400000" to "2.40 GHz",
    "2496000" to "2.50 GHz",
    "2592000" to "2.59 GHz",
    "2688000" to "2.69 GHz",
    "2784000" to "2.78 GHz",
    "2880000" to "2.88 GHz",
    "3000000" to "3.00 GHz",
    "3187200" to "3.19 GHz",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CpuClusterRow(
    clusterName: String,
    icon: ImageVector,
    accentColor: Color,
    minVal: String,
    maxVal: String,
    onMinSelect: (String) -> Unit,
    onMaxSelect: (String) -> Unit,
) {
    var minExpanded by remember { mutableStateOf(false) }
    var maxExpanded by remember { mutableStateOf(false) }
    val s = LocalStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(clusterName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(s.tweakCpuFreqMin,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(expanded = minExpanded, onExpandedChange = { minExpanded = it }) {
                OutlinedTextField(
                    value         = CPU_FREQ_OPTIONS.find { it.first == minVal }?.second ?: "Auto",
                    onValueChange = {},
                    readOnly      = true,
                    modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(92.dp),
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(minExpanded) },
                    textStyle     = MaterialTheme.typography.labelSmall,
                    singleLine    = true
                )
                ExposedDropdownMenu(expanded = minExpanded, onDismissRequest = { minExpanded = false }) {
                    CPU_FREQ_OPTIONS.forEach { (v, l) ->
                        DropdownMenuItem(text = { Text(l, style = MaterialTheme.typography.bodySmall) },
                            onClick = { onMinSelect(v); minExpanded = false })
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(s.tweakCpuFreqMax,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(expanded = maxExpanded, onExpandedChange = { maxExpanded = it }) {
                OutlinedTextField(
                    value         = CPU_FREQ_OPTIONS.find { it.first == maxVal }?.second ?: "Auto",
                    onValueChange = {},
                    readOnly      = true,
                    modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(92.dp),
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(maxExpanded) },
                    textStyle     = MaterialTheme.typography.labelSmall,
                    singleLine    = true
                )
                ExposedDropdownMenu(expanded = maxExpanded, onDismissRequest = { maxExpanded = false }) {
                    CPU_FREQ_OPTIONS.forEach { (v, l) ->
                        DropdownMenuItem(text = { Text(l, style = MaterialTheme.typography.bodySmall) },
                            onClick = { onMaxSelect(v); maxExpanded = false })
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThermalProfileRow(
    desc: String,
    current: String,
    labelDefault: String,
    labelPerformance: String,
    labelExtreme: String,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        "default"     to labelDefault,
        "performance" to labelPerformance,
        "extreme"     to labelExtreme,
    )
    val colors = mapOf(
        "default"     to MaterialTheme.colorScheme.primary,
        "performance" to MaterialTheme.colorScheme.error,
        "extreme"     to Color(0xFFB00020),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Thermostat, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp))
            }
            Column {
                Text(LocalStrings.current.tweakThermalProfile,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { idx, (key, label) ->
                val isSelected = current == key
                val accent = colors[key] ?: MaterialTheme.colorScheme.primary
                SegmentedButton(
                    selected = isSelected,
                    onClick  = { onSelect(key) },
                    shape    = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                    colors   = SegmentedButtonDefaults.colors(
                        activeContainerColor  = accent.copy(alpha = 0.12f),
                        activeContentColor    = accent,
                        activeBorderColor     = accent,
                    ),
                    icon = { SegmentedButtonDefaults.Icon(isSelected) }
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

private val GPU_FREQ_OPTIONS = listOf(
    ""          to "Auto",
    "150000000" to "150 MHz",
    "205000000" to "205 MHz",
    "270000000" to "270 MHz",
    "315000000" to "315 MHz",
    "370000000" to "370 MHz",
    "410000000" to "410 MHz",
    "441600000" to "441 MHz",
    "490000000" to "490 MHz",
    "530000000" to "530 MHz",
    "571000000" to "571 MHz",
    "587000000" to "587 MHz",
    "625000000" to "625 MHz",
    "647000000" to "647 MHz",
    "670000000" to "670 MHz",
    "700000000" to "700 MHz",
    "725000000" to "725 MHz",
    "750000000" to "750 MHz",
    "800000000" to "800 MHz",
    "840000000" to "840 MHz",
    "900000000" to "900 MHz",
    "950000000" to "950 MHz",
    "1000000000" to "1.00 GHz",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuFreqRow(current: String, label: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DropdownRow(
        icon     = Icons.Outlined.GraphicEq,
        title    = label,
        subtitle = "Hz — lock GPU ke frekuensi target",
        value    = GPU_FREQ_OPTIONS.find { it.first == current }?.second ?: "Auto",
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        GPU_FREQ_OPTIONS.forEach { (v, l) ->
            DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TouchSampleRateRow(current: String, label: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "default" to "120 Hz (Default)",
        "high"    to "180 Hz (High)",
        "max"     to "240 Hz (Max)",
    )
    DropdownRow(
        icon     = Icons.Outlined.TouchApp,
        title    = label,
        subtitle = "Polling rate layar sentuh",
        value    = options.find { it.first == current }?.second ?: "120 Hz (Default)",
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { (v, l) ->
            DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZramSizeRow(current: String, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("536870912" to "512 MB", "1073741824" to "1 GB", "2147483648" to "2 GB", "3221225472" to "3 GB")
    DropdownRow(
        icon    = Icons.Outlined.Memory,
        title   = s.tweakZramSize,
        subtitle = "Ukuran compressed swap",
        value   = options.find { it.first == current }?.second ?: "1 GB",
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { (v, l) ->
            DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZramAlgoRow(current: String, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("lz4", "lzo", "zstd", "lz4hc")
    DropdownRow(
        icon    = Icons.AutoMirrored.Outlined.CompareArrows,
        title   = s.tweakZramAlgo,
        subtitle = "Kompresi — LZ4 paling cepat",
        value   = current.ifBlank { "lz4" },
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { opt ->
            DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IoSchedulerRow(current: String, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("" to "Default", "none" to "none (UFS)", "noop" to "noop (eMMC)",
        "cfq" to "cfq", "deadline" to "deadline", "mq-deadline" to "mq-deadline",
        "bfq" to "bfq", "kyber" to "kyber")
    DropdownRow(
        icon    = Icons.AutoMirrored.Outlined.Sort,
        title   = s.tweakIoScheduler,
        subtitle = "Algoritma I/O",
        value   = options.find { it.first == current }?.second ?: "Default",
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { (v, l) ->
            DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(
                MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpand) {
            OutlinedTextField(
                value         = value,
                onValueChange = {},
                readOnly      = true,
                modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(105.dp),
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                textStyle     = MaterialTheme.typography.bodySmall,
                singleLine    = true
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
                menuContent()
            }
        }
    }
}
