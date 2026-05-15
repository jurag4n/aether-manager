package dev.aether.manager.i18n

import android.content.Context

val AppStrings.setupHeaderSubtitle: String
    get() = if (this === StringsId) "Setup awal aplikasi" else "Initial app setup"
val AppStrings.setupRequiredChip: String
    get() = if (this === StringsId) "Wajib" else "Required"
val AppStrings.setupCheckingPermission: String
    get() = if (this === StringsId) "Memeriksa izin…" else "Checking permission…"
val AppStrings.setupPermissionReady: String
    get() = if (this === StringsId) "Aktif dan siap digunakan" else "Active and ready to use"
val AppStrings.setupPermissionMissing: String
    get() = if (this === StringsId) "Belum diberikan" else "Not granted yet"
val AppStrings.setupPermissionSkipped: String
    get() = if (this === StringsId) "Dilewati / belum aktif" else "Skipped / inactive"
val AppStrings.setupPermissionSetupTitle: String
    get() = if (this === StringsId) "Permission Setup" else "Permission Setup"
val AppStrings.setupPermissionSetupRootOk: String
    get() = if (this === StringsId) "Root aktif. Lengkapi izin lain agar fitur berjalan stabil." else "Root is active. Complete the remaining permissions so every feature runs reliably."
val AppStrings.setupPermissionSetupRootRequired: String
    get() = if (this === StringsId) "Root wajib aktif sebelum masuk ke dashboard utama." else "Root must be active before opening the main dashboard."
val AppStrings.setupPendingPermissions: String
    get() = if (this === StringsId) "Selesaikan izin: %s" else "Complete permissions: %s"

val AppStrings.permRootTitle: String get() = setupRootTitle
val AppStrings.permRootDesc: String get() = if (this === StringsId) "Aktifkan kontrol performa tingkat sistem." else "Enable system-level performance control."
val AppStrings.permNotifTitle: String get() = setupNotifTitle
val AppStrings.permNotifDesc: String get() = if (this === StringsId) "Info status optimasi, peringatan, dan proses background." else "Optimization status, warnings, and background process updates."
val AppStrings.permStorageTitle: String get() = setupStorageTitle
val AppStrings.permStorageDesc: String get() = if (this === StringsId) "Simpan konfigurasi, profil, dan log aplikasi." else "Save app configuration, profiles, and logs."
val AppStrings.permBatteryTitle: String get() = if (this === StringsId) "Optimasi Baterai" else "Battery Optimization"
val AppStrings.permBatteryDesc: String get() = if (this === StringsId) "Cegah sistem membatasi proses Aether Manager." else "Prevent the system from limiting Aether Manager background tasks."
val AppStrings.permUsageTitle: String get() = if (this === StringsId) "Akses Penggunaan" else "Usage Access"
val AppStrings.permUsageDesc: String get() = if (this === StringsId) "Baca statistik aplikasi untuk mode per-aplikasi." else "Read app usage stats for per-app profiles."
val AppStrings.permWriteTitle: String get() = setupWriteTitle
val AppStrings.permWriteDesc: String get() = if (this === StringsId) "Terapkan tweak sistem, layar, dan performa." else "Apply system, display, and performance tweaks."

val AppStrings.setupFeaturedTitle: String get() = if (this === StringsId) "Fitur Unggulan" else "Featured Features"
val AppStrings.setupFeaturePerfTitle: String get() = if (this === StringsId) "Performa Maksimal" else "Maximum Performance"
val AppStrings.setupFeaturePerfDesc: String get() = if (this === StringsId) "Optimasi CPU, GPU, dan scheduler agar perangkat terasa lebih responsif." else "Optimize CPU, GPU, and scheduler behavior for a more responsive device."
val AppStrings.setupFeatureBatteryTitle: String get() = if (this === StringsId) "Manajemen Daya" else "Power Management"
val AppStrings.setupFeatureBatteryDesc: String get() = if (this === StringsId) "Profil hemat baterai tetap menjaga kestabilan performa harian." else "Battery-saving profiles keep daily performance stable."
val AppStrings.setupFeatureGamingTitle: String get() = if (this === StringsId) "Mode Gaming" else "Gaming Mode"
val AppStrings.setupFeatureGamingDesc: String get() = if (this === StringsId) "Prioritaskan resource untuk game dan kurangi gangguan proses latar belakang." else "Prioritize resources for games and reduce background process interruptions."

