package dev.aether.manager.ui.tweak

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.ApplyStatus
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.TweaksState
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.home.TabSectionTitle

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TweakScreen(vm: MainViewModel) {
    val s      = LocalStrings.current
    val tweaks by vm.tweaks.collectAsState()
    val status by vm.applyStatus.collectAsState()
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Profile Selector ─────────────────────────────────────
            ProfileSection(
                current  = tweaks.thermalProfile,
                onSelect = { vm.setProfile(it) }
            )

            // ── CPU Section ──────────────────────────────────────────
            TweakSection(
                icon  = Icons.Outlined.Memory,
                title = s.tweakSectionCpu
            ) {
                TweakToggleRow(
                    icon      = Icons.Outlined.FlashOn,
                    title     = s.tweakSchedBoost,
                    subtitle  = s.tweakSchedBoostDesc,
                    checked   = tweaks.schedboost,
                    onToggle  = { vm.setTweak("schedboost", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Speed,
                    title     = s.tweakCpuBoost,
                    subtitle  = s.tweakCpuBoostDesc,
                    checked   = tweaks.cpuBoost,
                    onToggle  = { vm.setTweak("cpuBoost", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.GridView,
                    title     = s.tweakGpuThrottle,
                    subtitle  = s.tweakGpuThrottleDesc,
                    checked   = tweaks.gpuThrottleOff,
                    onToggle  = { vm.setTweak("gpuThrottleOff", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Dashboard,
                    title     = s.tweakCpusetOpt,
                    subtitle  = s.tweakCpusetOptDesc,
                    checked   = tweaks.cpusetOpt,
                    onToggle  = { vm.setTweak("cpusetOpt", it) }
                )
                if (tweaks.mtkBoost != null) { // MTK only
                    TweakDivider()
                    TweakToggleRow(
                        icon      = Icons.Outlined.Bolt,
                        title     = s.tweakMtkBoost,
                        subtitle  = s.tweakMtkBoostDesc,
                        checked   = tweaks.mtkBoost ?: false,
                        onToggle  = { vm.setTweak("mtkBoost", it) }
                    )
                }
            }

            // ── CPU Frequency Section ────────────────────────────────
            TweakSection(
                icon  = Icons.Outlined.Tune,
                title = s.tweakSectionCpuFreq
            ) {
                TweakToggleRow(
                    icon      = Icons.Outlined.SwapVert,
                    title     = s.tweakCpuFreqEnable,
                    subtitle  = s.tweakCpuFreqEnableDesc,
                    checked   = tweaks.cpuFreqEnable,
                    onToggle  = { vm.setTweak("cpuFreqEnable", it) }
                )
                AnimatedVisibility(visible = tweaks.cpuFreqEnable) {
                    Column {
                        TweakDivider()
                        CpuFreqCluster(
                            label    = s.tweakCpuClusterPrime,
                            minValue = tweaks.cpuFreqPrimeMin,
                            maxValue = tweaks.cpuFreqPrimeMax,
                            onMin    = { vm.setTweakStr("cpuFreqPrimeMin", it) },
                            onMax    = { vm.setTweakStr("cpuFreqPrimeMax", it) },
                            color    = MaterialTheme.colorScheme.error
                        )
                        TweakDivider()
                        CpuFreqCluster(
                            label    = s.tweakCpuClusterGold,
                            minValue = tweaks.cpuFreqGoldMin,
                            maxValue = tweaks.cpuFreqGoldMax,
                            onMin    = { vm.setTweakStr("cpuFreqGoldMin", it) },
                            onMax    = { vm.setTweakStr("cpuFreqGoldMax", it) },
                            color    = MaterialTheme.colorScheme.tertiary
                        )
                        TweakDivider()
                        CpuFreqCluster(
                            label    = s.tweakCpuClusterSilver,
                            minValue = tweaks.cpuFreqSilverMin,
                            maxValue = tweaks.cpuFreqSilverMax,
                            onMin    = { vm.setTweakStr("cpuFreqSilverMin", it) },
                            onMax    = { vm.setTweakStr("cpuFreqSilverMax", it) },
                            color    = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Memory Section ───────────────────────────────────────
            TweakSection(
                icon  = Icons.Outlined.Dns,
                title = s.tweakSectionMemory
            ) {
                TweakToggleRow(
                    icon      = Icons.Outlined.ManageSearch,
                    title     = s.tweakLmk,
                    subtitle  = s.tweakLmkDesc,
                    checked   = tweaks.lmkAggressive,
                    onToggle  = { vm.setTweak("lmkAggressive", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Compress,
                    title     = s.tweakZram,
                    subtitle  = s.tweakZramDesc,
                    checked   = tweaks.zram,
                    onToggle  = { vm.setTweak("zram", it) }
                )
                AnimatedVisibility(visible = tweaks.zram) {
                    Column {
                        TweakDivider()
                        TweakDropdownRow(
                            icon    = Icons.Outlined.Storage,
                            label   = s.tweakZramSize,
                            options = listOf(
                                "536870912"  to "512 MB",
                                "1073741824" to "1 GB",
                                "1610612736" to "1.5 GB",
                                "2147483648" to "2 GB"
                            ),
                            selected  = tweaks.zramSize,
                            onSelect  = { vm.setTweakStr("zramSize", it) }
                        )
                        TweakDivider()
                        TweakDropdownRow(
                            icon    = Icons.Outlined.Code,
                            label   = s.tweakZramAlgo,
                            options = listOf(
                                "lz4"    to "LZ4",
                                "zstd"   to "ZSTD",
                                "lzo"    to "LZO",
                                "lzo-rle" to "LZO-RLE"
                            ),
                            selected  = tweaks.zramAlgo,
                            onSelect  = { vm.setTweakStr("zramAlgo", it) }
                        )
                    }
                }
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.AutoAwesome,
                    title     = s.tweakVmDirty,
                    subtitle  = s.tweakVmDirtyDesc,
                    checked   = tweaks.vmDirtyOpt,
                    onToggle  = { vm.setTweak("vmDirtyOpt", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.ContentCopy,
                    title     = s.tweakKsm,
                    subtitle  = s.tweakKsmDesc,
                    checked   = tweaks.ksm,
                    onToggle  = { vm.setTweak("ksm", it) }
                )
                AnimatedVisibility(visible = tweaks.ksm) {
                    Column {
                        TweakDivider()
                        TweakToggleRow(
                            icon      = Icons.Outlined.DoubleArrow,
                            title     = s.tweakKsmAggressive,
                            subtitle  = s.tweakKsmAggressiveDesc,
                            checked   = tweaks.ksmAggressive,
                            onToggle  = { vm.setTweak("ksmAggressive", it) },
                            indented  = true
                        )
                    }
                }
            }

            // ── I/O Section ──────────────────────────────────────────
            TweakSection(
                icon  = Icons.Outlined.Storage,
                title = s.tweakSectionIo
            ) {
                TweakToggleRow(
                    icon      = Icons.Outlined.Timelapse,
                    title     = s.tweakIoLatency,
                    subtitle  = s.tweakIoLatencyDesc,
                    checked   = tweaks.ioLatencyOpt,
                    onToggle  = { vm.setTweak("ioLatencyOpt", it) }
                )
                TweakDivider()
                TweakDropdownRow(
                    icon    = Icons.Outlined.SortByAlpha,
                    label   = s.tweakIoScheduler,
                    options = listOf(
                        ""        to "Auto",
                        "cfq"     to "CFQ",
                        "deadline" to "Deadline",
                        "noop"    to "Noop",
                        "bfq"     to "BFQ",
                        "mq-deadline" to "MQ-Deadline"
                    ),
                    selected  = tweaks.ioScheduler,
                    onSelect  = { vm.setTweakStr("ioScheduler", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Psychology,
                    title     = s.tweakEntropy,
                    subtitle  = s.tweakEntropyDesc,
                    checked   = tweaks.entropyBoost,
                    onToggle  = { vm.setTweak("entropyBoost", it) }
                )
            }

            // ── Network Section ──────────────────────────────────────
            TweakSection(
                icon  = Icons.Outlined.Wifi,
                title = s.tweakSectionNetwork
            ) {
                TweakToggleRow(
                    icon      = Icons.Outlined.NetworkCheck,
                    title     = s.tweakTcpBbr,
                    subtitle  = s.tweakTcpBbrDesc,
                    checked   = tweaks.tcpBbr,
                    onToggle  = { vm.setTweak("tcpBbr", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Dns,
                    title     = s.tweakDoh,
                    subtitle  = s.tweakDohDesc,
                    checked   = tweaks.doh,
                    onToggle  = { vm.setTweak("doh", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Cached,
                    title     = s.tweakNetBuffer,
                    subtitle  = s.tweakNetBufferDesc,
                    checked   = tweaks.netBuffer,
                    onToggle  = { vm.setTweak("netBuffer", it) }
                )
            }

            // ── Battery & UX Section ─────────────────────────────────
            TweakSection(
                icon  = Icons.Outlined.BatteryFull,
                title = s.tweakSectionBattery
            ) {
                TweakToggleRow(
                    icon      = Icons.Outlined.BatterySaver,
                    title     = s.tweakDoze,
                    subtitle  = s.tweakDozeDesc,
                    checked   = tweaks.doze,
                    onToggle  = { vm.setTweak("doze", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Animation,
                    title     = s.tweakFastAnim,
                    subtitle  = s.tweakFastAnimDesc,
                    checked   = tweaks.fastAnim,
                    onToggle  = { vm.setTweak("fastAnim", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Vibration,
                    title     = s.tweakTouchBoost,
                    subtitle  = s.tweakTouchBoostDesc,
                    checked   = tweaks.touchBoost,
                    onToggle  = { vm.setTweak("touchBoost", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.CleaningServices,
                    title     = s.tweakClearCache,
                    subtitle  = s.tweakClearCacheDesc,
                    checked   = tweaks.clearCache,
                    onToggle  = { vm.setTweak("clearCache", it) }
                )
                TweakDivider()
                TweakToggleRow(
                    icon      = Icons.Outlined.Lock,
                    title     = s.tweakGpuFreqLock,
                    subtitle  = s.tweakGpuFreqLockDesc,
                    checked   = tweaks.gpuFreqLock,
                    onToggle  = { vm.setTweak("gpuFreqLock", it) }
                )
            }
        }

        // ── Floating Apply Button ────────────────────────────────────
        ApplyBar(
            status    = status,
            onApply   = { vm.applyAll() },
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Selector (Chips row)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    current: String,
    onSelect: (String) -> Unit
) {
    val s = LocalStrings.current

    val profiles = listOf(
        Triple("default",     s.tweakThermalDefault,     Icons.Outlined.Balance),
        Triple("performance", s.tweakThermalPerformance, Icons.Outlined.FlashOn),
        Triple("extreme",     s.tweakThermalExtreme,     Icons.Filled.Bolt)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabSectionTitle(
            icon  = Icons.Outlined.Tune,
            title = s.tweakPerformanceProfile
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            profiles.forEach { (key, label, icon) ->
                val selected = current == key
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    tween(200), label = "profile_bg"
                )
                val fg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(200), label = "profile_fg"
                )
                Surface(
                    onClick = { onSelect(key) },
                    shape   = RoundedCornerShape(14.dp),
                    color   = bg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier            = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            icon, null,
                            tint     = fg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            label,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = fg
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Container
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabSectionTitle(icon = icon, title = title)
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toggle Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    indented: Boolean = false
) {
    val iconTint by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "toggle_icon"
    )
    val iconBg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200), label = "toggle_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start  = if (indented) 28.dp else 16.dp,
                end    = 16.dp,
                top    = 14.dp,
                bottom = 14.dp
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor  = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dropdown Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakDropdownRow(
    icon: ImageVector,
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.first == selected }?.second ?: selected

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            label,
            modifier   = Modifier.weight(1f),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )

        Box {
            Surface(
                onClick = { expanded = true },
                shape   = RoundedCornerShape(10.dp),
                color   = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        displayLabel,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown, null,
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded          = expanded,
                onDismissRequest  = { expanded = false },
                shape             = RoundedCornerShape(14.dp)
            ) {
                options.forEach { (value, display) ->
                    DropdownMenuItem(
                        text   = {
                            Text(
                                display,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected == value) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (selected == value) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        },
                        leadingIcon = if (selected == value) ({
                            Icon(
                                Icons.Filled.Check, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }) else null
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CPU Freq Cluster Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CpuFreqCluster(
    label: String,
    minValue: String,
    maxValue: String,
    onMin: (String) -> Unit,
    onMax: (String) -> Unit,
    color: Color
) {
    val s = LocalStrings.current

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(8.dp)
        ) {}

        Text(
            label,
            modifier   = Modifier.weight(1f),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FreqInput(
                hint     = "${s.tweakCpuFreqMin}: ${minValue.ifBlank { "Auto" }}",
                value    = minValue,
                onChange = onMin,
                color    = color
            )
            FreqInput(
                hint     = "${s.tweakCpuFreqMax}: ${maxValue.ifBlank { "Auto" }}",
                value    = maxValue,
                onChange = onMax,
                color    = color
            )
        }
    }
}

@Composable
private fun FreqInput(
    hint: String, value: String,
    onChange: (String) -> Unit, color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Text(
            hint,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style      = MaterialTheme.typography.labelSmall,
            color      = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Apply Bottom Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ApplyBar(
    status: ApplyStatus,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier      = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape         = RoundedCornerShape(20.dp),
        color         = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status indicator
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(
                    targetState    = status,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label          = "apply_status"
                ) { s ->
                    if (s.running) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Menerapkan…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (s.summary.isNotBlank()) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (s.lastOk) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                null,
                                tint     = if (s.lastOk) Color(0xFF2D7D46)
                                           else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                s.summary,
                                style  = MaterialTheme.typography.bodySmall,
                                color  = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
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

            // Apply button
            Button(
                onClick  = onApply,
                enabled  = !status.running,
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                if (status.running) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Outlined.PlayArrow, null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Apply",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakDivider() = HorizontalDivider(
    modifier  = Modifier.padding(start = 66.dp, end = 16.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    thickness = 0.5.dp
)
