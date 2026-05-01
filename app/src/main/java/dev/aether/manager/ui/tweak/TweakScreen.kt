package dev.aether.manager.ui.tweak

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TweakScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {}
) {
    val tweaks by vm.tweaks.collectAsState()
    val scroll = rememberScrollState()

    var expandedCard by remember { mutableStateOf<String?>(null) }
    var cpuGovernor by remember { mutableStateOf("Schedutil") }
    var gpuGovernor by remember { mutableStateOf("Balanced") }
    var gpuMinFreq by remember { mutableStateOf("300") }
    var gpuMaxFreq by remember { mutableStateOf("850") }
    var renderer by remember { mutableStateOf("OpenGL") }
    var rendererDialog by remember { mutableStateOf(false) }
    var dnsPrivate by remember { mutableStateOf(false) }
    var networkStable by remember { mutableStateOf(false) }
    var tcpMode by remember { mutableStateOf(tweaks.tcpBbr) }
    var swapEnabled by remember { mutableStateOf(false) }
    var ioScheduler by remember { mutableStateOf(tweaks.ioScheduler.ifBlank { "auto" }) }

    fun toggleExpand(key: String) {
        expandedCard = if (expandedCard == key) null else key
    }

    fun applyNow() {
        vm.applyAll()
    }

    fun setTweakNow(key: String, value: Boolean) {
        vm.setTweak(key, value)
        applyNow()
    }

    fun setProfileNow(profile: String) {
        vm.setProfile(profile)
        applyNow()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpandableTweakCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Memory,
                title = "CPU",
                subtitle = "Governor",
                badge = cpuGovernor,
                active = tweaks.cpuBoost,
                expanded = expandedCard == "cpu",
                fromLeft = true,
                onClick = { toggleExpand("cpu") }
            ) {
                DropdownAction(
                    title = "CPU Governor",
                    value = cpuGovernor,
                    options = listOf("Schedutil", "Performance", "Battery", "Ondemand"),
                    onSelect = {
                        cpuGovernor = it
                        setTweakNow("cpuBoost", it == "Performance" || it == "Schedutil" || it == "Ondemand")
                    }
                )
            }

            ExpandableTweakCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.GridView,
                title = "GPU",
                subtitle = "Profile & renderer",
                badge = if (tweaks.gpuThrottleOff) "Aktif" else gpuGovernor,
                active = tweaks.gpuThrottleOff,
                expanded = expandedCard == "gpu",
                fromLeft = false,
                onClick = { toggleExpand("gpu") }
            ) {
                CompactFeatureBlock(
                    icon = Icons.Outlined.Tune,
                    title = "GPU Governor Profile",
                    subtitle = gpuGovernor,
                    trailing = "Pilih",
                    onClick = {
                        gpuGovernor = when (gpuGovernor) {
                            "Performance" -> "Power Save"
                            "Power Save" -> "Balanced"
                            else -> "Performance"
                        }
                        setTweakNow("gpuThrottleOff", gpuGovernor == "Performance")
                    }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "GPU Frequency Lock",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gpuMinFreq,
                        onValueChange = { gpuMinFreq = it },
                        label = { Text("Min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gpuMaxFreq,
                        onValueChange = { gpuMaxFreq = it },
                        label = { Text("Max") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = { setTweakNow("gpuThrottleOff", true) },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lock Frequency")
                }

                CompactFeatureBlock(
                    icon = Icons.Outlined.GridView,
                    title = "Renderer",
                    subtitle = "Select GPU Renderer",
                    trailing = renderer,
                    onClick = { rendererDialog = true }
                )
            }
        }

        AppProfileCard(onClick = onOpenAppProfile)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpandableTweakCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Wifi,
                title = "Network",
                subtitle = "DNS, Stabil, TCP",
                badge = if (tcpMode || tweaks.tcpBbr) "TCP" else "Off",
                active = dnsPrivate || networkStable || tcpMode || tweaks.tcpBbr,
                expanded = expandedCard == "network",
                fromLeft = true,
                onClick = { toggleExpand("network") }
            ) {
                ToggleOption(
                    title = "DNS Private",
                    subtitle = "Aktifkan mode DNS private",
                    checked = dnsPrivate,
                    onClick = {
                        dnsPrivate = !dnsPrivate
                        setTweakNow("privateDns", dnsPrivate)
                    }
                )
                ToggleOption(
                    title = "Stabilkan Network",
                    subtitle = "Prioritas koneksi lebih stabil",
                    checked = networkStable,
                    onClick = {
                        networkStable = !networkStable
                        setTweakNow("networkStable", networkStable)
                    }
                )
                ToggleOption(
                    title = "TCP",
                    subtitle = "Optimasi TCP stack",
                    checked = tcpMode || tweaks.tcpBbr,
                    onClick = {
                        tcpMode = !(tcpMode || tweaks.tcpBbr)
                        setTweakNow("tcpBbr", tcpMode)
                    }
                )
            }

            ExpandableTweakCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Memory,
                title = "Memory",
                subtitle = "ZRAM, LMK, Swap",
                badge = if (tweaks.zram) "ZRAM" else if (tweaks.lmkAggressive) "LMK" else "Off",
                active = tweaks.zram || tweaks.lmkAggressive || swapEnabled,
                expanded = expandedCard == "memory",
                fromLeft = false,
                onClick = { toggleExpand("memory") }
            ) {
                ToggleOption(
                    title = "ZRAM",
                    subtitle = "Kompresi memori aktif",
                    checked = tweaks.zram,
                    onClick = { setTweakNow("zram", !tweaks.zram) }
                )
                ToggleOption(
                    title = "LMK",
                    subtitle = "Low Memory Killer agresif",
                    checked = tweaks.lmkAggressive,
                    onClick = { setTweakNow("lmkAggressive", !tweaks.lmkAggressive) }
                )
                ToggleOption(
                    title = "Swap",
                    subtitle = "Bantu ruang memori virtual",
                    checked = swapEnabled,
                    onClick = {
                        swapEnabled = !swapEnabled
                        setTweakNow("swap", swapEnabled)
                    }
                )
                CompactFeatureBlock(
                    icon = Icons.Outlined.Memory,
                    title = "Kill Background All",
                    subtitle = "Bersihkan app latar belakang",
                    trailing = "Run",
                    onClick = {
                        setTweakNow("killBackground", true)
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpandableTweakCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Storage,
                title = "I/O Scheduler",
                subtitle = "Disk latency",
                badge = ioScheduler.uppercase(),
                active = ioScheduler != "auto",
                expanded = expandedCard == "io",
                fromLeft = true,
                onClick = { toggleExpand("io") }
            ) {
                DropdownAction(
                    title = "Pilih Scheduler",
                    value = ioScheduler.uppercase(),
                    options = listOf("auto", "cfq", "deadline", "noop", "bfq", "maple"),
                    onSelect = {
                        ioScheduler = it
                        setTweakNow("ioScheduler", it != "auto")
                    }
                )
            }

            ExpandableTweakCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Speed,
                title = "Sched Boost",
                subtitle = "Game booster",
                badge = if (tweaks.schedboost) "Boost" else "Off",
                active = tweaks.schedboost,
                expanded = expandedCard == "sched",
                fromLeft = false,
                onClick = {
                    toggleExpand("sched")
                    setTweakNow("schedboost", !tweaks.schedboost)
                }
            ) {
                Speedometer(active = tweaks.schedboost)
                Text(
                    if (tweaks.schedboost) "Boost aktif untuk respon lebih cepat" else "Tap card untuk aktifkan boost",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ExpandableTweakCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Thermostat,
            title = "Thermal Profile",
            subtitle = "Muncul hanya saat expand",
            badge = thermalLabel(tweaks.thermalProfile),
            active = tweaks.thermalProfile != "default",
            expanded = expandedCard == "thermal",
            fromLeft = true,
            baseHeight = 92.dp,
            onClick = { toggleExpand("thermal") }
        ) {
            ProfileSelector(
                current = tweaks.thermalProfile,
                onSelect = { setProfileNow(it) }
            )
        }
    }

    if (rendererDialog) {
        RendererDialog(
            current = renderer,
            onDismiss = { rendererDialog = false },
            onSelect = {
                renderer = it
                rendererDialog = false
                setTweakNow("renderer", true)
            }
        )
    }
}

