package dev.aether.manager.ui.home

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.MonitorState
import dev.aether.manager.data.UiState
import dev.aether.manager.update.UpdateDialogHost
import dev.aether.manager.update.UpdateViewModel
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.SocType
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HomeScreen(vm: MainViewModel) {
    val deviceState by vm.deviceInfo.collectAsState()
    val monitorState by vm.monitorState.collectAsState()
    val updateVm: UpdateViewModel = viewModel()

    LaunchedEffect(Unit) { updateVm.checkUpdate() }
    UpdateDialogHost(viewModel = updateVm)

    val info = (deviceState as? UiState.Success)?.data

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
            label = "home_info_device"
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
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (it == 0) 150.dp else 120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    }
}

@Composable
private fun InfoDeviceError(msg: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CpuInfoCard(state, info, Modifier.weight(1f))
            TemperaturePagerCard(state, Modifier.weight(1f))
        }

        GpuInfoCard(state, info)
        MemoryInfoCard(state)
        DeviceInfoCard()
    }
}

@Composable
private fun CpuInfoCard(state: MonitorState, info: DeviceInfo?, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    var expanded by remember { mutableStateOf(false) }

    TappableCard(modifier = modifier, onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(Icons.Outlined.Memory, color)
                PillText("${state.cpuUsage}%")
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "cpu_card_content"
            ) { isExpanded ->
                if (!isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CPU", fontSize = 17.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text("FREKUENSI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.cpuFreq.ifBlank { "— MHz" }, fontSize = 30.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CPU", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CpuDetailChip("Tipe", info?.soc?.label ?: "—", color, Modifier.weight(1f))
                            CpuDetailChip("SoC", info?.socRaw?.uppercase()?.take(10)?.ifBlank { "—" } ?: "—", color, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CpuDetailChip("Governor", state.cpuGovernor.ifBlank { "—" }, color, Modifier.weight(1f))
                            CpuDetailChip("Frekuensi", state.cpuFreq.ifBlank { "—" }, color, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CpuDetailChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color.copy(alpha = 0.7f))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemperaturePagerCard(state: MonitorState, modifier: Modifier = Modifier) {
    val temps = listOf(
        TempItem("CPU", state.cpuTemp, Icons.Outlined.Memory),
        TempItem("GPU", state.gpuTemp, Icons.Outlined.GridView),
        TempItem("Thermal", state.thermalTemp, Icons.Outlined.Thermostat),
        TempItem("Baterai", state.batTemp, Icons.Outlined.BatteryFull)
    )
    val pager = rememberPagerState(pageCount = { temps.size })
    val accent = Color(0xFFFF9CAF)

    TappableCard(modifier = modifier.height(178.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Thermostat, accent)
                    Text("Suhu", fontSize = 17.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    val item = temps[page]
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (item.value > 0f) "%.0f°C".format(item.value) else "—°C",
                            fontSize = 34.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = accent
                        )
                        Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                temps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == pager.currentPage) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index == pager.currentPage) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                    )
                }
            }
        }
    }
}

