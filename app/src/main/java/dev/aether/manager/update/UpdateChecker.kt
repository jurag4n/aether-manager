package dev.aether.manager.update

import android.util.Log
import dev.aether.manager.NativeAether
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────────────────────────────────────

/** Data mentah dari GitHub Releases API */
data class ReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val tagName:     String,
    val downloadUrl: String,
    val changelog:   String,
    val htmlUrl:     String,
    val isPreRelease: Boolean,
)

/** Flat info untuk notifikasi & MainActivity */
data class UpdateInfo(
    val latestVersion: String,
    val versionCode:   Int,
    val releaseNotes:  String,
    val downloadUrl:   String,
    val htmlUrl:       String,
)

/** Hasil dari check() — dipakai UpdateNotificationHelper */
sealed class UpdateResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
    object UpToDate                                  : UpdateResult()
    data class Error(val msg: String)                : UpdateResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// UpdateChecker
// ─────────────────────────────────────────────────────────────────────────────

object UpdateChecker {

    private const val TAG           = "UpdateChecker"
    private const val FALLBACK_REPO = "aetherdev01/aether-manager"

    // Repo path diambil dari native layer (XOR-encoded di libaether.so).
    // Fallback ke konstanta hardcode jika library belum dimuat.
    private val repo: String
        get() = if (NativeAether.isLoaded) NativeAether.nativeGetGithubApi() else FALLBACK_REPO

    private val API: String
        get() = "https://api.github.com/repos/$repo/releases/latest"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Fetch & return ReleaseInfo mentah. Return null kalau gagal. */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(API)
                .header("Accept", "application/vnd.github+json")
                .build()

            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "GitHub API ${resp.code}")
                    return@withContext null
                }
                resp.body?.string() ?: return@withContext null
            }

            parseRelease(body)
        } catch (e: Exception) {
            Log.e(TAG, "fetchLatest failed", e)
            null
        }
    }

    /**
     * Higher-level API: fetch + compare dengan [installedVersionCode].
     * Dipakai UpdateNotificationHelper.
     */
    suspend fun check(installedVersionCode: Int): UpdateResult = withContext(Dispatchers.IO) {
        val release = fetchLatest()
        when {
            release == null -> UpdateResult.Error("Gagal fetch release")
            release.versionCode > installedVersionCode -> UpdateResult.UpdateAvailable(
                UpdateInfo(
                    latestVersion = release.versionName,
                    versionCode   = release.versionCode,
                    releaseNotes  = release.changelog,
                    downloadUrl   = release.downloadUrl,
                    htmlUrl       = release.htmlUrl,
                )
            )
            else -> UpdateResult.UpToDate
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun parseRelease(json: String): ReleaseInfo? = try {
        val obj        = Json.parseToJsonElement(json).jsonObject
        val tagName    = obj["tag_name"]?.jsonPrimitive?.content ?: return null
        val body       = obj["body"]?.jsonPrimitive?.contentOrNull ?: ""
        val htmlUrl    = obj["html_url"]?.jsonPrimitive?.content ?: ""
        val preRelease = obj["prerelease"]?.jsonPrimitive?.boolean ?: false

        // strip "v" prefix dan suffix "-beta" dll
        val versionName = tagName.removePrefix("v").split("-").first().trim()

        val versionCode = extractVersionCode(body)
            ?: versionNameToCode(versionName)
            ?: return null

        val assets     = obj["assets"]?.jsonArray ?: JsonArray(emptyList())
        val apkAsset   = assets.map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true }
        val downloadUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content ?: htmlUrl

        ReleaseInfo(
            versionName   = versionName,
            versionCode   = versionCode,
            tagName       = tagName,
            downloadUrl   = downloadUrl,
            changelog     = body,
            htmlUrl       = htmlUrl,
            isPreRelease  = preRelease,
        )
    } catch (e: Exception) {
        Log.e(TAG, "parseRelease failed", e)
        null
    }

    /** Cari "versionCode: 260" di body release */
    private fun extractVersionCode(body: String): Int? {
        val regex = Regex("""versionCode\s*[=:]\s*(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(body)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** "2.6" → 260, "2.6.1" → 261 */
    private fun versionNameToCode(name: String): Int? = try {
        val parts = name.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        // Cocok dengan build.gradle: versionCode = 260 untuk v2.6.0
        // major*100 + minor*10 + patch → 2*100 + 6*10 + 0 = 260
        major * 100 + minor * 10 + patch
    } catch (_: Exception) { null }
}
