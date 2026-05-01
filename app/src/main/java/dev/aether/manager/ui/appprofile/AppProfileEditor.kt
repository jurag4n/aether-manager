package dev.aether.manager.ui.appprofile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.AppExtraTweaks
import dev.aether.manager.data.AppProfile
import dev.aether.manager.data.CpuGovernors
import dev.aether.manager.data.RefreshRates

/**
 * Simple icon view for displaying an app icon. If a bitmap is supplied, it will be
 * rendered directly. Otherwise a coloured circle with the first letter of the app
 * label will be displayed as a fallback. The background tint subtly changes
 * depending on whether the profile is enabled to provide visual feedback.
 *
 * @param bitmap optional bitmap representing the app icon
 * @param label the app name used for the fallback initial
 * @param isEnabled whether the corresponding profile is enabled (affects background)
 * @param size the size of the icon container
 * @param cornerSize the rounding applied to the container shape
 */
@Composable
fun AppIconView(
    bitmap: Bitmap?,
    label: String,
    isEnabled: Boolean,
    size: Dp,
    cornerSize: Dp
) {
    val bgColor = if (isEnabled) {
        // Use a slightly tinted primary container when enabled
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isEnabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerSize))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Show first character as fallback
            val initial = label.trim().take(1).uppercase()
            Text(
                text = initial,
                color = contentColor,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Editor dialog allowing the user to modify an application profile. The caller
 * supplies the existing profile along with presentation data such as the app
 * label and icon. Changes are stored in local state and only persisted when
 * the Save button is pressed. While a save operation is in progress, the
 * confirm button is disabled and its label reflects the pending state.
 *
 * @param profile current profile state to edit
 * @param appLabel the human‑readable label of the app
 * @param appIcon optional drawable representing the app icon
 * @param saving whether a save operation is currently underway
 * @param onDismiss callback invoked when the dialog is dismissed without saving
 * @param onSave callback receiving the updated profile when the user confirms
 */
@Composable
fun AppProfileEditor(
    profile: AppProfile,
    appLabel: String,
    appIcon: Drawable?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppProfile) -> Unit
) {
    // Local editable state copied from the provided profile
    var enabled       by remember { mutableStateOf(profile.enabled) }
    var cpuGovernor   by remember { mutableStateOf(profile.cpuGovernor) }
    var refreshRate   by remember { mutableStateOf(profile.refreshRate) }
    var disableDoze   by remember { mutableStateOf(profile.extraTweaks.disableDoze) }
    var lockCpuMin    by remember { mutableStateOf(profile.extraTweaks.lockCpuMin) }
    var killBackground by remember { mutableStateOf(profile.extraTweaks.killBackground) }
    var gpuBoost      by remember { mutableStateOf(profile.extraTweaks.gpuBoost) }
    var ioLatency     by remember { mutableStateOf(profile.extraTweaks.ioLatency) }

    // Convert the supplied Drawable to a Bitmap for composable consumption. We
    // memoise this conversion so it only runs if the underlying drawable
    // instance changes.
    val iconBitmap by remember(appIcon) {
        mutableStateOf(appIcon?.let { drawableToBitmap(it) })
    }

    // Dropdown control state for governor selection
    var governorExpanded by remember { mutableStateOf(false) }
    // Dropdown control state for refresh rate selection
    var rrExpanded      by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconView(
                    bitmap     = iconBitmap,
                    label      = appLabel,
                    isEnabled  = enabled,
                    size       = 40.dp,
                    cornerSize = 12.dp
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = profile.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enabled toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        enabled = !saving
                    )
                }
                // CPU governor dropdown
                Column {
                    Text(
                        text = "CPU governor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { governorExpanded = true }
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = CpuGovernors.labels[cpuGovernor] ?: cpuGovernor,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = governorExpanded,
                        onDismissRequest = { governorExpanded = false }
                    ) {
                        CpuGovernors.all.forEach { gov ->
                            DropdownMenuItem(
                                text = { Text(CpuGovernors.labels[gov] ?: gov) },
                                onClick = {
                                    cpuGovernor = gov
                                    governorExpanded = false
                                },
                                enabled = !saving
                            )
                        }
                    }
                }
                // Refresh rate dropdown
                Column {
                    Text(
                        text = "Refresh rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rrExpanded = true }
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = RefreshRates.labels[refreshRate] ?: refreshRate,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = rrExpanded,
                        onDismissRequest = { rrExpanded = false }
                    ) {
                        RefreshRates.all.forEach { rr ->
                            DropdownMenuItem(
                                text = { Text(RefreshRates.labels[rr] ?: rr) },
                                onClick = {
                                    refreshRate = rr
                                    rrExpanded = false
                                },
                                enabled = !saving
                            )
                        }
                    }
                }
                // Extra tweak toggles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExtraToggle(
                        label = "Disable Doze",
                        checked = disableDoze,
                        enabled = !saving,
                        onCheckedChange = { disableDoze = it }
                    )
                    ExtraToggle(
                        label = "Lock CPU min",
                        checked = lockCpuMin,
                        enabled = !saving,
                        onCheckedChange = { lockCpuMin = it }
                    )
                    ExtraToggle(
                        label = "Kill background",
                        checked = killBackground,
                        enabled = !saving,
                        onCheckedChange = { killBackground = it }
                    )
                    ExtraToggle(
                        label = "GPU boost",
                        checked = gpuBoost,
                        enabled = !saving,
                        onCheckedChange = { gpuBoost = it }
                    )
                    ExtraToggle(
                        label = "IO latency",
                        checked = ioLatency,
                        enabled = !saving,
                        onCheckedChange = { ioLatency = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = profile.copy(
                        enabled = enabled,
                        cpuGovernor = cpuGovernor,
                        refreshRate = refreshRate,
                        extraTweaks = AppExtraTweaks(
                            disableDoze    = disableDoze,
                            lockCpuMin     = lockCpuMin,
                            killBackground = killBackground,
                            gpuBoost       = gpuBoost,
                            ioLatency      = ioLatency
                        )
                    )
                    onSave(updated)
                },
                enabled = !saving
            ) {
                Text(if (saving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExtraToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled
        )
    }
}

// Helper to convert a Drawable to a Bitmap. This mirrors the implementation in
// AppProfileScreen so that AppProfileEditor remains self contained.
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val w      = drawable.intrinsicWidth.coerceIn(1, 512)
    val h      = drawable.intrinsicHeight.coerceIn(1, 512)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}