package dev.aether.manager.i18n

import androidx.compose.runtime.staticCompositionLocalOf

// ─────────────────────────────────────────────────────────────────────────────
// Sub-classes (each well under the DEX 200-param limit)
// ─────────────────────────────────────────────────────────────────────────────

data class SetupStrings(
    val welcomeTitle: String,
    val welcomeDesc: String,
    val rootTitle: String,
    val rootDesc: String,
    val rootCta: String,
    val rootRequired: String,
    val rootChecking: String,
    val rootGranted: String,
    val rootDenied: String,
    val rootDeniedSub: String,
    val notifTitle: String,
    val notifDesc: String,
    val notifCta: String,
    val notifGranted: String,
    val notifDenied: String,
    val writeTitle: String,
    val writeDesc: String,
    val writeCta: String,
    val writeGranted: String,
    val writeDenied: String,
    val writeDeniedSub: String,
    val storageTitle: String,
    val storageDesc: String,
    val storageCta: String,
    val storageGranted: String,
    val storageDenied: String,
    val doneTitle: String,
    val doneDesc: String,
    val incompleteTitle: String,
    val incompleteDesc: String,
    val allPermsGranted: String,
    val btnStart: String,
    val btnNext: String,
    val btnBack: String,
    val btnRetry: String,
    val btnSkip: String,
    val stepOf: String,
    val brandLabel: String,
    val grantedSub: String,
    val missingTapHint: String,
    val chipRootFull: String,
    val chipRootShell: String,
    val chipRootRequired: String,
    val warnRoot: String,
    val chipNotifStatus: String,
    val chipNotifConfirm: String,
    val chipOptional: String,
    val chipWriteModify: String,
    val chipWriteFeatures: String,
    val warnWrite: String,
    val chipStorageRead: String,
    val chipStorageExport: String,
    val chipWelcomeMgr: String,
    val chipWelcomePerf: String,
    val chipWelcomeRoot: String,
)

data class HomeStrings(
    val systemStatus: String,
    val monitor: String,
    val retry: String,
    val labelOs: String,
    val labelKernel: String,
    val labelSoc: String,
    val labelGovernor: String,
    val labelUptime: String,
    val tempCpu: String,
    val tempBat: String,
    val selinux: String,
    val batCurrent: String,
    val batVoltage: String,
    val batCharging: String,
    val batDischarging: String,
    val batFull: String,
    val batNotCharging: String,
    val batStatusLabel: String,
)

data class TweakStrings(
    val performanceProfile: String,
    val sectionCpu: String,
    val sectionMemory: String,
    val sectionIo: String,
    val sectionNetwork: String,
    val sectionBattery: String,
    val schedBoost: String,
    val schedBoostDesc: String,
    val cpuBoost: String,
    val cpuBoostDesc: String,
    val gpuThrottle: String,
    val gpuThrottleDesc: String,
    val cpusetOpt: String,
    val cpusetOptDesc: String,
    val mtkBoost: String,
    val mtkBoostDesc: String,
    val lmk: String,
    val lmkDesc: String,
    val zram: String,
    val zramDesc: String,
    val vmDirty: String,
    val vmDirtyDesc: String,
    val ioLatency: String,
    val ioLatencyDesc: String,
    val tcpBbr: String,
    val tcpBbrDesc: String,
    val doh: String,
    val dohDesc: String,
    val netBuffer: String,
    val netBufferDesc: String,
    val doze: String,
    val dozeDesc: String,
    val clearCache: String,
    val clearCacheDesc: String,
    val fastAnim: String,
    val fastAnimDesc: String,
    val entropy: String,
    val entropyDesc: String,
    val zramSize: String,
    val zramAlgo: String,
    val ioScheduler: String,
    val sectionCpuFreq: String,
    val cpuFreqEnable: String,
    val cpuFreqEnableDesc: String,
    val cpuClusterPrime: String,
    val cpuClusterGold: String,
    val cpuClusterSilver: String,
    val cpuFreqMin: String,
    val cpuFreqMax: String,
    val thermalProfile: String,
    val thermalDefault: String,
    val thermalPerformance: String,
    val thermalExtreme: String,
    val thermalDesc: String,
    val gpuFreqLock: String,
    val gpuFreqLockDesc: String,
    val gpuFreqMax: String,
    val touchBoost: String,
    val touchBoostDesc: String,
    val touchSampleRate: String,
    val ksm: String,
    val ksmDesc: String,
    val ksmAggressive: String,
    val ksmAggressiveDesc: String,
)