val AppStrings.setupPermissionsTitle: String get() = if (this === StringsId) "Izin Aplikasi" else "App Permissions"
val AppStrings.setupPermissionsDesc: String get() = if (this === StringsId) "Aktifkan izin inti secara bertahap. Root wajib untuk mode performa, sedangkan izin tambahan membantu monitoring dan optimasi berjalan stabil." else "Enable core permissions step by step. Root is required for performance mode, while optional permissions help monitoring and optimization stay stable."
val AppStrings.setupCompleteTitle: String get() = if (this === StringsId) "Setup Complete" else "Setup Complete"
val AppStrings.setupCompleteDesc: String get() = if (this === StringsId) "Aether Manager sudah siap digunakan. Root mode aktif, izin utama selesai, dan fitur monitoring siap berjalan di background." else "Aether Manager is ready. Root mode is active, key permissions are complete, and monitoring can run in the background."
val AppStrings.setupIncompleteLongDesc: String get() = if (this === StringsId) "Beberapa izin belum selesai. Kembali ke halaman izin lalu aktifkan kartu yang masih belum aktif." else "Some permissions are still missing. Go back to the permissions page and enable the inactive cards."
val AppStrings.setupPreparedTitle: String get() = if (this === StringsId) "Yang sudah disiapkan" else "What is ready"
val AppStrings.setupNotCompleteCardTitle: String get() = if (this === StringsId) "Setup belum lengkap" else "Setup is not complete"
val AppStrings.setupDetailRootTitle: String get() = if (this === StringsId) "Root Mode" else "Root Mode"
val AppStrings.setupDetailRootDesc: String get() = if (this === StringsId) "Kontrol performa dan tuning sistem siap dipakai dari dashboard." else "Performance control and system tuning are ready from the dashboard."
val AppStrings.setupDetailMonitorTitle: String get() = if (this === StringsId) "Monitoring Stabil" else "Stable Monitoring"
val AppStrings.setupDetailMonitorDesc: String get() = if (this === StringsId) "Status perangkat, aplikasi, dan proses background dapat dipantau lebih rapi." else "Device, app, and background process status can be monitored clearly."
val AppStrings.setupDetailOptimizeTitle: String get() = if (this === StringsId) "Optimasi Siap" else "Optimization Ready"
val AppStrings.setupDetailOptimizeDesc: String get() = if (this === StringsId) "Profil performa, baterai, dan gaming siap dijalankan sesuai kebutuhan." else "Performance, battery, and gaming profiles are ready when needed."

object RuntimeUiText {
    fun isId(context: Context): Boolean = loadSavedLanguage(context) == AppLanguage.INDONESIAN

    fun phoneRequired(context: Context) = if (isId(context)) "Device ID wajib tersedia" else "Device ID is required"
    fun phoneInvalid(context: Context) = if (isId(context)) "Device ID tidak valid" else "Invalid Device ID"
    fun phoneTooShort(context: Context) = if (isId(context)) "Device ID terlalu pendek" else "Device ID is too short"
    fun phoneTooLong(context: Context) = if (isId(context)) "Device ID terlalu panjang" else "Device ID is too long"
    fun nameRequired(context: Context) = if (isId(context)) "Nama harus diisi" else "Name is required"
    fun paymentFailed(context: Context, status: String) = if (isId(context)) "Pembayaran $status. Hubungi admin." else "Payment $status. Contact admin."
    fun paymentTimeout(context: Context) = if (isId(context)) "Timeout – belum ada konfirmasi" else "Timeout – no confirmation yet"
    fun rootNoOutput(context: Context) = if (isId(context)) "Tidak ada output — pastikan root aktif dan Shell.cmd() terhubung" else "No output — make sure root is active and Shell.cmd() is connected"
}

