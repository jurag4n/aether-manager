package dev.aether.manager

/**
 * NativeAether — JNI bridge ke libaether.so
 *
 * Menggantikan NativeProtect.kt (libprotect.so).
 *
 * Semua fungsi external dipetakan ke JNI exports di libaether.cpp.
 * Nama library: "aether"  →  libaether.so
 */
object NativeAether {

    @Volatile private var loaded = false

    /**
     * Load libaether.so. Dipanggil sekali di AetherApplication.onCreate().
     * Idempotent — aman dipanggil berkali-kali, library hanya di-load sekali.
     * @return true jika berhasil dimuat, false jika tidak tersedia.
     */
    fun tryLoad(): Boolean {
        if (loaded) return true
        return runCatching {
            System.loadLibrary("aether")
            loaded = true
            true
        }.getOrDefault(false)
    }

    // ── Integrity ────────────────────────────────────────────────────────────

    /**
     * Verifikasi signature APK vs expected yang di-hardcode di C.
     * Jika mismatch → SIGKILL dari C (tidak return ke Kotlin).
     */
    external fun nativeCheckSignature(sigHashHex: String): Boolean

    // ── Anti-patch ───────────────────────────────────────────────────────────

    /**
     * Cek repack / Lucky Patcher (ZIP integrity, DEX magic, LP behavioral).
     * Jika terdeteksi → SIGKILL dari C.
     * @return true = clean.
     */
    external fun nativeCheckAntiPatch(ctx: android.content.Context): Boolean

    /**
     * Verifikasi Unity class + Unity strings masih ada setelah Unity init.
     * Panggil setelah UnityAds.initialize() selesai.
     * Jika gagal → SIGKILL dari C.
     */
    external fun nativeCheckUnityIntact(): Boolean

    // ── Master check — satu call atom ────────────────────────────────────────

    /**
     * Jalankan semua layer security sekaligus (hook + debug + repack + LP).
     * Lebih efisien dari memanggil satu-per-satu.
     * Jika ada yang gagal → SIGKILL dari C, tidak return.
     * @return true selalu jika selamat sampai sini.
     */
    external fun nativeCheckAll(ctx: android.content.Context): Boolean

    // ── Utilities ────────────────────────────────────────────────────────────

    /** SIGKILL dari C. Silent, tidak return. */
    external fun nativeKillProcess()

    // ── String vault ─────────────────────────────────────────────────────────

    /** Unity Ads Game ID (XOR-decoded dari C). */
    external fun nativeGetGameId(): String

    /** GitHub API URL (XOR-decoded dari C). */
    external fun nativeGetGithubApi(): String

    /** DNS-level adblock keyword list (XOR-decoded dari C). */
    external fun nativeGetAdblockDnsKeywords(): Array<String>

    /** Hosts-file adblock signature list (XOR-decoded dari C). */
    external fun nativeGetHostsSignatures(): Array<String>
}