data class SettingsStrings(
    val title: String,
    val sectionBackup: String,
    val btnBackup: String,
    val btnBackupNow: String,
    val btnResetDefault: String,
    val btnResetAll: String,
    val noBackup: String,
    val backupSaved: String,
    val resetTitle: String,
    val resetDesc: String,
    val resetConfirm: String,
    val btnCancel: String,
    val btnBack: String,
    val restoreTitle: String,
    val restoreDesc: String,
    val restoreConfirm: String,
    val backupProfile: String,
    val btnDelete: String,
    val btnResetProfiles: String,
    val resetProfilesTitle: String,
    val resetProfilesDesc: String,
    val btnResetMonitor: String,
    val resetMonitorTitle: String,
    val resetMonitorDesc: String,
    val sectionAppearance: String,
    val language: String,
    val darkMode: String,
    val darkModeDesc: String,
    val dynamicColor: String,
    val dynamicColorDesc: String,
    val sectionGeneral: String,
    val autoBackup: String,
    val autoBackupDesc: String,
    val applyOnBoot: String,
    val applyOnBootDesc: String,
    val notifications: String,
    val notificationsDesc: String,
    val sectionAdvanced: String,
    val rootMethod: String,
    val debugLog: String,
    val debugLogDesc: String,
    val clearCache: String,
    val clearCacheDesc: String,
    val sectionAbout: String,
    val version: String,
    val sourceCode: String,
    val sourceCodeDesc: String,
    val license: String,
    val licenseDesc: String,
)

data class AppProfileStrings(
    val title: String,
    val monitorOn: String,
    val monitorOff: String,
    val appsCount: String,
    val activeCount: String,
    val searchHint: String,
    val noResults: String,
    val empty: String,
    val deleteTitle: String,
    val deleteDesc: String,
    val deleteConfirm: String,
    val btnCancel: String,
    val loading: String,
    val retry: String,
    val editorActive: String,
    val editorInactive: String,
    val cpuGovernor: String,
    val refreshRate: String,
    val extraTweaks: String,
    val saveBtn: String,
    val disableDoze: String,
    val disableDozeDesc: String,
    val lockCpuMin: String,
    val lockCpuMinDesc: String,
    val killBg: String,
    val killBgDesc: String,
    val gpuBoost: String,
    val gpuBoostDesc: String,
    val ioLatency: String,
    val ioLatencyDesc: String,
    val govDescDefault: String,
    val govDescPerformance: String,
    val govDescPowersave: String,
    val govDescOndemand: String,
    val govDescConservative: String,
    val govDescSchedutil: String,
    val govDescInteractive: String,
)

data class LicenseStrings(
    val screenTitle: String,
    val invoiceHistoryIcon: String,
    val premiumActive: String,
    val premiumValidUntil: String,
    val daysLeft: String,
    val yourBenefits: String,
    val keyLabel: String,
    val keyCopied: String,
    val expiringSoon: String,
    val deactivateBtn: String,
    val freeTitle: String,
    val freeDesc: String,
    val upgradeTitle: String,
    val upgradeDesc: String,
    val price1Month: String,
    val price: String,
    val buyBtn: String,
    val pendingTitle: String,
    val continuePaymentBtn: String,
    val haveKeyTitle: String,
    val keyInputLabel: String,
    val activateBtn: String,
    val deactivateDialogTitle: String,
    val deactivateDialogBody: String,
    val deactivateConfirmBtn: String,
    val deactivateCancelBtn: String,
    val pollingWaiting: String,
    val pollingDesc: String,
    val buySheetTitle: String,
    val buySheetSubtitle: String,
    val buySheetFormDesc: String,
    val buySheetNameLabel: String,
    val buySheetPhoneLabel: String,
    val buySheetPhoneHint: String,
    val buySheetCreateInvoiceBtn: String,
    val payDetailTitle: String,
    val payDetailSubtitle: String,
    val orderIdLabel: String,
    val orderIdCopied: String,
    val orderIdWarning: String,
    val totalLabel: String,
    val duration1Month: String,
    val importantLabel: String,
    val importantBody: String,
    val afterTransferLabel: String,
    val verifyNowBtn: String,
    val cancelBtn: String,
    val payTypeEwallet: String,
    val payTypeBank: String,
    val payTypeInternational: String,
    val contactWhatsApp: String,
    val contactTelegram: String,
    val numberCopied: String,
    val copyLabel: String,
    val invoiceHistoryTitle: String,
    val invoiceHistoryEmpty: String,
    val invoiceStatusPaid: String,
    val invoiceStatusExpired: String,
    val invoiceStatusPending: String,
    val invoiceContinueBtn: String,
    val invoiceDeleteBtn: String,
    val successTitle: String,
    val successBody: String,
    val successKeyLabel: String,
    val successValidUntil: String,
    val successSaveHint: String,
    val successBenefits: List<String>,
    val successStartBtn: String,
    val expireLifetime: String,
    val benefitDeviceLocked: String,
    val stepNoteOrderId: String,
    val stepTransferExact: String,
    val stepTapPaid: String,
    val stepWaitAdmin: String,
    val selectPayMethod: String,
)

