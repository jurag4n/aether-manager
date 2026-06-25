package com.aether

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.aether.ads.AdManager
import com.aether.ads.InterstitialAdManager
import com.aether.notification.NotificationHelper
import com.aether.notification.NotificationScheduler
import com.aether.security.AetherSecurityNative
import com.aether.util.UiNativeBoost
import com.aether.util.UiPerformanceMonitor
import com.aether.remote.AetherAdminSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AetherApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        NativeAether.tryLoad(this)
        UiNativeBoost.init(this)
        if (BuildConfig.DEBUG) timber.log.Timber.plant(timber.log.Timber.DebugTree())
        UiPerformanceMonitor.install(this)

        runSecurityStartupCheck()
        initLibsu()
        initUnityAds()

        NotificationHelper.createChannels(this)
        NotificationScheduler.schedule(this)

        appScope.launch { AetherAdminSync.sync(this@AetherApplication) }
    }

    private fun runSecurityStartupCheck() {
        val status = runCatching { AetherSecurityNative.startupStatus(this) }
            .getOrElse { err ->
                AetherSecurityNative.SecurityStatus(
                    ok = false,
                    code = "security_runtime_error",
                    title = "Security runtime error",
                    message = err.message ?: err.javaClass.simpleName,
                    fix = "Cek logcat tag AetherSecurity. Aplikasi tetap dibuka agar tidak force close."
                )
            }

        getSharedPreferences("aether_security", MODE_PRIVATE)
            .edit()
            .putBoolean("security_status_ok", status.ok)
            .putString("security_status_code", status.code)
            .putString("security_status_title", status.title)
            .putString("security_status_message", status.message)
            .putString("security_status_fix", status.fix)
            .putString("security_current_sha256", status.currentSha256)
            .apply()

        if (status.ok) {
            Log.i("AetherSecurity", "${status.code}: ${status.message} ${status.currentSha256}".trim())
        } else {
            Log.w("AetherSecurity", "${status.code}: ${status.message}")
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
}
