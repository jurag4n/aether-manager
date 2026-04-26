package dev.aether.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.aether.manager.R
import dev.aether.manager.i18n.AppLanguage
import dev.aether.manager.i18n.getStringsForLanguage
import dev.aether.manager.i18n.loadSavedLanguage
import dev.aether.manager.util.RootUtils
import dev.aether.manager.util.TweakApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AetherService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "aether_service"
        const val NOTIF_ID   = 1001
        const val ACTION_REAPPLY = "dev.aether.manager.REAPPLY_TWEAKS"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        if (intent?.action == ACTION_REAPPLY) {
            scope.launch { reapplyTweaks("manual") }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─────────────────────────────────────────────────────────────────────────

    @Volatile private var isReapplying = false

    private suspend fun reapplyTweaks(reason: String) {
        if (isReapplying) return  // hindari concurrent shell execution
        isReapplying = true
        try {
            val tweaks = RootUtils.readTweaksConf()
            if (tweaks.isEmpty()) return
            TweakApplier.apply(tweaks)
        } catch (_: Exception) {
        } finally {
            isReapplying = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun strings() = getStringsForLanguage(
        loadSavedLanguage(this) ?: AppLanguage.fromSystemLocale(java.util.Locale.getDefault())
    )

    private fun createNotificationChannel() {
        val s = strings()
        val channel = NotificationChannel(
            CHANNEL_ID,
            s.serviceNotifChannelName,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = s.serviceNotifChannelDesc
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val s = strings()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(s.serviceNotifChannelName)
            .setContentText(s.serviceNotifText)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BootReceiver — apply tweaks on boot dengan delay adaptif
// Delay lebih panjang = lebih aman (kernel/HAL sudah stabil), tapi lebih lambat.
// Pakai boot_count: boot pertama lebih lambat (lebih hati-hati), boot selanjutnya lebih cepat.
// ─────────────────────────────────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "android.intent.action.QUICKBOOT_POWERON",   // HTC
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
        if (intent.action !in validActions) return

        // goAsync() supaya tidak timeout (max 10 detik di BroadcastReceiver)
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                applyOnBoot(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun applyOnBoot(context: Context) {

        // Increment boot count
        val bootCount = try {
            val raw = RootUtils.readFile(RootUtils.BOOT_COUNT_FILE).trim().toIntOrNull() ?: 0
            val next = raw + 1
            RootUtils.writeFile(RootUtils.BOOT_COUNT_FILE, next.toString())
            next
        } catch (_: Exception) { 1 }

        // Delay adaptif:
        // Boot ke-1 (fresh install / wipe): tunggu 20 detik — sistem belum settle
        // Boot ke-2..5: 15 detik
        // Boot ke-6+: 10 detik (sudah terbukti stabil)
        val delayMs = when {
            bootCount <= 1 -> 20_000L
            bootCount <= 5 -> 15_000L
            else           -> 10_000L
        }
        delay(delayMs)

        try {
            // Cek safe mode
            if (RootUtils.fileExists(RootUtils.SAFE_MODE_FILE)) {
                return
            }

            val tweaks = RootUtils.readTweaksConf()
            if (tweaks.isEmpty()) {
                return
            }

            val result = TweakApplier.apply(tweaks)

            // Retry sekali kalau ada yang gagal — beberapa node perlu waktu lebih
            if (!result.success) {
                delay(5_000)
                val retry = TweakApplier.apply(tweaks)
            }

        } catch (e: Exception) {
        }
    }
}
