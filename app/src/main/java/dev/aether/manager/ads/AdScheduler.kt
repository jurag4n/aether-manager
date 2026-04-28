package dev.aether.manager.ads

import android.app.Activity
import dev.aether.manager.license.LicenseManager
import kotlinx.coroutines.*

object AdScheduler {

    // Iklan pertama muncul 40 detik setelah app dibuka
    var startDelayMs: Long  = 20 * 1_000L
    // Interval antar iklan: 2.5 menit
    var intervalMs: Long    = 1 * 60 * 1_000L + 30 * 1_000L
    // Minimum jarak antar iklan (guard anti-spam)
    var minIntervalMs: Long = 2 * 60 * 1_000L

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var lastShownMs: Long = 0L
    @Volatile private var activityProvider: (() -> Activity?)? = null

    /**
     * Mulai scheduler otomatis.
     * Jika job sudah aktif (misalnya ON_RESUME dipanggil ulang setelah notifikasi),
     * TIDAK restart dari awal — timer tetap berjalan dari posisi sebelumnya.
     */
    fun start(provider: () -> Activity?) {
        activityProvider = provider
        // Jika job masih aktif, hanya update provider — jangan restart timer
        if (job?.isActive == true) return
        job = scope.launch {
            // Kalau sudah pernah tampil, mulai dari sisa interval — bukan dari awal
            val sinceLastMs = if (lastShownMs > 0L)
                System.currentTimeMillis() - lastShownMs
            else 0L
            val initialDelay = if (sinceLastMs < intervalMs)
                (intervalMs - sinceLastMs).coerceAtLeast(0L)
            else
                startDelayMs
            delay(initialDelay)
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
