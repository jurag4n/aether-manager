package dev.aether.manager.ads

import android.app.Activity
import dev.aether.manager.license.LicenseManager
import kotlinx.coroutines.*

object AdScheduler {

    var startDelayMs: Long  = 45 * 1_000L
    var intervalMs: Long    = 120 * 1_000L
    var minIntervalMs: Long = 90 * 1_000L

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // FIX: nilai awal diubah dari 30L ke 0L
    @Volatile private var lastShownMs: Long = 0L

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
        lastShownMs = System.currentTimeMillis()
        InterstitialAdManager.showIfReady(activity)
    }
}
