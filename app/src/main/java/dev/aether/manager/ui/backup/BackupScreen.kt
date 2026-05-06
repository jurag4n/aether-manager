package dev.aether.manager.ui.backup

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.util.BackupManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun BackupScreen(
    vm              : MainViewModel,
    apVm            : dev.aether.manager.data.AppProfileViewModel,
    onResetProfiles : () -> Unit,
    onResetMonitor  : () -> Unit,
) {
    val s                 = LocalStrings.current
    val backupList        by vm.backupList.collectAsState()
    val working           by vm.backupWorking.collectAsState()
    var showReset         by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor  by remember { mutableStateOf(false) }
    var restoreTarget     by remember { mutableStateOf<String?>(null) }
    var processingFile    by remember { mutableStateOf<String?>(null) }

    val appsUiState  by apVm.state.collectAsState()
    val hasProfiles  = (appsUiState as? dev.aether.manager.data.AppsUiState.Ready)
        ?.profiles?.isNotEmpty() ?: false
    val monitorActive = (appsUiState as? dev.aether.manager.data.AppsUiState.Ready)
        ?.monitorRunning ?: false

    val ctx             = LocalContext.current
    val backupEvent by vm.backupEvent.collectAsState()
    LaunchedEffect(backupEvent) {
        val ev = backupEvent ?: return@LaunchedEffect
        val msg = when (ev) {
            is dev.aether.manager.data.MainViewModel.BackupEvent.Success -> when (ev.msgKey) {
                "create"        -> s.backupSuccessCreate
                "restore"       -> s.backupSuccessRestore
                "delete"        -> s.backupSuccessDelete
                "reset"         -> s.backupSuccessReset
                "resetProfiles" -> s.backupSuccessResetProfiles
                "resetMonitor"  -> s.backupSuccessResetMonitor
                else            -> s.backupSuccessCreate
            }
            is dev.aether.manager.data.MainViewModel.BackupEvent.Failure -> when (ev.msgKey) {
                "create"  -> s.backupFailCreate
                "restore" -> s.backupFailRestore
                else      -> s.backupFailReset
            }
        }
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
        vm.clearBackupEvent()
    }

    LaunchedEffect(working) { if (!working) processingFile = null }
    LaunchedEffect(Unit) { vm.loadBackups() }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            s.settingsSectionBackup,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // ── Action cards ──────────────────────────────────────────────────
        Surface(
            shape  = RoundedCornerShape(16.dp),
            color  = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                BackupActionRow(
                    icon      = Icons.Outlined.Save,
                    title     = s.settingsBtnBackupNow,
                    subtitle  = s.backupSubtitleBackup,
                    iconTint  = MaterialTheme.colorScheme.primary,
                    isLoading = working && processingFile == null && !showReset
                                 && !showResetProfiles && !showResetMonitor,
                    enabled   = !working,
                    onClick   = { vm.createBackup() }
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 56.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                BackupActionRow(
                    icon     = Icons.Outlined.RestartAlt,
                    title    = s.settingsBtnResetAll,
                    subtitle = s.backupSubtitleReset,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBg   = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    enabled  = !working,
                    onClick  = { showReset = true }
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 56.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                BackupActionRow(
                    icon      = Icons.Outlined.ManageAccounts,
                    title     = s.settingsBtnResetProfiles,
                    subtitle  = if (hasProfiles) s.backupSubtitleResetProfiles else s.backupNoProfiles,
                    iconTint  = if (hasProfiles) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBg    = if (hasProfiles) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                    showChip  = !hasProfiles,
                    chipText  = s.backupNoProfiles,
                    enabled   = !working && hasProfiles,
                    onClick   = { showResetProfiles = true }
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 56.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                BackupActionRow(
                    icon      = Icons.Outlined.MonitorHeart,
                    title     = s.settingsBtnResetMonitor,
                    subtitle  = if (monitorActive) s.backupSubtitleResetMonitor else s.backupMonitorInactive,
                    iconTint  = if (monitorActive) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBg    = if (monitorActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                    showChip  = !monitorActive,
                    chipText  = s.backupMonitorInactive,
                    enabled   = !working && monitorActive,
                    onClick   = { showResetMonitor = true }
                )
            }
        }

        // ── Backup list label ─────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        Text(
            s.settingsBackupSaved,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Backup list ───────────────────────────────────────────────────
        if (backupList.isEmpty()) {
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.FolderOff, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        s.settingsNoBackup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerLow,
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    backupList.forEachIndexed { index, entry ->
                        BackupItem(
                            entry          = entry,
                            working        = working,
                            isProcessing   = processingFile == entry.filename,
                            onRestore      = {
                                processingFile = entry.filename
                                restoreTarget  = entry.filename
                            },
                            onDelete       = {
                                processingFile = entry.filename
                                vm.deleteBackup(entry.filename)
                            }
                        )
                        if (index < backupList.lastIndex) {
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 62.dp, end = 16.dp),
                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }

    } // Box

    // ── Dialogs ───────────────────────────────────────────────────────────

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetTitle) },
            text  = { Text(s.settingsResetDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showReset = false; vm.resetToDefaults() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    restoreTarget?.let { fname ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null; processingFile = null },
            icon  = { Icon(Icons.Outlined.Restore, null) },
            title = { Text(s.settingsRestoreTitle) },
            text  = { Text(s.settingsRestoreDesc) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; vm.restoreBackup(fname) }) {
                    Text(s.settingsRestoreConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null; processingFile = null }) {
                    Text(s.settingsBtnCancel)
                }
            }
        )
    }

    if (showResetProfiles) {
        AlertDialog(
            onDismissRequest = { showResetProfiles = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetProfilesTitle) },
            text  = { Text(s.settingsResetProfilesDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetProfiles = false; onResetProfiles() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetProfiles = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    if (showResetMonitor) {
        AlertDialog(
            onDismissRequest = { showResetMonitor = false },
            icon  = { Icon(Icons.Outlined.MonitorHeart, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text(s.settingsResetMonitorTitle) },
            text  = { Text(s.settingsResetMonitorDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetMonitor = false; onResetMonitor() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMonitor = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }
}

// ── Action row ────────────────────────────────────────────────────────────────

@Composable
private fun BackupActionRow(
    icon      : ImageVector,
    title     : String,
    subtitle  : String,
    iconTint  : Color,
    iconBg    : Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    isLoading : Boolean = false,
    enabled   : Boolean = true,
    showChip  : Boolean = false,
    chipText  : String  = "",
    onClick   : () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue   = if (enabled) 1f else 0.45f,
        animationSpec = tween(200),
        label         = "row_alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color       = iconTint
                )
            } else {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showChip && chipText.isNotEmpty()) {
            SuggestionChip(
                onClick = {},
                label   = { Text(chipText, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(24.dp)
            )
        }
        Icon(
            Icons.Outlined.ChevronRight, null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Backup item ───────────────────────────────────────────────────────────────

@Composable
private fun BackupItem(
    entry        : BackupManager.BackupEntry,
    working      : Boolean,
    isProcessing : Boolean,
    onRestore    : () -> Unit,
    onDelete     : () -> Unit,
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Outlined.Archive, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                entry.timestamp,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                s.settingsBackupProfile.format(entry.profile),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick  = onRestore,
            enabled  = !working,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Outlined.Restore, s.settingsRestoreConfirm,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        IconButton(
            onClick  = onDelete,
            enabled  = !working,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Outlined.Delete, s.settingsBtnDelete,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
