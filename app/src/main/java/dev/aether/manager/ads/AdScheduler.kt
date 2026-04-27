package dev.aether.manager.ads

import android.app.Activity
import dev.aether.manager.license.LicenseManager
import kotlinx.coroutines.*

object AdScheduler {

    // ── Timing config ──────────────────────────────────────────────────────
    // startDelayMs  : jeda setelah ON_RESUME sebelum iklan pertama tampil
    // intervalMs    : jeda antar iklan dalam satu sesi
    // minIntervalMs : jeda minimum antar dua tampilan iklan (guard tryShow)
    // sessionCooldownMs : jeda minimum setelah resume sebelum tryShow boleh jalan
    var startDelayMs: Long      = 60 * 1_000L      // 60 s  (was 45 s)
    var intervalMs: Long        = 5 * 60 * 1_000L  // 5 min (was 2 min)
    var minIntervalMs: Long     = 4 * 60 * 1_000L  // 4 min (was 90 s)
    var sessionCooldownMs: Long = 60 * 1_000L      // 60 s cooldown setelah ON_RESUME

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var lastShownMs: Long    = 0L
    @Volatile private var sessionStartMs: Long = 0L

    @Volatile private var activityProvider: (() -> Activity?)? = null

    fun start(provider: () -> Activity?) {
        activityProvider = provider
        sessionStartMs   = System.currentTimeMillis()
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
     * Panggil hanya pada event navigasi yang signifikan (bukan tiap ganti tab).
     * Guard: sessionCooldownMs + minIntervalMs harus terpenuhi keduanya.
     */
    fun tryShow() {
        val now = System.currentTimeMillis()
        if (now - sessionStartMs < sessionCooldownMs) return
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
