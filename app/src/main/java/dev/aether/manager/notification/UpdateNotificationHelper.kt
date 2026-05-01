package dev.aether.manager.notification

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.aether.manager.update.UpdateChecker
import dev.aether.manager.update.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cek update dari GitHub dan tampilkan notifikasi bila ada versi baru.
 *
 * Panggil [checkAndNotify] dari:
 *  - WorkManager periodic background job (sekali sehari)
 *  - Atau dari MainActivity.onCreate() untuk cek saat app dibuka
 *
 * Tidak menampilkan notifikasi kalau versi sudah sama (UpToDate).
 */
object UpdateNotificationHelper {

    suspend fun checkAndNotify(context: Context) = withContext(Dispatchers.IO) {
        val currentCode = getCurrentVersionCode(context)
        when (val result = UpdateChecker.check(currentCode)) {
            is UpdateResult.UpdateAvailable -> {
                NotificationHelper.showUpdateAvailable(
                    context     = context,
                    versionName = result.info.latestVersion,
                    releaseNotes = result.info.releaseNotes
                )
            }
            is UpdateResult.UpToDate -> {
                // Kalau ada notif update lama, hapus (versi sudah diupdate)
                NotificationHelper.cancelUpdate(context)
            }
            is UpdateResult.Error -> {
                // Gagal cek — diam saja, jangan spam error ke user
            }
        }
    }

    private fun getCurrentVersionCode(context: Context): Int = try {
        val pm  = context.packageManager
        val pkg = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode.toInt()
        else @Suppress("DEPRECATION") pkg.versionCode
    } catch (_: PackageManager.NameNotFoundException) { 0 }
}
