package dev.aether.manager.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.shizuku.ShizukuShell
import kotlinx.coroutines.delay

@Composable
fun NoRootTweakScreen(vm: MainViewModel) {
    val tweaks by vm.tweaks.collectAsState()
    val applyStatus by vm.applyStatus.collectAsState()
    val monitor by vm.monitorState.collectAsState()

    var shizukuRunning by remember { mutableStateOf(false) }
    var shizukuGranted by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            shizukuRunning = ShizukuShell.isAvailable()
            shizukuGranted = ShizukuShell.hasPermission()
            tick++
            delay(1200L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 150.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NoRootHeader(
            running = shizukuRunning,
            granted = shizukuGranted,
            onRequest = { ShizukuShell.requestPermissionIfNeeded() }
        )

        NoRootMonitorCard(
            cpu = monitor.cpuUsage,
            ram = if (monitor.ramTotalMb > 0) "${monitor.ramUsedMb}/${monitor.ramTotalMb} MB" else "Detecting",
            storage = if (monitor.storageTotalGb > 0f) "${monitor.storageUsedGb.toInt()}/${monitor.storageTotalGb.toInt()} GB" else "Detecting",
            uptime = monitor.uptime.ifBlank { "Detecting" }
        )

        SectionTitle("No Root Tweaks", "Only safe settings that can run with Shizuku are shown here.")

        NoRootToggleCard(
            icon = Icons.Outlined.Dns,
            title = "Private DNS",
            subtitle = "Use system Private DNS without root.",
            checked = tweaks.dnsProvider.equals("adguard", true) || tweaks.dnsProvider.equals("cloudflare", true) || tweaks.dnsProvider.equals("google", true),
            enabled = shizukuRunning && shizukuGranted,
            onChecked = { enabled ->
                vm.setTweakStr("dns_provider", if (enabled) "adguard" else "Off")
            }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.Speed,
            title = "Fast Animation",
            subtitle = "Set Android animation scale to 0.5x.",
            checked = tweaks.fastAnim,
            enabled = shizukuRunning && shizukuGranted,
            onChecked = { vm.setTweak("fast_anim", it) }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.BatterySaver,
            title = "Doze Basic",
            subtitle = "Enable safe device idle doze command.",
            checked = tweaks.doze,
            enabled = shizukuRunning && shizukuGranted,
            onChecked = { vm.setTweak("doze", it) }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.Cached,
            title = "Trim Cache",
            subtitle = "Run package cache trim via Shizuku shell.",
            checked = tweaks.clearCache,
            enabled = shizukuRunning && shizukuGranted,
            onChecked = { vm.setTweak("clear_cache", it) }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.NetworkCheck,
            title = "Network Lite",
            subtitle = "Disable always-on Wi‑Fi/BLE scan where supported.",
            checked = tweaks.networkStable,
            enabled = shizukuRunning && shizukuGranted,
            onChecked = { vm.setTweak("network_stable", it) }
        )

        RootLockedCard()

        FilledTonalButton(
            onClick = { vm.applyAll() },
            enabled = shizukuRunning && shizukuGranted && !applyStatus.running,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            AnimatedContent(
                targetState = applyStatus.running,
                transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) },
                label = "no_root_apply"
            ) { running ->
                if (running) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Apply No Root Tweaks", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (applyStatus.summary.isNotBlank()) {
            Text(
                text = applyStatus.summary,
                color = if (applyStatus.lastOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun NoRootHeader(
    running: Boolean,
    granted: Boolean,
    onRequest: () -> Unit
) {
    val color by animateColorAsState(
        when {
            running && granted -> MaterialTheme.colorScheme.primary
            running -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        },
        tween(220, easing = FastOutSlowInEasing),
        label = "shizuku_status_color"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (running && granted) Icons.Outlined.CheckCircle else Icons.Outlined.Security,
                        contentDescription = null,
                        tint = color
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("No Root Mode", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            running && granted -> "Shizuku running and permission granted"
                            running -> "Shizuku running, permission needed"
                            else -> "Shizuku not running — pair/start Shizuku again"
                        },
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                "CPU/GPU/kernel/root-only features are hidden in this mode. Use Root Mode if you need App Profile, governor, scheduler, thermal, or GPU controls.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            if (!granted) {
                OutlinedButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (running) "Grant Shizuku Permission" else "Open / Pair Shizuku Again")
                }
            }
        }
    }
}

@Composable
private fun NoRootMonitorCard(cpu: Int, ram: String, storage: String, uptime: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("No Root Monitor", "Safe monitor data only. CPU/GPU kernel stats require root on many devices.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SmallStat("CPU", if (cpu > 0) "$cpu%" else "0–1%", Modifier.weight(1f))
                SmallStat("RAM", ram, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SmallStat("Storage", storage, Modifier.weight(1f))
                SmallStat("Uptime", uptime, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallStat(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NoRootToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val borderColor = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(enabled = enabled) { onChecked(!checked) },
        shape = RoundedCornerShape(24.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (checked) 0.16f else 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onChecked(it) },
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun RootLockedCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Root Features Hidden", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Text(
                "App Profile, CPU governor, GPU frequency, thermal profile, I/O scheduler, ZRAM, LMK, and boot persistence are root-only. They are intentionally not shown for no-root users.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            Text(
                "Switch to Root Mode from Setup/Settings to unlock full kernel manager features.",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
