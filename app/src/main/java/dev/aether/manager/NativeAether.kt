package dev.aether.manager

import android.content.Context

object NativeAether {

    @Volatile private var _loaded = false

    val isLoaded: Boolean get() = _loaded

    /**
     * Attempt to load the native `libaether.so` library.  On most devices the
     * standard [System.loadLibrary] call will succeed.  However, certain
     * packaging or installation scenarios (for example split APKs or
     * relocated native library directories) can cause the standard loader
     * to fail.  To improve resiliency we fall back to directly loading
     * the absolute library path from the application's native library
     * directory if a [Context] is provided.  The [context] parameter is
     * optional; when omitted the fallback mechanism is skipped.
     *
     * @param context optional application context used to resolve the native
     * library directory for fallback loading
     * @return `true` if the library was successfully loaded or is already
     *         loaded, `false` otherwise
     */
    fun tryLoad(context: Context? = null): Boolean {
        if (_loaded) return true
        return runCatching {
            try {
                // Attempt the standard library load first.  This looks up
                // libaether.so based on the system's library search path.
                System.loadLibrary("aether")
            } catch (err: UnsatisfiedLinkError) {
                // Fallback: if a context is available, construct the
                // absolute path to libaether.so within the app's
                // native library directory (typically under
                // /data/app/<package>-.../lib).  Only attempt this
                // if the directory is non-null.  Otherwise rethrow
                // the original error to propagate the failure.
                val dir = context?.applicationInfo?.nativeLibraryDir
                val fallback = dir?.let { "$it/libaether.so" }
                if (fallback != null) {
                    System.load(fallback)
                } else {
                    throw err
                }
            }
            _loaded = true
            true
        }.getOrElse { false }
    }

    // ── Signature & integrity ─────────────────────────────────────────────
    external fun nativeCheckSignature(sigHashHex: String): Boolean
    external fun nativeGetApkHash(ctx: Context): String?
    external fun nativeCheckIntegrity(ctx: Context, expectedHash: String): Boolean

    // ── Hook & debug detection ────────────────────────────────────────────
    external fun nativeIsHooked(): Boolean
    external fun nativeIsDebugged(): Boolean

    // ── Anti-patch ────────────────────────────────────────────────────────
    external fun nativeCheckAntiPatch(ctx: Context): Boolean
    external fun nativeCheckUnityIntact(): Boolean

    // ── Master check (semua layer sekaligus) ──────────────────────────────
    external fun nativeCheckAll(ctx: Context): Boolean

    // ── Utilities ─────────────────────────────────────────────────────────
    external fun nativeKillProcess()
    external fun nativeGetGameId(): String
    external fun nativeGetGithubApi(): String
    external fun nativeGetAdblockDnsKeywords(): Array<String>
    external fun nativeGetHostsSignatures(): Array<String>
    external fun nativeGetPackageName(): String?
}
