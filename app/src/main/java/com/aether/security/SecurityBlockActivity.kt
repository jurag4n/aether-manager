package com.aether.security

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import com.aether.ui.AetherTheme

class SecurityBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val rawReason = intent.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "security_violation" }

        setContent {
            AetherTheme {
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
        finish()
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
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GppBad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(38.dp)
                        )
                    }

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
                        text = "Validasi keamanan aplikasi gagal. Proteksi ini hanya memakai pengecekan penting agar lebih akurat dan tidak mudah false positive.",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoSection(
                        title = "Yang Bisa Dilakukan",
                        body = "Cara memperbaiki",
                        supporting = reasonUi.fix
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoSection(
                        title = "Kode Deteksi",
                        body = rawReason.ifBlank { "security_violation" },
                        supporting = "Kode ini membantu developer tahu pengecekan mana yang gagal."
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
                            text = "Tutup",
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
    val fix: String,
)

private fun securityReasonUi(rawReason: String): SecurityReasonUi {
    return when (rawReason.lowercase()) {
        "signature_mismatch" -> SecurityReasonUi(
            title = "Signature aplikasi tidak cocok",
            description = "APK tidak ditandatangani dengan sertifikat resmi. Biasanya terjadi karena APK hasil modifikasi, repack, atau memakai key signing berbeda.",
            fix = "Install ulang APK resmi. Kalau ini build baru dari developer, update SHA-256 signature resmi di file native security."
        )
        "signature_check_failed" -> SecurityReasonUi(
            title = "Signature tidak bisa dibaca",
            description = "Sistem gagal membaca sertifikat APK. Penyebab umum: APK rusak, proses install tidak sempurna, atau metadata signing tidak valid.",
            fix = "Uninstall aplikasi, lalu install ulang APK yang masih utuh. Pastikan APK belum diubah setelah proses signing."
        )
        "package_mismatch" -> SecurityReasonUi(
            title = "Package name tidak sesuai",
            description = "Nama paket aplikasi berbeda dari package resmi yang diizinkan oleh proteksi.",
            fix = "Gunakan package resmi aplikasi. Jika package sengaja diganti, sesuaikan EXPECTED_PACKAGE di AetherSecurityNative.kt."
        )
        "native_not_loaded" -> SecurityReasonUi(
            title = "Library security tidak termuat",
            description = "File libaethersec.so tidak ditemukan atau gagal dimuat untuk ABI perangkat ini.",
            fix = "Pastikan CMake membuild libaethersec.so dan APK membawa library native untuk ABI target."
        )
        "security_check_failed" -> SecurityReasonUi(
            title = "Pengecekan keamanan gagal",
            description = "Aplikasi gagal menjalankan validasi signature karena error runtime.",
            fix = "Coba install ulang. Jika masih gagal, cek logcat pada tag AetherSecurity untuk melihat penyebab teknisnya."
        )
        else -> SecurityReasonUi(
            title = "Validasi keamanan gagal",
            description = "Aplikasi dihentikan karena pengecekan signature/package tidak lolos.",
            fix = "Gunakan APK resmi yang ditandatangani dengan key yang sesuai."
        )
    }
}
