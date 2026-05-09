package dev.aether.manager.security

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.ui.components.AetherIconTile
import dev.aether.manager.util.SettingsPrefs
import kotlin.system.exitProcess

class SecurityBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val rawReason = intent.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "security_violation" }

        setContent {
            val themePreset = SettingsPrefs.getThemePreset(this)
            val darkOverride = SettingsPrefs.isDarkModeOverride(this)
            val darkTheme = if (darkOverride) SettingsPrefs.getDarkMode(this) else isSystemInDarkTheme()
            AetherTheme(
                darkTheme = darkTheme,
                dynamicColor = SettingsPrefs.getDynamicColor(this),
                themePreset = themePreset
            ) {
                SecurityBlockScreen(
                    rawReason = rawReason,
                    onClose = ::closeApp
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = Unit

    private fun closeApp() {
        runCatching { finishAndRemoveTask() }
        runCatching { android.os.Process.killProcess(android.os.Process.myPid()) }
        exitProcess(10)
    }

    companion object {
        const val EXTRA_REASON = "reason"
    }
}

@Composable
private fun SecurityBlockScreen(
    rawReason: String,
    onClose: () -> Unit,
) {
    BackHandler(enabled = true) { }

    val reasonUi = remember(rawReason) { securityReasonUi(rawReason) }
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AetherIconTile(
                        icon = Icons.Rounded.GppBad,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        size = 76.dp,
                        iconSize = 38.dp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                text = "Security Alert",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ReportProblem,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                            disabledLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Aether Security Protection",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Lingkungan aplikasi terdeteksi tidak aman, sehingga sesi dihentikan untuk melindungi lisensi, data, dan integritas aplikasi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    InfoSection(
                        title = "Alasan Deteksi",
                        body = reasonUi.title,
                        supporting = reasonUi.description
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(
                            text = "Close App",
                            modifier = Modifier.padding(start = 10.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    body: String,
    supporting: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

private data class SecurityReasonUi(
    val title: String,
    val description: String,
)

private fun securityReasonUi(rawReason: String): SecurityReasonUi {
    return when (rawReason.lowercase()) {
        "signature_mismatch" -> SecurityReasonUi(
            title = "Signature aplikasi tidak cocok",
            description = "Versi aplikasi yang berjalan memiliki signature berbeda dari yang diizinkan."
        )
        "signature_check_failed" -> SecurityReasonUi(
            title = "Validasi signature gagal",
            description = "Sistem tidak bisa menyelesaikan pengecekan signature aplikasi dengan benar."
        )
        "lucky_patcher_detected" -> SecurityReasonUi(
            title = "Terdeteksi Lucky Patcher",
            description = "Aplikasi mendeteksi komponen, package, file, atau jejak Lucky Patcher/Chelpus yang bisa memodifikasi APK dan lisensi."
        )
        "lspatch_detected" -> SecurityReasonUi(
            title = "Terdeteksi LSPatch",
            description = "Aplikasi mendeteksi LSPatch atau loader LSPatch. LSPosed, Zygisk, dan Riru tetap diizinkan; yang diblokir adalah LSPatch."
        )
        "lucky_patcher_or_lspatch_detected" -> SecurityReasonUi(
            title = "Terdeteksi Lucky Patcher / LSPatch",
            description = "Ada indikasi patcher seperti Lucky Patcher, Chelpus, LSPatch, atau tool modifikasi APK yang mengubah keamanan aplikasi."
        )
        "frida_detected", "frida_or_injector_detected", "frida_maps_detected", "frida_fd_detected", "frida_tmp_detected" -> SecurityReasonUi(
            title = "Terdeteksi Frida / Injector",
            description = "Aplikasi menemukan artefak Frida, gadget, atau injector aktif di proses. LSPosed, Zygisk, dan Riru tidak diblokir otomatis."
        )
        "native_integrity_tamper", "native_hook_detected" -> SecurityReasonUi(
            title = "Terdeteksi perubahan native",
            description = "Ada indikasi perubahan pada library native aplikasi. Deteksi ini tidak dipicu hanya karena LSPosed, Zygisk, atau Riru."
        )
        "runtime_tamper", "multi_signal_tamper", "native_tamper", "loader_tamper", "got_hook_detected", "hook_detected" -> SecurityReasonUi(
            title = "Modifikasi runtime terdeteksi",
            description = "Ada indikasi hooking, patching, atau injeksi yang mengubah perilaku asli aplikasi."
        )
        "apk_repack", "package_repack", "patch_detected", "cloner_detected" -> SecurityReasonUi(
            title = "Aplikasi hasil modifikasi atau cloning",
            description = "Paket aplikasi tampak telah direpack, dikloning, atau dimodifikasi di luar rilis resmi."
        )
        "debugger_detected" -> SecurityReasonUi(
            title = "Debugger terdeteksi",
            description = "Aplikasi mendeteksi proses debugging aktif yang dapat memengaruhi keamanan runtime."
        )
        "unity_tamper", "unity_ads_tamper", "elf_tamper", "apk_zip_tamper", "dex_tamper" -> SecurityReasonUi(
            title = "Integritas library terganggu",
            description = "Ada indikasi perubahan pada komponen native atau library penting aplikasi."
        )
        "security_check_failed", "native_not_loaded" -> SecurityReasonUi(
            title = "Pengecekan keamanan gagal",
            description = "Mesin keamanan tidak dapat dijalankan dengan benar sehingga aplikasi dihentikan untuk keamanan."
        )
        else -> SecurityReasonUi(
            title = "Aktivitas tidak wajar terdeteksi",
            description = "Sistem proteksi mendeteksi kondisi yang tidak sesuai dengan lingkungan aman aplikasi."
        )
    }
}
