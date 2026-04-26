package dev.aether.manager.ads

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import dev.aether.manager.i18n.LocalStrings

/**
 * Dialog yang muncul ketika adblock terdeteksi.
 *
 * @param onDisableAds   Dipanggil ketika user menekan "Nonaktifkan AdBlock" (intent ke settings).
 */
@Composable
fun AdBlockDetectedDialog(
    onDisableAds: () -> Unit
) {
    val strings = LocalStrings.current

    val infiniteTransition = rememberInfiniteTransition(label = "adblock_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = { /* Non-dismissible dengan back/tap luar */ },
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false
        ),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector        = Icons.Filled.Block,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text       = strings.adBlockTitle,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text      = strings.adBlockBody,
                    textAlign = TextAlign.Center,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Info,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Text(
                            text  = strings.adBlockInfoCard,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.ShieldMoon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text  = strings.adBlockSafeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDisableAds,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(strings.adBlockBtnDisable, fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
