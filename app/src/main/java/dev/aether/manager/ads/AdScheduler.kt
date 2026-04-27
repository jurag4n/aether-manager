package dev.aether.manager.ads

import android.app.Activity
import dev.aether.manager.license.LicenseManager
import kotlinx.coroutines.*

object AdScheduler {

    var startDelayMs: Long  = 60L           // langsung tampil saat app buka
    var intervalMs: Long    = 120 * 1_000L
    var minIntervalMs: Long = 90 * 1_000L
    var crossAdGapMs: Long = 60 * 1_000L

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var lastShownMs: Long = 0L
    @Volatile private var lastUnityShownMs: Long = 0L
    @Volatile private var lastAdmobShownMs: Long = 0L
    @Volatile private var nextIsUnity: Boolean = true
    @Volatile private var activityProvider: (() -> Activity?)? = null

    fun start(provider: () -> Activity?) {
        activityProvider = provider
        if (job?.isActive == true) return
        job = scope.launch {
            delay(startDelayMs)
            while (isActive) {
                showNow()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        activityProvider = null
    }

    fun tryShow() {
        val now = System.currentTimeMillis()
        if (lastShownMs > 0L && now - lastShownMs < minIntervalMs) return
        showNow()
    }

    private fun showNow() {
        val activity = activityProvider?.invoke()
        if (activity == null || activity.isFinishing || activity.isDestroyed) return
        if (LicenseManager.isActive(activity)) return

        val now = System.currentTimeMillis()

        if (nextIsUnity) {
            val gapFromAdmob = now - lastAdmobShownMs
            if (lastAdmobShownMs > 0L && gapFromAdmob < crossAdGapMs) {
                // Jarak dari AdMob belum cukup — coba AdMob dulu
                showAdmob(activity, now)
                return
            }
            showUnity(activity, now)
        } else {
            val gapFromUnity = now - lastUnityShownMs
            if (lastUnityShownMs > 0L && gapFromUnity < crossAdGapMs) {
                // Jarak dari Unity belum cukup — coba Unity dulu
                showUnity(activity, now)
                return
            }
            showAdmob(activity, now)
        }
    }

    private fun showUnity(activity: Activity, now: Long) {
        if (!InterstitialAdManager.isReady()) {
            InterstitialAdManager.preload(activity)
            // Fallback ke AdMob kalau Unity belum siap
            showAdmob(activity, now)
            return
        }
        lastShownMs = now
        lastUnityShownMs = now
        nextIsUnity = false  // giliran berikutnya: AdMob
        InterstitialAdManager.showIfReady(activity)
    }

    private fun showAdmob(activity: Activity, now: Long) {
        if (!AdmobInterstitialManager.isReady()) {
            AdmobInterstitialManager.preload(activity)
            return
        }
        lastShownMs = now
        lastAdmobShownMs = now
        nextIsUnity = true   // giliran berikutnya: Unity
        AdmobInterstitialManager.showIfReady(activity)
    }
}
