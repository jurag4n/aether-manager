package dev.aether.manager

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.notification.NotificationHelper
import dev.aether.manager.notification.NotificationScheduler

class AetherApplication : Application() {

    // Background thread for security checks — avoids ANR from l1_frida_port()
    // which opens 3 socket connections on each call.
    private val securityExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "aether-sec").also { it.isDaemon = true }
    }

    private val periodicSecurityCheck = object : Runnable {
        override fun run() {
            if (!BuildConfig.DEBUG) {
                securityExecutor.execute { runSecurityChecks() }
            }
            securityHandler.postDelayed(this, SECURITY_INTERVAL_MS)
        }
    }

    private val securityHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        NativeAether.tryLoad(this)

        if (!BuildConfig.DEBUG) {
            // checkSignature runs on main thread (no I/O, just crypto) — fine
            checkSignature()
            // All other checks go to background: l1_frida_port() does 3 socket
            // connects per call; running on main thread risks ANR.
            securityExecutor.execute {
                runSecurityChecks()
                securityHandler.postDelayed(periodicSecurityCheck, SECURITY_INTERVAL_MS)
            }
        }

        initLibsu()
        initUnityAds()

        NotificationHelper.createChannels(this)
        NotificationScheduler.schedule(this)
    }

    private fun killSelf() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun checkSignature() {
        // Safety guard: if EXPECTED_SIGNATURE is still the placeholder, skip the
        // check entirely. Without this, every release build dies immediately because
        // no real cert hash can ever match the placeholder string.
        if (EXPECTED_SIGNATURE == SIGNATURE_PLACEHOLDER) return

        if (!NativeAether.isLoaded) { killSelf(); return }
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

            if (sigBytes == null) { killSelf(); return }

            val hex = java.security.MessageDigest.getInstance("SHA-256")
                .digest(sigBytes)
                .joinToString("") { "%02x".format(it) }

            if (hex != EXPECTED_SIGNATURE) killSelf()
        } catch (_: Throwable) {
            killSelf()
        }
    }

    private fun runSecurityChecks() {
        if (!NativeAether.isLoaded) return
        try {
            if (NativeAether.nativeIsHooked()) {
                killSelf(); return
            }
            if (NativeAether.nativeIsDebugged()) {
                killSelf(); return
            }
            if (!NativeAether.nativeCheckAntiPatch(this)) {
                killSelf(); return
            }
        } catch (_: Throwable) {
            if (NativeAether.isLoaded) killSelf()
        }
    }

    private fun initLibsu() {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
    }

    private fun initUnityAds() {
        val gameId = AdManager.GAME_ID
        if (gameId.isEmpty()) return

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
                    if (!BuildConfig.DEBUG) checkUnityIntact()
                }
            }
        )
    }

    private fun checkUnityIntact() {
        if (!NativeAether.isLoaded) return
        try {
            NativeAether.nativeCheckUnityIntact()
        } catch (_: Exception) {
            if (NativeAether.isLoaded) killSelf()
        }
    }

    companion object {
        private const val SECURITY_INTERVAL_MS = 45_000L

        // Replace EXPECTED_SIGNATURE with the actual SHA-256 hex of your signing cert.
        // To get it: run  `apksigner verify --print-certs your.apk`  and copy the
        // "Signer #1 certificate SHA-256 digest" value (lowercase hex, 64 chars).
        // Until it's set, checkSignature() is skipped (see SIGNATURE_PLACEHOLDER guard).
        private const val SIGNATURE_PLACEHOLDER = "GANTI_DENGAN_SHA256_SIGNING_CERT_KAMU"
        private const val EXPECTED_SIGNATURE    = SIGNATURE_PLACEHOLDER
    }
}
