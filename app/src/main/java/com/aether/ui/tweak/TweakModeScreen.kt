package com.aether.ui.tweak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
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
import com.aether.shizuku.ShizukuShell

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
    var shizukuState by remember { mutableStateOf(ShizukuShell.state()) }

    LaunchedEffect(mode, apply.summary, applying) {
        shizukuState = ShizukuShell.state()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AccessControlCard(
            mode = mode,
            rootGranted = rootGranted,
            shizukuState = shizukuState,
            status = apply,
            applying = applying,
            onMode = vm::setAccessMode,
            onGrantShizuku = vm::requestShizuku
        )

        ProfileCard(profile = tweaks.profile, onProfile = vm::setProfile)

        QuickActionsCard(
            mode = mode,
            applying = applying,
            shizukuReady = shizukuState == ShizukuShell.State.READY,
            onApply = vm::applyAll,
            onReset = vm::resetTweaks,
            onApps = onOpenAppProfile
        )

        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "tweak_mode_content"
        ) { targetMode ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (targetMode == AccessMode.ROOT) {
                    CapabilityBanner(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "Root Mode: fitur lengkap",
                        subtitle = "Menggunakan libsu untuk governor, cpufreq, GPU, ZRAM, LMK, I/O, thermal, dan network kernel.",
                        tone = MaterialTheme.colorScheme.primary
                    )

                    ExpandSection("CPU & GPU", "Full kernel tuning via libsu", Icons.Outlined.Speed, true, badge = "ROOT") {
                        ToggleRow("CPU Boost", "Prioritas proses aktif dan scheduler", tweaks.cpuBoost) { vm.setTweak("cpu_boost", it) }
                        ToggleRow("Sched Boost", "Respons scheduler lebih cepat", tweaks.schedboost) { vm.setTweak("schedboost", it) }
                        ToggleRow("GPU Throttle Off", "Kurangi pembatasan GPU jika node tersedia", tweaks.gpuThrottleOff) { vm.setTweak("gpu_throttle_off", it) }
                        ToggleRow("CPU Freq Lock", "Kunci rentang frekuensi CPU", tweaks.cpuFreqLock) { vm.setTweak("cpu_freq_lock", it) }
                        ToggleRow("GPU Freq Lock", "Kunci frekuensi GPU maksimum", tweaks.gpuFreqLock) { vm.setTweak("gpu_freq_lock", it) }
                        ChoiceRow("Governor", tweaks.cpuGovernor.ifBlank { "auto" }, listOf("auto", "schedutil", "performance", "powersave")) {
                            vm.setTweakStr("cpu_governor", if (it == "auto") "" else it)
                        }
                    }

                    ExpandSection("Memory", "ZRAM, LMK, swap, VM dirty", Icons.Outlined.Memory, false, badge = "ROOT") {
                        ToggleRow("ZRAM", "Aktifkan kompresi RAM jika didukung", tweaks.zram) { vm.setTweak("zram", it) }
                        ToggleRow("LMK Aggressive", "Bersihkan aplikasi berat lebih cepat", tweaks.lmkAggressive) { vm.setTweak("lmk_aggressive", it) }
                        ToggleRow("Swap", "Optimasi swap/ZRAM", tweaks.swap) { vm.setTweak("swap", it) }
                        ToggleRow("Kill Background", "Tutup proses background ringan", tweaks.killBackground) { vm.setTweak("kill_background", it) }
                        ToggleRow("VM Dirty Opt", "Writeback cache lebih rapi", tweaks.vmDirtyOpt) { vm.setTweak("vm_dirty_opt", it) }
                        ChoiceRow("ZRAM Size", zramSizeLabel(tweaks.zramSize), listOf("512 MB", "1 GB", "1.5 GB", "2 GB")) {
                            vm.setTweakStr("zram_size", zramSizeValue(it))
                        }
                    }

                    ExpandSection("Network", "Private DNS, TCP, BBR, buffer", Icons.Outlined.Wifi, false, badge = "ROOT") {
                        ToggleRow("Network Stable", "Stabil untuk game dan streaming", tweaks.networkStable) { vm.setTweak("network_stable", it) }
                        ToggleRow("TCP BBR", "Congestion control modern jika kernel mendukung", tweaks.tcpBbr) { vm.setTweak("tcp_bbr", it) }
                        ToggleRow("Net Buffer", "Buffer jaringan lebih agresif", tweaks.netBuffer) { vm.setTweak("net_buffer", it) }
                        ChoiceRow("Private DNS", dnsLabel(tweaks.dnsProvider), listOf("Off", "Cloudflare", "Google", "Quad9", "CleanBrowsing")) {
                            vm.setTweakStr("dns_provider", dnsValue(it))
                        }
                    }

                    ExpandSection("I/O & Thermal", "Scheduler storage dan profil thermal", Icons.Outlined.Storage, false, badge = "ROOT") {
                        ToggleRow("I/O Latency", "Kurangi delay baca tulis", tweaks.ioLatencyOpt) { vm.setTweak("io_latency_opt", it) }
                        ToggleRow("Clear Cache", "Bersihkan cache saat apply", tweaks.clearCache) { vm.setTweak("clear_cache", it) }
                        ToggleRow("Entropy Boost", "Respons random pool ringan", tweaks.entropyBoost) { vm.setTweak("entropy_boost", it) }
                        ChoiceRow("I/O Scheduler", tweaks.ioScheduler.ifBlank { "auto" }, listOf("auto", "cfq", "noop", "deadline", "bfq", "kyber")) {
                            vm.setTweakStr("io_scheduler", if (it == "auto") "" else it)
                        }
                        ChoiceRow("Thermal Profile", thermalLabel(tweaks.thermalProfile), listOf("Default", "Balance", "Performance", "Gaming", "Cool")) {
                            vm.setTweakStr("thermal_profile", it.lowercase())
                        }
                    }

                    ExpandSection("UI & Power", "Fitur aman tambahan", Icons.Outlined.Tune, false, badge = "ROOT") {
                        ToggleRow("Fast Animation", "Animasi sistem terasa ringan", tweaks.fastAnim) { vm.setTweak("fast_anim", it) }
                        ToggleRow("Doze", "Hemat baterai saat idle", tweaks.doze) { vm.setTweak("doze", it) }
                        ToggleRow("Touch Boost", "Respons sentuhan lebih cepat jika node tersedia", tweaks.touchBoost) { vm.setTweak("touch_boost", it) }
                        ToggleRow("KSM", "Deduplicate memory jika didukung", tweaks.ksm) { vm.setTweak("ksm", it) }
                        ToggleRow("KSM Aggressive", "Mode KSM lebih kuat", tweaks.ksmAggressive) { vm.setTweak("ksm_aggressive", it) }
                    }
                } else {
                    CapabilityBanner(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "No Root Mode: fitur ringan",
                        subtitle = "Tidak memakai SU. Fitur dibatasi ke pengaturan aman lewat Shizuku: DNS, animasi, doze, cache, dan stabilitas ringan.",
                        tone = if (shizukuState == ShizukuShell.State.READY) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )

                    ExpandSection("No Root Tools", "Aman via Shizuku, tidak menyentuh kernel", Icons.Outlined.PhoneAndroid, true, badge = "SHIZUKU") {
                        ChoiceRow("Private DNS", dnsLabel(tweaks.dnsProvider), listOf("Off", "Cloudflare", "Google", "Quad9", "CleanBrowsing")) {
                            vm.setTweakStr("dns_provider", dnsValue(it))
                        }
                        ToggleRow("Fast Animation", "Ubah scale animasi sistem", tweaks.fastAnim) { vm.setTweak("fast_anim", it) }
                        ToggleRow("Doze", "Aktifkan device idle deep mode", tweaks.doze) { vm.setTweak("doze", it) }
                        ToggleRow("Clear Cache", "Trim cache package via shell", tweaks.clearCache) { vm.setTweak("clear_cache", it) }
                        ToggleRow("Network Stable", "Matikan scan latar jika diizinkan", tweaks.networkStable) { vm.setTweak("network_stable", it) }
                    }

                    ExpandSection("App Profile", "Profil aplikasi tanpa kernel tweak", Icons.Outlined.Apps, false, badge = "NO ROOT") {
                        Text(
                            "Untuk No Root, App Profile fokus ke daftar aplikasi dan pengaturan aman. Tweak CPU/GPU per-app tetap butuh Root.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        ActionButton("Open Apps", Icons.Outlined.Apps, Modifier.fillMaxWidth(), enabled = true, onClick = onOpenAppProfile)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessControlCard(
    mode: AccessMode,
    rootGranted: Boolean,
    shizukuState: ShizukuShell.State,
    status: ApplyStatus,
    applying: Boolean,
    onMode: (AccessMode) -> Unit,
    onGrantShizuku: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .28f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Outlined.Security, MaterialTheme.colorScheme.primary, 46.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tweak Control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        text = if (mode == AccessMode.ROOT) "Root memakai libsu" else "No Root memakai Shizuku",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ModeButton(
                    title = "Root",
                    subtitle = if (rootGranted) "libsu ready" else "Grant SU",
                    icon = Icons.Outlined.AdminPanelSettings,
                    selected = mode == AccessMode.ROOT,
                    warning = mode == AccessMode.ROOT && !rootGranted,
                    modifier = Modifier.weight(1f)
                ) { onMode(AccessMode.ROOT) }
                ModeButton(
                    title = "No Root",
                    subtitle = if (shizukuState == ShizukuShell.State.READY) "Shizuku ready" else "Need Shizuku",
                    icon = Icons.Outlined.PhoneAndroid,
                    selected = mode == AccessMode.NO_ROOT,
                    warning = mode == AccessMode.NO_ROOT && shizukuState != ShizukuShell.State.READY,
                    modifier = Modifier.weight(1f)
                ) { onMode(AccessMode.NO_ROOT) }
            }

            AnimatedVisibility(visible = mode == AccessMode.NO_ROOT && shizukuState != ShizukuShell.State.READY) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .38f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .18f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shizukuState.label, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            Text(shizukuState.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                        ActionButton("Grant", Icons.Outlined.CheckCircle, enabled = true, onClick = onGrantShizuku)
                    }
                }
            }

            AnimatedVisibility(visible = applying || status.summary.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (applying) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(profile: String, onProfile: (String) -> Unit) {
    ExpandSection("Active Profile", "Balance, Performance, Extreme, Battery", Icons.Outlined.Bolt, true, locked = true, badge = "PROFILE") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("balance" to "Balance", "performance" to "Performance", "extreme" to "Extreme", "battery" to "Battery").forEach { (value, label) ->
                SelectChip(
                    text = label,
                    selected = profile.equals(value, true) || (value == "balance" && profile.isBlank()),
                    onClick = { onProfile(value) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    mode: AccessMode,
    applying: Boolean,
    shizukuReady: Boolean,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onApps: () -> Unit,
) {
    val applyEnabled = !applying && (mode == AccessMode.ROOT || shizukuReady)
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionButton(if (mode == AccessMode.ROOT) "Apply Root" else "Apply No Root", Icons.Outlined.Bolt, Modifier.weight(1f), enabled = applyEnabled, onClick = onApply)
                ActionButton("Reset", Icons.Outlined.RestartAlt, Modifier.weight(1f), enabled = !applying, onClick = onReset)
                ActionButton("Apps", Icons.Outlined.Apps, Modifier.weight(1f), enabled = true, onClick = onApps)
            }
            if (mode == AccessMode.NO_ROOT && !shizukuReady) {
                Text(
                    "Tombol Apply No Root aktif setelah Shizuku diizinkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CapabilityBanner(icon: ImageVector, title: String, subtitle: String, tone: Color) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = tone.copy(alpha = .10f),
        border = BorderStroke(1.dp, tone.copy(alpha = .18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(icon, tone, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = tone)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ExpandSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    defaultExpanded: Boolean,
    locked: Boolean = false,
    badge: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1f else .992f,
        animationSpec = spring(dampingRatio = .82f, stiffness = 500f),
        label = "section_scale_$title"
    )
    val borderColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = .25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .18f),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "section_border_$title"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (expanded) 2.dp else 1.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = !locked) { expanded = !expanded }
            .animateContentSize(tween(260, easing = FastOutSlowInEasing))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon, MaterialTheme.colorScheme.primary, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (badge.isNotBlank()) ModeTag(badge)
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!locked) Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(140)) + expandVertically(tween(230, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(tween(170, easing = FastOutSlowInEasing)) + fadeOut(tween(100))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            }
        }
    }
}

@Composable
private fun ModeButton(title: String, subtitle: String, icon: ImageVector, selected: Boolean, warning: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = when {
        warning -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bg = when {
        warning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = .44f)
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .82f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .62f)
    }
    Surface(
        modifier = modifier.clip(RoundedCornerShape(23.dp)).clickable { onClick() },
        shape = RoundedCornerShape(23.dp),
        color = bg,
        border = BorderStroke(1.dp, color.copy(alpha = .18f))
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, color, 34.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = color, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = .78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .36f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .52f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "toggle_bg_$title"
    )
    Surface(shape = RoundedCornerShape(20.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(title: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = option.equals(value, true) || (option == "auto" && value == "auto") || (option == "Off" && value == "Off")
                SelectChip(text = option, selected = selected, onClick = { onSelect(option) })
            }
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .85f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .62f),
        border = BorderStroke(1.dp, color.copy(alpha = .18f)),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClick() }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = color, modifier = Modifier.size(15.dp))
            Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .82f))
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusLine(status: ApplyStatus, applying: Boolean) {
    val ok = status.lastOk
    val color = when {
        applying -> MaterialTheme.colorScheme.primary
        ok -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = .10f), border = BorderStroke(1.dp, color.copy(alpha = .16f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Close, null, tint = color, modifier = Modifier.size(18.dp))
            Text(
                text = if (applying) "Menerapkan tweak…" else status.summary.ifBlank { "Siap" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * .52f))
    }
}

@Composable
private fun ModeTag(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f))) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .35.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), maxLines = 1)
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