@Composable
private fun GpuInfoCard(state: MonitorState, info: DeviceInfo?) {
    val accent = Color(0xFFFF9CAF)

    // Resolve GPU name: prioritize sysfs reading (state.gpuName),
    // fallback to SocType-based guess, then generic "GPU"
    val gpuFullName = state.gpuName.ifBlank {
        when (info?.soc) {
            SocType.SNAPDRAGON -> "Adreno GPU"
            SocType.MEDIATEK   -> "Mali GPU"
            SocType.EXYNOS     -> "Mali GPU"
            SocType.KIRIN      -> "Mali GPU"
            else               -> "GPU"
        }
    }
    // Short label for the info tile (strip trailing " GPU" / " Gpu")
    val gpuShortName = gpuFullName
        .replace(Regex("\\s+[Gg][Pp][Uu]$"), "")
        .ifBlank { "GPU" }

    TappableCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GPU", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallToggleIcon(Icons.Outlined.ViewAgenda, true)
                    SmallToggleIcon(Icons.Outlined.Widgets, false)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("FREKUENSI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.gpuFreq.ifBlank { "— MHz" }, fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("GPU Type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(gpuFullName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoTile(Icons.Outlined.Thermostat, "${state.gpuUsage}%", "Beban GPU", accent, Modifier.weight(1f))
                InfoTile(Icons.Outlined.GridView, gpuShortName, "GPU", accent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MemoryInfoCard(state: MonitorState) {
    TappableCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Dns, MaterialTheme.colorScheme.primary)
                    Text("Memori", fontSize = 21.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            MemoryMetricRow(Icons.Outlined.Memory, "RAM", fmtMb(state.ramUsedMb), fmtMb(state.ramTotalMb), ratio(state.ramUsedMb, state.ramTotalMb), MaterialTheme.colorScheme.primary)
            MemoryDivider()
            MemoryMetricRow(Icons.Outlined.Dns, "ZRAM", fmtMb(state.swapUsedMb), fmtMb(state.swapTotalMb), ratio(state.swapUsedMb, state.swapTotalMb), MaterialTheme.colorScheme.secondary)
            MemoryDivider()
            MemoryMetricRow(
                Icons.Outlined.Storage,
                "Penyimpanan Internal",
                "%.1f GB".format(state.storageUsedGb),
                advertisedStorageLabel(state.storageTotalGb),
                ratio(state.storageUsedGb, advertisedStorageGb(state.storageTotalGb)),
                Color(0xFFFF9CAF)
            )
        }
    }
}


@Composable
private fun DeviceInfoCard() {
    val deviceName = remember {
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { "Unknown" }
    }
    val codeName = remember { Build.DEVICE.ifBlank { Build.PRODUCT.ifBlank { "Unknown" } } }
    val androidVersion = remember { "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}" }
    val kernelVersion = remember { System.getProperty("os.version").orEmpty().ifBlank { "Unknown" } }

    TappableCard(modifier = Modifier.fillMaxWidth().height(178.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Info, MaterialTheme.colorScheme.primary)
                Text(
                    text = "Device Info",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeviceInfoTile(Icons.Outlined.Memory, "Nama Perangkat", deviceName, Modifier.weight(1f))
                    DeviceInfoTile(Icons.Outlined.Widgets, "CodeName", codeName, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeviceInfoTile(Icons.Outlined.Info, "Android", androidVersion, Modifier.weight(1f))
                    DeviceInfoTile(Icons.Outlined.Storage, "Kernel", kernelVersion, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TappableCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "card_press_scale"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (pressed) 4.dp else 1.dp,
        shadowElevation = if (pressed) 6.dp else 0.dp,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick?.invoke() }
    ) { content() }
}

@Composable
private fun IconBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PillText(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Black,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SmallToggleIcon(icon: ImageVector, active: Boolean) {
    val color = if (active) Color(0xFFFF9CAF) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = if (active) 0.16f else 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InfoTile(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MemoryMetricRow(icon: ImageVector, label: String, used: String, total: String, pct: Float, color: Color) {
    val anim by animateFloatAsState(pct, tween(700, easing = FastOutSlowInEasing), label = "memory_$label")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontSize = if (label.length > 12) 13.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$used / $total",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            WavyProgress(progress = anim, color = color)
        }
    }
}

@Composable
private fun WavyProgress(progress: Float, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
        val centerY = size.height / 2f
        drawLine(track, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 5f, cap = StrokeCap.Round)
        val width = size.width * progress.coerceIn(0f, 1f)
        if (width > 0f) {
            val path = Path()
            path.moveTo(0f, centerY)
            val step = 6f
            var x = 0f
            while (x <= width) {
                val y = centerY + (sin((x / step) * PI).toFloat() * 3.2f)
                path.lineTo(x, y)
                x += 3f
            }
            drawPath(path, brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)), style = Stroke(width = 5f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun MemoryDivider() = HorizontalDivider(
    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
    thickness = 0.5.dp
)

private data class TempItem(val label: String, val value: Float, val icon: ImageVector)

private fun ratio(used: Long, total: Long): Float = if (total > 0L) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
private fun ratio(used: Float, total: Float): Float = if (total > 0f) (used / total).coerceIn(0f, 1f) else 0f
private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size
private fun advertisedStorageGb(rawGb: Float): Float = when {
    rawGb <= 0f -> 0f
    rawGb <= 20f -> 16f
    rawGb <= 40f -> 32f
    rawGb <= 80f -> 64f
    rawGb <= 160f -> 128f
    rawGb <= 320f -> 256f
    rawGb <= 640f -> 512f
    else -> 1024f
}

private fun advertisedStorageLabel(rawGb: Float): String {
    val gb = advertisedStorageGb(rawGb)
    return if (gb <= 0f) "— GB" else "${gb.toInt()} GB"
}

private fun fmtMb(mb: Long): String = if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"

@Composable
fun TabSectionTitle(
    icon: ImageVector,
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.1.sp
            )
        }
        trailing?.invoke()
    }
}
