package dev.aether.manager.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * All UI strings in one data class.
 * Add new strings here, then provide values in StringsId and StringsEn.
 */
data class AppStrings(
    // ── Navigation ───────────────────────────────────────────
    val navHome: String,
    val navTweak: String,
    val navApps: String,

    // ── Setup ────────────────────────────────────────────────
    val setupWelcomeTitle: String,
    val setupWelcomeDesc: String,
    val setupRootTitle: String,
    val setupRootDesc: String,
    val setupRootCta: String,
    val setupRootRequired: String,
    val setupRootChecking: String,
    val setupRootGranted: String,
    val setupRootDenied: String,
    val setupRootDeniedSub: String,
    val setupNotifTitle: String,
    val setupNotifDesc: String,
    val setupNotifCta: String,
    val setupNotifGranted: String,
    val setupNotifDenied: String,
    val setupWriteTitle: String,
    val setupWriteDesc: String,
    val setupWriteCta: String,
    val setupWriteGranted: String,
    val setupWriteDenied: String,
    val setupWriteDeniedSub: String,
    val setupStorageTitle: String,
    val setupStorageDesc: String,
    val setupStorageCta: String,
    val setupStorageGranted: String,
    val setupStorageDenied: String,
    val setupDoneTitle: String,
    val setupDoneDesc: String,
    val setupIncompleteTitle: String,
    val setupIncompleteDesc: String,
    val setupAllPermsGranted: String,
    val setupBtnStart: String,
    val setupBtnNext: String,
    val setupBtnBack: String,
    val setupBtnRetry: String,
    val setupBtnSkip: String,

    // ── Home ─────────────────────────────────────────────────
    val homeSystemStatus: String,
    val homeMonitor: String,
    val homeRetry: String,
    val homeLabelOs: String,
    val homeLabelKernel: String,
    val homeLabelSoc: String,
    val homeLabelGovernor: String,
    val homeLabelUptime: String,
    val homeTempCpu: String,
    val homeTempBat: String,
    val homeSelinux: String,
    val homeBatCurrent: String,      // label "Arus"
    val homeBatVoltage: String,      // label "Tegangan"
    val homeBatCharging: String,     // label "Mengisi"
    val homeBatDischarging: String,  // label "Discharge"
    val homeBatFull: String,         // label "Penuh"
    val homeBatNotCharging: String,  // label "Tidak Mengisi"
    val homeBatStatusLabel: String,  // label header row

    // ── Tweak ────────────────────────────────────────────────
    val tweakPerformanceProfile: String,
    val tweakSectionCpu: String,
    val tweakSectionMemory: String,
    val tweakSectionIo: String,
    val tweakSectionNetwork: String,
    val tweakSectionBattery: String,
    val tweakSchedBoost: String,
    val tweakSchedBoostDesc: String,
    val tweakCpuBoost: String,
    val tweakCpuBoostDesc: String,
    val tweakGpuThrottle: String,
    val tweakGpuThrottleDesc: String,
    val tweakCpusetOpt: String,
    val tweakCpusetOptDesc: String,
    val tweakMtkBoost: String,
    val tweakMtkBoostDesc: String,
    val tweakLmk: String,
    val tweakLmkDesc: String,
    val tweakZram: String,
    val tweakZramDesc: String,
    val tweakVmDirty: String,
    val tweakVmDirtyDesc: String,
    val tweakIoLatency: String,
    val tweakIoLatencyDesc: String,
    val tweakTcpBbr: String,
    val tweakTcpBbrDesc: String,
    val tweakDoh: String,
    val tweakDohDesc: String,
    val tweakNetBuffer: String,
    val tweakNetBufferDesc: String,
    val tweakDoze: String,
    val tweakDozeDesc: String,
    val tweakClearCache: String,
    val tweakClearCacheDesc: String,
    val tweakFastAnim: String,
    val tweakFastAnimDesc: String,
    val tweakEntropy: String,
    val tweakEntropyDesc: String,
    val tweakZramSize: String,
    val tweakZramAlgo: String,
    val tweakIoScheduler: String,

    // ── CPU Freq Limiter ──────────────────────────────────────
    val tweakSectionCpuFreq: String,
    val tweakCpuFreqEnable: String,
    val tweakCpuFreqEnableDesc: String,
    val tweakCpuClusterPrime: String,
    val tweakCpuClusterGold: String,
    val tweakCpuClusterSilver: String,
    val tweakCpuFreqMin: String,
    val tweakCpuFreqMax: String,

    // ── Thermal Control ───────────────────────────────────────
    val tweakThermalProfile: String,
    val tweakThermalDefault: String,
    val tweakThermalPerformance: String,
    val tweakThermalExtreme: String,
    val tweakThermalDesc: String,

    // ── GPU Freq Lock ─────────────────────────────────────────
    val tweakGpuFreqLock: String,
    val tweakGpuFreqLockDesc: String,
    val tweakGpuFreqMax: String,

    // ── Touch Boost ───────────────────────────────────────────
    val tweakTouchBoost: String,
    val tweakTouchBoostDesc: String,
    val tweakTouchSampleRate: String,

    // ── KSM ──────────────────────────────────────────────────
    val tweakKsm: String,
    val tweakKsmDesc: String,
    val tweakKsmAggressive: String,
    val tweakKsmAggressiveDesc: String,

    // ── Log / Reboot sheet ───────────────────────────────────
    val logRebootOptions: String,
    val logRebootSystem: String,
    val logRebootSystemDesc: String,
    val logRebootRecovery: String,
    val logRebootRecoveryDesc: String,
    val logReloadUi: String,
    val logReloadUiDesc: String,
    val logBtnCancel: String,

    // ── Settings / Backup screen ─────────────────────────────
    val settingsTitle: String,
    val settingsSectionBackup: String,
    val settingsBtnBackup: String,
    val settingsBtnBackupNow: String,
    val settingsBtnResetDefault: String,
    val settingsBtnResetAll: String,
    val settingsNoBackup: String,
    val settingsBackupSaved: String,
    val settingsResetTitle: String,
    val settingsResetDesc: String,
    val settingsResetConfirm: String,
    val settingsBtnCancel: String,
    val settingsBtnBack: String,
    val settingsRestoreTitle: String,
    val settingsRestoreDesc: String,
    val settingsRestoreConfirm: String,
    val settingsBackupProfile: String,   // "Profile: %s"
    val settingsBtnDelete: String,

    // ── AdBlock dialog ───────────────────────────────────────
    val adBlockTitle: String,
    val adBlockBody: String,
    val adBlockInfoCard: String,
    val adBlockSafeLabel: String,
    val adBlockBtnDisable: String,
    val adBlockBtnLater: String,

    // ── Update dialog ────────────────────────────────────────
    val updateAvailable: String,
    val updateTabDesc: String,
    val updateTabChangelog: String,
    val updateBtnDownload: String,
    val updateBtnLater: String,
    val updateBtnRetry: String,
    val updateBtnBrowser: String,
    val updateInstalling: String,
    val updateFailed: String,          // "Failed: %s"
    val updateDownloadDone: String,
    val updateDownloading: String,
    val updateAboutTitle: String,
    val updateAboutDesc: String,
    val updateChangelogLoading: String,
    val updateChangelogEmpty: String,

    // ── App Profile screen ───────────────────────────────────
    val appProfileTitle: String,
    val appProfileMonitorOn: String,
    val appProfileMonitorOff: String,
    val appProfileAppsCount: String,    // "%d Aplikasi"
    val appProfileActiveCount: String,  // "%d Profile Aktif"
    val appProfileSearchHint: String,
    val appProfileNoResults: String,
    val appProfileEmpty: String,
    val appProfileDeleteTitle: String,
    val appProfileDeleteDesc: String,   // "Profile \"%s\" akan dihapus permanen."
    val appProfileDeleteConfirm: String,
    val appProfileBtnCancel: String,
    val appProfileLoading: String,
    val appProfileRetry: String,
    val appProfileEditorActive: String,
    val appProfileEditorInactive: String,
    val appProfileCpuGovernor: String,
    val appProfileRefreshRate: String,
    val appProfileExtraTweaks: String,
    val appProfileSaveBtn: String,
    val appProfileDisableDoze: String,
    val appProfileDisableDozeDesc: String,
    val appProfileLockCpuMin: String,
    val appProfileLockCpuMinDesc: String,
    val appProfileKillBg: String,
    val appProfileKillBgDesc: String,
    val appProfileGpuBoost: String,
    val appProfileGpuBoostDesc: String,
    val appProfileIoLatency: String,
    val appProfileIoLatencyDesc: String,

    // ── CPU Governor descriptions ─────────────────────────────
    val govDescDefault: String,
    val govDescPerformance: String,
    val govDescPowersave: String,
    val govDescOndemand: String,
    val govDescConservative: String,
    val govDescSchedutil: String,
    val govDescInteractive: String,

    // ── Backup screen — reset app profile & monitor ──────────
    val settingsBtnResetProfiles: String,
    val settingsResetProfilesTitle: String,
    val settingsResetProfilesDesc: String,
    val settingsBtnResetMonitor: String,
    val settingsResetMonitorTitle: String,
    val settingsResetMonitorDesc: String,

    // ── Settings — Appearance ─────────────────────────────────
    val settingsSectionAppearance: String,
    val settingsLanguage: String,
    val settingsDarkMode: String,
    val settingsDarkModeDesc: String,
    val settingsDynamicColor: String,
    val settingsDynamicColorDesc: String,

    // ── Settings — General ────────────────────────────────────
    val settingsSectionGeneral: String,
    val settingsAutoBackup: String,
    val settingsAutoBackupDesc: String,
    val settingsApplyOnBoot: String,
    val settingsApplyOnBootDesc: String,
    val settingsNotifications: String,
    val settingsNotificationsDesc: String,

    // ── Settings — Advanced ───────────────────────────────────
    val settingsSectionAdvanced: String,
    val settingsRootMethod: String,
    val settingsDebugLog: String,
    val settingsDebugLogDesc: String,
    val settingsClearCache: String,
    val settingsClearCacheDesc: String,

    // ── Settings — About App ──────────────────────────────────
    val settingsSectionAbout: String,
    val settingsVersion: String,
    val settingsSourceCode: String,
    val settingsSourceCodeDesc: String,
    val settingsLicense: String,
    val settingsLicenseDesc: String,

    // ── Backup — success toasts ───────────────────────────────
    val backupSuccessCreate: String,
    val backupSuccessRestore: String,
    val backupSuccessDelete: String,
    val backupSuccessReset: String,
    val backupSuccessResetProfiles: String,
    val backupSuccessResetMonitor: String,
    val backupFailCreate: String,
    val backupFailRestore: String,
    val backupFailReset: String,

    // ── Backup — empty-state info chips ──────────────────────
    val backupNoProfiles: String,
    val backupMonitorInactive: String,
    val backupSubtitleBackup: String,
    val backupSubtitleReset: String,
    val backupSubtitleResetProfiles: String,
    val backupSubtitleResetMonitor: String,

    // ── License / Premium screen ──────────────────────────────
    val licenseScreenTitle: String,
    val licenseInvoiceHistoryIcon: String,          // content desc icon riwayat invoice

    // Status card — active
    val licensePremiumActive: String,
    val licensePremiumValidUntil: String,           // "Berlaku hingga %s"
    val licenseDaysLeft: String,                    // "%d hari"
    val licenseYourBenefits: String,
    val licenseKeyLabel: String,
    val licenseKeyCopied: String,                   // toast
    val licenseExpiringSoon: String,                // "Premium kamu hampir habis! Perpanjang sebelum %s."
    val licenseDeactivateBtn: String,

    // Status card — free
    val licenseFreeTitle: String,
    val licenseFreeDesc: String,

    // Benefit card (upgrade prompt)
    val licenseUpgradeTitle: String,
    val licenseUpgradeDesc: String,
    val licensePrice1Month: String,
    val licensePrice: String,                       // "Rp 25.000"
    val licenseBuyBtn: String,

    // Pending payment card
    val licensePendingTitle: String,
    val licenseContinuePaymentBtn: String,

    // Key activation card
    val licenseHaveKeyTitle: String,
    val licenseKeyInputLabel: String,
    val licenseActivateBtn: String,

    // Deactivate dialog
    val licenseDeactivateDialogTitle: String,
    val licenseDeactivateDialogBody: String,
    val licenseDeactivateConfirmBtn: String,
    val licenseDeactivateCancelBtn: String,

    // Polling card
    val licensePollingWaiting: String,
    val licensePollingDesc: String,

    // Buy sheet
    val licenseBuySheetTitle: String,
    val licenseBuySheetSubtitle: String,
    val licenseBuySheetFormDesc: String,
    val licenseBuySheetNameLabel: String,
    val licenseBuySheetPhoneLabel: String,
    val licenseBuySheetPhoneHint: String,
    val licenseBuySheetCreateInvoiceBtn: String,

    // Payment detail sheet
    val licensePayDetailTitle: String,
    val licensePayDetailSubtitle: String,
    val licenseOrderIdLabel: String,
    val licenseOrderIdCopied: String,               // toast
    val licenseOrderIdWarning: String,
    val licenseTotalLabel: String,
    val licenseDuration1Month: String,
    val licenseImportantLabel: String,
    val licenseImportantBody: String,               // "Transfer Rp %s. Jangan sampai salah nominal…"
    val licenseAfterTransferLabel: String,
    val licenseVerifyNowBtn: String,
    val licenseCancelBtn: String,

    // Payment method type labels
    val licensePayTypeEwallet: String,              // "E-Wallet"
    val licensePayTypeBank: String,                 // "Transfer Bank"
    val licensePayTypeInternational: String,        // "International"

    // Contact admin row
    val licenseContactWhatsApp: String,
    val licenseContactTelegram: String,

    // Copy number row
    val licenseNumberCopied: String,                // "Nomor %s disalin"
    val licenseCopyLabel: String,                   // "Salin"

    // Invoice history sheet
    val licenseInvoiceHistoryTitle: String,
    val licenseInvoiceHistoryEmpty: String,
    val licenseInvoiceStatusPaid: String,
    val licenseInvoiceStatusExpired: String,
    val licenseInvoiceStatusPending: String,
    val licenseInvoiceContinueBtn: String,
    val licenseInvoiceDeleteBtn: String,

    // Success dialog
    val licenseSuccessTitle: String,
    val licenseSuccessBody: String,                 // "Terima kasih…"
    val licenseSuccessKeyLabel: String,             // "License Key kamu"
    val licenseSuccessValidUntil: String,           // "Berlaku hingga %s"
    val licenseSuccessSaveHint: String,
    val licenseSuccessBenefits: List<String>,
    val licenseSuccessStartBtn: String,

    // Lifetime label (expiry == -1)
    val licenseExpireLifetime: String,

    // Device-locked benefit item (shown in active card & benefit card)
    val licenseBenefitDeviceLocked: String,

    // Transfer instruction steps (shown in TransferInstructionContent)
    val licenseStepNoteOrderId: String,
    val licenseStepTransferExact: String,
    val licenseStepTapPaid: String,
    val licenseStepWaitAdmin: String,

    // ── License — payment method selection ───────────────────
    val licenseSelectPayMethod: String,             // "Pilih Metode Pembayaran"

    // ── Service / Notification ───────────────────────────────
    val serviceNotifChannelName: String,            // "Aether Manager Service"
    val serviceNotifChannelDesc: String,            // "Background service"
    val serviceNotifText: String,                   // "Tweaks active"
)

val LocalStrings = staticCompositionLocalOf<AppStrings> {
    error("AppStrings not provided")
}
