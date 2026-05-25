package com.aether.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.data.AccessMode
import com.aether.data.MainViewModel
import com.aether.data.MonitorState
import com.aether.data.UiState
import com.aether.util.DeviceInfo
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: MainViewModel) {
    val infoState by vm.deviceInfo.collectAsState()
    val monitor by vm.monitorState.collectAsState()
    val mode by vm.accessMode.collectAsState()
    val apply by vm.applyStatus.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshIfNeeded()
        vm.refreshMonitor()
    }

    val info = (infoState as? UiState.Success<DeviceInfo>)?.data ?: com.aether.util.RootEngine.getDeviceInfoFallback()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeroCard(
            info = info,
            mode = mode,
            status = apply.summary,
            onRefresh = { vm.refresh(); vm.refreshMonitor() }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            RingMetric(
                title = "CPU",
                value = percent(monitor.cpuUsage),
                detail = monitor.cpuFreq.ifBlank { "-" },
                progress = monitor.cpuUsage / 100f,
                icon = Icons.Outlined.Memory,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            RingMetric(
                title = "GPU",
                value = percent(monitor.gpuUsage),
                detail = monitor.gpuFreq.ifBlank { monitor.gpuName.ifBlank { "-" } },
                progress = monitor.gpuUsage / 100f,
                icon = Icons.Outlined.GraphicEq,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            InfoTile("RAM", ramText(monitor), ramProgress(monitor), Icons.Outlined.Speed, Modifier.weight(1f))
            InfoTile("Storage", storageText(monitor), storageProgress(monitor), Icons.Outlined.Storage, Modifier.weight(1f))
        }

        SystemMonitorCard(monitor = monitor)
        DeviceInfoCard(info = info, monitor = monitor)
    }
}

@Composable
private fun HeroCard(info: DeviceInfo, mode: AccessMode, status: String, onRefresh: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .88f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .34f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = .10f), radius = size.minDimension * .45f, center = Offset(size.width * .96f, 0f))
                drawCircle(Color.White.copy(alpha = .07f), radius = size.minDimension * .32f, center = Offset(0f, size.height))
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Outlined.Android, MaterialTheme.colorScheme.primary, 50.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = info.model.ifBlank { "Android Device" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            lineHeight = 26.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Android ${info.android} • ${info.soc.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = .65f))
                            .clickable { onRefresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = if (mode == AccessMode.ROOT) "ROOT" else "NO ROOT",
                        value = if (mode == AccessMode.ROOT) info.rootType.ifBlank { "SU" } else "System monitor",
                        icon = Icons.Outlined.Security,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        label = "PROFILE",
                        value = info.profile.ifBlank { "balance" },
                        icon = Icons.Outlined.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (status.isNotBlank()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RingMetric(title: String, value: String, detail: String, progress: Float, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "ring_$title"
    )
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon, tint, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            AnimatedContent(targetState = value, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "metric_value_$title") { text ->
                Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = tint)
            }
            LinearProgressIndicator(
                progress = animated,
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)),
                color = tint,
                trackColor = tint.copy(alpha = .14f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InfoTile(title: String, value: String, progress: Float, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 1.dp, modifier = modifier.animateContentSize()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon, MaterialTheme.colorScheme.primary, 36.dp)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            }
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SystemMonitorCard(monitor: MonitorState) {
    ExpandCard(title = "System Monitor", subtitle = "Terintegrasi langsung tanpa root", icon = Icons.Outlined.Thermostat) {
        MetricRow("CPU Temp", temp(monitor.cpuTemp), "GPU Temp", temp(monitor.gpuTemp))
        MetricRow("Thermal", temp(monitor.thermalTemp), "Battery", "${monitor.batLevel}% • ${temp(monitor.batTemp)}")
        MetricRow("Current", current(monitor.batCurrentMa), "Voltage", if (monitor.batVoltage > 0) "${monitor.batVoltage} mV" else "-")
        MetricRow("Swap", if (monitor.swapTotalMb > 0) "${monitor.swapUsedMb}/${monitor.swapTotalMb} MB" else "-", "Uptime", monitor.uptime.ifBlank { "-" })
    }
}

@Composable
private fun DeviceInfoCard(info: DeviceInfo, monitor: MonitorState) {
    ExpandCard(title = "Device Info", subtitle = "Kernel, SELinux, root, governor", icon = Icons.Outlined.Android, collapsed = true) {
        MetricRow("Kernel", info.kernel.ifBlank { "-" }, "SELinux", info.selinux.ifBlank { "-" })
        MetricRow("Root", info.rootType.ifBlank { "No Root" }, "Governor", monitor.cpuGovernor.ifBlank { "-" })
        MetricRow("GPU", monitor.gpuName.ifBlank { "System" }, "Status", monitor.batStatus.ifBlank { "-" })
    }
}

@Composable
private fun ExpandCard(title: String, subtitle: String, icon: ImageVector, collapsed: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val expanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(!collapsed) }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { expanded.value = !expanded.value }
            .animateContentSize(tween(260, easing = FastOutSlowInEasing))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon, MaterialTheme.colorScheme.primary, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (expanded.value) "Hide" else "Show", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded.value) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
            }
        }
    }
}

@Composable
private fun MetricRow(leftTitle: String, left: String, rightTitle: String, right: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SmallMetric(leftTitle, left, Modifier.weight(1f))
        SmallMetric(rightTitle, right, Modifier.weight(1f))
    }
}

@Composable
private fun SmallMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .55f), modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .62f), modifier = modifier) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * .52f))
    }
}

private fun percent(v: Int) = "${v.coerceIn(0, 100)}%"
private fun temp(v: Float) = if (v > 0f) "${v.roundToInt()}°C" else "-"
private fun current(v: Long) = if (v != 0L) "${v} mA" else "-"
private fun ramText(m: MonitorState) = if (m.ramTotalMb > 0) "${m.ramUsedMb}/${m.ramTotalMb} MB" else "-"
private fun ramProgress(m: MonitorState) = if (m.ramTotalMb > 0) m.ramUsedMb.toFloat() / m.ramTotalMb.toFloat() else 0f
private fun storageText(m: MonitorState) = if (m.storageTotalGb > 0) "${m.storageUsedGb.roundToInt()}/${m.storageTotalGb.roundToInt()} GB" else "-"
private fun storageProgress(m: MonitorState) = if (m.storageTotalGb > 0f) m.storageUsedGb / m.storageTotalGb else 0f
