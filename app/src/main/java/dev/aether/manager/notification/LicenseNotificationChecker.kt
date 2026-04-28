package dev.aether.manager.notification

import android.content.Context
import dev.aether.manager.license.LicensePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cek status lisensi dan tampilkan notifikasi bila perlu.
 *
 * Panggil [check] di:
 *  - MainActivity.onResume() — setiap buka app
 *  - WorkManager periodic job — sekali sehari di background
 */
object LicenseNotificationChecker {

    /**
     * Threshold: notifikasi "hampir expired" mulai muncul saat sisa <= N hari.
     */
    private const val EXPIRING_SOON_DAYS = 3

    /**
     * Periksa status lisensi dan kirim notifikasi yang sesuai.
     *
     * @param context Application context
     * @param forceCheck Kalau true, cek meski sudah pernah notif hari ini
     */
    fun check(context: Context, forceCheck: Boolean = false) {
        val key    = LicensePrefs.getKey(context) ?: return   // belum pernah aktivasi → skip
        if (key.isBlank()) return

        val expiry = LicensePrefs.getExpiry(context)

        when {
            expiry == -1L -> {
                // Lifetime license — tidak perlu notifikasi apapun
                NotificationHelper.cancelLicenseExpired(context)
                NotificationHelper.cancelLicenseExpiring(context)
            }

            expiry <= 0L -> {
                // Data rusak atau belum ada expiry
                return
            }

            System.currentTimeMillis() >= expiry -> {
                // Sudah expired
                NotificationHelper.cancelLicenseExpiring(context)
                NotificationHelper.showLicenseExpired(context)
            }

            else -> {
                // Masih aktif — cek apakah hampir habis
                val millisLeft = expiry - System.currentTimeMillis()
                val daysLeft   = TimeUnit.MILLISECONDS.toDays(millisLeft).toInt()

                NotificationHelper.cancelLicenseExpired(context)

                if (daysLeft <= EXPIRING_SOON_DAYS) {
                    val expiryDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                        .format(Date(expiry))
                    NotificationHelper.showLicenseExpiringSoon(context, daysLeft, expiryDate)
                } else {
                    // Masih banyak hari — hapus notif expiring kalau ada sisa dari sebelumnya
                    NotificationHelper.cancelLicenseExpiring(context)
                }
            }
        }
    }
}
