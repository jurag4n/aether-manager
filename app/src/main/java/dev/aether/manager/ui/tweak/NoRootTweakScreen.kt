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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.ui.components.AetherSwitch
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.shizuku.ShizukuShell
import kotlinx.coroutines.delay

@Composable
fun NoRootTweakScreen(vm: MainViewModel) {
    val tweaks by vm.tweaks.collectAsState()
    val applyStatus by vm.applyStatus.collectAsState()

    var shizukuRunning by remember { mutableStateOf(false) }
    var shizukuGranted by remember { mutableStateOf(false) }
    var dnsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            shizukuRunning = ShizukuShell.isAvailable()
            shizukuGranted = ShizukuShell.hasPermission()
            delay(1200L)
        }
    }

    val canApply = shizukuRunning && shizukuGranted
    val dnsOptions = listOf(
        "Off" to "Off",
        "Cloudflare" to "cloudflare",
        "Google DNS" to "google",
        "CleanBrowsing Family" to "cleanbrowsing"
    )
    val selectedDnsLabel = dnsOptions.firstOrNull { it.second.equals(tweaks.dnsProvider, true) }?.first
        ?: if (tweaks.dnsProvider.isBlank()) "Off" else tweaks.dnsProvider

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

        SectionTitle("No Root Tweaks", "Changes apply instantly when Shizuku is running.")

        DnsOptionCard(
            selected = selectedDnsLabel,
            expanded = dnsExpanded,
            enabled = canApply,
            options = dnsOptions,
            onExpand = { dnsExpanded = !dnsExpanded },
            onSelect = { value ->
                dnsExpanded = false
                vm.setTweakStr("dns_provider", value)
            }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.Speed,
            title = "Fast Animation",
            subtitle = "Set Android animation scale to 0.5x.",
            checked = tweaks.fastAnim,
            enabled = canApply,
            onChecked = { vm.setTweak("fast_anim", it) }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.BatterySaver,
            title = "Doze Basic",
            subtitle = "Enable safe device idle doze command.",
            checked = tweaks.doze,
            enabled = canApply,
            onChecked = { vm.setTweak("doze", it) }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.Cached,
            title = "Trim Cache",
            subtitle = "Run package cache trim via Shizuku shell.",
            checked = tweaks.clearCache,
            enabled = canApply,
            onChecked = { vm.setTweak("clear_cache", it) }
        )

        NoRootToggleCard(
            icon = Icons.Outlined.NetworkCheck,
            title = "Network Lite",
            subtitle = "Disable always-on Wi‑Fi/BLE scan where supported.",
            checked = tweaks.networkStable,
            enabled = canApply,
            onChecked = { vm.setTweak("network_stable", it) }
        )

        RootLockedCard()

        if (applyStatus.running || applyStatus.summary.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (applyStatus.running) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (applyStatus.lastOk) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (applyStatus.lastOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = if (applyStatus.running) "Applying instantly…" else applyStatus.summary,
                        color = if (applyStatus.lastOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
                "Mode ini hanya menyediakan fitur aman tanpa root. Untuk fitur kernel, gunakan Root Mode.",
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
private fun DnsOptionCard(
    selected: String,
    expanded: Boolean,
    enabled: Boolean,
    options: List<Pair<String, String>>,
    onExpand: () -> Unit,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(enabled = enabled) { onExpand() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Dns, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Private DNS", fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                    Text(selected, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedContent(targetState = expanded, label = "dns_expand") { open ->
                if (open) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { (label, value) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(value) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (label == selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        if (label == selected) Icons.Outlined.CheckCircle else Icons.Outlined.Dns,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (label == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(label, fontWeight = if (label == selected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
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
            AetherSwitch(
                checked = checked,
                onCheckedChange = { onChecked(it) },
                enabled = enabled,
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
                "Fitur kernel tidak ditampilkan di mode no-root agar UI tetap bersih dan tidak membingungkan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            Text(
                "Gunakan Root Mode untuk kontrol kernel penuh.",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
