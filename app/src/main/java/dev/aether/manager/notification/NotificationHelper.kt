package dev.aether.manager.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import dev.aether.manager.MainActivity

/**
 * NotificationHelper — sistem notifikasi terpusat untuk AE Manager.
 *
 * Channel yang dibuat:
 *  - CHANNEL_UPDATE   : Update tersedia (importance HIGH → bunyi + status bar)
 *  - CHANNEL_LICENSE  : Lisensi expired / hampir expired (importance HIGH)
 *  - CHANNEL_GENERAL  : Info umum (importance DEFAULT)
 *
 * Semua channel pakai suara default sistem.
 */
object NotificationHelper {

    // ─── Channel IDs ─────────────────────────────────────────────────────────

    const val CHANNEL_UPDATE  = "aether_update"
    const val CHANNEL_LICENSE = "aether_license"
    const val CHANNEL_GENERAL = "aether_general"

    // ─── Notification IDs ────────────────────────────────────────────────────

    const val NOTIF_UPDATE_AVAILABLE = 2001
    const val NOTIF_LICENSE_EXPIRED  = 2002
    const val NOTIF_LICENSE_EXPIRING = 2003
    const val NOTIF_GENERAL          = 2004
    const val NOTIF_TWEAK_APPLIED    = 2005

    // ─── Channel setup ───────────────────────────────────────────────────────

    /**
     * Panggil sekali di Application.onCreate() sebelum notifikasi pertama dikirim.
     */
    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Update channel — HIGH agar muncul di status bar + bunyi default
        val updateChannel = NotificationChannel(
            CHANNEL_UPDATE,
            "App Update",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description       = "Notifikasi saat ada versi baru Aether Manager"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }

        // License channel — HIGH
        val licenseChannel = NotificationChannel(
            CHANNEL_LICENSE,
            "Lisensi Premium",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description       = "Notifikasi lisensi expired atau hampir habis"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }

        // General channel — DEFAULT
        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "Informasi Umum",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description   = "Notifikasi umum Aether Manager"
            setShowBadge(false)
        }

        nm.createNotificationChannels(listOf(updateChannel, licenseChannel, generalChannel))
    }

    // ─── Notifikasi: Update Tersedia ─────────────────────────────────────────

    /**
     * Tampilkan notifikasi update di status bar.
     *
     * @param versionName  Nama versi baru, contoh "v2.1.0"
     * @param releaseNotes Ringkasan changelog (boleh kosong)
     */
    fun showUpdateAvailable(
        context: Context,
        versionName: String,
        releaseNotes: String = ""
    ) {
        val intent = mainActivityIntent(context)
        val pi = PendingIntent.getActivity(
            context, NOTIF_UPDATE_AVAILABLE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortNotes = if (releaseNotes.isNotBlank())
            releaseNotes.lines().take(3).joinToString("\n").trim()
        else
            "Versi baru tersedia. Tap untuk mengunduh."

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Update Tersedia: $versionName")
            .setContentText("Versi baru Aether Manager siap diunduh")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(shortNotes)
                    .setBigContentTitle("Update $versionName Tersedia")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        notify(context, NOTIF_UPDATE_AVAILABLE, notification)
    }

    // ─── Notifikasi: Lisensi Expired ─────────────────────────────────────────

    /**
     * Tampilkan notifikasi ketika lisensi sudah expired.
     */
    fun showLicenseExpired(context: Context) {
        val intent = mainActivityIntent(context)
        val pi = PendingIntent.getActivity(
            context, NOTIF_LICENSE_EXPIRED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LICENSE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚠️ Lisensi Premium Kamu Expired")
            .setContentText("Perpanjang sekarang untuk tetap menikmati semua fitur Premium.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Lisensi Premium kamu sudah berakhir.\n\n" +
                        "Fitur Premium tidak lagi tersedia sampai kamu memperpanjang. " +
                        "Tap untuk langsung ke halaman Lisensi."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        notify(context, NOTIF_LICENSE_EXPIRED, notification)
    }

    // ─── Notifikasi: Lisensi Hampir Habis ────────────────────────────────────

    /**
     * Tampilkan notifikasi ketika lisensi tinggal beberapa hari.
     *
     * @param daysLeft Sisa hari lisensi
     * @param expiryDate String tanggal expired, contoh "30 Mei 2025"
     */
    fun showLicenseExpiringSoon(
        context: Context,
        daysLeft: Int,
        expiryDate: String
    ) {
        val intent = mainActivityIntent(context)
        val pi = PendingIntent.getActivity(
            context, NOTIF_LICENSE_EXPIRING, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LICENSE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏳ Lisensi Hampir Berakhir ($daysLeft hari lagi)")
            .setContentText("Perpanjang sebelum $expiryDate agar tidak terputus.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Premium kamu akan berakhir dalam $daysLeft hari ($expiryDate).\n\n" +
                        "Perpanjang sekarang agar semua fitur tetap aktif tanpa gangguan."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        notify(context, NOTIF_LICENSE_EXPIRING, notification)
    }

    // ─── Notifikasi: Tweak Berhasil Diterapkan ───────────────────────────────

    /**
     * Notifikasi ringan (no sound) saat tweaks berhasil di-apply,
     * misalnya setelah boot atau manual reapply.
     *
     * @param profileName Nama profil yang aktif, atau null jika tidak ada profil
     */
    fun showTweakApplied(context: Context, profileName: String? = null) {
        val contentText = if (profileName != null)
            "Profil \"$profileName\" aktif"
        else
            "Semua tweak berhasil diterapkan"

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✅ Aether Manager")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)           // tidak bunyi, hanya muncul di status bar
            .setAutoCancel(true)
            .setTimeoutAfter(8_000L)   // auto-dismiss setelah 8 detik
            .build()

        notify(context, NOTIF_TWEAK_APPLIED, notification)
    }

    // ─── Notifikasi: Umum / Custom ───────────────────────────────────────────

    /**
     * Kirim notifikasi general dengan judul dan teks bebas.
     * Berguna untuk pesan dari server, info update config, dsb.
     *
     * @param withSound true = pakai suara default, false = silent
     */
    fun showGeneral(
        context: Context,
        title: String,
        message: String,
        withSound: Boolean = false
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (withSound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSilent(true)
        }

        notify(context, NOTIF_GENERAL, builder.build())
    }

    // ─── Dismiss ─────────────────────────────────────────────────────────────

    fun cancelUpdate(context: Context)         = cancel(context, NOTIF_UPDATE_AVAILABLE)
    fun cancelLicenseExpired(context: Context) = cancel(context, NOTIF_LICENSE_EXPIRED)
    fun cancelLicenseExpiring(context: Context)= cancel(context, NOTIF_LICENSE_EXPIRING)
    fun cancelAll(context: Context)            = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun notify(context: Context, id: Int, notification: Notification) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(id, notification)
    }

    private fun cancel(context: Context, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(id)
    }

    private fun mainActivityIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
}