@Composable
private fun ExpandableTweakCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    active: Boolean,
    expanded: Boolean,
    fromLeft: Boolean,
    baseHeight: Dp = 146.dp,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(220),
        label = "card_container"
    )
    val iconBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "icon_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "icon_tint"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "badge_bg"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (active || expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "badge_fg"
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = smoothSpring(),
        label = "arrow_rotation"
    )
    val detailAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (expanded) 260 else 160),
        label = "detail_alpha"
    )
    val detailOffset by animateDpAsState(
        targetValue = if (expanded) 0.dp else if (fromLeft) (-20).dp else 20.dp,
        animationSpec = smoothSpring(),
        label = "detail_offset"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = container,
        modifier = modifier
            .heightIn(min = baseHeight)
            .shadow(if (expanded) 8.dp else 0.dp, RoundedCornerShape(24.dp))
            .animateContentSize(animationSpec = smoothSpring())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
                }

                Surface(shape = RoundedCornerShape(50.dp), color = badgeBg) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeFg,
                        maxLines = 1
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = detailAlpha
                            translationX = detailOffset.toPx()
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun AppProfileCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .animateContentSize(animationSpec = smoothSpring())
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(25.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "App Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Atur profile performa per aplikasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            }
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = -90f }
            )
        }
    }
}

