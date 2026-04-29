package dev.aether.manager

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

    external fun nativeCheckSignature(sigHashHex: String): Boolean
    external fun nativeCheckAntiPatch(ctx: android.content.Context): Boolean
    external fun nativeCheckUnityIntact(): Boolean
    external fun nativeCheckAll(ctx: android.content.Context): Boolean
    external fun nativeKillProcess()
    external fun nativeGetGameId(): String
    external fun nativeGetGithubApi(): String
    external fun nativeGetAdblockDnsKeywords(): Array<String>
    external fun nativeGetHostsSignatures(): Array<String>
}
