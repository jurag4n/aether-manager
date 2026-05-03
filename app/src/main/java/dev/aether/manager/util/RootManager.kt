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
        ensureRootShell(requestIfNeeded = true)
    }

    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot = null
        ensureRootShell(requestIfNeeded = true)
    }

    fun ensureRootShellSync(requestIfNeeded: Boolean = true): Boolean {
        if (_cachedRoot == true) return true
        return try {
            val quick = Shell.isAppGrantedRoot()
            when {
                quick == true -> {
                    val ok = Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"
                    _cachedRoot = ok
                    ok
                }
                requestIfNeeded -> {
                    val shell = Shell.getShell()
                    val ok = shell.isRoot && Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"
                    _cachedRoot = ok
                    ok
                }
                else -> {
                    _cachedRoot = false
                    false
                }
            }
        } catch (_: Exception) {
            _cachedRoot = false
            false
        }
    }

    suspend fun ensureRootShell(requestIfNeeded: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        ensureRootShellSync(requestIfNeeded)
    }

    fun clearCache() { _cachedRoot = null }
    fun markGranted() { _cachedRoot = true }
    fun markDenied()  { _cachedRoot = false }
}