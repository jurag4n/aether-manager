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
import com.aether.remote.AetherAdminSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class AetherApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val periodicSecurityCheck = object : Runnable {
        override fun run() {
            if (!BuildConfig.DEBUG) checkSecurity()
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
            checkSecurity()
            securityHandler.postDelayed(periodicSecurityCheck, SECURITY_INTERVAL_MS)
        }

        initLibsu()
        initUnityAds()

        NotificationHelper.createChannels(this)
        NotificationScheduler.schedule(this)

        appScope.launch { AetherAdminSync.sync(this@AetherApplication) }
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

    private fun checkSecurity() {
        try {
            if (!AetherSecurityNative.highCheck(this)) {
                showSecurityBlock(AetherSecurityNative.tamperReason(this))
            }
        } catch (_: Throwable) {
            showSecurityBlock("security_check_failed")
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
            InterstitialAdManager.preload(this)
            return
        }

        UnityAds.initialize(
            this,
            gameId,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    InterstitialAdManager.preload(this@AetherApplication)
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    Unit
                }
            }
        )
    }

    companion object {
        private const val SECURITY_INTERVAL_MS = 60_000L
    }
}
