package com.aether.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    val monitorState by vm.monitorState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedContent(
            targetState = deviceState,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(120)) },
            label = "home_v3_device"
        ) { state ->
            when (state) {
                is UiState.Loading -> InfoDeviceSkeleton()
                is UiState.Error -> InfoDeviceError(state.msg) { vm.refresh() }
                is UiState.Success -> MonitorSection(
                    state = monitorState,
                    info = state.data
                )
            }
        }
    }
}

@Composable
private fun InfoDeviceSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(5) { index ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 168.dp else 118.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .6f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .3f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun InfoDeviceError(msg: String, onRetry: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(Icons.Outlined.Info, MaterialTheme.colorScheme.error)
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onRetry() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Refresh, null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                    Text("Refresh", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MonitorSection(state: MonitorState, info: DeviceInfo?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DeviceHeroCard(state, info)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricGaugeCard(
                title = "CPU",
                subtitle = state.cpuGovernor.ifBlank { info?.soc?.label().orEmpty() }.ifBlank { "Realtime" },
                value = percentText(state.cpuUsage),
                detail = freqText(state.cpuFreq),
                temp = tempText(state.cpuTemp),
                progress = state.cpuUsage / 100f,
                icon = Icons.Outlined.Memory,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricGaugeCard(
                title = "GPU",
                subtitle = gpuShortName(state, info),
                value = percentText(state.gpuUsage),
                detail = freqText(state.gpuFreq),
                temp = tempText(state.gpuTemp),
                progress = state.gpuUsage / 100f,
                icon = Icons.Outlined.GridView,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        TemperatureStrip(state)
        MemoryStorageCard(state)
        PowerStatusCard(state)
    }
}

@Composable
private fun DeviceHeroCard(state: MonitorState, info: DeviceInfo?) {
    val pulse by rememberInfiniteTransition(label = "hero_pulse").animateFloat(
        initialValue = .96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hero_pulse_anim"
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .86f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .38f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val c1 = Color.White.copy(alpha = .18f)
                val c2 = Color.White.copy(alpha = .08f)
                drawCircle(c1, radius = size.minDimension * .36f * pulse, center = Offset(size.width * .88f, size.height * .18f))
                drawCircle(c2, radius = size.minDimension * .28f, center = Offset(size.width * .12f, size.height * .86f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Speed, MaterialTheme.colorScheme.primary, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info?.model?.ifBlank { "Aether Manager" } ?: "Aether Manager",
                            fontSize = 23.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Android ${info?.android ?: "?"} • ${info?.rootType ?: "Root"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip("Profile", profileLabel(info?.profile), Icons.Outlined.Tune, Modifier.weight(1f))
                    HeroChip("Uptime", state.uptime.ifBlank { "—" }, Icons.Outlined.CheckCircle, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricGaugeCard(
    title: String,
    subtitle: String,
    value: String,
    detail: String,
    temp: String,
    progress: Float,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = .74f, stiffness = 180f),
        label = "${title}_usage"
    )

    GlassCard(modifier = modifier.height(218.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, accent)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        subtitle.ifBlank { "Realtime" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ArcMeter(progress = animatedProgress, color = accent, modifier = Modifier.size(112.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontSize = 27.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text("Load", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("Freq", detail, Modifier.weight(1f))
                MiniStat("Temp", temp, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TemperatureStrip(state: MonitorState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Thermostat, MaterialTheme.colorScheme.error, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Thermal Monitor", fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("CPU, GPU, board, dan baterai", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TempChip("CPU", state.cpuTemp, Modifier.weight(1f))
                TempChip("GPU", state.gpuTemp, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TempChip("Board", state.thermalTemp, Modifier.weight(1f))
                TempChip("Battery", state.batTemp, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MemoryStorageCard(state: MonitorState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Storage, MaterialTheme.colorScheme.secondary, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Memory & Storage", fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Progress dibuat dinamis dan ringan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            UsageBar(
                label = "RAM",
                value = "${state.ramUsedMb} / ${state.ramTotalMb} MB",
                progress = ratio(state.ramUsedMb.toFloat(), state.ramTotalMb.toFloat()),
                color = MaterialTheme.colorScheme.primary
            )
            UsageBar(
                label = "Swap / ZRAM",
                value = "${state.swapUsedMb} / ${state.swapTotalMb} MB",
                progress = ratio(state.swapUsedMb.toFloat(), state.swapTotalMb.toFloat()),
                color = MaterialTheme.colorScheme.tertiary
            )
            UsageBar(
                label = "Storage",
                value = "${oneDecimal(state.storageUsedGb)} / ${oneDecimal(state.storageTotalGb)} GB",
                progress = ratio(state.storageUsedGb, state.storageTotalGb),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun PowerStatusCard(state: MonitorState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PowerTile(Icons.Outlined.BatteryFull, "Battery", "${state.batLevel}%", Modifier.weight(1f))
            PowerTile(Icons.Outlined.Dns, "Current", currentText(state.batCurrentMa), Modifier.weight(1f))
            PowerTile(Icons.Outlined.Speed, "Voltage", voltageText(state.batVoltage), Modifier.weight(1f))
        }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f),
        tonalElevation = 3.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (MaterialTheme.colorScheme.surface.luminanceCompat() > .5f) .28f else .04f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .64f)
                        )
                    )
                ),
            content = content
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector, color: Color, size: androidx.compose.ui.unit.Dp = 42.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = .16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * .50f))
    }
}

@Composable
private fun ArcMeter(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val stroke = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = color.copy(alpha = .14f),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = 140f,
            sweepAngle = 260f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = stroke
        )
        val angle = (140f + 260f * progress.coerceIn(0f, 1f)) * (PI.toFloat() / 180f)
        val radius = size.minDimension / 2f - 7.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = 4.8.dp.toPx(),
            center = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        )
    }
}

@Composable
private fun HeroChip(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .52f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .72f))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun TempChip(label: String, value: Float, modifier: Modifier = Modifier) {
    MiniStat(label, tempText(value), modifier)
}

@Composable
private fun UsageBar(label: String, value: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WavyProgress(progress = progress, color = color)
    }
}

@Composable
private fun WavyProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = .8f, stiffness = 190f),
        label = "bar_progress"
    )
    Canvas(modifier = modifier.fillMaxWidth().height(12.dp)) {
        val r = size.height / 2f
        drawRoundRect(
            color = color.copy(alpha = .15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
        )
        val w = size.width * animated
        if (w > 0f) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                val steps = 7
                for (i in steps downTo 0) {
                    val x = w * i / steps
                    val y = size.height - (sin(i.toFloat()) * 1.5f)
                    lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
private fun PowerTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .68f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

private fun percentText(value: Int): String = if (value > 0) "${value.coerceIn(0, 100)}%" else "—%"
private fun tempText(value: Float): String = if (value > 0f) "${value.toInt()}°C" else "—°C"
private fun freqText(value: String): String = value.ifBlank { "— MHz" }.uppercase()
private fun oneDecimal(value: Float): String = "%.1f".format(value.coerceAtLeast(0f))
private fun ratio(used: Float, total: Float): Float = if (total <= 0f) 0f else (used / total).coerceIn(0f, 1f)
private fun currentText(value: Long): String = if (value == 0L) "— mA" else "${kotlin.math.abs(value)} mA"
private fun voltageText(value: Long): String = if (value <= 0L) "— mV" else "${value} mV"
private fun profileLabel(value: String?): String = when (value?.lowercase()) {
    "performance" -> "Performance"
    "extreme" -> "Extreme"
    "battery" -> "Battery"
    else -> "Balance"
}

private fun gpuShortName(state: MonitorState, info: DeviceInfo?): String = when {
    state.gpuName.isNotBlank() -> state.gpuName.lineSequence().first().take(22)
    info?.soc == SocType.SNAPDRAGON -> "Adreno"
    info?.soc == SocType.MEDIATEK -> "Mali / PowerVR"
    info?.soc == SocType.EXYNOS -> "Mali / Xclipse"
    else -> "Realtime"
}

private fun SocType.label(): String = when (this) {
    SocType.SNAPDRAGON -> "Snapdragon"
    SocType.MEDIATEK -> "MediaTek"
    SocType.EXYNOS -> "Exynos"
    SocType.KIRIN -> "Kirin"
    SocType.OTHER -> "Android SoC"
}

private fun Color.luminanceCompat(): Float = (0.2126f * red + 0.7152f * green + 0.0722f * blue)
