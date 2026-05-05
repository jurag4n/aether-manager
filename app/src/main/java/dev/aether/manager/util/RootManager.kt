package dev.aether.manager.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RootManager — single source of truth untuk status root.
 *
 * Aturan:
 *  - Hanya [markGranted]/[markDenied]/[clearCache] yang boleh mengubah _cachedRoot.
 *  - [ensureRootShell] adalah satu-satunya titik masuk async request root.
 *  - [ensureRootShellSync] hanya untuk konteks di mana coroutine tidak tersedia
 *    (mis. TweakApplier.runScript di thread IO). JANGAN panggil dari UI thread.
 *  - _cachedRoot == null  → belum diketahui (belum pernah dicek / setelah clearCache).
 *  - _cachedRoot == false → HANYA dicache setelah shell.isRoot == false (user tolak / SU tidak ada).
 *    Exception TIDAK cache false supaya retry berikutnya bisa berhasil.
 */
object RootManager {

    @Volatile private var _cachedRoot: Boolean? = null

    val isRootGranted: Boolean get() = _cachedRoot == true
    val isRootUnknown: Boolean get() = _cachedRoot == null

    // ── Root type detection ───────────────────────────────────────────────────

    fun detectRootType(): String {
        return if (Shell.isAppGrantedRoot() == true || _cachedRoot == true) {
            detectRootTypeViaShell()
        } else {
            // Fallback path check tanpa shell
            when {
                File("/data/adb/ksu").exists()    -> "KernelSU"
                File("/data/adb/ap").exists()     -> "APatch"
                File("/data/adb/magisk").exists() -> "Magisk"
                File("/sbin/.magisk").exists()    -> "Magisk"
                else                              -> "Unknown"
            }
        }
    }

    private fun detectRootTypeViaShell(): String {
        return try {
            val result = Shell.cmd(
                """
                if [ -d /data/adb/ksu ]; then echo KernelSU
                elif [ -d /data/adb/ap ]; then echo APatch
                elif [ -d /data/adb/magisk ]; then echo Magisk
                else echo Unknown
                fi
                """.trimIndent()
            ).exec()
            result.out.joinToString("").trim().ifBlank { "Unknown" }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Cek ketersediaan root tanpa meminta akses baru. Aman dari IO thread. */
    suspend fun isRooted(): Boolean = withContext(Dispatchers.IO) {
        ensureRootShell(requestIfNeeded = false)
    }

    /**
     * Minta akses root (trigger dialog SU manager).
     * Reset cache dulu agar bisa request ulang meskipun sebelumnya denied.
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        _cachedRoot = null          // izinkan re-request
        ensureRootShell(requestIfNeeded = true)
    }

    /** Versi suspend — selalu gunakan ini kecuali di konteks non-coroutine. */
    suspend fun ensureRootShell(requestIfNeeded: Boolean = true): Boolean =
        withContext(Dispatchers.IO) {
            ensureRootShellSync(requestIfNeeded)
        }

    /**
     * Versi sync — HANYA untuk TweakApplier / RootEngine.sh yang sudah di IO thread.
     * JANGAN panggil dari main thread (Shell.getShell() blocking → ANR).
     *
     * Aturan cache:
     *  - Cache true  → shell terbuka, uid == 0
     *  - Cache false → shell.isRoot == false (user menolak / su tidak ada)
     *  - Exception   → TIDAK cache apapun, return false (bisa retry)
     */
    fun ensureRootShellSync(requestIfNeeded: Boolean = true): Boolean {
        // Fast-path: sudah pernah granted
        if (_cachedRoot == true) return true

        return try {
            val quick = Shell.isAppGrantedRoot()
            when {
                quick == true -> {
                    // libsu konfirmasi granted → verifikasi uid
                    val ok = Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"
                    if (ok) _cachedRoot = true
                    ok
                }
                quick == false && !requestIfNeeded -> {
                    // SU manager lapor denied tapi tidak request → jangan cache
                    false
                }
                requestIfNeeded -> {
                    // Ini yang trigger dialog SU manager
                    val shell = Shell.getShell()
                    val isRoot = shell.isRoot
                    val uid0 = isRoot &&
                        Shell.cmd("id -u").exec().out.joinToString("").trim() == "0"

                    if (uid0) {
                        _cachedRoot = true
                        true
                    } else {
                        // Shell terbuka tapi tidak root → user menolak
                        _cachedRoot = false
                        false
                    }
                }
                else -> false
            }
        } catch (_: Exception) {
            // Exception (mis. timeout, shell crash) → TIDAK cache false → retry bisa berhasil
            false
        }
    }

    // ── Cache control ─────────────────────────────────────────────────────────

    fun clearCache()  { _cachedRoot = null  }
    fun markGranted() { _cachedRoot = true  }
    fun markDenied()  { _cachedRoot = false }
}
