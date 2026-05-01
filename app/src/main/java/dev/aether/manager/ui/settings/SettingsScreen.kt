package dev.aether.manager.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AppSettingsAlt
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import dev.aether.manager.license.LicenseManager
import dev.aether.manager.util.BackupManager
import dev.aether.manager.util.RootManager

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
    val s   = LocalStrings.current
    val ctx = LocalContext.current

    BackHandler(onBack = onBack)

    val backupList by vm.backupList.collectAsState()
    val working    by vm.backupWorking.collectAsState()

    var showReset         by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor  by remember { mutableStateOf(false) }
    var showClearCache    by remember { mutableStateOf(false) }
    var restoreTarget     by remember { mutableStateOf<String?>(null) }
    var processingFile    by remember { mutableStateOf<String?>(null) }

    val appsUiState by apVm.state.collectAsState()
    val hasProfiles = (appsUiState as? dev.aether.manager.data.AppsUiState.Ready)
        ?.profiles?.isNotEmpty() ?: false
    val monitorActive = (appsUiState as? dev.aether.manager.data.AppsUiState.Ready)
        ?.monitorRunning ?: false

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

    LaunchedEffect(working) {
        if (!working) processingFile = null
    }

    val isLicensed      = remember { LicenseManager.isActive(ctx) }
    val darkModeOverride by vm.darkModeOverride.collectAsState()
    val darkMode         by vm.darkMode.collectAsState()
    val dynamicColor     by vm.dynamicColor.collectAsState()
    val autoBackup       by vm.autoBackup.collectAsState()
    val applyOnBoot      by vm.applyOnBoot.collectAsState()
    val notifications    by vm.notifications.collectAsState()
    val debugLog         by vm.debugLog.collectAsState()

    val currentLanguage = LocalLanguage.current
    val setLanguage     = LocalSetLanguage.current
    var showLangSheet   by remember { mutableStateOf(false) }

    val rootMethod = remember { RootManager.detectRootType() }
    val versionName = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "v?"
        } catch (_: Exception) {
            "v?"
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) { vm.loadBackups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = s.settingsTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = s.settingsBtnBack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsHeroCard(
                isLicensed    = isLicensed,
                rootMethod    = rootMethod,
                versionName   = versionName,
                onOpenLicense = onOpenLicense
            )

            SectionHeader(
                icon  = Icons.Outlined.Tune,
                title = "Quick Actions"
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        modifier  = Modifier.weight(1f),
                        icon      = Icons.Outlined.Save,
                        title     = s.settingsBtnBackup,
                        subtitle  = s.backupSubtitleBackup,
                        tint      = MaterialTheme.colorScheme.primary,
                        bg        = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        loading   = working && processingFile == null && !showReset && !showResetProfiles && !showResetMonitor,
                        enabled   = !working,
                        onClick   = { vm.createBackup() }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.RestartAlt,
                        title    = s.settingsBtnResetDefault,
                        subtitle = s.backupSubtitleReset,
                        tint     = MaterialTheme.colorScheme.error,
                        bg       = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        enabled  = !working,
                        onClick  = { showReset = true }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.ManageAccounts,
                        title    = s.settingsBtnResetProfiles,
                        subtitle = if (hasProfiles) s.backupSubtitleResetProfiles else s.backupNoProfiles,
                        tint     = if (hasProfiles) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        bg       = if (hasProfiles) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
                        enabled  = !working && hasProfiles,
                        onClick  = { showResetProfiles = true }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.MonitorHeart,
                        title    = s.settingsBtnResetMonitor,
                        subtitle = if (monitorActive) s.backupSubtitleResetMonitor else s.backupMonitorInactive,
                        tint     = if (monitorActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        bg       = if (monitorActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
                        enabled  = !working && monitorActive,
                        onClick  = { showResetMonitor = true }
                    )
                }
            }

            SettingsPanel(
                icon  = Icons.Outlined.Palette,
                title = s.settingsSectionAppearance
            ) {
                val systemDark = isSystemInDarkTheme()
                SettingsSwitchRow(
                    icon            = Icons.Outlined.DarkMode,
                    title           = s.settingsDarkMode,
                    subtitle        = s.settingsDarkModeDesc,
                    checked         = if (darkModeOverride) darkMode else systemDark,
                    onCheckedChange = { vm.setDarkMode(it) }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon            = Icons.Outlined.ColorLens,
                    title           = s.settingsDynamicColor,
                    subtitle        = s.settingsDynamicColorDesc,
                    checked         = dynamicColor,
                    onCheckedChange = { vm.setDynamicColor(it) }
                )
                SettingsDivider()
                SettingsInfoRow(
                    icon     = Icons.Outlined.Language,
                    title    = s.settingsLanguage,
                    subtitle = currentLanguage.nativeName,
                    onClick  = { showLangSheet = true }
                )
            }

            SettingsPanel(
                icon  = Icons.Outlined.Tune,
                title = s.settingsSectionGeneral
            ) {
                SettingsSwitchRow(
                    icon            = Icons.Outlined.CloudUpload,
                    title           = s.settingsAutoBackup,
                    subtitle        = s.settingsAutoBackupDesc,
                    checked         = autoBackup,
                    onCheckedChange = { vm.setAutoBackup(it) }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon            = Icons.Outlined.FlashOn,
                    title           = s.settingsApplyOnBoot,
                    subtitle        = s.settingsApplyOnBootDesc,
                    checked         = applyOnBoot,
                    onCheckedChange = { vm.setApplyOnBoot(it) }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon            = Icons.Outlined.Notifications,
                    title           = s.settingsNotifications,
                    subtitle        = s.settingsNotificationsDesc,
                    checked         = notifications,
                    onCheckedChange = { vm.setNotifications(it) }
                )
            }

            SettingsPanel(
                icon  = Icons.Outlined.AdminPanelSettings,
                title = s.settingsSectionAdvanced
            ) {
                SettingsInfoRow(
                    icon     = Icons.Outlined.AdminPanelSettings,
                    title    = s.settingsRootMethod,
                    subtitle = rootMethod,
                    onClick  = null
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon            = Icons.Outlined.BugReport,
                    title           = s.settingsDebugLog,
                    subtitle        = s.settingsDebugLogDesc,
                    checked         = debugLog,
                    onCheckedChange = { vm.setDebugLog(it) }
                )
                SettingsDivider()
                SettingsInfoRow(
                    icon     = Icons.Outlined.CleaningServices,
                    title    = s.settingsClearCache,
                    subtitle = s.settingsClearCacheDesc,
                    onClick  = { showClearCache = true }
                )
            }

            SettingsPanel(
                icon  = Icons.Outlined.Archive,
                title = s.settingsSectionBackup
            ) {
                if (backupList.isEmpty()) {
                    EmptyBackupState(text = s.settingsNoBackup)
                } else {
                    backupList.forEachIndexed { index, entry ->
                        SettingsBackupItem(
                            entry        = entry,
                            working      = working,
                            isProcessing = processingFile == entry.filename,
                            profileLabel = s.settingsBackupProfile.format(entry.profile),
                            deleteLabel  = s.settingsBtnDelete,
                            onRestore    = {
                                processingFile = entry.filename
                                restoreTarget = entry.filename
                            },
                            onDelete     = {
                                processingFile = entry.filename
                                vm.deleteBackup(entry.filename)
                            }
                        )
                        if (index < backupList.lastIndex) SettingsDivider(start = 62.dp)
                    }
                }
            }

            SettingsPanel(
                icon  = Icons.Outlined.Info,
                title = s.settingsSectionAbout
            ) {
                SettingsInfoRow(
                    icon     = Icons.Outlined.Tag,
                    title    = s.settingsVersion,
                    subtitle = versionName,
                    onClick  = null
                )
                SettingsDivider()
                SettingsInfoRow(
                    icon     = Icons.Outlined.Code,
                    title    = s.settingsSourceCode,
                    subtitle = s.settingsSourceCodeDesc,
                    onClick  = {
                        ctx.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/aetherdev01/aether-manager")
                            )
                        )
                    }
                )
                SettingsDivider()
                SettingsInfoRow(
                    icon     = Icons.Outlined.AppSettingsAlt,
                    title    = "App Info",
                    subtitle = "System app settings",
                    onClick  = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", ctx.packageName, null)
                            }
                        )
                    }
                )
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetTitle) },
            text  = { Text(s.settingsResetDesc) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReset = false
                        vm.resetToDefaults()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
                TextButton(
                    onClick = {
                        restoreTarget = null
                        vm.restoreBackup(fname)
                    }
                ) { Text(s.settingsRestoreConfirm) }
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
                    onClick = {
                        showResetProfiles = false
                        onResetProfiles()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
                    onClick = {
                        showResetMonitor = false
                        onResetMonitor()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMonitor = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    if (showClearCache) {
        AlertDialog(
            onDismissRequest = { showClearCache = false },
            icon  = { Icon(Icons.Outlined.CleaningServices, null) },
            title = { Text(s.settingsClearCache) },
            text  = { Text(s.settingsClearCacheDesc) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCache = false
                        vm.clearAppCache()
                    }
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCache = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    if (showLangSheet) {
        LanguagePickerSheet(
            current   = currentLanguage,
            onSelect  = { lang ->
                setLanguage(lang)
                showLangSheet = false
            },
            onDismiss = { showLangSheet = false }
        )
    }
}

@Composable
private fun SettingsHeroCard(
    isLicensed    : Boolean,
    rootMethod    : String,
    versionName   : String,
    onOpenLicense : () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLicensed) Icons.Outlined.CheckCircle else Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = if (isLicensed) "Premium Active" else "Aether Manager",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text  = if (isLicensed) "License active — ads disabled" else "Clean settings, faster control",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isLicensed) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    modifier = Modifier.clickable(onClick = onOpenLicense)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isLicensed) Icons.Outlined.CheckCircle else Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text       = if (isLicensed) "Active" else "Upgrade",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(label = "Root", value = rootMethod, modifier = Modifier.weight(1f))
                InfoPill(label = "Version", value = versionName, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoPill(
    label    : String,
    value    : String,
    modifier : Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon  : ImageVector,
    title : String,
) {
    Row(
        modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsPanel(
    icon    : ImageVector,
    title   : String,
    content : @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape  = RoundedCornerShape(22.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f),
                thickness = 0.5.dp
            )
            content()
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier : Modifier = Modifier,
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    tint     : Color,
    bg       : Color,
    loading  : Boolean = false,
    enabled  : Boolean = true,
    onClick  : () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(120.dp)
            .alpha(if (enabled) 1f else 0.52f)
            .clickable(enabled = enabled, onClick = onClick),
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(bg, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = tint
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1
                )
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(icon = icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : (() -> Unit)?,
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(icon = icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun IconBubble(
    icon : ImageVector,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun SettingsDivider(
    start : androidx.compose.ui.unit.Dp = 62.dp,
    end   : androidx.compose.ui.unit.Dp = 14.dp,
) {
    HorizontalDivider(
        modifier  = Modifier.padding(start = start, end = end),
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f),
        thickness = 0.5.dp
    )
}

@Composable
private fun EmptyBackupState(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(icon = Icons.Outlined.FolderOff)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = "Backup History",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(13.dp)),
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
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text       = entry.timestamp,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = profileLabel,
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
                imageVector = Icons.Outlined.Restore,
                contentDescription = "Restore",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        IconButton(
            onClick  = onDelete,
            enabled  = !working,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = deleteLabel,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    current   : AppLanguage,
    onSelect  : (AppLanguage) -> Unit,
    onDismiss : () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = "Pilih Bahasa / Select Language",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(bottom = 8.dp)
            )
            AppLanguage.entries.forEach { lang ->
                val selected = lang == current
                Surface(
                    shape    = RoundedCornerShape(16.dp),
                    color    = if (selected) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceContainerLow,
                    border   = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text       = lang.langIcon,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                         else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text       = lang.nativeName,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                             else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text  = lang.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
