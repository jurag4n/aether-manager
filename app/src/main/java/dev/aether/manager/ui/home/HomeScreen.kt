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
import dev.aether.manager.update.UpdateDialogHost
import dev.aether.manager.update.UpdateViewModel
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.SocType

// ─────────────────────────────────────────────────────────────────────────────
// Screen root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(vm: MainViewModel) {
    val s            = LocalStrings.current
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
    ) {
        // Header — SetupActivity style (no icon)
        HomeHeader(
            onSettingsClick = { /* navigate */ },
            onRefreshClick  = { vm.refresh(); vm.refreshMonitor() }
        )

        Column(
            modifier            = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card
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

            // Monitor section
            AnimatedVisibility(
                visible = deviceState is UiState.Success,
                enter   = fadeIn(tween(400, 100)) + slideInVertically { 24 }
            ) {
                MonitorSection(
                    state     = monitorState,
                    onRefresh = { vm.refreshMonitor() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header — SetupActivity style, tanpa ikon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Aether Manager",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onRefreshClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Refresh, null,
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Settings, null,
                    modifier = Modifier.size(18.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared section title
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
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp))
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
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(info.model,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Android ${info.android}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                }
                SocBadge(info.soc)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoChip(s.homeLabelKernel,
                    info.kernel.substringBefore("-").take(14),
                    Icons.Outlined.Code,
                    Modifier.weight(1f))
                InfoChip(s.homeSelinux,
                    info.selinux.ifBlank { "Unknown" },
                    Icons.Outlined.Shield,
                    Modifier.weight(1f),
                    highlight = info.selinux.equals("Permissive", true))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monitor section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitorSection(state: MonitorState, onRefresh: () -> Unit) {
    val s = LocalStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        // CPU — full width card (4x4 style)
        CpuWideCard(state)

        // GPU + Battery — 2x2
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val batColor = when {
                state.batLevel <= 15 -> MaterialTheme.colorScheme.error
                state.batLevel <= 30 -> MaterialTheme.colorScheme.tertiary
                else                 -> Color(0xFF2ECC71)
            }
            val batIcon = when {
                state.batLevel <= 15 -> Icons.Outlined.BatteryAlert
                state.batLevel <= 50 -> Icons.Outlined.Battery3Bar
                else                 -> Icons.Outlined.BatteryFull
            }
            ArcGaugeCard(
                label    = "GPU",
                value    = state.gpuUsage,
                subText  = state.gpuFreq.ifBlank { "— MHz" },
                color    = MaterialTheme.colorScheme.tertiary,
                icon     = Icons.Outlined.GridView,
                modifier = Modifier.weight(1f)
            )
            ArcGaugeCard(
                label       = s.homeBatStatusLabel,
                value       = state.batLevel,
                subText     = if (state.batTemp > 0f) "%.1f°C".format(state.batTemp) else "—",
                color       = batColor,
                icon        = batIcon,
                modifier    = Modifier.weight(1f),
                invertColor = true
            )
        }

        // Temperature — 2x2 grid
        TemperatureGrid(state)

        // Storage / battery detail rows
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier  = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                BatteryCurrentRow(state.batCurrentMa, state.batVoltage, state.batStatus)
                MonDivider()
                GovernorUptimeRow(state.cpuGovernor, state.uptime)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CPU wide card (full-width / 4x4 kernel-manager style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CpuWideCard(state: MonitorState) {
    val animVal by animateFloatAsState(
        state.cpuUsage.coerceIn(0, 100) / 100f,
        tween(800, easing = FastOutSlowInEasing), label = "cpu_arc"
    )
    val arcColor = when {
        state.cpuUsage >= 90 -> MaterialTheme.colorScheme.error
        state.cpuUsage >= 70 -> MaterialTheme.colorScheme.tertiary
        else                 -> MaterialTheme.colorScheme.primary
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Arc gauge
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val pad    = stroke / 2f
                    val tl     = Offset(pad, pad)
                    val sz     = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2)
                    drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (animVal > 0f)
                        drawArc(arcColor, 135f, 270f * animVal, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text("${state.cpuUsage}%",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color    = MaterialTheme.colorScheme.onSurface)
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("CPU",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Outlined.Memory, null, tint = arcColor, modifier = Modifier.size(16.dp))
                }
                Text(state.cpuFreq.ifBlank { "— MHz" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Bar
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                        .background(track)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animVal).fillMaxHeight().clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(arcColor.copy(0.5f), arcColor)))
                    )
                }
                // Governor + Temp
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, null,
                            modifier = Modifier.size(11.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.cpuGovernor.ifBlank { "—" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.cpuTemp > 0f) {
                        val tempHot = state.cpuTemp > 70f
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Thermostat, null,
                                modifier = Modifier.size(11.dp),
                                tint     = if (tempHot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.1f°C".format(state.cpuTemp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tempHot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Temperature 2x2 grid
// ─────────────────────────────────────────────────────────────────────────────

private data class TempItemData(
    val label: String,
    val icon: ImageVector,
    val temp: Float,
    val color: Color,
    val maxTemp: Float
)

@Composable
private fun TemperatureGrid(state: MonitorState) {
    val items = listOf(
        TempItemData("CPU",     Icons.Outlined.Memory,       state.cpuTemp,     Color(0xFF1B74E8), 70f),
        TempItemData("GPU",     Icons.Outlined.GridView,     state.gpuTemp,     Color(0xFF7C4DFF), 75f),
        TempItemData("Thermal", Icons.Outlined.Thermostat,   state.thermalTemp, Color(0xFFE67E22), 55f),
        TempItemData("Battery", Icons.Outlined.BatteryFull,  state.batTemp,     Color(0xFF2ECC71), 40f),
    )

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Label
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Outlined.DeviceThermostat, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp))
            Text("Temperature",
                style         = MaterialTheme.typography.titleSmall,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.1.sp)
        }
        // 2x2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TempCard(items[0], Modifier.weight(1f))
                TempCard(items[1], Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TempCard(items[2], Modifier.weight(1f))
                TempCard(items[3], Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TempCard(item: TempItemData, modifier: Modifier = Modifier) {
    val isHot    = item.temp > item.maxTemp
    val accent   = if (isHot) MaterialTheme.colorScheme.error else item.color
    val animTemp by animateFloatAsState(item.temp.coerceAtLeast(0f), tween(600, easing = FastOutSlowInEasing), label = "tmp_${item.label}")
    val pct      = if (item.maxTemp > 0f) (item.temp / (item.maxTemp * 1.2f)).coerceIn(0f, 1f) else 0f
    val animPct  by animateFloatAsState(pct, tween(800), label = "pct_${item.label}")
    val track    = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Icon row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                if (isHot) {
                    Icon(Icons.Outlined.Warning, null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp))
                }
            }
            // Value
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (item.temp > 0f) "%.1f°C".format(animTemp) else "—",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accent
                )
            }
            // Bar
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(track)) {
                Box(modifier = Modifier.fillMaxWidth(animPct).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent.copy(0.4f), accent))))
            }
            Text("Max ${item.maxTemp.toInt()}°C",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Arc gauge card (2x2) — GPU / Battery
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArcGaugeCard(
    label: String, value: Int, subText: String,
    color: Color, icon: ImageVector, modifier: Modifier = Modifier,
    invertColor: Boolean = false
) {
    val animVal by animateFloatAsState(value.coerceIn(0, 100) / 100f, tween(800, easing = FastOutSlowInEasing), label = "arc")
    val arcColor = if (invertColor) color else when {
        value >= 90 -> MaterialTheme.colorScheme.error
        value >= 70 -> MaterialTheme.colorScheme.tertiary
        else        -> color
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, null, tint = arcColor, modifier = Modifier.size(14.dp))
            }
            Box(
                modifier         = Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val pad    = stroke / 2f
                    val tl     = Offset(pad, pad)
                    val sz     = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2)
                    drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (animVal > 0f)
                        drawArc(arcColor, 135f, 270f * animVal, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${value}%", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(subText, fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Misc helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(
    label: String, value: String, icon: ImageVector,
    modifier: Modifier = Modifier, highlight: Boolean = false
) {
    val tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
        modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f), lineHeight = 12.sp)
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SocBadge(soc: SocType) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)) {
        Text(soc.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

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
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(leftText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pctLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Box(modifier = Modifier.fillMaxWidth(anim).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(0.6f), color))))
            }
        }
    }
}

