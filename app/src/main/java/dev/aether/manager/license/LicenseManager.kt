package dev.aether.manager.license

import android.content.Context
import android.provider.Settings
import dev.aether.manager.NativeAether
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object LicenseManager {

    private val API_BASE: String get() = NativeAether.nativeGetVercelApi()

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
                val deviceId = getDeviceId(ctx)
                val url  = URL("$API_BASE/activate")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput       = true
                    connectTimeout = 15_000
                    readTimeout    = 15_000
                }

                val body = JSONObject().apply {
                    put("key",      key.trim().uppercase())
                    put("deviceId", deviceId)
                }.toString()

                conn.outputStream.use { it.write(body.toByteArray()) }

                val code     = conn.responseCode
                val response = (if (code == 200) conn.inputStream else conn.errorStream)
                    .bufferedReader().readText()
                val json     = JSONObject(response)

                if (code == 200) {
                    val expiresAt = json.optLong("expiresAt", -1L)
                    LicensePrefs.save(ctx, key.trim().uppercase(), expiresAt)
                    ActivateResult.Success(
                        licenseKey = key.trim().uppercase(),
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

    fun deactivate(ctx: Context) {
        LicensePrefs.clear(ctx)
    }
}