val AppStrings.licenseTimeoutTitle: String get() = if (this === StringsId) "Belum Dikonfirmasi" else "Not Confirmed Yet"
val AppStrings.licenseTimeoutBody: String get() = if (this === StringsId) "Pembayaranmu belum dikonfirmasi admin dalam 2 menit. Silakan hubungi admin untuk mempercepat proses." else "Your payment has not been confirmed by admin after 2 minutes. Contact admin to speed up the process."
val AppStrings.licenseWaitContinueBtn: String get() = if (this === StringsId) "Lanjut Tunggu" else "Keep Waiting"
val AppStrings.licenseAbortBtn: String get() = if (this === StringsId) "Batalkan" else "Cancel"
val AppStrings.licenseOfficialBadge: String get() = if (this === StringsId) "Official" else "Official"
val AppStrings.licenseNoAdsFallback: String get() = if (this === StringsId) "Tanpa Iklan" else "No Ads"
val AppStrings.licensePremiumTweaksFallback: String get() = if (this === StringsId) "Tweak premium" else "Premium tweaks"
val AppStrings.licenseAdminSupportFallback: String get() = if (this === StringsId) "Support admin" else "Admin support"
val AppStrings.licensePaymentPreviewNote: String get() = if (this === StringsId) "PayPal hanya untuk pembeli internasional. Pembeli Indonesia disarankan memakai DANA atau GoPay." else "PayPal is only for international buyers. Indonesian buyers should use DANA or GoPay."
val AppStrings.licenseIntlCreateTitle: String get() = "Create license invoice"
val AppStrings.licenseIntlCreateSubtitle: String get() = "Enter buyer name. Device ID is used automatically."
val AppStrings.licenseLocalCreateSubtitle: String get() = if (this === StringsId) "Isi nama pembeli untuk membuat order real. Device ID dipakai otomatis." else "Enter buyer name to create a real order. Device ID is used automatically."
val AppStrings.licenseIntlPaymentHint: String get() = "For international users, PayPal is recommended. Admin messages will use English."
val AppStrings.licenseLocalPaymentHint: String get() = if (this === StringsId) "Untuk Indonesia gunakan DANA/GoPay. Untuk luar Indonesia gunakan PayPal." else "Use DANA/GoPay for Indonesia. International buyers can use PayPal."
val AppStrings.licensePurchaseFlow: String get() = if (this === StringsId) "Alur pembelian" else "Purchase flow"
val AppStrings.licenseStepCreateInvoice: String get() = if (this === StringsId) "Buat invoice dari aplikasi" else "Create invoice from the app"
val AppStrings.licenseStepPayAvailable: String get() = if (this === StringsId) "Transfer sesuai nominal ke DANA, GoPay, atau PayPal" else "Pay using PayPal or an available method"
val AppStrings.licenseStepSendProof: String get() = if (this === StringsId) "Tap Sudah Bayar dan tunggu admin mengaktifkan lisensi" else "Send payment proof to Telegram admin"
val AppStrings.licenseIntlBuyerName: String get() = "Buyer name"
val AppStrings.licenseIntlPhoneHint: String get() = "Device ID otomatis dari perangkat"
val AppStrings.licenseIntlModeActive: String get() = "International mode active: payment instructions and admin confirmation template will be in English."
val AppStrings.licenseActivationAfterCheck: String get() = if (this === StringsId) "Pembayaran tidak dibuat palsu/instant. Lisensi aktif setelah pembayaran dicek dan disetujui admin." else "License is activated after your payment is checked and approved by admin."
val AppStrings.licenseManualPaymentNote: String get() = if (this === StringsId) "Transfer manual, lalu admin mengaktifkan lisensi setelah pembayaran valid." else "Manual payment. Admin will activate the license after the payment is verified."
val AppStrings.licenseRealProcess: String get() = if (this === StringsId) "Proses real" else "Real process"
val AppStrings.licenseStepChoosePay: String get() = if (this === StringsId) licenseSelectPayMethod else "Choose PayPal or another available method"
val AppStrings.licenseStepExactInvoice: String get() = if (this === StringsId) "Transfer tepat sesuai nominal invoice" else "Pay exactly according to the invoice"
val AppStrings.licenseStepConfirmPay: String get() = if (this === StringsId) "Tap Konfirmasi Pembayaran" else "Tap Confirm Payment"
val AppStrings.licenseStepTelegramProof: String get() = if (this === StringsId) "Kirim screenshot bukti transfer ke Telegram" else "Send payment screenshot to Telegram"
val AppStrings.licenseStepAdminVerify: String get() = if (this === StringsId) "Admin cek pembayaran dan lisensi aktif otomatis" else "Admin verifies and activates your license"
val AppStrings.licenseNoPaymentMethod: String get() = if (this === StringsId) "Metode pembayaran belum tersedia. Pastikan PaymentManager hanya mengirim DANA, GoPay, dan PayPal." else "Payment method is not available. Make sure PaymentManager only sends DANA, GoPay, and PayPal."
val AppStrings.licenseExactAmountTitle: String get() = if (this === StringsId) "Wajib transfer tepat" else "Pay the exact amount"
fun AppStrings.licenseExactAmountBody(amount: String): String = if (this === StringsId) "Transfer sebesar Rp $amount. Nominal berbeda bisa membuat proses verifikasi lebih lama." else "Please pay exactly Rp $amount or the equivalent amount requested by admin. Different amounts may slow down verification."
val AppStrings.licenseProofTitle: String get() = if (this === StringsId) licenseAfterTransferLabel else "Send payment proof"
val AppStrings.licenseProofBody: String get() = if (this === StringsId) "Setelah konfirmasi, Telegram admin akan dibuka. Paste pesan order lalu lampirkan screenshot bukti transfer." else "After confirmation, Telegram admin will open. Paste the copied order message and attach your payment screenshot."
val AppStrings.licenseIntlOnly: String get() = if (this === StringsId) "Khusus internasional" else "International only"
val AppStrings.licenseIndonesiaPayment: String get() = if (this === StringsId) "Pembayaran Indonesia" else "Indonesia payment"
val AppStrings.licensePaymentDestinationCopied: String get() = if (this === StringsId) "Tujuan pembayaran disalin" else "Payment destination copied"
val AppStrings.licensePaymentSubtitleEwallet: String get() = if (this === StringsId) "E-wallet Indonesia" else "Indonesian e-wallet"
val AppStrings.licensePaymentSubtitlePaypal: String get() = if (this === StringsId) "Khusus internasional" else "International only"
val AppStrings.licensePaymentDescPaypal: String get() = if (this === StringsId) "Gunakan PayPal hanya untuk pembeli dari luar Indonesia. Sertakan Order ID pada catatan pembayaran jika tersedia." else "Use PayPal only for international buyers. Include the Order ID in the payment note if available."
val AppStrings.licensePaymentDescDefault: String get() = if (this === StringsId) "Gunakan nama pembayaran yang sama dengan nama pembeli, lalu simpan bukti transfer sampai lisensi aktif." else "Use the same payment name as the buyer name, then keep the payment proof until the license is active."
val AppStrings.licenseProofCopied: String get() = if (this === StringsId) "Pesan konfirmasi disalin. Kirim screenshot bukti transfer ke Telegram admin." else "Confirmation message copied. Send your payment screenshot to Telegram admin."
val AppStrings.licenseSendProofChooser: String get() = if (this === StringsId) "Kirim bukti pembayaran" else "Send payment proof"
val AppStrings.licenseAskPaymentStatus: String get() = if (this === StringsId) "Halo Admin, saya ingin menanyakan status pembayaran Aether Manager Premium." else "Hello Admin, I want to ask about my Aether Manager Premium payment status."
val AppStrings.licensePremiumActiveBadge: String get() = if (this === StringsId) "Premium Aktif" else "Premium Active"

