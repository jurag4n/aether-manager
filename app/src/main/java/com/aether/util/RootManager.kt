package com.aether.util

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * RootManager — satu pintu akses root untuk seluruh app.
 *
 * Prinsip penting:
 *  - Jangan pakai FLAG_NON_ROOT_SHELL untuk builder utama aplikasi root.
 *    Kalau shell non-root ter-cache lebih dulu, Shell.cmd() berikutnya akan ikut memakai shell non-root
 *    dan prompt Magisk/KernelSU/APatch bisa tidak muncul.
 *  - Request root hanya dilakukan saat user menekan aksi root / fitur tweak.
 *  - Cek pasif tidak boleh membuat shell baru dan tidak boleh memunculkan dialog SU.
 */
object RootManager {

    @Volatile private var _cachedRoot: Boolean? = null

    val isRootGranted: Boolean get() = _cachedRoot == true
    val isRootUnknown: Boolean get() = _cachedRoot == null

    fun configureShell(context: Context? = null) {
        val builder = Shell.Builder.create()
            .setFlags(
                Shell.FLAG_REDIRECT_STDERR or
                Shell.FLAG_MOUNT_MASTER
            )
            .setTimeout(20)

        if (context != null) builder.setContext(context.applicationContext)
        Shell.setDefaultBuilder(builder)
    }

    fun detectRootType(): String {
        return if (Shell.isAppGrantedRoot() == true || _cachedRoot == true) {
            detectRootTypeViaShell()
        } else {
            detectRootTypeByPath()
        }
    }

    private fun detectRootTypeByPath(): String = when {
        File("/data/adb/ksu").exists()    -> "KernelSU"
        File("/data/adb/ap").exists()     -> "APatch"
        File("/data/adb/magisk").exists() -> "Magisk"
        File("/sbin/.magisk").exists()    -> "Magisk"
        else                              -> "Unknown"
    }

    private fun detectRootTypeViaShell(): String = runCatching {
        val result = Shell.cmd(
            """
            if [ -d /data/adb/ksu ]; then echo KernelSU
            elif [ -d /data/adb/ap ]; then echo APatch
            elif [ -d /data/adb/magisk ]; then echo Magisk
            elif [ -d /sbin/.magisk ]; then echo Magisk
            else echo Unknown
            fi
            """.trimIndent()
        ).exec()
        result.out.joinToString("").trim().ifBlank { "Unknown" }
    }.getOrDefault("Unknown")

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        ensureRootShellSync(requestIfNeeded = false)
    }

    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot = null
        ensureRootShellSync(requestIfNeeded = true)
    }

    suspend fun ensureRootShell(requestIfNeeded: Boolean = true): Boolean =
        withContext(Dispatchers.IO) { ensureRootShellSync(requestIfNeeded) }

    /**
     * requestIfNeeded=false: hanya cek shell yang sudah ada / status yang sudah final.
     * requestIfNeeded=true : benar-benar menjalankan su sehingga Magisk/KernelSU/APatch menampilkan prompt.
     */
    fun ensureRootShellSync(requestIfNeeded: Boolean = true): Boolean {
        if (_cachedRoot == true) return verifyUid0()

        return try {
            val cached = Shell.getCachedShell()
            if (cached != null && cached.isAlive) {
                if (cached.isRoot && verifyUid0()) {
                    _cachedRoot = true
                    return true
                }

                if (!requestIfNeeded) return false

                // Tutup shell non-root yang mungkin terlanjur dibuat sebelum request root.
                runCatching { cached.waitAndClose(1, TimeUnit.SECONDS) }
            }

            val quick = Shell.isAppGrantedRoot()
            if (quick == true && verifyUid0()) {
                _cachedRoot = true
                return true
            }

            if (!requestIfNeeded) return false

            configureShell()
            val shell = Shell.getShell()
            val ok = shell.isAlive && shell.isRoot && verifyUid0()
            _cachedRoot = ok
            ok
        } catch (_: Throwable) {
            false
        }
    }

    private fun verifyUid0(): Boolean = runCatching {
        val result = Shell.cmd("id -u").exec()
        result.isSuccess && result.out.joinToString("").trim() == "0"
    }.getOrDefault(false)

    fun clearCache()  { _cachedRoot = null  }
    fun markGranted() { _cachedRoot = true  }
    fun markDenied()  { _cachedRoot = false }
}
