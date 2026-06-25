package com.aether.ads

import android.app.Activity
import com.aether.license.LicenseManager
import kotlinx.coroutines.*

object AdScheduler {

    // Iklan pertama muncul lebih lambat setelah app dibuka untuk mengurangi kesan spam
    var startDelayMs: Long  = 90 * 1_000L
    // Interval antar iklan diperpanjang agar penayangan iklan tidak terlalu sering
    var intervalMs: Long    = 3 * 60 * 1_000L  // 3 menit
    // Minimum jarak antar iklan (guard anti-spam) — harus <= intervalMs
    // agar tryShowAfterAction tidak selalu diblokir oleh scheduler yang baru show
    var minIntervalMs: Long = 3 * 60 * 1_000L

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
            // Kalau belum pernah tampil (lastShownMs == 0), gunakan startDelayMs
            val initialDelay = if (lastShownMs > 0L) {
                val sinceLastMs = System.currentTimeMillis() - lastShownMs
                (intervalMs - sinceLastMs).coerceAtLeast(0L)
            } else {
                startDelayMs
            }
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