val AppStrings.appProfileListTitle: String get() = if (this === StringsId) "Daftar Aplikasi" else "App List"
fun AppStrings.appProfileAppsVisible(count: Int): String = if (this === StringsId) "$count aplikasi" else "$count apps"
val AppStrings.appProfileSearchApp: String get() = appProfileSearchHint
val AppStrings.appProfileCardTitle: String get() = appProfileTitle
fun AppStrings.appProfileMonitorSummary(enabled: Boolean, activeCount: Int, totalApps: Int): String =
    if (enabled) {
        if (this === StringsId) "$activeCount aktif • $totalApps aplikasi" else "$activeCount active • $totalApps apps"
    } else {
        if (this === StringsId) "Nonaktif • tap switch untuk mengaktifkan" else "Disabled • tap switch to enable"
    }
val AppStrings.appProfileEnableProfile: String get() = if (this === StringsId) "Aktifkan Profil" else "Enable Profile"
val AppStrings.appProfileEnabledForApp: String get() = if (this === StringsId) "Aktif untuk aplikasi ini" else "Active for this app"
val AppStrings.appProfileDisabledForApp: String get() = if (this === StringsId) "Nonaktif untuk aplikasi ini" else "Inactive for this app"
val AppStrings.appProfileNotSet: String get() = if (this === StringsId) "Belum Diatur" else "Not Set"

