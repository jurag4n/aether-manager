package dev.aether.manager.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.AppsUiState
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.AppLanguage
import dev.aether.manager.i18n.LocalLanguage
import dev.aether.manager.i18n.LocalSetLanguage
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.license.LicenseManager
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
    val s   = LocalStrings.current
    val ctx = LocalContext.current

    BackHandler(onBack = onBack)

    val backupList by vm.backupList.collectAsState()
    val working    by vm.backupWorking.collectAsState()
    val appsUiState by apVm.state.collectAsState()

    val hasProfiles = (appsUiState as? AppsUiState.Ready)?.profiles?.isNotEmpty() ?: false
    val monitorActive = (appsUiState as? AppsUiState.Ready)?.monitorRunning ?: false

    var showReset by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<String?>(null) }
    var processingFile by remember { mutableStateOf<String?>(null) }

    var appearanceExpanded by remember(Unit) { mutableStateOf(false) }
    var generalExpanded by remember(Unit) { mutableStateOf(false) }
    var backupExpanded by remember(Unit) { mutableStateOf(false) }

    val isLicensed = remember { LicenseManager.isActive(ctx) }
    val scrollState = rememberScrollState()

    val darkModeOverride by vm.darkModeOverride.collectAsState()
    val darkMode by vm.darkMode.collectAsState()
    val dynamicColor by vm.dynamicColor.collectAsState()
    val autoBackup by vm.autoBackup.collectAsState()
    val applyOnBoot by vm.applyOnBoot.collectAsState()
    val notifications by vm.notifications.collectAsState()

    val currentLanguage = LocalLanguage.current
    val setLanguage = LocalSetLanguage.current
    var showLangSheet by remember { mutableStateOf(false) }

    val backupEvent by vm.backupEvent.collectAsState()
    LaunchedEffect(backupEvent) {
        val ev = backupEvent ?: return@LaunchedEffect
        val msg = when (ev) {
            is MainViewModel.BackupEvent.Success -> when (ev.msgKey) {
                "create" -> s.backupSuccessCreate
                "restore" -> s.backupSuccessRestore
                "delete" -> s.backupSuccessDelete
                "reset" -> s.backupSuccessReset
                "resetProfiles" -> s.backupSuccessResetProfiles
                "resetMonitor" -> s.backupSuccessResetMonitor
                else -> s.backupSuccessCreate
            }
            is MainViewModel.BackupEvent.Failure -> when (ev.msgKey) {
                "create" -> s.backupFailCreate
                "restore" -> s.backupFailRestore
                else -> s.backupFailReset
            }
        }
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
        vm.clearBackupEvent()
    }

    LaunchedEffect(Unit) { vm.loadBackups() }
    LaunchedEffect(working) { if (!working) processingFile = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.settingsTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = s.settingsBtnBack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
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
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsLicenseCard(
                isLicensed = isLicensed,
                onOpenLicense = onOpenLicense
            )

            SettingsSectionCard(
                icon = Icons.Outlined.Palette,
                title = s.settingsSectionAppearance,
                subtitle = "Tema, warna, dan tampilan aplikasi",
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                val systemDark = isSystemInDarkTheme()
                SettingsRowSwitch(
                    icon = Icons.Outlined.DarkMode,
                    title = s.settingsDarkMode,
                    subtitle = s.settingsDarkModeDesc,
                    checked = if (darkModeOverride) darkMode else systemDark,
                    onCheckedChange = { vm.setDarkMode(it) }
                )
                SettingsDivider()
                SettingsRowSwitch(
                    icon = Icons.Outlined.ColorLens,
                    title = s.settingsDynamicColor,
                    subtitle = s.settingsDynamicColorDesc,
                    checked = dynamicColor,
                    onCheckedChange = { vm.setDynamicColor(it) }
                )
            }

            SettingsSectionCard(
                icon = Icons.Outlined.Tune,
                title = s.settingsSectionGeneral,
                subtitle = "Boot, notifikasi, dan bahasa",
                expanded = generalExpanded,
                onToggle = { generalExpanded = !generalExpanded }
            ) {
                SettingsRowSwitch(
                    icon = Icons.Outlined.CloudUpload,
                    title = s.settingsAutoBackup,
                    subtitle = s.settingsAutoBackupDesc,
                    checked = autoBackup,
                    onCheckedChange = { vm.setAutoBackup(it) }
                )
                SettingsDivider()
                SettingsRowSwitch(
                    icon = Icons.Outlined.FlashOn,
                    title = s.settingsApplyOnBoot,
                    subtitle = s.settingsApplyOnBootDesc,
                    checked = applyOnBoot,
                    onCheckedChange = { vm.setApplyOnBoot(it) }
                )
                SettingsDivider()
                SettingsRowSwitch(
                    icon = Icons.Outlined.Notifications,
                    title = s.settingsNotifications,
                    subtitle = s.settingsNotificationsDesc,
                    checked = notifications,
                    onCheckedChange = { vm.setNotifications(it) }
                )
                SettingsDivider()
                SettingsRowInfo(
                    icon = Icons.Outlined.Language,
                    title = s.settingsLanguage,
                    subtitle = currentLanguage.nativeName,
                    onClick = { showLangSheet = true }
                )
            }

            SettingsSectionCard(
                icon = Icons.Outlined.Archive,
                title = "Backup & Reset",
                subtitle = if (backupList.isEmpty()) s.settingsNoBackup else "${backupList.size} backup tersimpan",
                expanded = backupExpanded,
                onToggle = { backupExpanded = !backupExpanded }
            ) {
                SettingsActionRow(
                    icon = Icons.Outlined.Save,
                    title = s.settingsBtnBackup,
                    subtitle = s.backupSubtitleBackup,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    isLoading = working && processingFile == null && !showReset && !showResetProfiles && !showResetMonitor,
                    enabled = !working,
                    onClick = { vm.createBackup() }
                )
                SettingsDivider()
                SettingsActionRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = s.settingsBtnResetDefault,
                    subtitle = s.backupSubtitleReset,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f),
                    enabled = !working,
                    onClick = { showReset = true }
                )
                SettingsDivider()
                SettingsActionRow(
                    icon = Icons.Outlined.ManageAccounts,
                    title = s.settingsBtnResetProfiles,
                    subtitle = if (hasProfiles) s.backupSubtitleResetProfiles else s.backupNoProfiles,
                    iconTint = if (hasProfiles) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBg = if (hasProfiles) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surfaceVariant,
                    enabled = !working && hasProfiles,
                    showChip = !hasProfiles,
                    chipText = s.backupNoProfiles,
                    onClick = { showResetProfiles = true }
                )
                SettingsDivider()
                SettingsActionRow(
                    icon = Icons.Outlined.MonitorHeart,
                    title = s.settingsBtnResetMonitor,
                    subtitle = if (monitorActive) s.backupSubtitleResetMonitor else s.backupMonitorInactive,
                    iconTint = if (monitorActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBg = if (monitorActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surfaceVariant,
                    enabled = !working && monitorActive,
                    showChip = !monitorActive,
                    chipText = s.backupMonitorInactive,
                    onClick = { showResetMonitor = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )

                if (backupList.isEmpty()) {
                    EmptyBackupRow(text = s.settingsNoBackup)
                } else {
                    backupList.forEachIndexed { index, entry ->
                        SettingsBackupItem(
                            entry = entry,
                            working = working,
                            isProcessing = processingFile == entry.filename,
                            profileLabel = s.settingsBackupProfile.format(entry.profile),
                            deleteLabel = s.settingsBtnDelete,
                            onRestore = {
                                processingFile = entry.filename
                                restoreTarget = entry.filename
                            },
                            onDelete = {
                                processingFile = entry.filename
                                vm.deleteBackup(entry.filename)
                            }
                        )
                        if (index < backupList.lastIndex) {
                            SettingsDivider(start = 70.dp)
                        }
                    }
                }
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetTitle) },
            text = { Text(s.settingsResetDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showReset = false; vm.resetToDefaults() },
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
            icon = { Icon(Icons.Outlined.Restore, null) },
            title = { Text(s.settingsRestoreTitle) },
            text = { Text(s.settingsRestoreDesc) },
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
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetProfilesTitle) },
            text = { Text(s.settingsResetProfilesDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetProfiles = false; onResetProfiles() },
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
            icon = { Icon(Icons.Outlined.MonitorHeart, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text(s.settingsResetMonitorTitle) },
            text = { Text(s.settingsResetMonitorDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetMonitor = false; onResetMonitor() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMonitor = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    if (showLangSheet) {
        LanguagePickerSheet(
            current = currentLanguage,
            onSelect = { lang ->
                setLanguage(lang)
                showLangSheet = false
            },
            onDismiss = { showLangSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    current  : AppLanguage,
    onSelect : (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pilih Bahasa / Select Language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AppLanguage.entries.forEach { lang ->
                val selected = lang == current
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(lang) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.langIcon,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLicenseCard(
    isLicensed    : Boolean,
    onOpenLicense : () -> Unit,
) {
    val bg = if (isLicensed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
             else MaterialTheme.colorScheme.surfaceContainerLow
    val border = if (isLicensed) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onOpenLicense)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isLicensed) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLicensed) Icons.Outlined.CheckCircle else Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (isLicensed) "Premium Active" else "Aether Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isLicensed) "License aktif dan fitur premium tersedia" else "Aktivasi lisensi untuk membuka fitur premium",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    modifier = Modifier.size(20.dp)
                )
            }

            StatusPill(
                text = if (isLicensed) "Licensed" else "Tap untuk aktivasi",
                icon = if (isLicensed) Icons.Outlined.CheckCircle else Icons.Outlined.WorkspacePremium,
                tint = if (isLicensed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                container = if (isLicensed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    expanded : Boolean,
    onToggle : () -> Unit,
    content  : @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "settings_chevron"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(21.dp)
                            .rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(durationMillis = 160, delayMillis = 35)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(durationMillis = 120))
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider(start: androidx.compose.ui.unit.Dp = 16.dp) = HorizontalDivider(
    modifier = Modifier.padding(start = start, end = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
    thickness = 0.5.dp
)

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
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        IconBubble(
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRowInfo(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : (() -> Unit)?,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
        .padding(horizontal = 16.dp, vertical = 14.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        IconBubble(
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

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
            .alpha(if (enabled) 1f else 0.48f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = iconTint
                )
            } else {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (showChip) {
                StatusPill(
                    text = chipText,
                    icon = Icons.Outlined.Info,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            } else {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!showChip) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun EmptyBackupRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBubble(
            icon = Icons.Outlined.FolderOff,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(Icons.Outlined.Archive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(entry.timestamp, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(profileLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRestore, enabled = !working, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Restore, "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete, enabled = !working, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Delete, deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun IconBubble(
    icon      : ImageVector,
    tint      : Color,
    container : Color,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(container, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StatusPill(
    text      : String,
    icon      : ImageVector,
    tint      : Color,
    container : Color,
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = tint
            )
        }
    }
}
