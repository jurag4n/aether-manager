package dev.aether.manager.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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

@Composable
fun HomeScreen(vm: MainViewModel) {
    val deviceState by vm.deviceInfo.collectAsState()
    val monitorState by vm.monitorState.collectAsState()
    val scroll = rememberScrollState()

    val updateVm: UpdateViewModel = viewModel()
    LaunchedEffect(Unit) { updateVm.checkUpdate() }
    UpdateDialogHost(viewModel = updateVm)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedContent(
            targetState = deviceState,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "hero"
        ) { state ->
            when (state) {
                is UiState.Loading -> HeroSkeleton()
                is UiState.Success -> HeroCard(state.data)
                is UiState.Error -> HeroError(state.msg) { vm.refresh() }
            }
        }

        AnimatedVisibility(
            visible = (deviceState as? UiState.Success)?.data?.bootCount?.let { it >= 2 } == true,
            enter = fadeIn(tween(300)) + expandVertically(),
            exit = fadeOut(tween(200)) + shrinkVertically()
        ) {
            val info = (deviceState as? UiState.Success)?.data
            if (info != null) BootloopBanner(info, vm)
        }

        AnimatedVisibility(
            visible = deviceState is UiState.Success,
            enter = fadeIn(tween(400, 100)) + slideInVertically { 24 }
        ) {
            MonitorSection(state = monitorState)
        }
    }
}

@Composable
private fun HeroCard(info: DeviceInfo) {
    val s = LocalStrings.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        info.model,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                InfoChip(
                    s.homeLabelKernel, info.kernel.substringBefore("-").take(14),
                    Icons.Outlined.Code, Modifier.weight(1f)
                )
                InfoChip(
                    s.homeSelinux, info.selinux.ifBlank { "Unknown" },
                    Icons.Outlined.Shield, Modifier.weight(1f),
                    highlight = info.selinux.equals("Permissive", true)
                )
            }
        }
    }
}

@Composable
private fun MonitorSection(state: MonitorState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CpuCompactCard(state, Modifier.weight(1f))
            TempPagerCard(state, Modifier.weight(1f))
        }

        GpuCard(state)

        MemorySystemCard(state)
    }
}

@Composable
private fun CpuCompactCard(state: MonitorState, modifier: Modifier = Modifier) {
    var showDetail by remember { mutableStateOf(false) }
    val usageColor = when {
        state.cpuUsage >= 90 -> MaterialTheme.colorScheme.error
        state.cpuUsage >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.clickable { showDetail = true }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(usageColor.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Memory, null, tint = usageColor, modifier = Modifier.size(16.dp))
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(
                        "${state.cpuUsage}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = usageColor
                    )
                }
            }

            Column {
                Text("CPU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(
                    state.cpuFreq.ifBlank { "— MHz" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
                Text(state.cpuGovernor.ifBlank { "schedutil" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text("Close") } },
            title = { Text("CPU Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Usage", "${state.cpuUsage}%")
                    DetailRow("Frequency", state.cpuFreq)
                    DetailRow("Governor", state.cpuGovernor)
                    DetailRow("Cores", Runtime.getRuntime().availableProcessors().toString())
                }
            }
        )
    }
}

@Composable
private fun TempPagerCard(state: MonitorState, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val temps = listOf(
        Triple("CPU", state.cpuTemp, Icons.Outlined.Memory),
        Triple("GPU", state.gpuTemp, Icons.Outlined.GridView),
        Triple("Thermal", state.thermalTemp, Icons.Outlined.Thermostat),
        Triple("Battery", state.batTemp, Icons.Outlined.BatteryFull)
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Thermostat, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.error.copy(.1f)) {
                    Text(
                        "SUHU",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                val (label, temp, icon) = temps[page]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (temp > 0f) "%.0f°C".format(temp) else "—°C",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GpuCard(state: MonitorState) {
    var showDetail by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetail = true }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("GPU", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(
                        "ADRENO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                state.gpuFreq.ifBlank { "— MHz" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text("Frekuensi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BentoChip("${state.gpuUsage}%", "Beban", Modifier.weight(1f))
                BentoChip("Adreno", "GPU", Modifier.weight(1f))
            }
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text("Close") } },
            title = { Text("GPU Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Load", "${state.gpuUsage}%")
                    DetailRow("Frequency", state.gpuFreq)
                    DetailRow("Renderer", "Adreno (TM)")
                }
            }
        )
    }
}

@Composable
private fun MemorySystemCard(state: MonitorState) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.List, null, Modifier.size(20.dp))
                }
                Text("Memori", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            WavyMetricRow("RAM", state.ramUsedMb, state.ramTotalMb, isGb = true)
            WavyMetricRow("ZRAM", state.swapUsedMb, state.swapTotalMb, isGb = true)
            WavyMetricRow("Penyimpanan Internal", state.storageUsedGb.toLong(), state.storageTotalGb.toLong(), isGb = true, isFloat = true, fUsed = state.storageUsedGb, fTotal = state.storageTotalGb)
        }
    }
}

@Composable
private fun WavyMetricRow(
    label: String, used: Long, total: Long, 
    isGb: Boolean = false, isFloat: Boolean = false,
    fUsed: Float = 0f, fTotal: Float = 0f
) {
    val pct = if (isFloat) (if (fTotal > 0) fUsed / fTotal else 0f) 
              else (if (total > 0) used.toFloat() / total else 0f)
    
    val usedStr = if (isFloat) "%.1f GB".format(fUsed) else fmtMb(used)
    val totalStr = if (isFloat) "%.1f GB".format(fTotal) else fmtMb(total)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("$usedStr / $totalStr", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        // Custom Wavy/Squiggly Progress Path
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val points = 20
                val segmentWidth = width / points
                val amplitude = 4f
                
                // Background Track
                val trackPath = Path().apply {
                    moveTo(0f, height / 2)
                    for (i in 1..points) {
                        val x = i * segmentWidth
                        val y = height / 2 + if (i % 2 == 0) -amplitude else amplitude
                        quadraticTo((i - 0.5f) * segmentWidth, height / 2 + if (i % 2 != 0) -amplitude else amplitude, x, y)
                    }
                }
                drawPath(trackPath, Color.Gray.copy(0.2f), style = Stroke(width = 8f, cap = StrokeCap.Round))

                // Active Progress
                val activePath = Path().apply {
                    moveTo(0f, height / 2)
                    val activePoints = (points * pct).toInt()
                    for (i in 1..activePoints) {
                        val x = i * segmentWidth
                        val y = height / 2 + if (i % 2 == 0) -amplitude else amplitude
                        quadraticTo((i - 0.5f) * segmentWidth, height / 2 + if (i % 2 != 0) -amplitude else amplitude, x, y)
                    }
                }
                drawPath(activePath, Color.White, style = Stroke(width = 8f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BentoChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SocBadge(soc: SocType) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.10f)) {
        Text(
            soc.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
}

private fun fmtMb(mb: Long): String = if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"

@Composable
private fun HeroSkeleton() { /* ... */ }

@Composable
private fun HeroError(msg: String, onRetry: () -> Unit) { /* ... */ }

@Composable
private fun BootloopBanner(info: DeviceInfo, vm: MainViewModel) { /* ... */ }

@Composable
private fun InfoChip(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .08f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.6f))
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
