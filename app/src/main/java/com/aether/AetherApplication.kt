package com.aether

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.aether.ads.AdManager
import com.aether.ads.InterstitialAdManager
import com.aether.notification.NotificationHelper
import com.aether.notification.NotificationScheduler
import com.aether.security.AetherSecurityNative
import com.aether.security.SecurityBlockActivity
import com.aether.util.UiNativeBoost
import com.aether.util.UiPerformanceMonitor
import kotlin.system.exitProcess

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
        UiNativeBoost.init(this)
        if (BuildConfig.DEBUG) timber.log.Timber.plant(timber.log.Timber.DebugTree())
        UiPerformanceMonitor.install(this)

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

    @Volatile private var securityBlocked = false

    private fun showSecurityBlock(reason: String) {
        if (securityBlocked) return
        securityBlocked = true
        securityHandler.removeCallbacks(periodicSecurityCheck)
        val intent = Intent(this, SecurityBlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(SecurityBlockActivity.EXTRA_REASON, reason)
        runCatching { startActivity(intent) }.onFailure {
            try {
                android.os.Process.killProcess(android.os.Process.myPid())
            } finally {
                exitProcess(10)
            }
        }
    }

    private fun checkSignature() {
        try {
            if (!AetherSecurityNative.verifyAppSignature(this)) showSecurityBlock("signature_mismatch")
        } catch (_: Throwable) {
            showSecurityBlock("signature_check_failed")
        }
    }

    private fun runSecurityChecks() {
        if (!NativeAether.isLoaded) return
        try {
            val jembutOk = AetherSecurityNative.highCheck(this)
            if (!jembutOk) {
                showSecurityBlock(AetherSecurityNative.tamperReason(this)); return
            }
            // Native hook detection is intentionally strict and reason-based.
            // LSPosed, Zygisk, and Riru are allowed. Frida/injector artifacts still block.
            if (NativeAether.nativeIsHooked()) {
                showSecurityBlock("frida_or_injector_detected"); return
            }
            if (NativeAether.nativeIsDebugged()) {
                showSecurityBlock("debugger_detected"); return
            }
            if (!NativeAether.nativeCheckAntiPatch(this)) {
                showSecurityBlock("lucky_patcher_or_lspatch_detected"); return
            }
            if (NativeAether.nativeCheckCloner(this)) {
                showSecurityBlock("cloner_detected"); return
            }
            if (!NativeAether.nativeCheckElfIntegrity()) {
                showSecurityBlock("elf_tamper"); return
            }
            // Generic GOT-hook checks are disabled in native layer to avoid false positives
            // from allowed frameworks. Keep this only as a compatibility guard.
            if (!NativeAether.nativeCheckGotHook()) {
                showSecurityBlock("native_integrity_tamper"); return
            }
        } catch (_: Throwable) {
            if (NativeAether.isLoaded) showSecurityBlock("security_check_failed")
        }
    }

    private fun initLibsu() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        com.aether.util.RootManager.configureShell(this)

        val prefs = getSharedPreferences("aether_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("setup_done", false)) {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                val ok = com.aether.util.RootManager.ensureRootShellSync(requestIfNeeded = false)
                if (ok) com.aether.util.RootManager.markGranted()
            }
        }
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
            if (NativeAether.isLoaded) showSecurityBlock("unity_tamper")
        }
    }

    companion object {
        private const val SECURITY_INTERVAL_MS = 45_000L
    }
}
