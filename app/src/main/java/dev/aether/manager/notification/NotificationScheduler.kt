package dev.aether.manager.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker background yang berjalan ~1x sehari untuk:
 * 1. Cek update baru dari GitHub → notifikasi di status bar
 * 2. Cek status lisensi → notifikasi expired / hampir habis
 *
 * Cara daftarkan (panggil di Application.onCreate atau MainActivity):
 * ```
 * NotificationScheduler.schedule(this)
 * ```
 */
class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Cek & notifikasi update
        UpdateNotificationHelper.checkAndNotify(context)

        // 2. Cek & notifikasi lisensi
        LicenseNotificationChecker.check(context)

        return Result.success()
    }
}

object NotificationScheduler {

    private const val WORK_NAME = "aether_daily_notification_check"

    /**
     * Jadwalkan pengecekan harian.
     * Aman dipanggil berulang — KEEP akan skip kalau sudah ada job yang berjalan.
     *
     * @param requireNetwork true = hanya jalan saat ada internet (default untuk cek update)
     */
    fun schedule(context: Context, requireNetwork: Boolean = true) {
        val constraints = Constraints.Builder()
            .apply {
                if (requireNetwork) setRequiredNetworkType(NetworkType.CONNECTED)
            }
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,   // jangan replace kalau sudah ada
            request
        )
    }

    /** Batalkan semua job notifikasi (misalnya saat user uninstall / logout). */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