@Composable
private fun DropdownAction(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(180),
        label = "toggle_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180),
        label = "toggle_fg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = fg)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.68f))
            }
        }
    }
}

@Composable
private fun CompactFeatureBlock(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primary) {
                Text(
                    trailing,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ProfileSelector(
    current: String,
    onSelect: (String) -> Unit
) {
    val profiles = listOf(
        Triple("default", "Balance", Icons.Outlined.Balance),
        Triple("performance", "Performance", Icons.Outlined.FlashOn),
        Triple("extreme", "Extreme", Icons.Filled.Bolt)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        profiles.forEach { (key, label, icon) ->
            val selected = current == key
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = tween(180),
                label = "profile_bg"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(180),
                label = "profile_fg"
            )
            Surface(
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(15.dp),
                color = bg,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Speedometer(active: Boolean) {
    val progress by animateFloatAsState(
        targetValue = if (active) 0.86f else 0.28f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "speedometer_progress"
    )
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val needle = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = track,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = primary,
                startAngle = 150f,
                sweepAngle = 240f * progress,
                useCenter = false,
                style = stroke
            )
            val angle = Math.toRadians((150f + 240f * progress).toDouble())
            val center = Offset(size.width / 2f, size.height / 2f)
            val end = Offset(
                x = center.x + cos(angle).toFloat() * 38.dp.toPx(),
                y = center.y + sin(angle).toFloat() * 38.dp.toPx()
            )
            drawLine(color = needle, start = center, end = end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(color = primary, radius = 6.dp.toPx(), center = center)
        }
        Text(
            if (active) "BOOST" else "IDLE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RendererDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val renderers = listOf("OpenGL", "Vulkan", "ANGLE", "SkiaVulkan", "SkiaGL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select GPU Renderer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                renderers.forEach { item ->
                    ToggleOption(
                        title = item,
                        subtitle = if (item == current) "Sedang dipilih" else "Tap untuk memilih",
                        checked = item == current,
                        onClick = { onSelect(item) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

private fun thermalLabel(value: String): String {
    return when (value) {
        "performance" -> "Performance"
        "extreme" -> "Extreme"
        else -> "Balance"
    }
}

private fun <T> smoothSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
