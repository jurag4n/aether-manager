package dev.aether.manager

import android.app.Application
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.CimolAgent
import dev.aether.manager.notification.NotificationHelper
import dev.aether.manager.notification.NotificationScheduler

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Load libaether.so PERTAMA — sebelum apapun yang depend ke NativeAether.
        // Wajib eksplisit di sini karena checkAll() hanya dipanggil di RELEASE build.
        // Tanpa ini, di RELEASE build pun urutan eksekusi:
        //   checkAll() → tryLoad() [load] → initUnityAds() → GAME_ID → tryLoad() [sudah loaded, ok]
        // sudah benar karena tryLoad() sekarang idempotent. Tapi eksplisit lebih aman
        // dan memastikan library ready sebelum checkAll() dipanggil.
        NativeAether.tryLoad()

        // Security check hanya aktif di RELEASE build yang benar-benar signed.
        // Di debug build, semua check di-skip agar tidak FC saat development/testing.
        // checkSignature() DIHAPUS — sudah inline di dalam nativeCheckAll() di C,
        // tidak bisa di-bypass dari Kotlin level oleh Lucky Patcher.
        if (!BuildConfig.DEBUG) {
            checkAll()
        }

        initLibsu()
        CimolAgent.tryLoad()
        initUnityAds()

        // ── Buat notification channels sekali di awal ──────────────────────
        // Harus dipanggil sebelum notifikasi pertama dikirim.
        // Aman dipanggil berulang (Android no-op kalau channel sudah ada).
        NotificationHelper.createChannels(this)
        NotificationScheduler.schedule(this)
    }

    // ─── Security checks ──────────────────────────────────────────────────────
    // checkSignature() DIHAPUS — sudah dilakukan di dalam nativeCheckAll() (C layer).
    // Memindahkan ke native mencegah Lucky Patcher bypass via Kotlin bytecode patch.

    private fun checkAll() {
        if (!NativeAether.tryLoad()) return
        try {
            NativeAether.nativeCheckAll(this)
        } catch (_: Exception) {
            NativeAether.nativeKillProcess()
        }
    }

    // ─── libsu ────────────────────────────────────────────────────────────────

    private fun initLibsu() {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    // ─── Unity Ads ────────────────────────────────────────────────────────────
    // checkUnityIntact() DIHAPUS — sudah dicek di nativeCheckAll() (Layer 5)
    // dan background watcher thread di C. Memanggil dari Kotlin hanya membuka
    // celah bagi LP untuk patch call-site ini.

    private fun initUnityAds() {
        if (UnityAds.isInitialized) {
            InterstitialAdManager.preload(this)
            return
        }

        UnityAds.initialize(
            this,
            AdManager.GAME_ID,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    InterstitialAdManager.preload(this@AetherApplication)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    // Tidak perlu kill di sini — watcher thread akan catch kalau Unity di-strip
                }
            }
        )
    }
}
