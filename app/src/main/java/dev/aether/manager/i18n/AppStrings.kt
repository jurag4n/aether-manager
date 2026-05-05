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
    val setupRootRequired: String,
    val setupRootDenied: String,
    val setupIncompleteTitle: String,
    val setupAllPermsGranted: String,
    val setupBtnStart: String,
    val setupBtnNext: String,
    val setupBtnBack: String,
    // ── Home ─────────────────────────────────────────────────

    // ── Tweak ────────────────────────────────────────────────
    // ── CPU Freq Limiter ──────────────────────────────────────
    // ── Thermal Control ───────────────────────────────────────
    // ── GPU Freq Lock ─────────────────────────────────────────
    // ── Touch Boost ───────────────────────────────────────────
    // ── KSM ──────────────────────────────────────────────────
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
    val appProfileNoResults: String,
    val appProfileEmpty: String,
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
    // ── Settings — About App ──────────────────────────────────
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
    // Benefit card (upgrade prompt)
    val licensePrice1Month: String,
    val licensePrice: String,                       // "Rp 25.000"
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
    val licenseBuySheetCreateInvoiceBtn: String,

    // Payment detail sheet
    val licensePayDetailTitle: String,
    val licenseOrderIdLabel: String,
    val licenseOrderIdCopied: String,               // toast
    val licenseDuration1Month: String,
    val licenseCancelBtn: String,

    // Payment method type labels

    // Contact admin row
    val licenseContactWhatsApp: String,
    val licenseContactTelegram: String,

    // Copy number row

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
    val licenseSuccessBenefits: List<String>,
    val licenseSuccessStartBtn: String,

    // Lifetime label (expiry == -1)
    val licenseExpireLifetime: String,

    // Device-locked benefit item (shown in active card & benefit card)
    val licenseBenefitDeviceLocked: String,

    // Transfer instruction steps (shown in TransferInstructionContent)
    // ── License — payment method selection ───────────────────
    val licenseSelectPayMethod: String,             // "Pilih Metode Pembayaran"


    // ── Additional i18n coverage ─────────────────────────────
    val setupHeaderSubtitle: String,
    val setupRequiredChip: String,
    val setupCheckingPermission: String,
    val setupGrantedReady: String,
    val setupDeniedOptional: String,
    val setupRootActiveHint: String,
    val setupRootRequiredHint: String,
    val setupPermRootTitle: String,
    val setupPermRootDesc: String,
    val setupPermNotifTitle: String,
    val setupPermNotifDesc: String,
    val setupPermStorageTitle: String,
    val setupPermStorageDesc: String,
    val setupPermBatteryTitle: String,
    val setupPermBatteryDesc: String,
    val setupPermUsageTitle: String,
    val setupPermUsageDesc: String,
    val setupPermWriteTitle: String,
    val setupPermWriteDesc: String,
    val setupFeatureTitle: String,
    val setupFeaturePerformanceTitle: String,
    val setupFeaturePerformanceDesc: String,
    val setupFeaturePowerTitle: String,
    val setupFeaturePowerDesc: String,
    val setupFeatureGamingTitle: String,
    val setupFeatureGamingDesc: String,
    val setupPermissionsTitle: String,
    val setupPermissionsDesc: String,
    val setupCompleteTitle: String,
    val setupReadyLongDesc: String,
    val setupIncompleteLongDesc: String,
    val setupPreparedTitle: String,
    val setupIncompleteShortTitle: String,
    val setupRootModeTitle: String,
    val setupRootModeDesc: String,
    val setupStableMonitorTitle: String,
    val setupStableMonitorDesc: String,
    val setupOptimizationReadyTitle: String,
    val setupOptimizationReadyDesc: String,
    val splashTagline: String,
    val licensePendingWaitButton: String,
    val licensePendingCancelButton: String,
    val licensePremiumHeadline: String,
    val licensePremiumDescription: String,
    val licensePremiumFeatureTweaks: String,
    val licenseBuyOfficialTitle: String,
    val licenseOfficialBadge: String,
    val licenseBuyFormTitleIntl: String,
    val licenseBuyFormTitleLocal: String,
    val licenseBuyFormDescIntl: String,
    val licenseBuyFormDescLocal: String,
    val licenseInvoiceFromAppIntl: String,
    val licenseInvoiceFromAppLocal: String,
    val licenseInvoiceAdminWait: String,
    val licenseBuyerNameLabel: String,
    val licenseBuyerPhoneHint: String,
    val licenseInternationalBuyerDesc: String,
    val licenseInternationalPaymentHint: String,
    val licensePurchaseFlow: String,
    val licensePurchaseFlowLocal: String,
    val licensePayUsingPayPal: String,
    val licenseTransferLocalMethods: String,
    val licenseSendPaymentProofAdmin: String,
    val licenseInternationalPhoneHint: String,
    val licenseInternationalModeActive: String,
    val licenseActivatedAfterAdminCheck: String,
    val licenseAmountToPay: String,
    val licenseTotalPay: String,
    val licenseManualTransferDesc: String,
    val licenseInvoiceExactIntl: String,
    val licenseInvoiceExactLocal: String,
    val licenseInvoiceAdminActivation: String,
    val licenseDestinationNumber: String,
    val licenseTransferNameHint: String,
    val licenseAdminStatusMessage: String,
    val licensePremiumActiveBadge: String,
    val settingsAppearanceSubtitle: String,
    val settingsGeneralSubtitle: String,
    val settingsBackupSavedCount: String,
    val settingsLanguageDialogTitle: String,
    val settingsPremiumActiveBadge: String,
    val settingsLicenseActiveDesc: String,
    val settingsLicenseInactiveDesc: String,
    val homeBatteryTitle: String,
    val homeMemoryTitle: String,
    val tweakGpuSummary: String,
    val tweakGpuGovernorProfile: String,
    val tweakDeviceName: String,
    val tweakActiveProfileCpuGpu: String,
    val tweakAppProfileTitle: String,
    val tweakAppProfileDesc: String,
    val tweakSelectedNow: String,
    val appProfileListTitle: String,
    val appProfilePerformanceProfile: String,
    val appProfileEnableProfile: String,
    val appProfileEnabledForThisApp: String,
    val appProfileDisabledForThisApp: String,
    val notificationChannelUpdateName: String,
    val notificationChannelUpdateDesc: String,
    val notificationChannelLicenseName: String,
    val notificationChannelLicenseDesc: String,
    val notificationChannelGeneralName: String,
    val notificationChannelGeneralDesc: String,
    val notificationLicenseExpiredTitle: String,
    val notificationLicenseExpiredText: String,
    val notificationLicenseExpiredBody: String,
    val notificationLicenseExpiringTitle: String,
    val notificationLicenseExpiringText: String,
    val notificationLicenseExpiringBody: String,
    val notificationProfileActiveTitle: String,
    val notificationTweaksApplied: String,
    val paymentNameRequired: String,
    val paymentPhoneRequired: String,
    val paymentPhoneInvalid: String,
    val paymentPhoneTooShort: String,
    val paymentPhoneTooLong: String,
    val paymentCreateOrderFailed: String,
    val licenseActivationSuccessMessage: String,
    val licenseActivationInvalidMessage: String,
    val appProfileLoadTimeout: String,
    val appProfileLoadFailed: String,
    val rootGrantFirst: String,
    val appProfileSaveFailed: String,
    val appProfileMonitorFailed: String,
    val cacheClearFailed: String,
    val updateFetchFailed: String,
    val updateCheckFailed: String,
    val updateDownloadFailedShort: String,
    val rootNoOutput: String,

    // ── Service / Notification ───────────────────────────────
    val setupPendingPermissions: String,
    val licenseRealProcess: String,
    val licenseChoosePayPal: String,
    val licenseTapConfirmPayment: String,
    val licenseSendPaymentScreenshot: String,
    val licensePaymentMethodUnavailable: String,
    val licensePayPalNote: String,
    val licensePaymentConfirmTitle: String,
    val licensePaymentNameLine: String,
    val licensePaymentMethodLine: String,
    val licensePaymentProofLine: String,
    val settingsBackupResetTitle: String,

    val licensePaypalAccount: String,
    val licensePaymentDestinationCopied: String,
    val licensePaymentIndonesia: String,
    val licenseInternationalOnly: String,
    val licenseEwalletIndonesia: String,

    val licensePaymentConfirmationClipLabel: String,
    val licenseConfirmationCopied: String,

    val licensePendingTimeoutTitle: String,
    val licensePendingTimeoutBody: String,
    val licenseNoAdsFallback: String,
    val licenseSupportFallback: String,
    val licensePaymentMethodsTitle: String,
    val licensePayPalBuyerNote: String,
    val licensePayExactTitle: String,
    val licensePayExactBody: String,
    val licenseSendProofTitle: String,
    val licenseSendProofBody: String,
    val licenseConfirmPaymentBtn: String,
    val paymentPollTimeoutLong: String,
    val paymentStatusFailed: String,
    val paymentPollTimeoutShort: String,

    val setupNotGrantedYet: String,

    val homeRefresh: String,
    val homeTemperature: String,
    val homeFrequency: String,
    val homeGpuType: String,
    val homeGpuLoad: String,
    val homeInternalStorage: String,
    val tweakClose: String,
    val tweakSelectGpuRenderer: String,

    val tweakTapToSelect: String,

    val serviceNotifChannelName: String,            // "Aether Manager Service"
    val serviceNotifChannelDesc: String,            // "Background service"
    val serviceNotifText: String,                   // "Tweaks active"
)

val LocalStrings = staticCompositionLocalOf<AppStrings> {
    error("AppStrings not provided")
}
