package com.aether.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.data.MainViewModel
import com.aether.data.MonitorState
import com.aether.data.UiState
import com.aether.util.DeviceInfo
import com.aether.util.SocType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(vm: MainViewModel) {
    val deviceState by vm.deviceInfo.collectAsState()
    val monitor by vm.monitorState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedContent(
            targetState = deviceState,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(120)) },
            label = "home_rework"
        ) { state ->
            when (state) {
                is UiState.Loading -> HomeLoading()
                is UiState.Error -> ErrorCard(text = state.msg, onRetry = { vm.refresh() })
                is UiState.Success -> HomeContent(
                    monitor = monitor,
                    info = state.data,
                    onRefresh = { vm.refreshMonitor() }
                )
            }
        }
    }
}

@Composable
private fun HomeLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { i ->
            CleanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (i == 0) 156.dp else 104.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .72f),
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun HomeContent(monitor: MonitorState, info: DeviceInfo, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OverviewCard(info = info, monitor = monitor, onRefresh = onRefresh)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            CircularMetric(
                title = "CPU",
                value = percentText(monitor.cpuUsage),
                detail = freqText(monitor.cpuFreq),
                sub = monitor.cpuGovernor.ifBlank { info.soc.label },
                progress = monitor.cpuUsage / 100f,
                icon = Icons.Outlined.Memory,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            CircularMetric(
                title = "GPU",
                value = percentText(monitor.gpuUsage),
                detail = freqText(monitor.gpuFreq),
                sub = gpuLabel(monitor, info),
                progress = monitor.gpuUsage / 100f,
                icon = Icons.Outlined.GraphicEq,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        TemperatureCard(monitor)
        MemoryCard(monitor)
        PowerCard(monitor)
    }
}

@Composable
private fun OverviewCard(info: DeviceInfo, monitor: MonitorState, onRefresh: () -> Unit) {
    CleanCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .35f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = .10f),
                    radius = size.minDimension * .45f,
                    center = Offset(size.width * .92f, size.height * .08f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = .08f),
                    radius = size.minDimension * .28f,
                    center = Offset(size.width * .04f, size.height * .96f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(icon = Icons.Outlined.Speed, tint = MaterialTheme.colorScheme.primary, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = info.model.ifBlank { "Android Device" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            lineHeight = 27.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Android ${info.android} • ${info.rootType.ifBlank { "No Root" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .55f),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onRefresh() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Refresh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    InfoChip("Profile", profileLabel(info.profile), Icons.Outlined.Tune, Modifier.weight(1f))
                    InfoChip("Uptime", monitor.uptime.ifBlank { "—" }, Icons.Outlined.Wifi, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CircularMetric(
    title: String,
    value: String,
    detail: String,
    sub: String,
    progress: Float,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = .82f, stiffness = 170f),
        label = "metric_$title"
    )
    CleanCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconBox(icon, color, 34.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(sub, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
                Arc(progress = p, color = color, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text(detail, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun TemperatureCard(state: MonitorState) {
    CleanCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(icon = Icons.Outlined.Thermostat, title = "Thermal", subtitle = "Sensor terbaik dari sistem")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SmallTile("CPU", tempText(state.cpuTemp), Modifier.weight(1f))
                SmallTile("GPU", tempText(state.gpuTemp), Modifier.weight(1f))
                SmallTile("Body", tempText(state.thermalTemp), Modifier.weight(1f))
                SmallTile("Battery", tempText(state.batTemp), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MemoryCard(state: MonitorState) {
    CleanCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(icon = Icons.Outlined.Storage, title = "Memory & Storage", subtitle = "Realtime tanpa akses khusus")
            ProgressRow("RAM", "${state.ramUsedMb} / ${state.ramTotalMb} MB", ratio(state.ramUsedMb.toFloat(), state.ramTotalMb.toFloat()), MaterialTheme.colorScheme.primary)
            ProgressRow("Swap/ZRAM", "${state.swapUsedMb} / ${state.swapTotalMb} MB", ratio(state.swapUsedMb.toFloat(), state.swapTotalMb.toFloat()), MaterialTheme.colorScheme.tertiary)
            ProgressRow("Storage", "${oneDecimal(state.storageUsedGb)} / ${oneDecimal(state.storageTotalGb)} GB", ratio(state.storageUsedGb, state.storageTotalGb), MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun PowerCard(state: MonitorState) {
    CleanCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(icon = Icons.Outlined.BatteryFull, title = "Battery", subtitle = state.batStatus.ifBlank { "Status sistem" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SmallTile("Level", "${state.batLevel.coerceIn(0, 100)}%", Modifier.weight(1f))
                SmallTile("Current", currentText(state.batCurrentMa), Modifier.weight(1f))
                SmallTile("Voltage", voltageText(state.batVoltage), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ErrorCard(text: String, onRetry: () -> Unit) {
    CleanCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBox(Icons.Outlined.Bolt, MaterialTheme.colorScheme.error, 44.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
            ElevatedButton(onClick = onRetry, colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Coba lagi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CleanCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .96f),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = .58f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .76f)
                        )
                    )
                ),
            content = content
        )
    }
}

@Composable
private fun IconBox(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2.8f))
            .background(tint.copy(alpha = .13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * .52f))
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBox(icon, MaterialTheme.colorScheme.primary, 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .56f))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SmallTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .68f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun ProgressRow(label: String, value: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = color.copy(alpha = .14f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun Arc(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color.copy(alpha = .14f), 140f, 260f, false, style = stroke)
        drawArc(color, 140f, 260f * progress.coerceIn(0f, 1f), false, style = stroke)
        val angle = (140f + 260f * progress.coerceIn(0f, 1f)) * (PI.toFloat() / 180f)
        val radius = size.minDimension / 2f - 6.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color, radius = 4.6.dp.toPx(), center = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius))
    }
}

private fun percentText(value: Int): String = "${value.coerceIn(0, 100)}%"
private fun tempText(value: Float): String = if (value > 0f) "${value.toInt()}°C" else "—"
private fun freqText(value: String): String = value.ifBlank { "— MHz" }.uppercase()
private fun oneDecimal(value: Float): String = "%.1f".format(value.coerceAtLeast(0f))
private fun ratio(used: Float, total: Float): Float = if (total <= 0f) 0f else (used / total).coerceIn(0f, 1f)
private fun currentText(value: Long): String = if (value == 0L) "—" else "${kotlin.math.abs(value)} mA"
private fun voltageText(value: Long): String = if (value <= 0L) "—" else "${value} mV"
private fun profileLabel(value: String?): String = when (value?.lowercase()) {
    "performance" -> "Performance"
    "extreme" -> "Extreme"
    "battery" -> "Battery"
    else -> "Balance"
}
private fun gpuLabel(state: MonitorState, info: DeviceInfo): String = when {
    state.gpuName.isNotBlank() -> state.gpuName.lineSequence().first().take(18)
    info.soc == SocType.SNAPDRAGON -> "Adreno"
    info.soc == SocType.MEDIATEK -> "Mali / PowerVR"
    info.soc == SocType.EXYNOS -> "Mali / Xclipse"
    else -> "Android GPU"
}
