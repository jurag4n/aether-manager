package dev.aether.manager.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.aether.manager.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// UpdateDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UpdateDialog(
    info: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val s       = LocalStrings.current
    val ctx     = LocalContext.current
    val scope   = rememberCoroutineScope()

    var tab             by remember { mutableIntStateOf(0) }  // 0=Desc, 1=Changelog
    var downloading     by remember { mutableStateOf(false) }
    var progress        by remember { mutableFloatStateOf(0f) }  // 0..1, -1 = indeterminate
    var downloadDone    by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf<String?>(null) }
    var apkFile         by remember { mutableStateOf<File?>(null) }

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape  = RoundedCornerShape(24.dp),
            color  = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Header ──────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint   = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text  = s.updateAvailable,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text  = "v${info.versionName}  (build ${info.versionCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!downloading) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Tutup")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Tab ─────────────────────────────────────────────────────
                TabRow(
                    selectedTabIndex = tab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = {},
                    divider  = {},
                ) {
                    listOf(s.updateTabDesc, s.updateTabChangelog).forEachIndexed { i, label ->
                        Tab(
                            selected = tab == i,
                            onClick  = { tab = i },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (tab == i) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(vertical = 4.dp),
                            text = {
                                Text(
                                    text  = label,
                                    color = if (tab == i)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Tab content ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "updateTab"
                    ) { t ->
                        when (t) {
                            0 -> Text(
                                text  = s.updateAboutDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            else -> {
                                val cl = info.changelog.trim()
                                Text(
                                    text  = cl.ifBlank { s.updateChangelogEmpty },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Download progress ────────────────────────────────────────
                AnimatedVisibility(visible = downloading || downloadDone || errorMsg != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when {
                            errorMsg != null -> {
                                Text(
                                    text  = s.updateFailed.format(errorMsg),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            downloadDone -> {
                                Text(
                                    text  = s.updateDownloadDone,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            else -> {
                                Text(
                                    text  = if (progress < 0) s.updateDownloading
                                            else "${s.updateDownloading}  ${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(6.dp))
                                if (progress < 0) LinearProgressIndicator(Modifier.fillMaxWidth())
                                else LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Buttons ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Browser button
                    OutlinedButton(
                        onClick  = { ctx.openUrl(info.htmlUrl) },
                        enabled  = !downloading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Outlined.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(s.updateBtnBrowser, fontSize = 12.sp)
                    }

                    when {
                        // Error — retry
                        errorMsg != null -> Button(
                            onClick = {
                                errorMsg    = null
                                downloadDone = false
                                scope.launch {
                                    downloadApk(
                                        ctx, info.downloadUrl,
                                        onProgress   = { progress = it },
                                        onDone       = { file -> apkFile = file; downloadDone = true; downloading = false },
                                        onError      = { e -> errorMsg = e; downloading = false },
                                        onStart      = { downloading = true; progress = -1f },
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(s.updateBtnRetry, fontSize = 12.sp) }

                        // Download selesai — install
                        downloadDone -> Button(
                            onClick = { apkFile?.let { ctx.installApk(it) } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.InstallMobile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(s.updateInstalling, fontSize = 12.sp)
                        }

                        // Sedang download
                        downloading -> Button(
                            onClick  = {},
                            enabled  = false,
                            modifier = Modifier.weight(1f),
                        ) { Text(s.updateDownloading, fontSize = 12.sp) }

                        // Idle — Download & Install
                        else -> Button(
                            onClick = {
                                scope.launch {
                                    downloadApk(
                                        ctx, info.downloadUrl,
                                        onProgress = { progress = it },
                                        onDone     = { file -> apkFile = file; downloadDone = true; downloading = false },
                                        onError    = { e -> errorMsg = e; downloading = false },
                                        onStart    = { downloading = true; progress = -1f },
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(s.updateBtnDownload, fontSize = 12.sp)
                        }
                    }
                }

                // Later button
                if (!downloading) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(s.updateBtnLater, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Download helper
// ─────────────────────────────────────────────────────────────────────────────

private val dlClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

private suspend fun downloadApk(
    ctx: Context,
    url: String,
    onStart:    () -> Unit,
    onProgress: (Float) -> Unit,
    onDone:     (File) -> Unit,
    onError:    (String) -> Unit,
) = withContext(Dispatchers.IO) {
    withContext(Dispatchers.Main) { onStart() }
    try {
        val req  = Request.Builder().url(url).build()
        val resp = dlClient.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")

        val body   = resp.body ?: throw Exception("Empty body")
        val total  = body.contentLength()
        val file   = File(ctx.cacheDir, "aether_update.apk")

        file.outputStream().use { out ->
            val buf     = ByteArray(8192)
            var written = 0L
            body.byteStream().use { inp ->
                while (true) {
                    val n = inp.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    written += n
                    if (total > 0) {
                        val p = written.toFloat() / total
                        withContext(Dispatchers.Main) { onProgress(p) }
                    }
                }
            }
        }

        withContext(Dispatchers.Main) { onDone(file) }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Install APK via Intent
// ─────────────────────────────────────────────────────────────────────────────

private fun Context.installApk(file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: Exception) {
        openUrl("https://github.com/aetherdev01/aether-manager/releases/latest")
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {}
}
