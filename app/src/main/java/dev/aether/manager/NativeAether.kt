package dev.aether.manager

import android.content.Context

object NativeAether {

    @Volatile private var _loaded = false

    val isLoaded: Boolean get() = _loaded

    fun tryLoad(context: Context? = null): Boolean {
        if (_loaded) return true
        return runCatching {
            try {
                System.loadLibrary("aether")
            } catch (err: UnsatisfiedLinkError) {
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

    // ── Security checks ──────────────────────────────────────────────────────
    external fun nativeIsHooked(): Boolean
    external fun nativeIsDebugged(): Boolean

    external fun nativeCheckAntiPatch(ctx: Context): Boolean
    external fun nativeCheckUnityIntact(): Boolean

    external fun nativeCheckCloner(ctx: Context): Boolean
    external fun nativeCheckElfIntegrity(): Boolean
    external fun nativeCheckGotHook(): Boolean

    external fun nativeCheckAll(ctx: Context): Boolean

    // ── Data / URL getters (decoded from obfuscated constants at runtime) ────
    external fun nativeGetGameId(): String
    external fun nativeGetGithubApi(): String

    /** https://aether-app-weld.vercel.app/api */
    external fun nativeGetVercelApi(): String

    /** https://github.com/aetherdev01/aether-manager */
    external fun nativeGetGithubRepo(): String

    /** https://t.me/AetherDev22 */
    external fun nativeGetTelegram(): String

    external fun nativeGetAdblockDnsKeywords(): Array<String>
    external fun nativeGetHostsSignatures(): Array<String>
    external fun nativeGetPackageName(): String?
}
