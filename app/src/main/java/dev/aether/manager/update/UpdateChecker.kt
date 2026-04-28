package dev.aether.manager.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Data class hasil parse dari GitHub Releases API.
 *
 * GitHub tag format kita: "v2.6" atau "v2.6-beta" dsb.
 * versionCode di-embed di body release dengan format:
 *   versionCode: 260
 * (kalau tidak ada, fallback parse dari tag: "v2.6" → 260)
 */
data class ReleaseInfo(
    val versionName: String,   // e.g. "2.6"
    val versionCode: Int,      // e.g. 260
    val tagName: String,       // e.g. "v2.6"
    val downloadUrl: String,   // URL APK asset
    val changelog: String,     // body release (markdown)
    val htmlUrl: String,       // URL halaman release di browser
    val isPreRelease: Boolean,
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val REPO = "aetherdev01/aether-manager"
    private const val API  = "https://api.github.com/repos/$REPO/releases/latest"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetch release terbaru dari GitHub Releases API.
     * Return null kalau gagal (no internet, 404, parse error, dll).
     */
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

    // ─────────────────────────────────────────────────────────────────────────

    private fun parseRelease(json: String): ReleaseInfo? = try {
        val obj        = Json.parseToJsonElement(json).jsonObject
        val tagName    = obj["tag_name"]?.jsonPrimitive?.content ?: return null
        val body       = obj["body"]?.jsonPrimitive?.contentOrNull ?: ""
        val htmlUrl    = obj["html_url"]?.jsonPrimitive?.content ?: ""
        val preRelease = obj["prerelease"]?.jsonPrimitive?.boolean ?: false

        // ── Parse versionName dari tag ────────────────────────────────────
        // tag: "v2.6" / "v2.6.1" / "v2.6-beta"
        val versionName = tagName
            .removePrefix("v")
            .split("-").first()          // buang suffix "-beta", "-rc" dll
            .trim()

        // ── Parse versionCode ─────────────────────────────────────────────
        // Prioritas 1: cari "versionCode: 260" di body release
        // Prioritas 2: konversi dari versionName ("2.6" → 260, "2.6.1" → 261)
        val versionCode = extractVersionCode(body)
            ?: versionNameToCode(versionName)
            ?: return null

        // ── Cari APK asset ────────────────────────────────────────────────
        val assets     = obj["assets"]?.jsonArray ?: JsonArray(emptyList())
        val apkAsset   = assets
            .map { it.jsonObject }
            .firstOrNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: ""
                name.endsWith(".apk", ignoreCase = true)
            }
        val downloadUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content ?: htmlUrl

        ReleaseInfo(
            versionName  = versionName,
            versionCode  = versionCode,
            tagName      = tagName,
            downloadUrl  = downloadUrl,
            changelog    = body,
            htmlUrl      = htmlUrl,
            isPreRelease = preRelease,
        )
    } catch (e: Exception) {
        Log.e(TAG, "parseRelease failed", e)
        null
    }

    /**
     * Cari "versionCode: 260" atau "versionCode=260" di body release.
     * Case-insensitive, whitespace-tolerant.
     */
    private fun extractVersionCode(body: String): Int? {
        val regex = Regex("""versionCode\s*[=:]\s*(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(body)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Fallback: konversi versionName ke versionCode.
     *
     * "2.6"   → 260
     * "2.6.1" → 261
     * "2.10"  → 2100  (major*1000 + minor*10 + patch)
     *
     * Rumus: major * 1000 + minor * 10 + patch
     */
    private fun versionNameToCode(name: String): Int? = try {
        val parts = name.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        major * 1000 + minor * 10 + patch
    } catch (_: Exception) { null }
}
