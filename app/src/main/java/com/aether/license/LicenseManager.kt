package com.aether.license

import android.content.Context
import android.provider.Settings
import com.aether.NativeAether
import com.aether.remote.AetherAdminSync
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object LicenseManager {

    /**
     * Kembalikan URL endpoint activate dari native layer.
     * Fallback ke hardcoded jika library belum ter-load (dev/test env).
     */
    private fun activateUrl(): String = AetherAdminSync.endpoint("/activate")

    private fun verifyUrl(): String = AetherAdminSync.endpoint("/verify")

    fun formatKeyInput(value: String): String {
        val trimmed = value.trim()
        val compact = trimmed.filter { it.isLetterOrDigit() }

        // Format baru Aether: AETH-XXXX-XXXX-XXXX.
        // Hanya key yang diawali AETH yang otomatis di-uppercase dan diberi strip.
        if (compact.uppercase(Locale.US).startsWith("AETH")) {
            val raw = compact.uppercase(Locale.US).take(16)
            val tail = raw.drop(4)
            return buildString {
                append("AETH")
                tail.chunked(4).forEach { part -> append('-').append(part) }
            }.take(19)
        }

        // Format legacy/custom seperti NLlHOl9oGb harus case-sensitive.
        // Jangan uppercase, jangan paksa strip, agar kode lama tetap valid.
        return trimmed.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(64)
    }

    fun normalizeKey(value: String): String {
        val formatted = formatKeyInput(value).trim()
        val compact = formatted.filter { it.isLetterOrDigit() }
        return if (compact.uppercase(Locale.US).startsWith("AETH")) {
            formatKeyInput(compact).uppercase(Locale.US)
        } else {
            formatted
        }
    }

    fun getDeviceId(ctx: Context): String {
        val androidId = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        return MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray())
            .take(4)
            .joinToString("") { "%02X".format(it) }
    }

    fun isActive(ctx: Context): Boolean {
        val key    = LicensePrefs.getKey(ctx) ?: return false
        if (key.isBlank()) return false
        val expiry = LicensePrefs.getExpiry(ctx)
        return expiry == -1L || (expiry > 0L && System.currentTimeMillis() < expiry)
    }

    sealed class ActivateResult {
        data class Success(val licenseKey: String, val message: String, val expiresAt: Long) : ActivateResult()
        data class Failure(val message: String) : ActivateResult()
    }

    suspend fun activate(ctx: Context, key: String): ActivateResult =
        withContext(Dispatchers.IO) {
            try {
                val cleanKey = normalizeKey(key)
                val deviceId = getDeviceId(ctx)
                val url  = URL(activateUrl())
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    AetherAdminSync.applyProjectHeaders(this, ctx)
                    doOutput       = true
                    connectTimeout = 15_000
                    readTimeout    = 15_000
                }

                val body = AetherAdminSync.clientPayload(ctx).apply {
                    put("key",      cleanKey)
                    put("deviceId", deviceId)
                }.toString()

                conn.outputStream.use { it.write(body.toByteArray()) }

                val code     = conn.responseCode
                val response = (if (code == 200) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText() ?: "{}"
                val json     = runCatching { JSONObject(response) }.getOrDefault(JSONObject())

                if (code == 200) {
                    val expiresAt = json.optLong("expiresAt", -1L)
                    AetherAdminSync.sync(ctx)
                    LicensePrefs.save(ctx, cleanKey, expiresAt)
                    ActivateResult.Success(
                        licenseKey = cleanKey,
                        message    = json.optString("message", "License berhasil diaktifkan!"),
                        expiresAt  = expiresAt
                    )
                } else {
                    ActivateResult.Failure(
                        json.optString("error", "License tidak valid atau sudah digunakan")
                    )
                }
            } catch (e: Exception) {
                ActivateResult.Failure(e.message ?: "Network error")
            }
        }

    suspend fun verify(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        val key = LicensePrefs.getKey(ctx) ?: return@withContext false
        val deviceId = getDeviceId(ctx)
        try {
            val conn = (URL(verifyUrl()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                AetherAdminSync.applyProjectHeaders(this, ctx)
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val body = AetherAdminSync.clientPayload(ctx).apply {
                put("key", normalizeKey(key))
                put("deviceId", deviceId)
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val response = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: "{}"
            val json = runCatching { JSONObject(response) }.getOrDefault(JSONObject())
            val valid = json.optBoolean("valid", false)
            if (valid) {
                val expiresAt = json.optLong("expiresAt", LicensePrefs.getExpiry(ctx))
                LicensePrefs.save(ctx, normalizeKey(key), expiresAt)
                AetherAdminSync.sync(ctx)
            }
            valid
        } catch (_: Exception) {
            isActive(ctx)
        }
    }

    fun deactivate(ctx: Context) {
        LicensePrefs.clear(ctx)
    }
}
