package com.aether.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.aether.BuildConfig
import java.security.MessageDigest
import java.util.Locale

object AetherSecurityNative {
    @Volatile private var loaded = false

    val isLoaded: Boolean get() = loaded

    data class SecurityStatus(
        val ok: Boolean,
        val code: String,
        val title: String,
        val message: String,
        val fix: String,
        val currentSha256: String = "",
    )

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

    fun startupStatus(context: Context): SecurityStatus {
        if (BuildConfig.DEBUG) {
            return SecurityStatus(
                ok = true,
                code = "debug_build",
                title = "Debug build",
                message = "Security check dilewati untuk build debug.",
                fix = "Gunakan release build untuk validasi final."
            )
        }

        if (context.packageName != BuildConfig.APPLICATION_ID) {
            return SecurityStatus(
                ok = false,
                code = "package_mismatch",
                title = "Package name tidak sesuai",
                message = "Package terpasang: ${context.packageName}, sedangkan applicationId build: ${BuildConfig.APPLICATION_ID}.",
                fix = "Samakan applicationId/package name atau install APK resmi."
            )
        }

        val digests = collectSigningSha256(context)
        val current = digests.joinToString(",")
        if (digests.isEmpty()) {
            return SecurityStatus(
                ok = false,
                code = "signature_read_failed",
                title = "Signature tidak bisa dibaca",
                message = "Android gagal membaca SHA-256 signature APK.",
                fix = "Install ulang APK yang sudah ditandatangani dengan benar.",
            )
        }

        val expected = EXPECTED_RELEASE_SHA256.trim().lowercase(Locale.US)
        if (!isExpectedSignatureConfigured(expected)) {
            return SecurityStatus(
                ok = true,
                code = "signature_not_configured",
                title = "Signature belum dikunci",
                message = "Aplikasi berjalan normal. Untuk mode high security, isi EXPECTED_RELEASE_SHA256 dengan SHA-256 release key.",
                fix = "Ambil SHA-256 dari logcat tag AetherSecurity, lalu tempel ke EXPECTED_RELEASE_SHA256.",
                currentSha256 = current
            )
        }

        val javaMatch = digests.any { it.equals(expected, ignoreCase = true) }
        val nativeMatch = if (tryLoad(context)) {
            digests.any { runCatching { nativeVerifySignature(it) }.getOrDefault(false) }
        } else {
            false
        }

        return if (javaMatch || nativeMatch) {
            SecurityStatus(
                ok = true,
                code = "ok",
                title = "Signature valid",
                message = "Package dan signature APK sesuai.",
                fix = "Tidak ada tindakan.",
                currentSha256 = current
            )
        } else {
            SecurityStatus(
                ok = false,
                code = "signature_mismatch",
                title = "Signature aplikasi tidak cocok",
                message = "APK ditandatangani dengan key berbeda. SHA-256 saat ini: $current",
                fix = "Kalau ini APK resmi baru, update EXPECTED_RELEASE_SHA256. Kalau bukan, install ulang APK resmi.",
                currentSha256 = current
            )
        }
    }

    fun highCheck(context: Context): Boolean {
        val status = startupStatus(context)
        return status.ok || !SECURITY_ENFORCE_BLOCK
    }

    fun tamperReason(context: Context): String = startupStatus(context).code

    fun verifyAppSignature(context: Context): Boolean {
        val status = startupStatus(context)
        return status.ok || !SECURITY_ENFORCE_BLOCK
    }

    private fun isExpectedSignatureConfigured(value: String): Boolean {
        if (value.length != 64) return false
        if (value == "0000000000000000000000000000000000000000000000000000000000000000") return false
        if (value == "replace_with_release_sha256") return false
        return value.all { it in '0'..'9' || it in 'a'..'f' }
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
                if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners?.toList().orEmpty()
                } else {
                    signingInfo?.signingCertificateHistory?.toList().orEmpty()
                }
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.toList().orEmpty()
            }
            signatures.mapNotNull { sig ->
                runCatching {
                    MessageDigest.getInstance("SHA-256")
                        .digest(sig.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                        .lowercase(Locale.US)
                }.getOrNull()
            }.toSet()
        }.getOrDefault(emptySet())
    }

    external fun nativeVerifySignature(sha256Hex: String): Boolean

    private const val SECURITY_ENFORCE_BLOCK = false
    private const val EXPECTED_RELEASE_SHA256 = "0000000000000000000000000000000000000000000000000000000000000000"
}