@Composable
private fun RamRow(usedMb: Long, totalMb: Long) {
    val pct = if (totalMb > 0) (usedMb.toFloat() / totalMb).coerceIn(0f, 1f) else 0f
    val col = if (pct > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    BarRow(Icons.Outlined.Dns, "RAM", "${fmtMb(usedMb)} / ${fmtMb(totalMb)}", pct, "${(pct * 100).toInt()}%", col)
}

@Composable
private fun StorageRow(usedGb: Float, totalGb: Float) {
    val pct = if (totalGb > 0f) (usedGb / totalGb).coerceIn(0f, 1f) else 0f
    val col = if (pct > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    BarRow(Icons.Outlined.Storage, "Storage", "%.1f / %.1f GB".format(usedGb, totalGb), pct, "${(pct * 100).toInt()}%", col)
}

@Composable
private fun SwapRow(usedMb: Long, totalMb: Long) {
    val pct = if (totalMb > 0) (usedMb.toFloat() / totalMb).coerceIn(0f, 1f) else 0f
    BarRow(Icons.Outlined.SwapVert, "Swap", "${fmtMb(usedMb)} / ${fmtMb(totalMb)}", pct, "${(pct * 100).toInt()}%", MaterialTheme.colorScheme.tertiary)
}

@Composable
private fun BatteryCurrentRow(currentMa: Long, voltageMv: Long, status: String) {
    val s          = LocalStrings.current
    val isCharging = status.equals("Charging", ignoreCase = true)
    val isFull     = status.equals("Full", ignoreCase = true)
    val isNotChg   = status.equals("Not charging", ignoreCase = true)
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
    val currentText = if (Math.abs(currentMa) > 0L) "${Math.abs(currentMa)} mA" else "— mA"
    val voltageText = if (voltageMv > 0L) "${"%.2f".format(voltageMv / 1000f)} V" else "— V"

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(statusIcon, null, tint = accentColor, modifier = Modifier.size(17.dp))
        }
        Row(modifier = Modifier.weight(1f), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.homeBatStatusLabel, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.12f)) {
                    Text(statusText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(currentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isCharging) accentColor else MaterialTheme.colorScheme.onSurface)
                Text(voltageText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GovernorUptimeRow(governor: String, uptime: String) {
    val s   = LocalStrings.current
    val col = MaterialTheme.colorScheme.secondary
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(col.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Timer, null, tint = col, modifier = Modifier.size(17.dp))
        }
        Row(modifier = Modifier.weight(1f), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.homeLabelGovernor, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(governor.ifBlank { "—" }, fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                Text(s.homeLabelUptime, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(uptime.ifBlank { "—" }, fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HeroSkeleton() {
    Card(shape = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(width = 160.dp, height = 24.dp, cornerRadius = 12.dp)
                    SkeletonBox(width = 80.dp,  height = 14.dp, cornerRadius = 7.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.ErrorOutline, null,
                tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(40.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
            FilledTonalButton(onClick = onRetry, colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor   = MaterialTheme.colorScheme.onError)) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.Warning, null,
                tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Bootloop Terdeteksi", fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Boot ke-${info.bootCount} — Aktifkan Safe Mode",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
            }
            Switch(checked = info.safeMode, onCheckedChange = { vm.toggleSafeMode(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.error))
        }
    }
}

private fun fmtMb(mb: Long): String =
    if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"
