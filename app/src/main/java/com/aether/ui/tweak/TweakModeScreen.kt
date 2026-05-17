package com.aether.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.data.AccessMode
import com.aether.data.ApplyStatus
import com.aether.data.MainViewModel
import com.aether.data.TweaksState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TweakModeScreen(
    vm: MainViewModel,
    onOpenAppProfile: () -> Unit = {},
) {
    val mode by vm.accessMode.collectAsState()
    val rootGranted by vm.rootGranted.collectAsState()
    val tweaks by vm.tweaks.collectAsState()
    val apply by vm.applyStatus.collectAsState()
    val applying by vm.applyingTweak.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AccessHeader(
            mode = mode,
            rootGranted = rootGranted,
            status = apply,
            applying = applying,
            onMode = { vm.setAccessMode(it) }
        )

        ProfileCard(
            profile = tweaks.profile,
            onProfile = { vm.setProfile(it) }
        )

        CleanSection(
            title = "Quick Actions",
            subtitle = "Akses cepat untuk tweak harian",
            icon = Icons.Outlined.Tune
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton("Apply", Icons.Outlined.Bolt, Modifier.weight(1f)) { vm.applyAll() }
                ActionButton("Reset", Icons.Outlined.RestartAlt, Modifier.weight(1f)) { vm.resetTweaks() }
                ActionButton("Apps", Icons.Outlined.Apps, Modifier.weight(1f)) { onOpenAppProfile() }
            }
        }

        CleanSection(
            title = "Performance",
            subtitle = if (mode == AccessMode.ROOT) "CPU, GPU, scheduler" else "Disimpan aman, aktif penuh jika Shizuku tersedia",
            icon = Icons.Outlined.Speed
        ) {
            ToggleRow("CPU Boost", "Prioritas respons dan scheduler", tweaks.cpuBoost) { vm.setTweak("cpu_boost", it) }
            ToggleRow("Sched Boost", "Optimasi task latency", tweaks.schedboost) { vm.setTweak("schedboost", it) }
            ToggleRow("GPU Throttle Off", "Mengurangi pembatasan GPU", tweaks.gpuThrottleOff) { vm.setTweak("gpu_throttle_off", it) }
            ToggleRow("CPU Freq Lock", "Kunci frekuensi sesuai konfigurasi", tweaks.cpuFreqLock) { vm.setTweak("cpu_freq_lock", it) }
            ToggleRow("Touch Boost", "Respons sentuhan lebih cepat", tweaks.touchBoost) { vm.setTweak("touch_boost", it) }
            ChoiceRow(
                title = "Governor",
                value = tweaks.cpuGovernor.ifBlank { "auto" },
                options = listOf("auto", "schedutil", "performance", "powersave"),
                onSelect = { vm.setTweakStr("cpu_governor", if (it == "auto") "" else it) }
            )
        }

        CleanSection(
            title = "Memory",
            subtitle = "RAM, ZRAM, cache, background app",
            icon = Icons.Outlined.Memory
        ) {
            ToggleRow("ZRAM", "Aktifkan kompresi memori", tweaks.zram) { vm.setTweak("zram", it) }
            ToggleRow("LMK Aggressive", "Bersihkan proses berat", tweaks.lmkAggressive) { vm.setTweak("lmk_aggressive", it) }
            ToggleRow("Swap", "Optimasi swap/ZRAM", tweaks.swap) { vm.setTweak("swap", it) }
            ToggleRow("Kill Background", "Tutup proses background ringan", tweaks.killBackground) { vm.setTweak("kill_background", it) }
            ToggleRow("VM Dirty Opt", "Optimasi writeback cache", tweaks.vmDirtyOpt) { vm.setTweak("vm_dirty_opt", it) }
            ChoiceRow(
                title = "ZRAM Size",
                value = zramSizeLabel(tweaks.zramSize),
                options = listOf("512 MB", "1 GB", "1.5 GB", "2 GB"),
                onSelect = { vm.setTweakStr("zram_size", zramSizeValue(it)) }
            )
        }

        CleanSection(
            title = "Network",
            subtitle = "DNS, TCP, buffer jaringan",
            icon = Icons.Outlined.Wifi
        ) {
            ToggleRow("Network Stable", "Stabilkan jaringan saat gaming/streaming", tweaks.networkStable) { vm.setTweak("network_stable", it) }
            ToggleRow("TCP BBR", "Congestion control modern", tweaks.tcpBbr) { vm.setTweak("tcp_bbr", it) }
            ToggleRow("Net Buffer", "Buffer jaringan lebih agresif", tweaks.netBuffer) { vm.setTweak("net_buffer", it) }
            ChoiceRow(
                title = "Private DNS",
                value = dnsLabel(tweaks.dnsProvider),
                options = listOf("Off", "Cloudflare", "Google", "Quad9", "CleanBrowsing"),
                onSelect = { vm.setTweakStr("dns_provider", dnsValue(it)) }
            )
        }

        CleanSection(
            title = "I/O & Thermal",
            subtitle = "Storage latency dan profil suhu",
            icon = Icons.Outlined.Storage
        ) {
            ToggleRow("I/O Latency", "Kurangi delay baca/tulis", tweaks.ioLatencyOpt) { vm.setTweak("io_latency_opt", it) }
            ToggleRow("Clear Cache", "Bersihkan cache sistem aplikasi", tweaks.clearCache) { vm.setTweak("clear_cache", it) }
            ToggleRow("Entropy Boost", "Optimasi entropy ringan", tweaks.entropyBoost) { vm.setTweak("entropy_boost", it) }
            ChoiceRow(
                title = "I/O Scheduler",
                value = tweaks.ioScheduler.ifBlank { "auto" },
                options = listOf("auto", "cfq", "noop", "deadline", "bfq", "kyber"),
                onSelect = { vm.setTweakStr("io_scheduler", if (it == "auto") "" else it) }
            )
            ChoiceRow(
                title = "Thermal Profile",
                value = thermalLabel(tweaks.thermalProfile),
                options = listOf("Default", "Balance", "Performance", "Gaming", "Cool"),
                onSelect = { vm.setTweakStr("thermal_profile", it.lowercase()) }
            )
        }

        CleanSection(
            title = "No Root Safe",
            subtitle = "Fitur aman tanpa akses root",
            icon = Icons.Outlined.PhoneAndroid
        ) {
            ToggleRow("Doze", "Hemat baterai saat idle", tweaks.doze) { vm.setTweak("doze", it) }
            ToggleRow("Fast Animation", "Animasi sistem terasa lebih cepat", tweaks.fastAnim) { vm.setTweak("fast_anim", it) }
            ToggleRow("KSM", "Deduplicate memory jika didukung", tweaks.ksm) { vm.setTweak("ksm", it) }
            ToggleRow("KSM Aggressive", "Mode KSM lebih kuat", tweaks.ksmAggressive) { vm.setTweak("ksm_aggressive", it) }
        }
    }
}