val AppStrings.splashSubtitle: String get() = if (this === StringsId) "Kontrol Cerdas • Boost Bersih • Tool Premium" else "Smart Control • Clean Boost • Premium Tools"
val AppStrings.splashPreparing: String get() = if (this === StringsId) "Menyiapkan workspace" else "Preparing your workspace"

fun RuntimeUiText.updateChannelName(context: Context) = if (isId(context)) "Update Aplikasi" else "App Update"
fun RuntimeUiText.updateChannelDesc(context: Context) = if (isId(context)) "Notifikasi saat ada versi baru Aether Manager" else "Notifications when a new Aether Manager version is available"
fun RuntimeUiText.licenseChannelName(context: Context) = if (isId(context)) "Lisensi Premium" else "Premium License"
fun RuntimeUiText.licenseChannelDesc(context: Context) = if (isId(context)) "Notifikasi lisensi expired atau hampir habis" else "Notifications when your license expires or is about to expire"
fun RuntimeUiText.generalChannelName(context: Context) = if (isId(context)) "Informasi Umum" else "General Information"
fun RuntimeUiText.generalChannelDesc(context: Context) = if (isId(context)) "Notifikasi umum Aether Manager" else "General Aether Manager notifications"
fun RuntimeUiText.updateShortNotes(context: Context) = if (isId(context)) "Versi baru tersedia. Tap untuk mengunduh." else "A new version is available. Tap to download."
fun RuntimeUiText.updateTitle(context: Context, version: String) = if (isId(context)) "Update Tersedia: $version" else "Update Available: $version"
fun RuntimeUiText.updateContent(context: Context) = if (isId(context)) "Versi baru Aether Manager siap diunduh" else "A new Aether Manager version is ready to download"
fun RuntimeUiText.updateBigTitle(context: Context, version: String) = if (isId(context)) "Update $version Tersedia" else "Update $version Available"
fun RuntimeUiText.licenseExpiredTitle(context: Context) = if (isId(context)) "⚠️ Lisensi Premium Kamu Expired" else "⚠️ Your Premium License Expired"
fun RuntimeUiText.licenseExpiredContent(context: Context) = if (isId(context)) "Perpanjang sekarang untuk tetap menikmati semua fitur Premium." else "Renew now to keep enjoying all Premium features."
fun RuntimeUiText.licenseExpiredBig(context: Context) = if (isId(context)) "Lisensi Premium kamu sudah berakhir.\n\nFitur Premium tidak lagi tersedia sampai kamu memperpanjang. Tap untuk langsung ke halaman Lisensi." else "Your Premium license has expired.\n\nPremium features are unavailable until you renew. Tap to open the License page."
fun RuntimeUiText.licenseExpiringTitle(context: Context, days: Int) = if (isId(context)) "⏳ Lisensi Hampir Berakhir ($days hari lagi)" else "⏳ License Ends Soon ($days days left)"
fun RuntimeUiText.licenseExpiringContent(context: Context, date: String) = if (isId(context)) "Perpanjang sebelum $date agar tidak terputus." else "Renew before $date to keep access uninterrupted."
fun RuntimeUiText.licenseExpiringBig(context: Context, days: Int, date: String) = if (isId(context)) "Premium kamu akan berakhir dalam $days hari ($date).\n\nPerpanjang sekarang agar semua fitur tetap aktif tanpa gangguan." else "Your Premium license will end in $days days ($date).\n\nRenew now so all features stay active without interruption."
fun RuntimeUiText.profileActive(context: Context, profile: String) = if (isId(context)) "Profil \"$profile\" aktif" else "Profile \"$profile\" is active"
fun RuntimeUiText.tweakApplied(context: Context) = if (isId(context)) "Semua tweak berhasil diterapkan" else "All tweaks applied successfully"
