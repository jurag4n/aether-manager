package dev.aether.manager.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RootManager — Root state machine menggunakan libsu.
 *
 * libsu menangani Magisk dialog grant secara proper — tidak ada deadlock,
 * tidak perlu spawn/block manual. Shell.getShell() akan trigger dialog
 * Magisk/KernelSU/APatch otomatis saat pertama kali dipanggil.
 */
object RootManager {


    @Volatile private var _cachedRoot: Boolean? = null

    val isRootGranted: Boolean get() = _cachedRoot == true
    val isRootUnknown: Boolean get() = _cachedRoot == null

    fun detectRootType(): String {
        // Jika sudah granted, gunakan shell untuk deteksi yang lebih akurat
        if (Shell.isAppGrantedRoot() == true || _cachedRoot == true) {
            return detectRootTypeViaShell()
        }
        // Fallback ke deteksi file (kurang akurat di Android 11+ tanpa root)
        return when {
            File("/data/adb/magisk").exists()     -> "Magisk"
            File("/sbin/.magisk").exists()        -> "Magisk"
            File("/data/adb/ksu").exists()        -> "KernelSU"
            File("/data/adb/ap").exists()         -> "APatch"
            else                                  -> "Unknown"
        }
    }

    private fun detectRootTypeViaShell(): String {
        return try {
            val result = Shell.cmd(
                "if [ -d /data/adb/ksu ]; then echo KernelSU",
                "elif [ -d /data/adb/ap ]; then echo APatch",
                "elif [ -d /data/adb/magisk ]; then echo Magisk",
                "else echo Unknown; fi"
            ).exec()
            val out = result.out.joinToString("").trim()
            out.ifBlank { "Unknown" }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot ?: run {
            // isAppGrantedRoot() return null = shell belum pernah di-init → belum tahu.
            // Jangan cache sebagai false (denied) — biarkan tetap null (unknown)
            // supaya SetupActivity bisa request root nanti.
            val quick = Shell.isAppGrantedRoot()
            if (quick == true) {
                val ok = runCatching {
                    Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"
                }.getOrDefault(false)
                _cachedRoot = ok
                ok
            } else {
                // null atau false — jangan cache, return false tanpa trigger popup
                false
            }
        }
    }

    /**
     * REQUEST root — HANYA dipanggil dari SetupActivity saat user klik tombol.
     *
     * Shell.getShell() dari libsu akan:
     * 1. Spawn su dan trigger dialog Magisk/KernelSU/APatch secara proper
     * 2. Tunggu user grant (non-deadlock, libsu mengelola thread sendiri)
     * 3. Return shell yang sudah granted, atau throw kalau denied
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot = null

        return@withContext try {
            // getShell() blocking — trigger dialog dan tunggu user grant
            val shell = Shell.getShell()
            val granted = shell.isRoot && Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"
            _cachedRoot = granted
            granted
        } catch (e: Exception) {
            _cachedRoot = false
            false
        }
    }

    fun clearCache() { _cachedRoot = null }
    fun markGranted() { _cachedRoot = true }
    fun markDenied()  { _cachedRoot = false }
}