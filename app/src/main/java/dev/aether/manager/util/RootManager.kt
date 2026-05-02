package dev.aether.manager.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RootManager {
    @Volatile private var _cachedRoot: Boolean? = null

    val isRootGranted: Boolean get() = _cachedRoot == true
    val isRootUnknown: Boolean get() = _cachedRoot == null

    fun detectRootType(): String {
        if (Shell.isAppGrantedRoot() == true || _cachedRoot == true) {
            return detectRootTypeViaShell()
        }
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
            val result = Shell.cmd("""
                if [ -d /data/adb/ksu ]; then echo KernelSU
                elif [ -d /data/adb/ap ]; then echo APatch
                elif [ -d /data/adb/magisk ]; then echo Magisk
                else echo Unknown
                fi
            """.trimIndent()).exec()
            val out = result.out.joinToString("").trim()
            out.ifBlank { "Unknown" }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        if (_cachedRoot == true) return@withContext true

        // Shell.isAppGrantedRoot() bisa return null kalau Shell belum pernah di-init.
        // Dalam kondisi itu, kita perlu panggil getShell() untuk trigger inisialisasi.
        val quick = Shell.isAppGrantedRoot()
        if (quick == true) {
            val ok = runCatching {
                Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"
            }.getOrDefault(false)
            _cachedRoot = ok
            return@withContext ok
        }

        // quick == null → Shell belum di-init. Delegate ke requestRoot() yang panggil getShell().
        if (quick == null) {
            return@withContext requestRoot()
        }

        false
    }

    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot = null
        return@withContext try {
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