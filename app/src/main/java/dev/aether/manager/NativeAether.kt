package dev.aether.manager

import android.content.Context

object NativeAether {

    @Volatile private var _loaded = false

    val isLoaded: Boolean get() = _loaded

    fun tryLoad(): Boolean {
        if (_loaded) return true
        return runCatching {
            System.loadLibrary("aether")
            _loaded = true
            true
        }.getOrDefault(false)
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
