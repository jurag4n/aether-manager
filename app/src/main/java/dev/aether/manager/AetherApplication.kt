package dev.aether.manager

import android.app.Application
import android.content.Intent
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
import dev.aether.manager.security.AetherSecurityNative
import dev.aether.manager.security.SecurityBlockActivity
import dev.aether.manager.util.UiNativeBoost
import dev.aether.manager.util.UiPerformanceMonitor
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
            if (NativeAether.nativeIsHooked()) {
                showSecurityBlock("hook_detected"); return
            }
            if (NativeAether.nativeIsDebugged()) {
                showSecurityBlock("debugger_detected"); return
            }
            if (!NativeAether.nativeCheckAntiPatch(this)) {
                showSecurityBlock("patch_detected"); return
            }
            if (NativeAether.nativeCheckCloner(this)) {
                showSecurityBlock("cloner_detected"); return
            }
            if (!NativeAether.nativeCheckElfIntegrity()) {
                showSecurityBlock("elf_tamper"); return
            }
            if (NativeAether.nativeCheckGotHook()) {
                showSecurityBlock("got_hook_detected"); return
            }
        } catch (_: Throwable) {
            if (NativeAether.isLoaded) showSecurityBlock("security_check_failed")
        }
    }

    private fun initLibsu() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        dev.aether.manager.util.RootManager.configureShell(this)

        val prefs = getSharedPreferences("aether_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("setup_done", false)) {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                val ok = dev.aether.manager.util.RootManager.ensureRootShellSync(requestIfNeeded = false)
                if (ok) dev.aether.manager.util.RootManager.markGranted()
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
