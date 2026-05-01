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
            val result = silentCheck()
            _cachedRoot = result
            result
        }
    }

    private fun silentCheck(): Boolean {
        return try {
            // Cek dulu lewat isAppGrantedRoot() (non-blocking, tidak trigger dialog)
            val quickCheck = Shell.isAppGrantedRoot()
            if (quickCheck == true) {
                return true
            }

            // quickCheck null atau false — shell belum di-init atau belum granted.
            // Gunakan getShell() agar shell ter-init dan root dialog muncul jika perlu.
            // Ini blocking tapi dipanggil dari Dispatchers.IO di isRooted().
            val shell = Shell.getShell()
            val granted = shell.isRoot
            granted
        } catch (e: Exception) {
            false
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
            val granted = shell.isRoot
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