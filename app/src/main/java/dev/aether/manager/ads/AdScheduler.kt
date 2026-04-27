package dev.aether.manager.ads

import android.app.Activity
import dev.aether.manager.license.LicenseManager
import kotlinx.coroutines.*

object AdScheduler {

    // Iklan pertama muncul 60 detik setelah app dibuka
    var startDelayMs: Long  = 60 * 1_000L
    // Interval antar iklan: 5 menit
    var intervalMs: Long    = 5 * 60 * 1_000L
    // Minimum jarak antar iklan: 4 menit (guard anti-spam)
    var minIntervalMs: Long = 4 * 60 * 1_000L

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var lastShownMs: Long = 0L
    @Volatile private var activityProvider: (() -> Activity?)? = null

    /**
     * Mulai scheduler otomatis.
     * Iklan akan muncul sendiri sesuai interval tanpa perlu dipicu manual.
     */
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

    /**
     * Hanya dipakai untuk trigger manual yang benar-benar diinginkan
     * (contoh: setelah user selesai aksi penting seperti apply tweak).
     * Jangan dipanggil saat ganti tab biasa.
     */
    fun tryShowAfterAction() {
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