data class MiscStrings(
    // Nav
    val navHome: String,
    val navTweak: String,
    val navApps: String,
    // Log/Reboot
    val logRebootOptions: String,
    val logRebootSystem: String,
    val logRebootSystemDesc: String,
    val logRebootRecovery: String,
    val logRebootRecoveryDesc: String,
    val logReloadUi: String,
    val logReloadUiDesc: String,
    val logBtnCancel: String,
    // AdBlock
    val adBlockTitle: String,
    val adBlockBody: String,
    val adBlockInfoCard: String,
    val adBlockSafeLabel: String,
    val adBlockBtnDisable: String,
    // Update
    val updateAvailable: String,
    val updateTabDesc: String,
    val updateTabChangelog: String,
    val updateBtnDownload: String,
    val updateBtnLater: String,
    val updateBtnRetry: String,
    val updateBtnBrowser: String,
    val updateInstalling: String,
    val updateFailed: String,
    val updateDownloadDone: String,
    val updateDownloading: String,
    val updateAboutTitle: String,
    val updateAboutDesc: String,
    val updateChangelogLoading: String,
    val updateChangelogEmpty: String,
    // Backup toasts
    val backupSuccessCreate: String,
    val backupSuccessRestore: String,
    val backupSuccessDelete: String,
    val backupSuccessReset: String,
    val backupSuccessResetProfiles: String,
    val backupSuccessResetMonitor: String,
    val backupFailCreate: String,
    val backupFailRestore: String,
    val backupFailReset: String,
    val backupNoProfiles: String,
    val backupMonitorInactive: String,
    val backupSubtitleBackup: String,
    val backupSubtitleReset: String,
    val backupSubtitleResetProfiles: String,
    val backupSubtitleResetMonitor: String,
    // Service
    val serviceNotifChannelName: String,
    val serviceNotifChannelDesc: String,
    val serviceNotifText: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Main AppStrings: holds sub-objects + flat delegating properties so that
// existing call sites like `s.setupRootTitle` continue to compile unchanged.
// ─────────────────────────────────────────────────────────────────────────────

data class AppStrings(
    val setup: SetupStrings,
    val home: HomeStrings,
    val tweak: TweakStrings,
    val settings: SettingsStrings,
    val appProfile: AppProfileStrings,
    val license: LicenseStrings,
    val misc: MiscStrings,
) {
    // ── Navigation ────────────────────────────────────────────
    val navHome  get() = misc.navHome
    val navTweak get() = misc.navTweak
    val navApps  get() = misc.navApps

    // ── Setup ─────────────────────────────────────────────────
    val setupWelcomeTitle     get() = setup.welcomeTitle
    val setupWelcomeDesc      get() = setup.welcomeDesc
    val setupRootTitle        get() = setup.rootTitle
    val setupRootDesc         get() = setup.rootDesc
    val setupRootCta          get() = setup.rootCta
    val setupRootRequired     get() = setup.rootRequired
    val setupRootChecking     get() = setup.rootChecking
    val setupRootGranted      get() = setup.rootGranted
    val setupRootDenied       get() = setup.rootDenied
    val setupRootDeniedSub    get() = setup.rootDeniedSub
    val setupNotifTitle       get() = setup.notifTitle
    val setupNotifDesc        get() = setup.notifDesc
    val setupNotifCta         get() = setup.notifCta
    val setupNotifGranted     get() = setup.notifGranted
    val setupNotifDenied      get() = setup.notifDenied
    val setupWriteTitle       get() = setup.writeTitle
    val setupWriteDesc        get() = setup.writeDesc
    val setupWriteCta         get() = setup.writeCta
    val setupWriteGranted     get() = setup.writeGranted
    val setupWriteDenied      get() = setup.writeDenied
    val setupWriteDeniedSub   get() = setup.writeDeniedSub
    val setupStorageTitle     get() = setup.storageTitle
    val setupStorageDesc      get() = setup.storageDesc
    val setupStorageCta       get() = setup.storageCta
    val setupStorageGranted   get() = setup.storageGranted
    val setupStorageDenied    get() = setup.storageDenied
    val setupDoneTitle        get() = setup.doneTitle
    val setupDoneDesc         get() = setup.doneDesc
    val setupIncompleteTitle  get() = setup.incompleteTitle
    val setupIncompleteDesc   get() = setup.incompleteDesc
    val setupAllPermsGranted  get() = setup.allPermsGranted
    val setupBtnStart         get() = setup.btnStart
    val setupBtnNext          get() = setup.btnNext
    val setupBtnBack          get() = setup.btnBack
    val setupBtnRetry         get() = setup.btnRetry
    val setupBtnSkip          get() = setup.btnSkip
    val setupStepOf           get() = setup.stepOf
    val setupBrandLabel       get() = setup.brandLabel
    val setupGrantedSub       get() = setup.grantedSub
    val setupMissingTapHint   get() = setup.missingTapHint
    val setupChipRootFull     get() = setup.chipRootFull
    val setupChipRootShell    get() = setup.chipRootShell
    val setupChipRootRequired get() = setup.chipRootRequired
    val setupWarnRoot         get() = setup.warnRoot
    val setupChipNotifStatus  get() = setup.chipNotifStatus
    val setupChipNotifConfirm get() = setup.chipNotifConfirm
    val setupChipOptional     get() = setup.chipOptional
    val setupChipWriteModify  get() = setup.chipWriteModify
    val setupChipWriteFeatures get() = setup.chipWriteFeatures
    val setupWarnWrite        get() = setup.warnWrite
    val setupChipStorageRead  get() = setup.chipStorageRead
    val setupChipStorageExport get() = setup.chipStorageExport
    val setupChipWelcomeMgr   get() = setup.chipWelcomeMgr
    val setupChipWelcomePerf  get() = setup.chipWelcomePerf
    val setupChipWelcomeRoot  get() = setup.chipWelcomeRoot

    // ── Home ──────────────────────────────────────────────────
    val homeSystemStatus  get() = home.systemStatus
    val homeMonitor       get() = home.monitor
    val homeRetry         get() = home.retry
    val homeLabelOs       get() = home.labelOs
    val homeLabelKernel   get() = home.labelKernel
    val homeLabelSoc      get() = home.labelSoc
    val homeLabelGovernor get() = home.labelGovernor
    val homeLabelUptime   get() = home.labelUptime
    val homeTempCpu       get() = home.tempCpu
    val homeTempBat       get() = home.tempBat
    val homeSelinux       get() = home.selinux
    val homeBatCurrent    get() = home.batCurrent
    val homeBatVoltage    get() = home.batVoltage
    val homeBatCharging   get() = home.batCharging
    val homeBatDischarging get() = home.batDischarging
    val homeBatFull       get() = home.batFull
    val homeBatNotCharging get() = home.batNotCharging
    val homeBatStatusLabel get() = home.batStatusLabel

    // ── Tweak ─────────────────────────────────────────────────
    val tweakPerformanceProfile  get() = tweak.performanceProfile
    val tweakSectionCpu          get() = tweak.sectionCpu
    val tweakSectionMemory       get() = tweak.sectionMemory
    val tweakSectionIo           get() = tweak.sectionIo
    val tweakSectionNetwork      get() = tweak.sectionNetwork
    val tweakSectionBattery      get() = tweak.sectionBattery
    val tweakSchedBoost          get() = tweak.schedBoost
    val tweakSchedBoostDesc      get() = tweak.schedBoostDesc
    val tweakCpuBoost            get() = tweak.cpuBoost
    val tweakCpuBoostDesc        get() = tweak.cpuBoostDesc
    val tweakGpuThrottle         get() = tweak.gpuThrottle
    val tweakGpuThrottleDesc     get() = tweak.gpuThrottleDesc
    val tweakCpusetOpt           get() = tweak.cpusetOpt
    val tweakCpusetOptDesc       get() = tweak.cpusetOptDesc
    val tweakMtkBoost            get() = tweak.mtkBoost
    val tweakMtkBoostDesc        get() = tweak.mtkBoostDesc
    val tweakLmk                 get() = tweak.lmk
    val tweakLmkDesc             get() = tweak.lmkDesc
    val tweakZram                get() = tweak.zram
    val tweakZramDesc            get() = tweak.zramDesc
    val tweakVmDirty             get() = tweak.vmDirty
    val tweakVmDirtyDesc         get() = tweak.vmDirtyDesc
    val tweakIoLatency           get() = tweak.ioLatency
    val tweakIoLatencyDesc       get() = tweak.ioLatencyDesc
    val tweakTcpBbr              get() = tweak.tcpBbr
    val tweakTcpBbrDesc          get() = tweak.tcpBbrDesc
    val tweakDoh                 get() = tweak.doh
    val tweakDohDesc             get() = tweak.dohDesc
    val tweakNetBuffer           get() = tweak.netBuffer
    val tweakNetBufferDesc       get() = tweak.netBufferDesc
    val tweakDoze                get() = tweak.doze
    val tweakDozeDesc            get() = tweak.dozeDesc
    val tweakClearCache          get() = tweak.clearCache
    val tweakClearCacheDesc      get() = tweak.clearCacheDesc
    val tweakFastAnim            get() = tweak.fastAnim
    val tweakFastAnimDesc        get() = tweak.fastAnimDesc
    val tweakEntropy             get() = tweak.entropy
    val tweakEntropyDesc         get() = tweak.entropyDesc
    val tweakZramSize            get() = tweak.zramSize
    val tweakZramAlgo            get() = tweak.zramAlgo
    val tweakIoScheduler         get() = tweak.ioScheduler
    val tweakSectionCpuFreq      get() = tweak.sectionCpuFreq
    val tweakCpuFreqEnable       get() = tweak.cpuFreqEnable
    val tweakCpuFreqEnableDesc   get() = tweak.cpuFreqEnableDesc
    val tweakCpuClusterPrime     get() = tweak.cpuClusterPrime
    val tweakCpuClusterGold      get() = tweak.cpuClusterGold
    val tweakCpuClusterSilver    get() = tweak.cpuClusterSilver
    val tweakCpuFreqMin          get() = tweak.cpuFreqMin
    val tweakCpuFreqMax          get() = tweak.cpuFreqMax
    val tweakThermalProfile      get() = tweak.thermalProfile
    val tweakThermalDefault      get() = tweak.thermalDefault
    val tweakThermalPerformance  get() = tweak.thermalPerformance
    val tweakThermalExtreme      get() = tweak.thermalExtreme
    val tweakThermalDesc         get() = tweak.thermalDesc
    val tweakGpuFreqLock         get() = tweak.gpuFreqLock
    val tweakGpuFreqLockDesc     get() = tweak.gpuFreqLockDesc
    val tweakGpuFreqMax          get() = tweak.gpuFreqMax
    val tweakTouchBoost          get() = tweak.touchBoost
    val tweakTouchBoostDesc      get() = tweak.touchBoostDesc
    val tweakTouchSampleRate     get() = tweak.touchSampleRate
    val tweakKsm                 get() = tweak.ksm
    val tweakKsmDesc             get() = tweak.ksmDesc
    val tweakKsmAggressive       get() = tweak.ksmAggressive
    val tweakKsmAggressiveDesc   get() = tweak.ksmAggressiveDesc

    // ── Settings ──────────────────────────────────────────────
    val settingsTitle                get() = settings.title
    val settingsSectionBackup        get() = settings.sectionBackup
    val settingsBtnBackup            get() = settings.btnBackup
    val settingsBtnBackupNow         get() = settings.btnBackupNow
    val settingsBtnResetDefault      get() = settings.btnResetDefault
    val settingsBtnResetAll          get() = settings.btnResetAll
    val settingsNoBackup             get() = settings.noBackup
    val settingsBackupSaved          get() = settings.backupSaved
    val settingsResetTitle           get() = settings.resetTitle
    val settingsResetDesc            get() = settings.resetDesc
    val settingsResetConfirm         get() = settings.resetConfirm
    val settingsBtnCancel            get() = settings.btnCancel
    val settingsBtnBack              get() = settings.btnBack
    val settingsRestoreTitle         get() = settings.restoreTitle
    val settingsRestoreDesc          get() = settings.restoreDesc
    val settingsRestoreConfirm       get() = settings.restoreConfirm
    val settingsBackupProfile        get() = settings.backupProfile
    val settingsBtnDelete            get() = settings.btnDelete
    val settingsBtnResetProfiles     get() = settings.btnResetProfiles
    val settingsResetProfilesTitle   get() = settings.resetProfilesTitle
    val settingsResetProfilesDesc    get() = settings.resetProfilesDesc
    val settingsBtnResetMonitor      get() = settings.btnResetMonitor
    val settingsResetMonitorTitle    get() = settings.resetMonitorTitle
    val settingsResetMonitorDesc     get() = settings.resetMonitorDesc
    val settingsSectionAppearance    get() = settings.sectionAppearance
    val settingsLanguage             get() = settings.language
    val settingsDarkMode             get() = settings.darkMode
    val settingsDarkModeDesc         get() = settings.darkModeDesc
    val settingsDynamicColor         get() = settings.dynamicColor
    val settingsDynamicColorDesc     get() = settings.dynamicColorDesc
    val settingsSectionGeneral       get() = settings.sectionGeneral
    val settingsAutoBackup           get() = settings.autoBackup
    val settingsAutoBackupDesc       get() = settings.autoBackupDesc
    val settingsApplyOnBoot          get() = settings.applyOnBoot
    val settingsApplyOnBootDesc      get() = settings.applyOnBootDesc
    val settingsNotifications        get() = settings.notifications
    val settingsNotificationsDesc    get() = settings.notificationsDesc
    val settingsSectionAdvanced      get() = settings.sectionAdvanced
    val settingsRootMethod           get() = settings.rootMethod
    val settingsDebugLog             get() = settings.debugLog
    val settingsDebugLogDesc         get() = settings.debugLogDesc
    val settingsClearCache           get() = settings.clearCache
    val settingsClearCacheDesc       get() = settings.clearCacheDesc
    val settingsSectionAbout         get() = settings.sectionAbout
    val settingsVersion              get() = settings.version
    val settingsSourceCode           get() = settings.sourceCode
    val settingsSourceCodeDesc       get() = settings.sourceCodeDesc
    val settingsLicense              get() = settings.license
    val settingsLicenseDesc          get() = settings.licenseDesc

    // ── App Profile ───────────────────────────────────────────
    val appProfileTitle            get() = appProfile.title
    val appProfileMonitorOn        get() = appProfile.monitorOn
    val appProfileMonitorOff       get() = appProfile.monitorOff
    val appProfileAppsCount        get() = appProfile.appsCount
    val appProfileActiveCount      get() = appProfile.activeCount
    val appProfileSearchHint       get() = appProfile.searchHint
    val appProfileNoResults        get() = appProfile.noResults
    val appProfileEmpty            get() = appProfile.empty
    val appProfileDeleteTitle      get() = appProfile.deleteTitle
    val appProfileDeleteDesc       get() = appProfile.deleteDesc
    val appProfileDeleteConfirm    get() = appProfile.deleteConfirm
    val appProfileBtnCancel        get() = appProfile.btnCancel
    val appProfileLoading          get() = appProfile.loading
    val appProfileRetry            get() = appProfile.retry
    val appProfileEditorActive     get() = appProfile.editorActive
    val appProfileEditorInactive   get() = appProfile.editorInactive
    val appProfileCpuGovernor      get() = appProfile.cpuGovernor
    val appProfileRefreshRate      get() = appProfile.refreshRate
    val appProfileExtraTweaks      get() = appProfile.extraTweaks
    val appProfileSaveBtn          get() = appProfile.saveBtn
    val appProfileDisableDoze      get() = appProfile.disableDoze
    val appProfileDisableDozeDesc  get() = appProfile.disableDozeDesc
    val appProfileLockCpuMin       get() = appProfile.lockCpuMin
    val appProfileLockCpuMinDesc   get() = appProfile.lockCpuMinDesc
    val appProfileKillBg           get() = appProfile.killBg
    val appProfileKillBgDesc       get() = appProfile.killBgDesc
    val appProfileGpuBoost         get() = appProfile.gpuBoost
    val appProfileGpuBoostDesc     get() = appProfile.gpuBoostDesc
    val appProfileIoLatency        get() = appProfile.ioLatency
    val appProfileIoLatencyDesc    get() = appProfile.ioLatencyDesc
    val govDescDefault             get() = appProfile.govDescDefault
    val govDescPerformance         get() = appProfile.govDescPerformance
    val govDescPowersave           get() = appProfile.govDescPowersave
    val govDescOndemand            get() = appProfile.govDescOndemand
    val govDescConservative        get() = appProfile.govDescConservative
    val govDescSchedutil           get() = appProfile.govDescSchedutil
    val govDescInteractive         get() = appProfile.govDescInteractive

    // ── License ───────────────────────────────────────────────
    val licenseScreenTitle           get() = license.screenTitle
    val licenseInvoiceHistoryIcon    get() = license.invoiceHistoryIcon
    val licensePremiumActive         get() = license.premiumActive
    val licensePremiumValidUntil     get() = license.premiumValidUntil
    val licenseDaysLeft              get() = license.daysLeft
    val licenseYourBenefits          get() = license.yourBenefits
    val licenseKeyLabel              get() = license.keyLabel
    val licenseKeyCopied             get() = license.keyCopied
    val licenseExpiringSoon          get() = license.expiringSoon
    val licenseDeactivateBtn         get() = license.deactivateBtn
    val licenseFreeTitle             get() = license.freeTitle
    val licenseFreeDesc              get() = license.freeDesc
    val licenseUpgradeTitle          get() = license.upgradeTitle
    val licenseUpgradeDesc           get() = license.upgradeDesc
    val licensePrice1Month           get() = license.price1Month
    val licensePrice                 get() = license.price
    val licenseBuyBtn                get() = license.buyBtn
    val licensePendingTitle          get() = license.pendingTitle
    val licenseContinuePaymentBtn    get() = license.continuePaymentBtn
    val licenseHaveKeyTitle          get() = license.haveKeyTitle
    val licenseKeyInputLabel         get() = license.keyInputLabel
    val licenseActivateBtn           get() = license.activateBtn
    val licenseDeactivateDialogTitle get() = license.deactivateDialogTitle
    val licenseDeactivateDialogBody  get() = license.deactivateDialogBody
    val licenseDeactivateConfirmBtn  get() = license.deactivateConfirmBtn
    val licenseDeactivateCancelBtn   get() = license.deactivateCancelBtn
    val licensePollingWaiting        get() = license.pollingWaiting
    val licensePollingDesc           get() = license.pollingDesc
    val licenseBuySheetTitle         get() = license.buySheetTitle
    val licenseBuySheetSubtitle      get() = license.buySheetSubtitle
    val licenseBuySheetFormDesc      get() = license.buySheetFormDesc
    val licenseBuySheetNameLabel     get() = license.buySheetNameLabel
    val licenseBuySheetPhoneLabel    get() = license.buySheetPhoneLabel
    val licenseBuySheetPhoneHint     get() = license.buySheetPhoneHint
    val licenseBuySheetCreateInvoiceBtn get() = license.buySheetCreateInvoiceBtn
    val licensePayDetailTitle        get() = license.payDetailTitle
    val licensePayDetailSubtitle     get() = license.payDetailSubtitle
    val licenseOrderIdLabel          get() = license.orderIdLabel
    val licenseOrderIdCopied         get() = license.orderIdCopied
    val licenseOrderIdWarning        get() = license.orderIdWarning
    val licenseTotalLabel            get() = license.totalLabel
    val licenseDuration1Month        get() = license.duration1Month
    val licenseImportantLabel        get() = license.importantLabel
    val licenseImportantBody         get() = license.importantBody
    val licenseAfterTransferLabel    get() = license.afterTransferLabel
    val licenseVerifyNowBtn          get() = license.verifyNowBtn
    val licenseCancelBtn             get() = license.cancelBtn
    val licensePayTypeEwallet        get() = license.payTypeEwallet
    val licensePayTypeBank           get() = license.payTypeBank
    val licensePayTypeInternational  get() = license.payTypeInternational
    val licenseContactWhatsApp       get() = license.contactWhatsApp
    val licenseContactTelegram       get() = license.contactTelegram
    val licenseNumberCopied          get() = license.numberCopied
    val licenseCopyLabel             get() = license.copyLabel
    val licenseInvoiceHistoryTitle   get() = license.invoiceHistoryTitle
    val licenseInvoiceHistoryEmpty   get() = license.invoiceHistoryEmpty
    val licenseInvoiceStatusPaid     get() = license.invoiceStatusPaid
    val licenseInvoiceStatusExpired  get() = license.invoiceStatusExpired
    val licenseInvoiceStatusPending  get() = license.invoiceStatusPending
    val licenseInvoiceContinueBtn    get() = license.invoiceContinueBtn
    val licenseInvoiceDeleteBtn      get() = license.invoiceDeleteBtn
    val licenseSuccessTitle          get() = license.successTitle
    val licenseSuccessBody           get() = license.successBody
    val licenseSuccessKeyLabel       get() = license.successKeyLabel
    val licenseSuccessValidUntil     get() = license.successValidUntil
    val licenseSuccessSaveHint       get() = license.successSaveHint
    val licenseSuccessBenefits       get() = license.successBenefits
    val licenseSuccessStartBtn       get() = license.successStartBtn
    val licenseExpireLifetime        get() = license.expireLifetime
    val licenseBenefitDeviceLocked   get() = license.benefitDeviceLocked
    val licenseStepNoteOrderId       get() = license.stepNoteOrderId
    val licenseStepTransferExact     get() = license.stepTransferExact
    val licenseStepTapPaid           get() = license.stepTapPaid
    val licenseStepWaitAdmin         get() = license.stepWaitAdmin
    val licenseSelectPayMethod       get() = license.selectPayMethod

    // ── Misc ──────────────────────────────────────────────────
    val logRebootOptions          get() = misc.logRebootOptions
    val logRebootSystem           get() = misc.logRebootSystem
    val logRebootSystemDesc       get() = misc.logRebootSystemDesc
    val logRebootRecovery         get() = misc.logRebootRecovery
    val logRebootRecoveryDesc     get() = misc.logRebootRecoveryDesc
    val logReloadUi               get() = misc.logReloadUi
    val logReloadUiDesc           get() = misc.logReloadUiDesc
    val logBtnCancel              get() = misc.logBtnCancel
    val adBlockTitle              get() = misc.adBlockTitle
    val adBlockBody               get() = misc.adBlockBody
    val adBlockInfoCard           get() = misc.adBlockInfoCard
    val adBlockSafeLabel          get() = misc.adBlockSafeLabel
    val adBlockBtnDisable         get() = misc.adBlockBtnDisable
    val updateAvailable           get() = misc.updateAvailable
    val updateTabDesc             get() = misc.updateTabDesc
    val updateTabChangelog        get() = misc.updateTabChangelog
    val updateBtnDownload         get() = misc.updateBtnDownload
    val updateBtnLater            get() = misc.updateBtnLater
    val updateBtnRetry            get() = misc.updateBtnRetry
    val updateBtnBrowser          get() = misc.updateBtnBrowser
    val updateInstalling          get() = misc.updateInstalling
    val updateFailed              get() = misc.updateFailed
    val updateDownloadDone        get() = misc.updateDownloadDone
    val updateDownloading         get() = misc.updateDownloading
    val updateAboutTitle          get() = misc.updateAboutTitle
    val updateAboutDesc           get() = misc.updateAboutDesc
    val updateChangelogLoading    get() = misc.updateChangelogLoading
    val updateChangelogEmpty      get() = misc.updateChangelogEmpty
    val backupSuccessCreate       get() = misc.backupSuccessCreate
    val backupSuccessRestore      get() = misc.backupSuccessRestore
    val backupSuccessDelete       get() = misc.backupSuccessDelete
    val backupSuccessReset        get() = misc.backupSuccessReset
    val backupSuccessResetProfiles get() = misc.backupSuccessResetProfiles
    val backupSuccessResetMonitor get() = misc.backupSuccessResetMonitor
    val backupFailCreate          get() = misc.backupFailCreate
    val backupFailRestore         get() = misc.backupFailRestore
    val backupFailReset           get() = misc.backupFailReset
    val backupNoProfiles          get() = misc.backupNoProfiles
    val backupMonitorInactive     get() = misc.backupMonitorInactive
    val backupSubtitleBackup      get() = misc.backupSubtitleBackup
    val backupSubtitleReset       get() = misc.backupSubtitleReset
    val backupSubtitleResetProfiles get() = misc.backupSubtitleResetProfiles
    val backupSubtitleResetMonitor get() = misc.backupSubtitleResetMonitor
    val serviceNotifChannelName   get() = misc.serviceNotifChannelName
    val serviceNotifChannelDesc   get() = misc.serviceNotifChannelDesc
    val serviceNotifText          get() = misc.serviceNotifText
}

val LocalStrings = staticCompositionLocalOf<AppStrings> {
    error("AppStrings not provided")
}