package com.aether.remote

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.aether.BuildConfig
import com.aether.NativeAether
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

object AetherAdminSync {
    private const val PREF = "aether_admin_sync"
    private const val KEY_LAST_JSON = "last_project_config_json"
    private const val FALLBACK_API = "https://aether-app-weld.vercel.app/api"

    fun apiBase(): String {
        val fromBuild = BuildConfig.AETHER_API_BASE.trim()
        if (fromBuild.isNotBlank()) return fromBuild.trimEnd('/')
        val fromNative = if (NativeAether.isLoaded) runCatching { NativeAether.nativeGetVercelApi() }.getOrNull() else null
        return (fromNative ?: FALLBACK_API).trimEnd('/')
    }

    fun endpoint(path: String): String = apiBase() + if (path.startsWith('/')) path else "/$path"

    fun projectApiKey(): String = BuildConfig.AETHER_PROJECT_API_KEY.trim()

    fun applyProjectHeaders(conn: HttpURLConnection, ctx: Context) {
        val apiKey = projectApiKey()
        if (apiKey.isNotBlank()) conn.setRequestProperty("X-Aether-Api-Key", apiKey)
        conn.setRequestProperty("X-Aether-Package", ctx.packageName)
        conn.setRequestProperty("X-Aether-App-Version", appVersionName(ctx))
        conn.setRequestProperty("X-Aether-App-Version-Code", appVersionCode(ctx).toString())
    }

    fun clientPayload(ctx: Context): JSONObject = JSONObject().apply {
        put("packageName", ctx.packageName)
        put("signatureSha256", signingSha256(ctx))
        put("appVersion", appVersionName(ctx))
        put("appVersionCode", appVersionCode(ctx))
        put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        put("model", Build.MODEL ?: "unknown")
    }

    suspend fun sync(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        val apiKey = projectApiKey()
        if (apiKey.isBlank()) return@withContext false
        runCatching {
            val params = mapOf(
                "packageName" to ctx.packageName,
                "signatureSha256" to signingSha256(ctx),
                "appVersion" to appVersionName(ctx),
                "appVersionCode" to appVersionCode(ctx).toString()
            ).map { (k, v) -> "${enc(k)}=${enc(v)}" }.joinToString("&")
            val conn = (URL("${endpoint("/public/project-config")}?$params").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                applyProjectHeaders(this, ctx)
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) return@withContext false
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAST_JSON, body)
                .putLong("last_sync_at", System.currentTimeMillis())
                .apply()
            true
        }.getOrDefault(false)
    }

    fun cachedConfig(ctx: Context): JSONObject? = runCatching {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LAST_JSON, null) ?: return null
        JSONObject(raw)
    }.getOrNull()

    fun remoteConfig(ctx: Context): JSONObject? = cachedConfig(ctx)?.optJSONObject("remoteConfig")

    fun isMaintenance(ctx: Context): Boolean = remoteConfig(ctx)?.optBoolean("maintenance", false) == true

    fun maintenanceMessage(ctx: Context): String = remoteConfig(ctx)?.optString("maintenanceMessage")
        ?.takeIf { it.isNotBlank() } ?: "Aplikasi sedang dalam perawatan."

    fun isFeatureDisabled(ctx: Context, feature: String): Boolean {
        val killSwitch = remoteConfig(ctx)?.optJSONObject("killSwitch") ?: return false
        return killSwitch.optBoolean(feature, false)
    }

    fun latestVersion(ctx: Context): String? = remoteConfig(ctx)?.optJSONObject("rollout")?.optString("latestVersion")?.takeIf { it.isNotBlank() }

    fun updateUrl(ctx: Context): String? = remoteConfig(ctx)?.optJSONObject("rollout")?.optString("updateUrl")?.takeIf { it.isNotBlank() }

    fun forceUpdate(ctx: Context): Boolean = remoteConfig(ctx)?.optJSONObject("rollout")?.optBoolean("forceUpdate", false) == true

    fun appVersionName(ctx: Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
    }.getOrDefault(BuildConfig.VERSION_NAME)

    fun appVersionCode(ctx: Context): Long = runCatching {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(BuildConfig.VERSION_CODE.toLong())

    fun signingSha256(ctx: Context): String = runCatching {
        val pm = ctx.packageManager
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            when {
                signingInfo == null -> emptyArray()
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners ?: emptyArray()
                else -> signingInfo.signingCertificateHistory ?: signingInfo.apkContentsSigners ?: emptyArray()
            }
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures ?: emptyArray()
        }
        val cert = signatures.firstOrNull()?.toByteArray() ?: return@runCatching ""
        MessageDigest.getInstance("SHA-256").digest(cert).joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
