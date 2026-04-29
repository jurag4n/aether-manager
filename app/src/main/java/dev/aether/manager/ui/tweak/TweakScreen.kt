package dev.aether.manager.ui.tweak

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.ApplyStatus
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.TweaksState
import dev.aether.manager.i18n.LocalStrings

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TweakScreen(vm: MainViewModel) {
    val s      = LocalStrings.current
    val tweaks by vm.tweaks.collectAsState()
    val status by vm.applyStatus.collectAsState()
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Active Profile Banner ────────────────────────────────
            ActiveProfileBanner(
                current  = tweaks.thermalProfile,
                onSelect = { vm.setProfile(it) }
            )

            // ── 2x2 Primary Feature Grid ─────────────────────────────
            Text(
                text  = "Performa",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Row 1: CPU Governor + GPU Governor
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.Memory,
                    label       = "CPU Governor",
                    badge       = if (tweaks.cpuBoost) "Aktif" else "Off",
                    badgeActive = tweaks.cpuBoost,
                    onClick     = { vm.setTweak("cpuBoost", !tweaks.cpuBoost) }
                )
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.GridView,
                    label       = "GPU Governor",
                    badge       = if (tweaks.gpuThrottleOff) "Aktif" else "Off",
                    badgeActive = tweaks.gpuThrottleOff,
                    onClick     = { vm.setTweak("gpuThrottleOff", !tweaks.gpuThrottleOff) }
                )
            }

            // Row 2: Thermal + Battery Profile
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.Thermostat,
                    label       = "Thermal",
                    badge       = tweaks.thermalProfile.replaceFirstChar { it.uppercase() },
                    badgeActive = tweaks.thermalProfile != "default",
                    onClick     = {
                        // Cycle: default → performance → extreme → default
                        val next = when (tweaks.thermalProfile) {
                            "default"     -> "performance"
                            "performance" -> "extreme"
                            else          -> "default"
                        }
                        vm.setProfile(next)
                    }
                )
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.BatteryFull,
                    label       = "Baterai",
                    badge       = if (tweaks.doze) "Hemat" else "Normal",
                    badgeActive = tweaks.doze,
                    onClick     = { vm.setTweak("doze", !tweaks.doze) }
                )
            }

            // ── Full-width: Balance / Performance mode ────────────────
            ProfileModeCard(
                current  = tweaks.thermalProfile,
                onSelect = { vm.setProfile(it) }
            )

            // ── Memory ───────────────────────────────────────────────
            Text(
                text  = "Memori & Sistem",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.Dns,
                    label       = "Memori",
                    badge       = if (tweaks.zram) "ZRAM" else "Off",
                    badgeActive = tweaks.zram || tweaks.lmkAggressive,
                    onClick     = { vm.setTweak("zram", !tweaks.zram) }
                )
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.Storage,
                    label       = "I/O Scheduler",
                    badge       = if (tweaks.ioScheduler.isBlank()) "Auto" else tweaks.ioScheduler.uppercase(),
                    badgeActive = tweaks.ioScheduler.isNotBlank(),
                    onClick     = { /* open detail */ }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.Wifi,
                    label       = "Jaringan",
                    badge       = if (tweaks.tcpBbr) "BBR" else "Off",
                    badgeActive = tweaks.tcpBbr,
                    onClick     = { vm.setTweak("tcpBbr", !tweaks.tcpBbr) }
                )
                FeatureCard(
                    modifier    = Modifier.weight(1f),
                    icon        = Icons.Outlined.Speed,
                    label       = "Sched Boost",
                    badge       = if (tweaks.schedboost) "Aktif" else "Off",
                    badgeActive = tweaks.schedboost,
                    onClick     = { vm.setTweak("schedboost", !tweaks.schedboost) }
                )
            }

            // ── MTK-only Boost (conditional) ─────────────────────────
            if (tweaks.mtkBoost != null) {
                WideFeatureCard(
                    icon        = Icons.Outlined.Bolt,
                    label       = "MTK Boost",
                    badge       = if (tweaks.mtkBoost == true) "Aktif" else "Off",
                    badgeActive = tweaks.mtkBoost == true,
                    onClick     = { vm.setTweak("mtkBoost", !(tweaks.mtkBoost ?: false)) }
                )
            }
        }

        // ── Floating Apply Button ────────────────────────────────────
        ApplyBar(
            status   = status,
            onApply  = { vm.applyAll() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Active Profile Banner (pill, like "Balance — Profil Aktif" in screenshots)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveProfileBanner(
    current: String,
    onSelect: (String) -> Unit
) {
    val label = when (current) {
        "performance" -> "Performance"
        "extreme"     -> "Extreme"
        else          -> "Balance"
    }
    val icon = when (current) {
        "performance" -> Icons.Outlined.FlashOn
        "extreme"     -> Icons.Filled.Bolt
        else          -> Icons.Outlined.Balance
    }

    Surface(
        shape  = RoundedCornerShape(50.dp),
        color  = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier             = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text  = "Profil Aktif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Mode Card (full-width, 3 chips: Balance / Performance / Extreme)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileModeCard(
    current: String,
    onSelect: (String) -> Unit
) {
    val profiles = listOf(
        Triple("default",     "Balance",     Icons.Outlined.Balance),
        Triple("performance", "Performance", Icons.Outlined.FlashOn),
        Triple("extreme",     "Extreme",     Icons.Filled.Bolt)
    )

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            profiles.forEach { (key, label, icon) ->
                val selected = current == key
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    tween(200), label = "profile_bg"
                )
                val fg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(200), label = "profile_fg"
                )
                Surface(
                    onClick  = { onSelect(key) },
                    shape    = RoundedCornerShape(12.dp),
                    color    = bg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier            = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                        Text(
                            label,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = fg
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feature Card (square-ish, icon + label + badge)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    badge: String,
    badgeActive: Boolean,
    onClick: () -> Unit
) {
    val iconBg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200), label = "icon_bg"
    )
    val iconTint by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "icon_tint"
    )
    val badgeBg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200), label = "badge_bg"
    )
    val badgeFg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "badge_fg"
    )

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: icon + badge
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = badgeBg
                ) {
                    Text(
                        text     = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color    = badgeFg
                    )
                }
            }

            // Label
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wide Feature Card (full-width, single row)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WideFeatureCard(
    icon: ImageVector,
    label: String,
    badge: String,
    badgeActive: Boolean,
    onClick: () -> Unit
) {
    val iconBg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200), label = "wide_icon_bg"
    )
    val iconTint by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "wide_icon_tint"
    )
    val badgeBg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(200), label = "wide_badge_bg"
    )
    val badgeFg by animateColorAsState(
        if (badgeActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "wide_badge_fg"
    )

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Text(
                text       = label,
                modifier   = Modifier.weight(1f),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = badgeBg
            ) {
                Text(
                    text     = badge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color    = badgeFg
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Apply Bottom Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ApplyBar(
    status: ApplyStatus,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier       = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape          = RoundedCornerShape(20.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(
                    targetState    = status,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label          = "apply_status"
                ) { s ->
                    if (s.running) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Menerapkan…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (s.summary.isNotBlank()) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (s.lastOk) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                null,
                                tint     = if (s.lastOk) Color(0xFF2D7D46)
                                           else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                s.summary,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            "Tweaks siap diterapkan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick        = onApply,
                enabled        = !status.running,
                shape          = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                if (status.running) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Apply",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}