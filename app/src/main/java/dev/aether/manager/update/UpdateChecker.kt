package dev.aether.manager.update

import dev.aether.manager.NativeAether
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val FALLBACK_GITHUB_API =
    "https://api.github.com/repos/aetherdev01/aether-manager/releases/latest"

data class ReleaseInfo(
    val latestVersion    : String,
    val latestVersionCode: Int,
    val releaseNotes     : String,
    val downloadUrl      : String,
    val releasePageUrl   : String,
)

sealed class UpdateResult {
    data class UpdateAvailable(val info: ReleaseInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {

    private fun resolveApiUrl(): String {
        if (NativeAether.isLoaded || NativeAether.tryLoad()) {
            val url = runCatching { NativeAether.nativeGetGithubApi() }.getOrNull()
            if (!url.isNullOrEmpty()) return url
        }
        return FALLBACK_GITHUB_API
    }

    suspend fun check(currentVersionCode: Int): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val apiUrl = resolveApiUrl()

            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AetherManager-Android")
                connectTimeout = 10_000
                readTimeout    = 10_000
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                return@withContext UpdateResult.Error("HTTP $responseCode")
            }

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json        = JSONObject(body)
            val tagName     = json.getString("tag_name")
            val releaseBody = json.optString("body", "").trim()
            val htmlUrl     = json.getString("html_url")

            val (versionName, versionCode) = parseTag(tagName)
                ?: return@withContext UpdateResult.Error("Tag tidak valid: $tagName")

            val assets      = json.getJSONArray("assets")
            val downloadUrl = findApkDownloadUrl(assets)
                ?: return@withContext UpdateResult.Error("Tidak ada APK di release ini")

            if (versionCode <= currentVersionCode) {
                return@withContext UpdateResult.UpToDate
            }

            UpdateResult.UpdateAvailable(
                ReleaseInfo(
                    latestVersion      = versionName,
                    latestVersionCode  = versionCode,
                    releaseNotes       = releaseBody,
                    downloadUrl        = downloadUrl,
                    releasePageUrl     = htmlUrl,
                )
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun parseTag(tag: String): Pair<String, Int>? {
        val raw = tag.trim()
        if (raw.contains('+')) {
            val idx  = raw.indexOf('+')
            val name = ensureVPrefix(raw.substring(0, idx))
            val code = raw.substring(idx + 1).toIntOrNull() ?: return null
            return name to code
        }
        val clean    = raw.trimStart('v', 'V')
        val segments = clean.split('.')
        if (segments.isEmpty()) return null
        val name = "v$clean"
        val code = when (segments.size) {
            1    -> (segments[0].toIntOrNull() ?: return null) * 10
            2    -> {
                val major = segments[0].toIntOrNull() ?: return null
                val minor = segments[1].toIntOrNull() ?: return null
                major * 10 + minor   // V2.6 → 26, V2.7 → 27, dst
            }
            else -> {
                val major = segments[0].toIntOrNull() ?: return null
                val minor = segments[1].toIntOrNull() ?: return null
                val patch = segments[2].toIntOrNull() ?: return null
                major * 100 + minor * 10 + patch
            }
        }
        return name to code
    }

    private fun ensureVPrefix(s: String): String =
        if (s.startsWith('v') || s.startsWith('V')) s.trim() else "v${s.trim()}"

    private fun findApkDownloadUrl(assets: org.json.JSONArray): String? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.getString("browser_download_url")
            }
        }
        return null
    }
}
