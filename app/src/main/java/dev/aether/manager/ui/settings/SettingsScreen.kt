package dev.aether.manager.ui.settings

import dev.aether.manager.license.LicenseManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.AppLanguage
import dev.aether.manager.i18n.LocalLanguage
import dev.aether.manager.i18n.LocalSetLanguage
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.util.BackupManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm              : MainViewModel,
    apVm            : dev.aether.manager.data.AppProfileViewModel,
    onBack          : () -> Unit,
    onResetProfiles : () -> Unit,
    onResetMonitor  : () -> Unit,
    onOpenLicense   : () -> Unit = {},
) {
    val s           = LocalStrings.current
    val ctx         = LocalContext.current

    BackHandler(onBack = onBack)
    val backupList  by vm.backupList.collectAsState()
    val working     by vm.backupWorking.collectAsState()

    var showReset         by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor  by remember { mutableStateOf(false) }
    var restoreTarget     by remember { mutableStateOf<String?>(null) }
    val scrollState       = rememberScrollState()

    // ── AppProfile state (untuk cek apakah ada profiles/monitor) ─────────
    val appsUiState by apVm.state.collectAsState()
    val hasProfiles = (appsUiState as? dev.aether.manager.data.AppsUiState.Ready)
        ?.profiles?.isNotEmpty() ?: false
    val monitorActive = (appsUiState as? dev.aether.manager.data.AppsUiState.Ready)
        ?.monitorRunning ?: false

    // ── Toast ─────────────────────────────────────────────────────────────
    val backupEvent by vm.backupEvent.collectAsState()
    LaunchedEffect(backupEvent) {
        val ev = backupEvent ?: return@LaunchedEffect
        val msg = when (ev) {
            is dev.aether.manager.data.MainViewModel.BackupEvent.Success -> when (ev.msgKey) {
                "create"         -> s.backupSuccessCreate
                "restore"        -> s.backupSuccessRestore
                "delete"         -> s.backupSuccessDelete
                "reset"          -> s.backupSuccessReset
                "resetProfiles"  -> s.backupSuccessResetProfiles
                "resetMonitor"   -> s.backupSuccessResetMonitor
                else             -> s.backupSuccessCreate
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

    // ── Collapsible section states ────────────────────────────────────────
    // Tidak pakai rememberSaveable agar selalu collapsed saat Settings dibuka ulang
    var backupExpanded     by remember(Unit) { mutableStateOf(false) }
    var appearanceExpanded by remember(Unit) { mutableStateOf(false) }
    var generalExpanded    by remember(Unit) { mutableStateOf(false) }
    val isLicensed         = remember { LicenseManager.isActive(ctx) }

    // ── Settings state from ViewModel (persisted) ─────────────────────────
    val darkModeOverride by vm.darkModeOverride.collectAsState()
    val darkMode         by vm.darkMode.collectAsState()
    val dynamicColor     by vm.dynamicColor.collectAsState()
    val autoBackup       by vm.autoBackup.collectAsState()
    val applyOnBoot      by vm.applyOnBoot.collectAsState()
    val notifications    by vm.notifications.collectAsState()

    // ── Language ──────────────────────────────────────────────────────────
    val currentLanguage = LocalLanguage.current
    val setLanguage     = LocalSetLanguage.current
    var showLangSheet   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadBackups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.settingsTitle,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = s.settingsBtnBack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            )
        }
    ) { paddingValues ->

        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SettingsLicenseCard(
                isLicensed    = isLicensed,
                onOpenLicense = onOpenLicense
            )

            // ══ Appearance ════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Palette,
                title    = s.settingsSectionAppearance,
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    val systemDark = isSystemInDarkTheme()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.DarkMode,
                        title           = s.settingsDarkMode,
                        subtitle        = if (darkModeOverride) s.settingsDarkModeDesc
                                          else s.settingsDarkModeDesc,
                        checked         = if (darkModeOverride) darkMode else systemDark,
                        onCheckedChange = { vm.setDarkMode(it) }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.ColorLens,
                        title           = s.settingsDynamicColor,
                        subtitle        = s.settingsDynamicColorDesc,
                        checked         = dynamicColor,
                        onCheckedChange = { vm.setDynamicColor(it) }
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Language,
                        title    = s.settingsLanguage,
                        subtitle = currentLanguage.nativeName,
                        onClick  = { showLangSheet = true }
                    )
                }
            }

            // ══ General ═══════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Tune,
                title    = s.settingsSectionGeneral,
                expanded = generalExpanded,
                onToggle = { generalExpanded = !generalExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.CloudUpload,
                        title           = s.settingsAutoBackup,
                        subtitle        = s.settingsAutoBackupDesc,
                        checked         = autoBackup,
                        onCheckedChange = { vm.setAutoBackup(it) }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.FlashOn,
                        title           = s.settingsApplyOnBoot,
                        subtitle        = s.settingsApplyOnBootDesc,
                        checked         = applyOnBoot,
                        onCheckedChange = { vm.setApplyOnBoot(it) }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.Notifications,
                        title           = s.settingsNotifications,
                        subtitle        = s.settingsNotificationsDesc,
                        checked         = notifications,
                        onCheckedChange = { vm.setNotifications(it) }
                    )
                }
            }

            // ══ Backup & Reset ════════════════════════════════════════════
            var processingFile by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(working) { if (!working) processingFile = null }

            SettingsSectionCard(
                icon     = Icons.Outlined.Archive,
                title    = "Backup Reset",
                expanded = backupExpanded,
                onToggle = { backupExpanded = !backupExpanded }
            ) {
                Column(
                    modifier            = Modifier.padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    SettingsActionRow(
                        icon      = Icons.Outlined.Save,
                        title     = s.settingsBtnBackup,
                        subtitle  = s.backupSubtitleBackup,
                        iconTint  = MaterialTheme.colorScheme.primary,
                        iconBg    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        isLoading = working && processingFile == null
                                     && !showReset && !showResetProfiles && !showResetMonitor,
                        enabled   = !working,
                        onClick   = { vm.createBackup() }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon     = Icons.Outlined.RestartAlt,
                        title    = s.settingsBtnResetDefault,
                        subtitle = s.backupSubtitleReset,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBg   = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        enabled  = !working,
                        onClick  = { showReset = true }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon      = Icons.Outlined.ManageAccounts,
                        title     = s.settingsBtnResetProfiles,
                        subtitle  = if (hasProfiles) s.backupSubtitleResetProfiles
                                    else s.backupNoProfiles,
                        iconTint  = if (hasProfiles) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconBg    = if (hasProfiles) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                        enabled   = !working && hasProfiles,
                        showChip  = !hasProfiles,
                        chipText  = s.backupNoProfiles,
                        onClick   = { showResetProfiles = true }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon      = Icons.Outlined.MonitorHeart,
                        title     = s.settingsBtnResetMonitor,
                        subtitle  = if (monitorActive) s.backupSubtitleResetMonitor
                                    else s.backupMonitorInactive,
                        iconTint  = if (monitorActive) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconBg    = if (monitorActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                        enabled   = !working && monitorActive,
                        showChip  = !monitorActive,
                        chipText  = s.backupMonitorInactive,
                        onClick   = { showResetMonitor = true }
                    )

                    // ── Backup list ───────────────────────────────────────
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 14.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    if (backupList.isEmpty()) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FolderOff, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                s.settingsNoBackup,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        backupList.forEachIndexed { index, entry ->
                            SettingsBackupItem(
                                entry        = entry,
                                working      = working,
                                isProcessing = processingFile == entry.filename,
                                profileLabel = s.settingsBackupProfile.format(entry.profile),
                                deleteLabel  = s.settingsBtnDelete,
                                onRestore    = { processingFile = entry.filename; restoreTarget = entry.filename },
                                onDelete     = { processingFile = entry.filename; vm.deleteBackup(entry.filename) }
                            )
                            if (index < backupList.lastIndex) {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(start = 62.dp, end = 14.dp),
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
    }

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
            onDismissRequest = { restoreTarget = null },
            icon  = { Icon(Icons.Outlined.Restore, null) },
            title = { Text(s.settingsRestoreTitle) },
            text  = { Text(s.settingsRestoreDesc) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; vm.restoreBackup(fname) }) {
                    Text(s.settingsRestoreConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text(s.settingsBtnCancel) }
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


    // ── Language picker bottom sheet ──────────────────────────────────────
    if (showLangSheet) {
        LanguagePickerSheet(
            current  = currentLanguage,
            onSelect = { lang -> setLanguage(lang); showLangSheet = false },
            onDismiss = { showLangSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    current  : AppLanguage,
    onSelect : (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Pilih Bahasa / Select Language",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(bottom = 8.dp)
            )
            AppLanguage.entries.forEach { lang ->
                val selected = lang == current
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = if (selected) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(lang) }
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            lang.langIcon,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                         else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                lang.nativeName,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                             else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                lang.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Outlined.CheckCircle, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsLicenseCard(
    isLicensed    : Boolean,
    onOpenLicense : () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenLicense),
        shape  = RoundedCornerShape(18.dp),
        color  = if (isLicensed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
                 else MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLicensed) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isLicensed) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLicensed) Icons.Outlined.CheckCircle else Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isLicensed) "Premium Active" else "Aether Manager",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isLicensed) "License active" else "Tap untuk aktivasi lisensi dan hapus iklan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() = HorizontalDivider(
    modifier  = Modifier.padding(horizontal = 14.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

@Composable
private fun SettingsSectionCard(
    icon     : ImageVector,
    title    : String,
    expanded : Boolean,
    onToggle : () -> Unit,
    content  : @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label       = "chevron"
    )
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    title,
                    modifier   = Modifier.weight(1f),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(rotation)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsRowSwitch(
    icon            : ImageVector,
    title           : String,
    subtitle        : String,
    checked         : Boolean,
    onCheckedChange : (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRowInfo(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : (() -> Unit)?,
) {
    val mod = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)
    else
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)

    Row(
        modifier              = mod,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsActionRow(
    icon      : ImageVector,
    title     : String,
    subtitle  : String,
    iconTint  : Color,
    iconBg    : Color,
    isLoading : Boolean = false,
    enabled   : Boolean = true,
    showChip  : Boolean = false,
    chipText  : String  = "",
    onClick   : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(36.dp).background(iconBg, RoundedCornerShape(10.dp)),
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (showChip) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info, null,
                            modifier = Modifier.size(11.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            chipText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!showChip) {
            Icon(
                Icons.Outlined.ChevronRight, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsBackupItem(
    entry        : BackupManager.BackupEntry,
    working      : Boolean,
    isProcessing : Boolean = false,
    profileLabel : String,
    deleteLabel  : String,
    onRestore    : () -> Unit,
    onDelete     : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(Icons.Outlined.Archive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(entry.timestamp, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(profileLabel,    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRestore, enabled = !working, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Restore, "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onDelete, enabled = !working, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Delete, deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
        }
    }
}