@Composable
private fun AccessHeader(
    mode: AccessMode,
    rootGranted: Boolean,
    status: ApplyStatus,
    applying: Boolean,
    onMode: (AccessMode) -> Unit,
) {
    CleanSection(
        title = "Tweak Control",
        subtitle = if (mode == AccessMode.ROOT) "Mode Root" else "Mode No Root",
        icon = Icons.Outlined.Security,
        compactHeader = true
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeButton(
                title = "Root",
                subtitle = if (rootGranted) "SU aktif" else "Butuh izin SU",
                icon = Icons.Outlined.AdminPanelSettings,
                selected = mode == AccessMode.ROOT,
                warning = mode == AccessMode.ROOT && !rootGranted,
                modifier = Modifier.weight(1f)
            ) { onMode(AccessMode.ROOT) }
            ModeButton(
                title = "No Root",
                subtitle = "Aman",
                icon = Icons.Outlined.PhoneAndroid,
                selected = mode == AccessMode.NO_ROOT,
                warning = false,
                modifier = Modifier.weight(1f)
            ) { onMode(AccessMode.NO_ROOT) }
        }

        if (applying || status.summary.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (applying) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .14f),
                        strokeCap = StrokeCap.Round
                    )
                }
                StatusLine(status = status, applying = applying)
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: String, onProfile: (String) -> Unit) {
    CleanSection(
        title = "Active Profile",
        subtitle = "Pilih mode performa",
        icon = Icons.Outlined.Bolt,
        compactHeader = true
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("balance" to "Balance", "performance" to "Performance", "extreme" to "Extreme", "battery" to "Battery").forEach { (value, label) ->
                SelectChip(
                    text = label,
                    selected = profile.equals(value, ignoreCase = true) || (value == "balance" && profile.isBlank()),
                    onClick = { onProfile(value) }
                )
            }
        }
    }
}

@Composable
private fun CleanSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    compactHeader: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .96f),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .20f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = .48f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .70f)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactHeader) 12.dp else 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(icon = icon, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            content()
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    warning: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .76f)
            warning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = .40f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .66f)
        },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "mode_bg_$title"
    )
    val fg = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        warning -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = spring(dampingRatio = .82f, stiffness = 420f),
        label = "mode_scale_$title"
    )
    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = .16f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(icon = icon, tint = fg, size = 34.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = fg, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = fg.copy(alpha = .78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .48f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChecked(!checked) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(title: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                SelectChip(text = option, selected = option.equals(value, ignoreCase = true), onClick = { onSelect(option) })
            }
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .82f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .68f),
        border = BorderStroke(1.dp, color.copy(alpha = .18f)),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = color, modifier = Modifier.size(15.dp))
            Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .84f))
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun StatusLine(status: ApplyStatus, applying: Boolean) {
    val ok = status.lastOk
    val color = when {
        applying -> MaterialTheme.colorScheme.primary
        ok -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = .10f),
        border = BorderStroke(1.dp, color.copy(alpha = .16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Security, null, tint = color, modifier = Modifier.size(18.dp))
            Text(
                text = if (applying) "Menerapkan tweak…" else status.summary.ifBlank { "Siap" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = .12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * .52f))
    }
}

private fun dnsValue(label: String): String = when (label.lowercase()) {
    "cloudflare" -> "one.one.one.one"
    "google" -> "dns.google"
    "quad9" -> "dns.quad9.net"
    "cleanbrowsing" -> "security-filter-dns.cleanbrowsing.org"
    else -> ""
}

private fun dnsLabel(value: String): String = when (value.lowercase()) {
    "one.one.one.one", "cloudflare" -> "Cloudflare"
    "dns.google", "google" -> "Google"
    "dns.quad9.net", "quad9" -> "Quad9"
    "security-filter-dns.cleanbrowsing.org", "cleanbrowsing" -> "CleanBrowsing"
    else -> "Off"
}

private fun zramSizeValue(label: String): String = when (label) {
    "512 MB" -> "536870912"
    "1.5 GB" -> "1610612736"
    "2 GB" -> "2147483648"
    else -> "1073741824"
}

private fun zramSizeLabel(value: String): String = when (value) {
    "536870912" -> "512 MB"
    "1610612736" -> "1.5 GB"
    "2147483648" -> "2 GB"
    else -> "1 GB"
}

private fun thermalLabel(value: String): String = when (value.lowercase()) {
    "balance", "balanced" -> "Balance"
    "performance" -> "Performance"
    "gaming" -> "Gaming"
    "cool", "battery" -> "Cool"
    else -> "Default"
}
