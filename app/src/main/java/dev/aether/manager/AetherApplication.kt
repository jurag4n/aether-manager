package dev.aether.manager

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.AdMobInterstitialManager
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.notification.NotificationHelper
import dev.aether.manager.notification.NotificationScheduler

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        NativeAether.tryLoad()

        if (!BuildConfig.DEBUG) {
            checkSignature()
            checkAll()
        }

        initLibsu()
        CimolAgent.tryLoad()
        initUnityAds()
        initAdMob()

        NotificationHelper.createChannels(this)
        NotificationScheduler.schedule(this)
    }

    private fun checkSignature() {
        if (!NativeAether.isLoaded) return
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
        } catch (_: Exception) {}
    }

    private fun checkAll() {
        if (!NativeAether.isLoaded) return
        try {
            NativeAether.nativeCheckAll(this)
        } catch (_: Throwable) {
            NativeAether.nativeKillProcess()
        }
    }

    private fun initLibsu() {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    private fun initUnityAds() {
        val gameId = AdManager.GAME_ID
        if (gameId.isEmpty()) {
            AdMobInterstitialManager.preload(this)
            return
        }

        if (UnityAds.isInitialized) {
            if (!BuildConfig.DEBUG) checkUnityIntact()
            InterstitialAdManager.preload(this)
            return
        }

        UnityAds.initialize(
            this,
            gameId,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    if (!BuildConfig.DEBUG) checkUnityIntact()
                    InterstitialAdManager.preload(this@AetherApplication)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    if (!BuildConfig.DEBUG &&
                        error != UnityAds.UnityAdsInitializationError.INTERNAL_ERROR
                    ) {
                        checkUnityIntact()
                    }
                }
            }
        )
    }

    private fun checkUnityIntact() {
        if (!NativeAether.isLoaded) return
        try {
            NativeAether.nativeCheckUnityIntact()
        } catch (_: Exception) {
            NativeAether.nativeKillProcess()
        }
    }

    private fun initAdMob() {
        MobileAds.initialize(this) {
            AdMobInterstitialManager.preload(this)
        }
    }
}
