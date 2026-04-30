package dev.aether.manager.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.MonitorState
import dev.aether.manager.data.UiState
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.components.*
import dev.aether.manager.update.UpdateDialogHost
import dev.aether.manager.update.UpdateViewModel
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.SocType
import androidx.lifecycle.viewmodel.compose.viewModel

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(vm: MainViewModel) {
    val deviceState  by vm.deviceInfo.collectAsState()
    val monitorState by vm.monitorState.collectAsState()
    val scroll       = rememberScrollState()

    val updateVm: UpdateViewModel = viewModel()
    LaunchedEffect(Unit) { updateVm.checkUpdate() }
    UpdateDialogHost(viewModel = updateVm)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero device card
        AnimatedContent(
            targetState    = deviceState,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label          = "hero"
        ) { state ->
            when (state) {
                is UiState.Loading -> HeroSkeleton()
                is UiState.Success -> HeroCard(state.data)
                is UiState.Error   -> HeroError(state.msg) { vm.refresh() }
            }
        }

        // Bootloop banner
        AnimatedVisibility(
            visible = (deviceState as? UiState.Success)?.data?.bootCount?.let { it >= 2 } == true,
            enter   = fadeIn(tween(300)) + expandVertically(),
            exit    = fadeOut(tween(200)) + shrinkVertically()
        ) {
            val info = (deviceState as? UiState.Success)?.data
            if (info != null) BootloopBanner(info, vm)
        }

        // Monitor
        AnimatedVisibility(
            visible = deviceState is UiState.Success,
            enter   = fadeIn(tween(400, 100)) + slideInVertically { 24 }
        ) {
            MonitorSection(state = monitorState, onRefresh = { vm.refreshMonitor() })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared section label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TabSectionTitle(
    icon: ImageVector,
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(
                title,
                style         = MaterialTheme.typography.titleSmall,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.1.sp
            )
        }
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(info: DeviceInfo) {
    val s = LocalStrings.current
    Card(
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        info.model,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Android ${info.android}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
                SocBadge(info.soc)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoChip(s.homeLabelKernel, info.kernel.substringBefore("-").take(14),
                    Icons.Outlined.Code, Modifier.weight(1f))
                InfoChip(s.homeSelinux, info.selinux.ifBlank { "Unknown" },
                    Icons.Outlined.Shield, Modifier.weight(1f),
                    highlight = info.selinux.equals("Permissive", true))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monitor section — bento grid, no arcs, bold text style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitorSection(state: MonitorState, onRefresh: () -> Unit) {
    val s = LocalStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabSectionTitle(
            icon     = Icons.Outlined.Analytics,
            title    = s.homeMonitor,
            trailing = {
                IconButton(onClick = onRefresh, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Refresh, null,
                        modifier = Modifier.size(15.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )

        // Row 1 — CPU (full width)
        CpuCard(state)

        // Row 2 — GPU + Battery (2 equal columns)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GpuCard(state, Modifier.weight(1f))
            BatteryCard(state, Modifier.weight(1f))
        }

        // Row 3 — Temperature 2×2
        SectionLabel(Icons.Outlined.DeviceThermostat, "Temperature")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TempCard("CPU",     Icons.Outlined.Memory,      state.cpuTemp,     Color(0xFF1B74E8), 70f, Modifier.weight(1f))
            TempCard("GPU",     Icons.Outlined.GridView,    state.gpuTemp,     Color(0xFF7C4DFF), 75f, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TempCard("Thermal", Icons.Outlined.Thermostat,  state.thermalTemp, Color(0xFFE67E22), 55f, Modifier.weight(1f))
            TempCard("Battery", Icons.Outlined.BatteryFull, state.batTemp,     Color(0xFF2ECC71), 40f, Modifier.weight(1f))
        }

        // Row 4 — Memory / storage
        SectionLabel(Icons.Outlined.Dns, "Memory & System")
        StorageCard(state)
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.padding(top = 2.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Text(
            text,
            style         = MaterialTheme.typography.labelMedium,
            fontWeight    = FontWeight.SemiBold,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.2.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CPU — full width, bold freq + % badge + governor
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CpuCard(state: MonitorState) {
    val usageColor = when {
        state.cpuUsage >= 90 -> MaterialTheme.colorScheme.error
        state.cpuUsage >= 70 -> MaterialTheme.colorScheme.tertiary
        else                 -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon container
            Box(
                modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(usageColor.copy(.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Memory, null, tint = usageColor, modifier = Modifier.size(22.dp))
            }

            // Main content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Freq — headline
                Text(
                    state.cpuFreq.ifBlank { "— MHz" },
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
                // Governor label
                Text(
                    state.cpuGovernor.ifBlank { "—" },
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right side: % badge + temp
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Usage badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = usageColor.copy(.12f)
                ) {
                    Text(
                        "${state.cpuUsage}%",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = usageColor
                    )
                }
                // Temp
                if (state.cpuTemp > 0f) {
                    val hot = state.cpuTemp > 70f
                    val tColor = if (hot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Outlined.Thermostat, null, tint = tColor, modifier = Modifier.size(12.dp))
                        Text("%.1f°C".format(state.cpuTemp), fontSize = 12.sp, color = tColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GPU — bento card, bold freq + load chip + chipset name
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GpuCard(state: MonitorState, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    BentoMonitorCard(
        icon        = Icons.Outlined.GridView,
        iconColor   = color,
        headline    = state.gpuFreq.ifBlank { "— MHz" },
        subLabel    = "Frekuensi",
        chip1Label  = "${state.gpuUsage}%",
        chip1Sub    = "Beban",
        chip2Label  = "Adreno",
        chip2Sub    = "GPU",
        modifier    = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Battery — bento card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BatteryCard(state: MonitorState, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val color = when {
        state.batLevel <= 15 -> MaterialTheme.colorScheme.error
        state.batLevel <= 30 -> MaterialTheme.colorScheme.tertiary
        else                 -> Color(0xFF2ECC71)
    }
    val icon = when {
        state.batLevel <= 15 -> Icons.Outlined.BatteryAlert
        state.batLevel <= 50 -> Icons.Outlined.Battery3Bar
        else                 -> Icons.Outlined.BatteryFull
    }
    val statusShort = when {
        state.batStatus.equals("Charging", ignoreCase = true)     -> "Charging"
        state.batStatus.equals("Full", ignoreCase = true)         -> "Full"
        state.batStatus.equals("Not charging", ignoreCase = true) -> "Not chg."
        else                                                       -> "Discharging"
    }
    BentoMonitorCard(
        icon       = icon,
        iconColor  = color,
        headline   = "${state.batLevel}%",
        subLabel   = "SUHU",
        chip1Label = if (state.batTemp > 0f) "%.0f°C".format(state.batTemp) else "—°C",
        chip1Sub   = "Baterai",
        chip2Label = statusShort,
        chip2Sub   = "Status",
        modifier   = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared bento card — icon top-left, headline big, 2 info chips bottom
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BentoMonitorCard(
    icon: ImageVector,
    iconColor: Color,
    headline: String,
    subLabel: String,
    chip1Label: String,
    chip1Sub: String,
    chip2Label: String,
    chip2Sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: icon + sub-label badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(
                        subLabel,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Headline value
            Text(
                headline,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp,
                maxLines   = 1
            )

            // Bottom chips row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoChip(chip1Label, chip1Sub, Modifier.weight(1f))
                BentoChip(chip2Label, chip2Sub, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BentoChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Temperature card — bold °C, no progress bar, colored icon badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TempCard(
    label: String, icon: ImageVector, temp: Float,
    color: Color, maxTemp: Float, modifier: Modifier = Modifier
) {
    val isHot  = temp > maxTemp
    val accent = if (isHot) MaterialTheme.colorScheme.error else color
    val animTemp by animateFloatAsState(temp.coerceAtLeast(0f), tween(600, easing = FastOutSlowInEasing), label = "tmp_$label")

    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Icon row
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                if (isHot) Icon(Icons.Outlined.Warning, null, tint = accent, modifier = Modifier.size(13.dp))
            }

            // Label + temp
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (temp > 0f) "%.1f°C".format(animTemp) else "—",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accent,
                    lineHeight = 24.sp
                )
            }

            // Max label (no bar)
            Text(
                "Max ${maxTemp.toInt()}°C",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Memory / Storage card (list rows with wavy progress style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StorageCard(state: MonitorState) {
    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            RamRow(state.ramUsedMb, state.ramTotalMb)
            RowDivider()
            if (state.swapTotalMb > 0L) {
                ZramRow(state.swapUsedMb, state.swapTotalMb)
                RowDivider()
            }
            StorageRow(state.storageUsedGb, state.storageTotalGb)
            RowDivider()
            BatteryDetailRow(state.batCurrentMa, state.batVoltage, state.batStatus)
            RowDivider()
            UptimeRow(state.cpuGovernor, state.uptime)
        }
    }
}

@Composable
private fun RamRow(usedMb: Long, totalMb: Long) {
    val pct = if (totalMb > 0) (usedMb.toFloat()/totalMb).coerceIn(0f,1f) else 0f
    val col = if (pct > .85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    MetricRow(Icons.Outlined.Dns, "RAM",
        "${(pct*100).toInt()}%", "${fmtMb(usedMb)} / ${fmtMb(totalMb)}", col, pct)
}

@Composable
private fun ZramRow(usedMb: Long, totalMb: Long) {
    val pct = if (totalMb > 0) (usedMb.toFloat()/totalMb).coerceIn(0f,1f) else 0f
    MetricRow(Icons.Outlined.Compress, "ZRAM",
        "${(pct*100).toInt()}%", "${fmtMb(usedMb)} / ${fmtMb(totalMb)}", MaterialTheme.colorScheme.secondary, pct)
}

@Composable
private fun StorageRow(usedGb: Float, totalGb: Float) {
    val pct = if (totalGb > 0f) (usedGb/totalGb).coerceIn(0f,1f) else 0f
    val col = if (pct > .9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    MetricRow(Icons.Outlined.Storage, "Penyimpanan Internal",
        "${(pct*100).toInt()}%", "%.1f / %.1f GB".format(usedGb, totalGb), col, pct)
}

@Composable
private fun BatteryDetailRow(currentMa: Long, voltageMv: Long, status: String) {
    val s          = LocalStrings.current
    val isCharging = status.equals("Charging", ignoreCase = true)
    val isFull     = status.equals("Full", ignoreCase = true)
    val isNotChg   = status.equals("Not charging", ignoreCase = true)
    val color = when {
        isCharging -> MaterialTheme.colorScheme.primary
        isFull     -> Color(0xFF2D7D46)
        isNotChg   -> MaterialTheme.colorScheme.error
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when {
        isCharging -> Icons.Outlined.BatteryChargingFull
        isFull     -> Icons.Outlined.BatteryFull
        isNotChg   -> Icons.Outlined.BatteryAlert
        else       -> Icons.Outlined.Battery3Bar
    }
    val statusText = when {
        isCharging -> s.homeBatCharging
        isFull     -> s.homeBatFull
        isNotChg   -> s.homeBatNotCharging
        else       -> s.homeBatDischarging
    }
    val ma = if (Math.abs(currentMa) > 0L) "${Math.abs(currentMa)} mA" else "— mA"
    val v  = if (voltageMv > 0L) "${"%.2f".format(voltageMv/1000f)} V" else "— V"
    MetricRow(icon, s.homeBatStatusLabel, ma, "$statusText · $v", color)
}

@Composable
private fun UptimeRow(governor: String, uptime: String) {
    val s   = LocalStrings.current
    val col = MaterialTheme.colorScheme.secondary
    MetricRow(Icons.Outlined.Timer, s.homeLabelUptime, uptime.ifBlank { "—" },
        "${s.homeLabelGovernor}: ${governor.ifBlank { "—" }}", col)
}

@Composable
private fun RowDivider() = HorizontalDivider(
    modifier  = Modifier.padding(start = 54.dp, end = 16.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f),
    thickness = 0.5.dp
)

@Composable
private fun MetricRow(
    icon: ImageVector, label: String,
    valueText: String, subText: String,
    color: Color, barPct: Float? = null
) {
    val anim by animateFloatAsState(barPct ?: 0f, tween(800, easing = FastOutSlowInEasing), label = "mrow")
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(valueText, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, color = color)
            }
            // Wavy/thin bar (keep for memory rows, skip for non-pct rows)
            if (barPct != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(2.5.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(anim).fillMaxHeight().clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(color.copy(.4f), color)))
                    )
                }
            }
            Text(subText, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero skeleton / error / bootloop banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSkeleton() {
    Card(shape = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(width = 160.dp, height = 24.dp, cornerRadius = 12.dp)
                    SkeletonBox(width = 80.dp, height = 14.dp, cornerRadius = 7.dp)
                }
                SkeletonBox(width = 70.dp, height = 28.dp, cornerRadius = 14.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) { SkeletonBox(Modifier.weight(1f), height = 52.dp, cornerRadius = 14.dp) }
            }
        }
    }
}

@Composable
private fun HeroError(msg: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Card(shape = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(40.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            FilledTonalButton(onClick = onRetry, colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.homeRetry)
            }
        }
    }
}

@Composable
private fun BootloopBanner(info: DeviceInfo, vm: MainViewModel) {
    Card(shape = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Bootloop Terdeteksi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Boot ke-${info.bootCount} — Aktifkan Safe Mode", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .7f))
            }
            Switch(checked = info.safeMode, onCheckedChange = { vm.toggleSafeMode(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.error))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Misc helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(label: String, value: String, icon: ImageVector,
    modifier: Modifier = Modifier, highlight: Boolean = false) {
    val tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .08f),
        modifier = modifier) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.6f), lineHeight = 12.sp)
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SocBadge(soc: SocType) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.10f)) {
        Text(soc.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun fmtMb(mb: Long): String = if (mb >= 1024) "%.1f GB".format(mb/1024f) else "$mb MB"
