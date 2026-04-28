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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.MonitorState
import dev.aether.manager.data.UiState
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.components.*
import dev.aether.manager.update.UpdateDialog
import dev.aether.manager.update.UpdateState
import dev.aether.manager.update.UpdateViewModel
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.SocType

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(vm: MainViewModel) {
    val s            = LocalStrings.current
    val deviceState  by vm.deviceInfo.collectAsState()
    val monitorState by vm.monitorState.collectAsState()
    val scroll       = rememberScrollState()

    // ── Update checker ───────────────────────────────────────────────────────
    val updateVm: UpdateViewModel = viewModel()
    val updateState by updateVm.state.collectAsState()

    LaunchedEffect(Unit) { updateVm.checkUpdate() }

    if (updateState is UpdateState.Available) {
        UpdateDialog(
            info      = (updateState as UpdateState.Available).info,
            onDismiss = { updateVm.dismiss() },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Device Card ──────────────────────────────────────────────
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

        // ── Bootloop Banner ──────────────────────────────────────────
        AnimatedVisibility(
            visible = (deviceState as? UiState.Success)?.data?.bootCount?.let { it >= 2 } == true,
            enter   = fadeIn(tween(300)) + expandVertically(),
            exit    = fadeOut(tween(200)) + shrinkVertically()
        ) {
            val info = (deviceState as? UiState.Success)?.data
            if (info != null) BootloopBanner(info, vm)
        }

        // ── Real-time Monitor ────────────────────────────────────────
        AnimatedVisibility(
            visible = deviceState is UiState.Success,
            enter   = fadeIn(tween(400, 100)) + slideInVertically { 24 }
        ) {
            MonitorSection(
                state  = monitorState,
                onRefresh = { vm.refreshMonitor() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Title — shared across tabs
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Device Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(info: DeviceInfo) {
    val s = LocalStrings.current

    ElevatedCard(
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: device name + SOC badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        info.model,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Android ${info.android}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                SocBadge(info.soc)
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )

            // Info chips grid
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoChip(
                    label    = s.homeLabelKernel,
                    value    = info.kernel.substringBefore("-").take(14),
                    icon     = Icons.Outlined.Code,
                    modifier = Modifier.weight(1f)
                )
                InfoChip(
                    label     = s.homeSelinux,
                    value     = info.selinux.ifBlank { "Unknown" },
                    icon      = Icons.Outlined.Shield,
                    modifier  = Modifier.weight(1f),
                    highlight = info.selinux.equals("Permissive", true)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monitor Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitorSection(state: MonitorState, onRefresh: () -> Unit) {
    val s = LocalStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header
        TabSectionTitle(
            icon     = Icons.Outlined.Analytics,
            title    = s.homeMonitor,
            trailing = {
                IconButton(
                    onClick  = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh, null,
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Gauge row: CPU / GPU / Battery
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val batColor = when {
                state.batLevel <= 15 -> MaterialTheme.colorScheme.error
                state.batLevel <= 30 -> MaterialTheme.colorScheme.tertiary
                else                 -> MaterialTheme.colorScheme.secondary
            }
            val batIcon = when {
                state.batLevel <= 15 -> Icons.Outlined.BatteryAlert
                state.batLevel <= 50 -> Icons.Outlined.Battery3Bar
                else                 -> Icons.Outlined.BatteryFull
            }

            ArcGaugeCard(
                label   = "CPU",
                value   = state.cpuUsage,
                subText = state.cpuFreq.ifBlank { "— MHz" },
                color   = MaterialTheme.colorScheme.primary,
                icon    = Icons.Outlined.Memory,
                modifier = Modifier.weight(1f)
            )
            ArcGaugeCard(
                label   = "GPU",
                value   = state.gpuUsage,
                subText = state.gpuFreq.ifBlank { "— MHz" },
                color   = MaterialTheme.colorScheme.tertiary,
                icon    = Icons.Outlined.GridView,
                modifier = Modifier.weight(1f)
            )
            ArcGaugeCard(
                label        = s.homeBatStatusLabel,
                value        = state.batLevel,
                subText      = if (state.batTemp > 0f) "%.1f°C".format(state.batTemp) else "—",
                color        = batColor,
                icon         = batIcon,
                modifier     = Modifier.weight(1f),
                invertColor  = true
            )
        }

        // Detail card: RAM, Storage, Temp, Battery, Governor
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                RamRow(state.ramUsedMb, state.ramTotalMb)
                MonDivider()
                StorageRow(state.storageUsedGb, state.storageTotalGb)
                if (state.swapTotalMb > 0L) {
                    MonDivider()
                    SwapRow(state.swapUsedMb, state.swapTotalMb)
                }
                MonDivider()
                TempRow(state.cpuTemp, state.batTemp)
                MonDivider()
                BatteryCurrentRow(state.batCurrentMa, state.batVoltage, state.batStatus)
                MonDivider()
                GovernorUptimeRow(state.cpuGovernor, state.uptime)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoChip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(
    label: String, value: String, icon: ImageVector,
    modifier: Modifier = Modifier, highlight: Boolean = false
) {
    val tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    label,
                    fontSize  = 10.sp,
                    color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    lineHeight = 12.sp
                )
                Text(
                    value,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (highlight) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines   = 1,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SocBadge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SocBadge(soc: SocType) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
    ) {
        Text(
            soc.label,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color      = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Arc Gauge Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArcGaugeCard(
    label: String, value: Int, subText: String,
    color: Color, icon: ImageVector, modifier: Modifier = Modifier,
    invertColor: Boolean = false
) {
    val animVal by animateFloatAsState(
        value.coerceIn(0, 100) / 100f,
        tween(800, easing = FastOutSlowInEasing), label = "arc"
    )
    val arcColor = if (invertColor) color else when {
        value >= 90 -> MaterialTheme.colorScheme.error
        value >= 70 -> MaterialTheme.colorScheme.tertiary
        else        -> color
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style         = MaterialTheme.typography.labelSmall,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color         = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(icon, null, tint = arcColor, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val pad    = stroke / 2f
                    val tl     = Offset(pad, pad)
                    val sz     = androidx.compose.ui.geometry.Size(
                        size.width - pad * 2, size.height - pad * 2
                    )
                    drawArc(track, 135f, 270f, false, tl, sz,
                        style = Stroke(stroke, cap = StrokeCap.Round))
                    if (animVal > 0f)
                        drawArc(arcColor, 135f, 270f * animVal, false, tl, sz,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${value}%",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subText,
                        fontSize = 8.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monitor Detail Rows
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonDivider() = HorizontalDivider(
    modifier  = Modifier.padding(start = 56.dp, end = 16.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

@Composable
private fun BarRow(
    icon: ImageVector, label: String, leftText: String,
    pct: Float, pctLabel: String, color: Color
) {
    val anim by animateFloatAsState(pct, tween(800, easing = FastOutSlowInEasing), label = "bar")
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
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        leftText,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        pctLabel,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = color
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(anim)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(color.copy(0.6f), color))
                        )
                )
            }
        }
    }
}

@Composable
private fun RamRow(usedMb: Long, totalMb: Long) {
    val pct = if (totalMb > 0) (usedMb.toFloat() / totalMb).coerceIn(0f, 1f) else 0f
    val col = if (pct > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    BarRow(
        Icons.Outlined.Dns, "RAM",
        "${fmtMb(usedMb)} / ${fmtMb(totalMb)}",
        pct, "${(pct * 100).toInt()}%", col
    )
}

@Composable
private fun StorageRow(usedGb: Float, totalGb: Float) {
    val pct = if (totalGb > 0f) (usedGb / totalGb).coerceIn(0f, 1f) else 0f
    val col = if (pct > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    BarRow(
        Icons.Outlined.Storage, "Storage",
        "%.1f / %.1f GB".format(usedGb, totalGb),
        pct, "${(pct * 100).toInt()}%", col
    )
}

@Composable
private fun SwapRow(usedMb: Long, totalMb: Long) {
    val pct = if (totalMb > 0) (usedMb.toFloat() / totalMb).coerceIn(0f, 1f) else 0f
    BarRow(
        Icons.Outlined.SwapVert, "Swap",
        "${fmtMb(usedMb)} / ${fmtMb(totalMb)}",
        pct, "${(pct * 100).toInt()}%",
        MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun TempRow(cpuTemp: Float, batTemp: Float) {
    val s   = LocalStrings.current
    val hot = cpuTemp > 60f
    val col = if (hot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

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
                .background(col.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Thermostat, null, tint = col, modifier = Modifier.size(18.dp))
        }
        Row(
            modifier              = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    s.homeTempCpu,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (cpuTemp > 0f) "%.1f°C".format(cpuTemp) else "—",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = col
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    s.homeTempBat,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (batTemp > 0f) "%.1f°C".format(batTemp) else "—",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BatteryCurrentRow(currentMa: Long, voltageMv: Long, status: String) {
    val s           = LocalStrings.current
    val isCharging  = status.equals("Charging", ignoreCase = true)
    val isFull      = status.equals("Full", ignoreCase = true)
    val isNotChg    = status.equals("Not charging", ignoreCase = true)

    val accentColor = when {
        isCharging -> MaterialTheme.colorScheme.primary
        isFull     -> Color(0xFF2D7D46)
        isNotChg   -> MaterialTheme.colorScheme.error
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when {
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

    val absCurrentMa = Math.abs(currentMa)
    val currentText  = if (absCurrentMa > 0L) "${absCurrentMa} mA" else "— mA"
    val voltageText  = if (voltageMv > 0L) "${"%.2f".format(voltageMv / 1000f)} V" else "— V"

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
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(statusIcon, null, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Row(
            modifier              = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    s.homeBatStatusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusText,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    currentText,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isCharging) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    voltageText,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GovernorUptimeRow(governor: String, uptime: String) {
    val s   = LocalStrings.current
    val col = MaterialTheme.colorScheme.secondary

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
                .background(col.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Timer, null, tint = col, modifier = Modifier.size(18.dp))
        }
        Row(
            modifier              = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    s.homeLabelGovernor,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    governor.ifBlank { "—" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    s.homeLabelUptime,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    uptime.ifBlank { "—" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton & Error states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSkeleton() {
    ElevatedCard(
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(width = 160.dp, height = 22.dp, cornerRadius = 11.dp)
                    SkeletonBox(width = 80.dp, height = 14.dp, cornerRadius = 7.dp)
                }
                SkeletonBox(width = 70.dp, height = 26.dp, cornerRadius = 13.dp)
            }
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 1.dp, cornerRadius = 0.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) { SkeletonBox(Modifier.weight(1f), height = 52.dp, cornerRadius = 14.dp) }
            }
        }
    }
}

@Composable
private fun HeroError(msg: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Card(
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement  = Arrangement.spacedBy(14.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.ErrorOutline, null,
                tint     = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(40.dp)
            )
            Text(
                msg,
                style  = MaterialTheme.typography.bodyMedium,
                color  = MaterialTheme.colorScheme.onErrorContainer
            )
            FilledTonalButton(
                onClick = onRetry,
                colors  = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.homeRetry)
            }
        }
    }
}

@Composable
private fun BootloopBanner(info: DeviceInfo, vm: MainViewModel) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.Outlined.Warning, null,
                tint     = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Bootloop Terdeteksi",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Boot ke-${info.bootCount} — Aktifkan Safe Mode",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked         = info.safeMode,
                onCheckedChange = { vm.toggleSafeMode(it) },
                colors          = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

private fun fmtMb(mb: Long): String =
    if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"
