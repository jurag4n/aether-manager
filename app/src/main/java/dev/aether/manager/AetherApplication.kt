package dev.aether.manager

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager
import com.google.android.gms.ads.MobileAds
import dev.aether.manager.ads.AdmobInterstitialManager

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Load native library PERTAMA sebelum apapun yang bergantung padanya.
        NativeAether.tryLoad()

        // Security check hanya aktif di RELEASE build yang benar-benar signed.
        if (!BuildConfig.DEBUG) {
            checkSignature()
            checkAll()
        }

        initLibsu()
        CimolAgent.tryLoad()

        // FIX: Wrap ads init dalam try-catch agar exception dari SDK
        // tidak menyebabkan FC sebelum SplashActivity sempat dibuka.
        runCatching { initUnityAds() }.onFailure {
            Log.e("AetherApp", "Unity Ads init failed", it)
        }
        runCatching { initAdmob() }.onFailure {
            Log.e("AetherApp", "AdMob init failed", it)
        }
    }

    // ─── Security checks ──────────────────────────────────────────────────────

    private fun checkSignature() {
        if (!NativeAether.tryLoad()) return
        try {
            @Suppress("DEPRECATION")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                PackageManager.GET_SIGNING_CERTIFICATES
            else
                PackageManager.GET_SIGNATURES

            val info = packageManager.getPackageInfo(packageName, flags)

            val sigBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }

            if (sigBytes == null) return

            val hex = java.security.MessageDigest.getInstance("SHA-256")
                .digest(sigBytes).joinToString("") { "%02x".format(it) }

            NativeAether.nativeCheckSignature(hex)
        } catch (_: Exception) {
            // Jangan kill — bisa false positive Samsung Knox / binder error
        }
    }

    private fun checkAll() {
        if (!NativeAether.tryLoad()) return
        try {
            NativeAether.nativeCheckAll(this)
        } catch (_: Throwable) {
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

    private fun initUnityAds() {
        // FIX: Ambil GAME_ID dengan runCatching — jika .so tidak ada,
        // nativeGetGameId() lempar UnsatisfiedLinkError dan ditangkap di sini.
        val gameId = runCatching { AdManager.GAME_ID }.getOrDefault("")
        if (gameId.isEmpty()) return  // .so tidak tersedia — skip Unity init

        if (UnityAds.isInitialized) {
            if (!BuildConfig.DEBUG) runCatching { checkUnityIntact() }
            runCatching { InterstitialAdManager.preload(this) }
            return
        }

        UnityAds.initialize(
            this,
            gameId,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    if (!BuildConfig.DEBUG) runCatching { checkUnityIntact() }
                    runCatching { InterstitialAdManager.preload(this@AetherApplication) }
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    if (!BuildConfig.DEBUG &&
                        error != UnityAds.UnityAdsInitializationError.INTERNAL_ERROR) {
                        runCatching { checkUnityIntact() }
                    }
                }
            }
        )
    }

    // ─── AdMob ────────────────────────────────────────────────────────────────

    private fun initAdmob() {
        // FIX: MobileAds.initialize() kadang lempar exception internal pada device
        // tanpa GMS terbaru. Wrap preload() dalam runCatching agar aman.
        MobileAds.initialize(this) {
            runCatching {
                AdmobInterstitialManager.preload(this)
            }.onFailure {
                Log.e("AetherApp", "AdMob preload failed", it)
            }
        }
    }

    private fun checkUnityIntact() {
        if (!NativeAether.tryLoad()) return
        try {
            NativeAether.nativeCheckUnityIntact()
        } catch (_: Throwable) {
            NativeAether.nativeKillProcess()
        }
    }
}
