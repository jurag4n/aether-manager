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

    external fun nativeGetVercelApi(): String

    external fun nativeGetActivateUrl(): String

    external fun nativeGetCreateOrderUrl(): String

    external fun nativeGetPollOrderUrl(): String

    external fun nativeGetGithubRepo(): String

    external fun nativeGetTelegram(): String

    external fun nativeGetWhatsappUrl(): String
    external fun nativeGetPaymentHolder(): String
    external fun nativeGetDanaLabel(): String
    external fun nativeGetGopayLabel(): String
    external fun nativeGetPaypalLabel(): String
    external fun nativeGetPaymentNumberLabel(): String
    external fun nativeGetPaypalAccountLabel(): String
 
    external fun nativeGetAdblockDnsKeywords(): Array<String>
    external fun nativeGetHostsSignatures(): Array<String>
    external fun nativeGetPackageName(): String?
}

object NativeSecrets {
    fun gameId() = NativeAether.safeString({ nativeGetGameId() }, "")
    fun githubApi() = NativeAether.safeString({ nativeGetGithubApi() }, "")
    fun vercelApi() = NativeAether.safeString({ nativeGetVercelApi() }, "")
    fun activateUrl() = NativeAether.safeString({ nativeGetActivateUrl() }, "")
    fun createOrderUrl() = NativeAether.safeString({ nativeGetCreateOrderUrl() }, "")
    fun pollOrderUrl() = NativeAether.safeString({ nativeGetPollOrderUrl() }, "")
    fun telegramUrl() = NativeAether.safeString({ nativeGetTelegram() }, "")
    fun whatsappUrl() = NativeAether.safeString({ nativeGetWhatsappUrl() }, "")
    fun holderName() = NativeAether.safeString({ nativeGetPaymentHolder() }, "")
    fun danaLabel() = NativeAether.safeString({ nativeGetDanaLabel() }, "DANA")
    fun gopayLabel() = NativeAether.safeString({ nativeGetGopayLabel() }, "GoPay")
    fun paypalLabel() = NativeAether.safeString({ nativeGetPaypalLabel() }, "PayPal")
    fun paymentNumberLabel() = NativeAether.safeString({ nativeGetPaymentNumberLabel() }, "Nomor tujuan")
    fun paypalAccountLabel() = NativeAether.safeString({ nativeGetPaypalAccountLabel() }, "Akun PayPal")
}

private inline fun NativeAether.safeString(block: NativeAether.() -> String, fallback: String): String =
    if (isLoaded) runCatching { block() }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback else fallback
