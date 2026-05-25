package com.aether.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.aether.BuildConfig
import java.security.MessageDigest

object AetherSecurityNative {
    @Volatile private var loaded = false

    val isLoaded: Boolean get() = loaded

    fun tryLoad(context: Context? = null): Boolean {
        if (loaded) return true
        return runCatching {
            try {
                System.loadLibrary("aethersec")
            } catch (err: UnsatisfiedLinkError) {
                val dir = context?.applicationInfo?.nativeLibraryDir
                val fallback = dir?.let { "$it/libaethersec.so" }
                if (fallback != null) System.load(fallback) else throw err
            }
            loaded = true
            true
        }.getOrDefault(false)
    }

    fun highCheck(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        if (!tryLoad(context)) return false
        return verifyPackageName(context) && verifyAppSignature(context)
    }

    fun tamperReason(context: Context): String {
        if (BuildConfig.DEBUG) return "ok"
        if (!tryLoad(context)) return "native_not_loaded"
        if (!verifyPackageName(context)) return "package_mismatch"
        if (!verifyAppSignature(context)) return "signature_mismatch"
        return "ok"
    }

    fun verifyAppSignature(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        if (!tryLoad(context)) return false
        val digests = collectSigningSha256(context)
        if (digests.isEmpty()) return false
        return digests.any { nativeVerifySignature(it) }
    }

    private fun verifyPackageName(context: Context): Boolean {
        return context.packageName == BuildConfig.APPLICATION_ID && context.packageName == EXPECTED_PACKAGE
    }

    private fun collectSigningSha256(context: Context): Set<String> {
        return runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val info = pm.getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo
                val current = signingInfo?.apkContentsSigners?.toList().orEmpty()
                val history = signingInfo?.signingCertificateHistory?.toList().orEmpty()
                current + history
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.toList().orEmpty()
            }
            signatures.mapNotNull { sig ->
                runCatching {
                    MessageDigest.getInstance("SHA-256")
                        .digest(sig.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                        .lowercase()
                }.getOrNull()
            }.toSet()
        }.getOrDefault(emptySet())
    }

    external fun nativeVerifySignature(sha256Hex: String): Boolean

    private const val EXPECTED_PACKAGE = "com.aether"
}
