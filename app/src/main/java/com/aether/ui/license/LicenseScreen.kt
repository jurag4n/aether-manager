package com.aether.ui.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.aether.payment.PaymentManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.i18n.LocalStrings
import com.aether.license.LicenseManager
import com.aether.license.LicensePrefs
import com.aether.license.LicenseViewModel
import com.aether.payment.InvoicePrefs
import com.aether.payment.PaymentViewModel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(onBack: () -> Unit) {
    val ctx   = LocalContext.current
    val licVm: LicenseViewModel  = viewModel()
    val payVm: PaymentViewModel  = viewModel()

    val licState by licVm.uiState.collectAsState()
    val payState by payVm.uiState.collectAsState()
    val showTimeoutWarning by payVm.showTimeoutWarning.collectAsState()

    var keyInput             by remember { mutableStateOf("") }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showBuySheet         by remember { mutableStateOf(false) }
    var buyerName            by remember { mutableStateOf("") }
    var showInvoiceHistory   by remember { mutableStateOf(false) }
    var resumeInvoice        by remember { mutableStateOf<InvoicePrefs.Invoice?>(null) }

    // Ticker untuk paksa recompose setelah aksi lokal (hapus invoice / deactivate)
    var refreshTick by remember { mutableIntStateOf(0) }
    fun triggerRefresh() { refreshTick++ }

    val isActive   = remember(licState, payState, refreshTick) { LicenseManager.isActive(ctx) }
    val currentKey = remember(licState, payState, refreshTick) { LicensePrefs.getKey(ctx) }
    val expiresAt  = remember(licState, payState, refreshTick) { LicensePrefs.getExpiry(ctx) }
    val deviceId   = remember { LicenseManager.getDeviceId(ctx) }
    val invoices   = remember(payState, refreshTick) { InvoicePrefs.getAll(ctx) }

    val s = LocalStrings.current

    val expLabel = when {
        !isActive        -> "-"
        expiresAt == -1L -> s.licenseExpireLifetime
        else             -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expiresAt))
    }

    val daysLeft = if (isActive && expiresAt != -1L) {
        val diff = expiresAt - System.currentTimeMillis()
        (diff / 86_400_000).coerceAtLeast(0)
    } else -1L

    LaunchedEffect(payState) {
        if (payState is PaymentViewModel.UiState.Success) {
            showBuySheet = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.licenseScreenTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                },
                actions = {
                    val pendingCount = invoices.count { it.status == "pending" }
                    BadgedBox(
                        badge = {
                            if (pendingCount > 0) Badge { Text(pendingCount.toString()) }
                        }
                    ) {
                        IconButton(onClick = { showInvoiceHistory = true }) {
                            Icon(Icons.Outlined.Receipt, s.licenseInvoiceHistoryIcon)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isActive) {
                PremiumActiveCard(expLabel = expLabel, daysLeft = daysLeft, currentKey = currentKey ?: "", ctx = ctx)
            }

            DeviceIdCard(deviceId = deviceId, ctx = ctx)

            AnimatedVisibility(visible = payState is PaymentViewModel.UiState.Polling) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(s.licensePollingWaiting, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        Text(s.licensePollingDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        ContactAdminRow(ctx)
                    }
                }
            }

            if (!isActive && payState !is PaymentViewModel.UiState.Polling) {
                PremiumBenefitCard(
                    onBuy     = { showBuySheet = true; payVm.reset() },
                    isLoading = payState is PaymentViewModel.UiState.CreatingOrder
                )

                val pendingInvoices = invoices.filter { it.status == "pending" }
                if (pendingInvoices.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Pending, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                Text(s.licensePendingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            pendingInvoices.take(1).forEach { inv ->
                                val fmt = NumberFormat.getNumberInstance(Locale("id","ID"))
                                Text("${inv.orderId}\n${inv.dateLabel} · Rp ${fmt.format(inv.nominal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Button(
                                    onClick  = { resumeInvoice = inv },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(10.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Outlined.Replay, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(s.licenseContinuePaymentBtn)
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.VpnKey, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(s.licenseHaveKeyTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedTextField(
                            value = keyInput, onValueChange = { keyInput = it.trim().filter { ch -> ch.isLetterOrDigit() }.take(10) },
                            label = { Text(s.licenseKeyInputLabel) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = licState !is LicenseViewModel.UiState.Loading
                        )
                        if (licState is LicenseViewModel.UiState.Failure) {
                            Text((licState as LicenseViewModel.UiState.Failure).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick  = { licVm.activate(keyInput); keyInput = "" },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            enabled  = keyInput.isNotBlank() && licState !is LicenseViewModel.UiState.Loading
                        ) {
                            if (licState is LicenseViewModel.UiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else { Text(s.licenseActivateBtn) }
                        }
                    }
                }
            }

            if (isActive) {
                OutlinedButton(
                    onClick  = { showDeactivateDialog = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.LinkOff, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.licenseDeactivateBtn)
                }
            }
        }
    }

    if (showBuySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBuySheet = false; payVm.reset() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val s = payState
            when {
                s is PaymentViewModel.UiState.Idle ||
                s is PaymentViewModel.UiState.CreatingOrder ||
                s is PaymentViewModel.UiState.Failure -> {
                    BuyFormContent(
                        buyerName = buyerName,
                        deviceId = deviceId,
                        ctx = ctx,
                        onNameChange = { buyerName = it },
                        isLoading = s is PaymentViewModel.UiState.CreatingOrder,
                        error     = (s as? PaymentViewModel.UiState.Failure)?.message,
                        onSubmit  = { payVm.createOrder(buyerName) }
                    )
                }
                s is PaymentViewModel.UiState.WaitingTransfer -> {
                    LaunchedEffect(s.orderId) {
                        InvoicePrefs.add(ctx, InvoicePrefs.Invoice(
                            orderId        = s.orderId,
                            name           = buyerName,
                            deviceId       = s.deviceId,
                            nominal        = s.nominal,
                            createdAt      = System.currentTimeMillis(),
                            paymentMethods = s.paymentMethods,
                            status         = "pending",
                        ))
                    }
                    TransferInstructionContent(
                        state = s, ctx = ctx,
                        onConfirm = { selectedMethod ->
                            sendPaymentNotificationSilent(ctx, buyerName, s.deviceId, s.orderId, listOfNotNull(selectedMethod))
                            payVm.confirmAndPoll()
                            showBuySheet = false
                        },
                        onCancel  = { showBuySheet = false; payVm.reset() }
                    )
                }
                else -> {}
            }
        }
    }

    resumeInvoice?.let { inv ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { resumeInvoice = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TransferInstructionContent(
                state = PaymentViewModel.UiState.WaitingTransfer(
                    orderId = inv.orderId, paymentMethods = inv.paymentMethods, nominal = inv.nominal, deviceId = inv.deviceId
                ),
                ctx = ctx,
                onConfirm = { selectedMethod ->
                    sendPaymentNotificationSilent(ctx, inv.name, inv.deviceId, inv.orderId, listOfNotNull(selectedMethod))
                    payVm.resumePoll(inv.orderId)
                    resumeInvoice = null
                },
                onCancel  = { resumeInvoice = null }
            )
        }
    }

    if (showInvoiceHistory) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInvoiceHistory = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            InvoiceHistoryContent(
                invoices = invoices, ctx = ctx,
                onResume = { inv -> showInvoiceHistory = false; resumeInvoice = inv },
                onDelete = { inv ->
                    InvoicePrefs.remove(ctx, inv.orderId)
                    triggerRefresh()
                }
            )
        }
    }

    // ── Timeout warning dialog (2 menit belum dikonfirmasi) ──────────────────
    if (showTimeoutWarning) {
        AlertDialog(
            onDismissRequest = { /* user harus memilih action */ },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AccessTime, null, tint = Color(0xFFFF9800), modifier = Modifier.size(34.dp))
                }
            },
            title = {
                Text(
                    "Belum Dikonfirmasi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Pembayaranmu belum dikonfirmasi admin dalam 2 menit. Silakan hubungi admin untuk mempercepat proses.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ContactAdminRow(ctx)
                }
            },
            confirmButton = {
                TextButton(onClick = { /* tutup peringatan, tetap polling */ payVm.dismissTimeoutWarning() }) {
                    Text("Lanjut Tunggu", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { payVm.reset() }) {
                    Text("Batalkan", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (licState is LicenseViewModel.UiState.Success) {
        PremiumSuccessDialog(
            licenseKey = (licState as LicenseViewModel.UiState.Success).licenseKey,
            expLabel   = expLabel,
            onDismiss  = { licVm.reset() }
        )
    }

    // Saat payment selesai, paksa refresh list invoice (supaya status berubah jadi "paid")
    LaunchedEffect(payState) {
        if (payState is PaymentViewModel.UiState.Success) triggerRefresh()
    }

    if (payState is PaymentViewModel.UiState.Success) {
        PremiumSuccessDialog(
            licenseKey = (payState as PaymentViewModel.UiState.Success).licenseKey,
            expLabel   = expLabel,
            onDismiss  = { payVm.reset(); triggerRefresh() }
        )
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text(s.licenseDeactivateDialogTitle) },
            text  = { Text(s.licenseDeactivateDialogBody) },
            confirmButton = {
                TextButton(onClick = { licVm.deactivate(); showDeactivateDialog = false; triggerRefresh() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.licenseDeactivateConfirmBtn) }
            },
            dismissButton = { TextButton(onClick = { showDeactivateDialog = false }) { Text(s.licenseDeactivateCancelBtn) } }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumActiveCard(expLabel: String, daysLeft: Long, currentKey: String, ctx: Context) {
    val s         = LocalStrings.current
    val primary   = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.WorkspacePremium, null, tint = onPrimary, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(s.licensePremiumActive, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(s.licensePremiumValidUntil.format(expLabel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (daysLeft >= 0) {
                    Surface(shape = RoundedCornerShape(8.dp), color = primary) {
                        Text(s.licenseDaysLeft.format(daysLeft), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = onPrimary)
                    }
                }
            }

            HorizontalDivider(color = primary.copy(alpha = 0.2f))

            Text(s.licenseYourBenefits, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                Icons.Outlined.Block        to s.licenseSuccessBenefits.getOrElse(0) { "" },
                Icons.Outlined.Speed        to s.licenseSuccessBenefits.getOrElse(1) { "" },
                Icons.Outlined.Devices      to s.licenseBenefitDeviceLocked,
                Icons.Outlined.SupportAgent to s.licenseSuccessBenefits.getOrElse(2) { "" },
                Icons.Outlined.NewReleases  to s.licenseSuccessBenefits.getOrElse(3) { "" },
            ).forEach { (icon, text) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = primary)
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            HorizontalDivider(color = primary.copy(alpha = 0.2f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.licenseKeyLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("License", currentKey))
                            Toast.makeText(ctx, s.licenseKeyCopied, Toast.LENGTH_SHORT).show()
                        }.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(currentKey, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (daysLeft in 0..7) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Text(s.licenseExpiringSoon.format(expLabel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}


@Composable
private fun DeviceIdCard(deviceId: String, ctx: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Devices, null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Device ID", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(deviceId, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("ID ini dipakai untuk aktivasi lisensi. Tanpa nomor ponsel.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Aether Device ID", deviceId))
                Toast.makeText(ctx, "Device ID disalin", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Outlined.ContentCopy, null)
            }
        }
    }
}


@Composable
private fun PremiumBenefitCard(onBuy: () -> Unit, isLoading: Boolean) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.WorkspacePremium, null, modifier = Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Aether Manager Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Lisensi resmi 1 bulan, diproses lewat invoice real dan konfirmasi admin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(s.licensePrice1Month, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s.licensePrice, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Verified, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Official", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CleanBenefitRow(Icons.Outlined.Block, s.licenseSuccessBenefits.getOrElse(0) { "Tanpa iklan" })
                CleanBenefitRow(Icons.Outlined.Speed, s.licenseSuccessBenefits.getOrElse(1) { "Fitur tweak premium" })
                CleanBenefitRow(Icons.Outlined.Devices, s.licenseBenefitDeviceLocked)
                CleanBenefitRow(Icons.Outlined.SupportAgent, s.licenseSuccessBenefits.getOrElse(2) { "Support admin" })
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            Text("Metode pembayaran", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PaymentPreviewChip("DANA", Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f))
                PaymentPreviewChip("GoPay", Icons.Outlined.Wallet, Modifier.weight(1f))
                PaymentPreviewChip("PayPal", Icons.Outlined.Language, Modifier.weight(1f))
            }
            Text("PayPal hanya untuk pembeli internasional. Pembeli Indonesia disarankan memakai DANA atau GoPay.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(onClick = onBuy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Outlined.ShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Beli lisensi resmi", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CleanBenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, modifier = Modifier.padding(7.dp).size(15.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PaymentPreviewChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun BuyFormContent(
    buyerName: String,
    deviceId: String,
    ctx: Context,
    onNameChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onSubmit: () -> Unit,
) {
    val isFormValid = buyerName.isNotBlank()

    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.ReceiptLong, null, modifier = Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Buat invoice lisensi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Isi nama pembeli. Device ID otomatis dipakai sebagai identitas aktivasi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Alur pembelian", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                StepRow(1, "Buat invoice dari aplikasi")
                StepRow(2, "Transfer sesuai nominal invoice")
                StepRow(3, "Kirim bukti transfer ke admin Telegram")
                StepRow(4, "Admin cek nama dan Device ID, lalu lisensi aktif otomatis")
            }
        }

        OutlinedTextField(
            value = buyerName,
            onValueChange = onNameChange,
            label = { Text("Nama pembeli") },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        )

        CopyBox(ctx = ctx, label = "Device ID", value = deviceId, toast = "Device ID disalin")

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Security, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Lisensi dikunci ke Device ID ini. Tidak ada nomor ponsel yang dikirim ke server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = isFormValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Outlined.ReceiptLong, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buat invoice", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TransferInstructionContent(
    state: PaymentViewModel.UiState.WaitingTransfer,
    ctx: Context,
    onConfirm: (selected: PaymentManager.PaymentMethod?) -> Unit, onCancel: () -> Unit,
) {
    val isInternational = false
    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val realMethods = remember(state.paymentMethods, isInternational) {
        state.paymentMethods
            .filter { it.isRealPaymentMethod() }
            .sortedBy { it.realPaymentSort() }
    }
    var selectedMethodId by remember(realMethods) { mutableStateOf(realMethods.firstOrNull()?.id ?: "") }
    val selectedMethod = realMethods.firstOrNull { it.id == selectedMethodId } ?: realMethods.firstOrNull()

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Payments, null, modifier = Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (isInternational) "Payment details" else "Detail pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (isInternational) "Manual payment. Admin will activate the license after the payment is verified." else "Transfer manual, lalu admin mengaktifkan lisensi setelah pembayaran valid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(if (isInternational) "Amount to pay" else "Total bayar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rp ${fmt.format(state.nominal)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
                        Text("1 Bulan", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                CopyBox(ctx = ctx, label = "Order ID", value = state.orderId, toast = "Order ID disalin")
            }
        }

        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (isInternational) "Real process" else "Proses real", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                StepRow(1, if (isInternational) "Choose PayPal or another available method" else "Pilih metode pembayaran")
                StepRow(2, if (isInternational) "Pay exactly according to the invoice" else "Transfer tepat sesuai nominal invoice")
                StepRow(3, if (isInternational) "Tap Confirm Payment" else "Tap Konfirmasi Pembayaran")
                StepRow(4, if (isInternational) "Send payment screenshot to Telegram" else "Kirim screenshot bukti transfer ke Telegram")
                StepRow(5, if (isInternational) "Admin verifies and activates your license" else "Admin cek pembayaran dan lisensi aktif otomatis")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isInternational) "Choose payment method" else "Pilih metode pembayaran", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (realMethods.isEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Metode pembayaran belum tersedia. Pastikan PaymentManager hanya mengirim DANA, GoPay, dan PayPal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            } else {
                PaymentMethodGrid(methods = realMethods, selectedId = selectedMethodId, onSelect = { selectedMethodId = it })
                AnimatedContent(
                    targetState = selectedMethod,
                    transitionSpec = { (fadeIn(tween(160)) + slideInVertically { it / 6 }).togetherWith(fadeOut(tween(120))) },
                    label = "payment_detail"
                ) { method ->
                    if (method != null) SelectedMethodDetail(method = method, ctx = ctx)
                }
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isInternational) "Pay the exact amount" else "Wajib transfer tepat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(if (isInternational) "Please pay exactly Rp ${fmt.format(state.nominal)} or the equivalent amount requested by admin. Different amounts may slow down verification." else "Transfer sebesar Rp ${fmt.format(state.nominal)}. Nominal berbeda bisa membuat proses verifikasi lebih lama.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Send, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isInternational) "Send payment proof" else "Kirim bukti transfer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (isInternational) "After confirmation, Telegram admin will open. Paste the copied order message and attach your payment screenshot." else "Setelah konfirmasi, Telegram admin akan dibuka. Paste pesan order lalu lampirkan screenshot bukti transfer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Button(
            onClick = { onConfirm(selectedMethod) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedMethod != null
        ) {
            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isInternational) "Confirm payment" else "Konfirmasi pembayaran", fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(if (isInternational) "Cancel invoice" else "Batalkan invoice", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CopyBox(ctx: Context, label: String, value: String, toast: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .clickable {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(label, value))
                    Toast.makeText(ctx, toast, Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentMethodGrid(
    methods: List<PaymentManager.PaymentMethod>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        methods.forEach { method ->
            val isSelected = method.id == selectedId
            val animatedBorder by animateDpAsState(if (isSelected) 2.dp else 1.dp, label = "payment_border")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = animatedBorder,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(method.id) },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                        Icon(paymentIcon(method.id), null, modifier = Modifier.padding(10.dp).size(20.dp), tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(realPaymentLabel(method), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(realPaymentDescription(method), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSelected) Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SelectedMethodDetail(method: PaymentManager.PaymentMethod, ctx: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(paymentIcon(method.id), null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(realPaymentLabel(method), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (method.isPayPalMethod()) "International only" else "Indonesia payment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            CopyBox(ctx = ctx, label = if (method.isPayPalMethod()) "Akun PayPal" else "Nomor tujuan", value = method.number, toast = "Tujuan pembayaran disalin")

            if (method.holderName.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(method.holderName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(realPaymentNote(method), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun PaymentManager.PaymentMethod.isDanaMethod(): Boolean {
    val q = "${id} ${label}".lowercase(Locale.ROOT)
    return "dana" in q
}

private fun PaymentManager.PaymentMethod.isGoPayMethod(): Boolean {
    val q = "${id} ${label}".lowercase(Locale.ROOT)
    return "gopay" in q || "go pay" in q
}

private fun PaymentManager.PaymentMethod.isPayPalMethod(): Boolean {
    val q = "${id} ${label}".lowercase(Locale.ROOT)
    return "paypal" in q || "pay pal" in q
}

private fun PaymentManager.PaymentMethod.isRealPaymentMethod(): Boolean = isDanaMethod() || isGoPayMethod() || isPayPalMethod()

private fun PaymentManager.PaymentMethod.realPaymentSort(): Int = when {
    isDanaMethod() -> 0
    isGoPayMethod() -> 1
    isPayPalMethod() -> 2
    else -> 99
}

private fun realPaymentLabel(method: PaymentManager.PaymentMethod): String = when {
    method.isDanaMethod() -> "DANA"
    method.isGoPayMethod() -> "GoPay"
    method.isPayPalMethod() -> "PayPal"
    else -> method.label
}

private fun realPaymentDescription(method: PaymentManager.PaymentMethod): String = when {
    method.isDanaMethod() -> "E-wallet Indonesia"
    method.isGoPayMethod() -> "E-wallet Indonesia"
    method.isPayPalMethod() -> "Khusus internasional"
    else -> method.label
}

private fun realPaymentNote(method: PaymentManager.PaymentMethod): String = when {
    method.isPayPalMethod() -> "Gunakan PayPal hanya untuk pembeli dari luar Indonesia. Sertakan Order ID pada catatan pembayaran jika tersedia."
    else -> "Gunakan nama pembayaran yang sama dengan nama pembeli, lalu simpan bukti transfer sampai lisensi aktif."
}

private fun paymentIcon(id: String): androidx.compose.ui.graphics.vector.ImageVector = when (id) {
    "gopay", "ovo", "dana", "shopeepay", "linkaja" -> Icons.Outlined.Wallet
    "bri", "bca", "mandiri", "bni", "seabank"      -> Icons.Outlined.AccountBalance
    "paypal"                                        -> Icons.Outlined.Language
    else                                            -> Icons.Outlined.Payment
}


/**
 * Setelah user menekan "Sudah Bayar", buat template konfirmasi berisi nama, Device ID, Order ID, dan metode pembayaran.
 */
/**
 * Setelah user konfirmasi pembayaran, buka Telegram admin dan salin template pesan.
 * User tinggal paste pesan lalu lampirkan screenshot bukti transfer.
 */
private fun sendPaymentNotificationSilent(
    ctx: Context,
    buyerName: String,
    buyerDeviceId: String,
    orderId: String,
    paymentMethods: List<PaymentManager.PaymentMethod>,
) {
    val method = paymentMethods.firstOrNull()?.let { realPaymentLabel(it) } ?: "Not selected"
    val msg = buildString {
        appendLine("Konfirmasi Pembayaran Aether Manager")
        appendLine()
        appendLine("Nama: $buyerName")
        appendLine("Device ID: $buyerDeviceId")
        appendLine("Order ID: $orderId")
        appendLine("Metode: $method")
        appendLine()
        appendLine("Saya sudah transfer. Berikut saya kirim screenshot bukti pembayaran.")
    }.trim()

    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Aether Payment Confirmation", msg))
    Toast.makeText(ctx, "Pesan konfirmasi disalin. Kirim screenshot bukti transfer ke Telegram admin.", Toast.LENGTH_LONG).show()

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AetherDev22"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, msg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(Intent.createChooser(intent, "Kirim bukti pembayaran").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun ContactAdminRow(ctx: Context) {
    val s = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AetherDev22"))) },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.Chat, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Telegram Admin", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
            onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AetherDev22"))) },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.licenseContactTelegram, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StepRow(step: Int, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text(step.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InvoiceHistoryContent(
    invoices: List<InvoicePrefs.Invoice>,
    ctx: Context,
    onResume: (InvoicePrefs.Invoice) -> Unit,
    onDelete: (InvoicePrefs.Invoice) -> Unit,
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(s.licenseInvoiceHistoryTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(s.licenseInvoiceHistoryEmpty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            invoices.forEach { inv ->
                val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
                val statusColor = when (inv.status) {
                    "paid"    -> Color(0xFF4CAF50)
                    "expired" -> MaterialTheme.colorScheme.error
                    else      -> MaterialTheme.colorScheme.secondary
                }
                val statusLabel = when (inv.status) {
                    "paid"    -> s.licenseInvoiceStatusPaid
                    "expired" -> s.licenseInvoiceStatusExpired
                    else      -> s.licenseInvoiceStatusPending
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(inv.orderId, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                                Text(statusLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rp ${fmt.format(inv.nominal)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(inv.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${inv.name} · Device ${inv.deviceId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        when (inv.status) {
                            "pending" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onResume(inv) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Outlined.Replay, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.licenseInvoiceContinueBtn, style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(onClick = { onDelete(inv) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.licenseInvoiceDeleteBtn, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            "paid", "expired" -> Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { onDelete(inv) }, shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(s.licenseInvoiceDeleteBtn, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSuccessDialog(licenseKey: String, expLabel: String, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val green = Color(0xFF2E7D32)
    val ctx = LocalContext.current

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "successScale"
    )
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180)
        showContent = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = green.copy(alpha = 0.10f), modifier = Modifier.size(96.dp)) {}
                    Surface(shape = CircleShape, color = green.copy(alpha = 0.16f), modifier = Modifier.size(76.dp)) {}
                    Surface(shape = CircleShape, color = green, modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                AnimatedVisibility(visible = showContent, enter = fadeIn(tween(220)) + slideInVertically { it / 5 }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = green.copy(alpha = 0.12f)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Verified, null, tint = green, modifier = Modifier.size(15.dp))
                                Text("Premium Active", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = green)
                            }
                        }
                        Text(s.licenseSuccessTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Text(s.licenseSuccessBody, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(s.licenseSuccessKeyLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)).clickable {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Aether License", licenseKey))
                                Toast.makeText(ctx, s.licenseKeyCopied, Toast.LENGTH_SHORT).show()
                            }.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(licenseKey, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EventAvailable, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(s.licenseSuccessValidUntil.format(expLabel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                AnimatedVisibility(visible = showContent, enter = fadeIn(tween(300)) + expandVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        s.licenseSuccessBenefits.take(4).forEach { label ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(10.dp), color = green.copy(alpha = 0.10f)) {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = green, modifier = Modifier.padding(6.dp).size(14.dp))
                                }
                                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = green)
                ) {
                    Icon(Icons.Outlined.RocketLaunch, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.licenseSuccessStartBtn, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
