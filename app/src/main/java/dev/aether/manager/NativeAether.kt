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

    external fun nativeCheckSignature(sigHashHex: String): Boolean
    external fun nativeGetApkHash(ctx: Context): String?
    external fun nativeCheckIntegrity(ctx: Context, expectedHash: String): Boolean

    external fun nativeIsHooked(): Boolean
    external fun nativeIsDebugged(): Boolean
    external fun nativeKillProcess()

    external fun nativeCheckAntiPatch(ctx: Context): Boolean
    external fun nativeCheckUnityIntact(): Boolean

    external fun nativeCheckCloner(ctx: Context): Boolean
    external fun nativeCheckElfIntegrity(): Boolean
    external fun nativeCheckGotHook(): Boolean

    external fun nativeCheckAll(ctx: Context): Boolean

    external fun nativeGetGameId(): String
    external fun nativeGetGithubApi(): String
    external fun nativeGetAdblockDnsKeywords(): Array<String>
    external fun nativeGetHostsSignatures(): Array<String>
    external fun nativeGetPackageName(): String?
}
