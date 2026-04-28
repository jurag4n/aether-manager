package dev.aether.manager.ui.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.window.Dialog
import dev.aether.manager.payment.PaymentManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.license.LicenseManager
import dev.aether.manager.license.LicensePrefs
import dev.aether.manager.license.LicenseViewModel
import dev.aether.manager.payment.InvoicePrefs
import dev.aether.manager.payment.PaymentViewModel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
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
    var buyerPhone           by remember { mutableStateOf("") }
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
                            value = keyInput, onValueChange = { keyInput = it.uppercase() },
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
                        buyerName = buyerName, buyerPhone = buyerPhone,
                        onNameChange = { buyerName = it }, onPhoneChange = { buyerPhone = it },
                        isLoading = s is PaymentViewModel.UiState.CreatingOrder,
                        error     = (s as? PaymentViewModel.UiState.Failure)?.message,
                        onSubmit  = { payVm.createOrder(buyerName, buyerPhone) }
                    )
                }
                s is PaymentViewModel.UiState.WaitingTransfer -> {
                    LaunchedEffect(s.orderId) {
                        InvoicePrefs.add(ctx, InvoicePrefs.Invoice(
                            orderId        = s.orderId,
                            name           = buyerName,
                            phone          = buyerPhone,
                            nominal        = s.nominal,
                            createdAt      = System.currentTimeMillis(),
                            paymentMethods = s.paymentMethods,
                            status         = "pending",
                        ))
                    }
                    TransferInstructionContent(
                        state = s, ctx = ctx,
                        onConfirm = { selectedMethod ->
                            sendPaymentNotificationSilent(ctx, buyerName, s.orderId, listOfNotNull(selectedMethod))
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
                    orderId = inv.orderId, paymentMethods = inv.paymentMethods, nominal = inv.nominal
                ),
                ctx = ctx,
                onConfirm = { selectedMethod ->
                    sendPaymentNotificationSilent(ctx, inv.name, inv.orderId, listOfNotNull(selectedMethod))
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
private fun FreeStatusCard() {
    val s = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.LockOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
            Text(s.licenseFreeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(s.licenseFreeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PremiumBenefitCard(onBuy: () -> Unit, isLoading: Boolean) {
    val s = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.licenseUpgradeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(s.licenseUpgradeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                Icons.Outlined.Block        to s.licenseSuccessBenefits.getOrElse(0) { "" },
                Icons.Outlined.Speed        to s.licenseSuccessBenefits.getOrElse(1) { "" },
                Icons.Outlined.Devices      to s.licenseBenefitDeviceLocked,
                Icons.Outlined.SupportAgent to s.licenseSuccessBenefits.getOrElse(2) { "" },
                Icons.Outlined.NewReleases  to s.licenseSuccessBenefits.getOrElse(3) { "" },
            ).forEach { (icon, text) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(text, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(s.licensePrice1Month, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.licensePrice, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Button(onClick = onBuy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Outlined.ShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.licenseBuyBtn)
                }
            }
        }
    }
}

@Composable
private fun BuyFormContent(
    buyerName: String, buyerPhone: String,
    onNameChange: (String) -> Unit, onPhoneChange: (String) -> Unit,
    isLoading: Boolean, error: String?, onSubmit: () -> Unit,
) {
    val s = LocalStrings.current
    // Validasi nomor realtime — hanya tampil setelah user mulai mengetik
    val phoneError = remember(buyerPhone) {
        if (buyerPhone.isBlank()) null // belum diisi, jangan tampil error dulu
        else PaymentViewModel.validatePhone(buyerPhone)
    }
    val isPhoneValid = buyerPhone.isNotBlank() && phoneError == null
    val isFormValid  = buyerName.isNotBlank() && isPhoneValid

    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.ShoppingCart, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(s.licenseBuySheetTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(s.licenseBuySheetSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        Text(s.licenseBuySheetFormDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Field: Nama
        OutlinedTextField(
            value = buyerName, onValueChange = onNameChange,
            label = { Text(s.licenseBuySheetNameLabel) },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(12.dp), enabled = !isLoading
        )

        // Field: Nomor HP — wajib valid sebelum bisa submit
        OutlinedTextField(
            value = buyerPhone, onValueChange = onPhoneChange,
            label = { Text(s.licenseBuySheetPhoneLabel) },
            leadingIcon = { Icon(Icons.Outlined.Phone, null) },
            trailingIcon = {
                when {
                    buyerPhone.isNotBlank() && isPhoneValid ->
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    phoneError != null ->
                        Icon(Icons.Outlined.Error, null, tint = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError != null,
            supportingText = when {
                phoneError != null -> {{ Text(phoneError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }}
                else               -> null
            },
            enabled = !isLoading
        )

        // Info hint nomor HP
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.licenseBuySheetPhoneHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

        Button(
            onClick = onSubmit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            enabled = isFormValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else { Text(s.licenseBuySheetCreateInvoiceBtn) }
        }
    }
}

@Composable
private fun TransferInstructionContent(
    state: PaymentViewModel.UiState.WaitingTransfer,
    ctx: Context, onConfirm: (selected: PaymentManager.PaymentMethod?) -> Unit, onCancel: () -> Unit,
) {
    val s   = LocalStrings.current
    val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    // State untuk metode yang dipilih
    var selectedMethodId by remember {
        mutableStateOf(state.paymentMethods.firstOrNull()?.id ?: "")
    }
    val selectedMethod = state.paymentMethods.firstOrNull { it.id == selectedMethodId }
        ?: state.paymentMethods.firstOrNull()

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.AccountBalance, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(s.licensePayDetailTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(s.licensePayDetailSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Langkah-langkah ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(s.licenseStepNoteOrderId, s.licenseStepTransferExact, s.licenseStepTapPaid, s.licenseStepWaitAdmin)
                .forEachIndexed { i, label -> StepRow(step = i + 1, label = label) }
        }

        HorizontalDivider()

        // ── Order ID ────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.licenseOrderIdLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Order ID", state.orderId))
                        Toast.makeText(ctx, s.licenseOrderIdCopied, Toast.LENGTH_SHORT).show()
                    }.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(state.orderId, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(s.licenseOrderIdWarning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ── Total ────────────────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape  = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(s.licenseTotalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Rp ${fmt.format(state.nominal)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                    Text(s.licenseDuration1Month, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Pilih Metode Pembayaran ───────────────────────────────────────────
        if (state.paymentMethods.isNotEmpty()) {
            Text(
                s.licenseSelectPayMethod,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Pisah e-wallet, bank, dan international
            val ewallets      = state.paymentMethods.filter { it.type == "ewallet" }
            val banks         = state.paymentMethods.filter { it.type == "bank" }
            val international = state.paymentMethods.filter { it.type == "international" }

            if (ewallets.isNotEmpty()) {
                Text(s.licensePayTypeEwallet, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PaymentMethodGrid(
                    methods    = ewallets,
                    selectedId = selectedMethodId,
                    onSelect   = { selectedMethodId = it }
                )
            }

            if (banks.isNotEmpty()) {
                Text(s.licensePayTypeBank, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PaymentMethodGrid(
                    methods    = banks,
                    selectedId = selectedMethodId,
                    onSelect   = { selectedMethodId = it }
                )
            }

            if (international.isNotEmpty()) {
                Text(s.licensePayTypeInternational, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PaymentMethodGrid(
                    methods    = international,
                    selectedId = selectedMethodId,
                    onSelect   = { selectedMethodId = it }
                )
            }

            // ── Detail metode terpilih ────────────────────────────────────────
            AnimatedContent(
                targetState = selectedMethod,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }).togetherWith(fadeOut())
                },
                label = "payment_detail"
            ) { method ->
                if (method != null) {
                    SelectedMethodDetail(method = method, ctx = ctx)
                }
            }
        }

        // ── Peringatan jumlah tepat ───────────────────────────────────────────
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.licenseImportantLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(s.licenseImportantBody.format(fmt.format(state.nominal)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Text(s.licenseAfterTransferLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ContactAdminRow(ctx)

        Button(onClick = { onConfirm(selectedMethod) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.licenseVerifyNowBtn, fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(s.licenseCancelBtn, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentMethodGrid(
    methods: List<PaymentManager.PaymentMethod>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    // Chip grid 3 kolom
    val rows = methods.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { method ->
                    val isSelected = method.id == selectedId
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(method.id) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier            = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val icon = paymentIcon(method.id)
                            Icon(
                                icon, null,
                                modifier = Modifier.size(20.dp),
                                tint     = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                method.label,
                                style    = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color    = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
                // Isi sisa kolom kosong agar grid rapi
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SelectedMethodDetail(method: PaymentManager.PaymentMethod, ctx: Context) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        paymentIcon(method.id), null,
                        modifier = Modifier.padding(6.dp).size(16.dp),
                        tint     = MaterialTheme.colorScheme.primary
                    )
                }
                Text(method.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(
                        if (method.type == "ewallet") s.licensePayTypeEwallet
                        else if (method.type == "international") s.licensePayTypeInternational
                        else s.licensePayTypeBank,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            // Nomor rekening / dompet
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText(method.label, method.number))
                        Toast.makeText(ctx, s.licenseNumberCopied.format(method.label), Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        method.number,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        ${method.holderName},
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(
                        modifier  = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(s.licenseCopyLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun paymentIcon(id: String): androidx.compose.ui.graphics.vector.ImageVector = when (id) {
    "gopay", "ovo", "dana", "shopeepay", "linkaja" -> Icons.Outlined.Wallet
    "bri", "bca", "mandiri", "bni", "seabank"      -> Icons.Outlined.AccountBalance
    "paypal"                                        -> Icons.Outlined.Language
    else                                            -> Icons.Outlined.Payment
}


/**
 * Kirim notifikasi otomatis ke WA dan Telegram setelah user menekan "Sudah Bayar".
 * Pesan berisi: Nama, Order ID, dan metode pembayaran yang dipilih.
 */
private fun sendPaymentNotification(
    ctx: Context,
    buyerName: String,
    orderId: String,
    paymentMethods: List<PaymentManager.PaymentMethod>,
) {
    val methodNames = paymentMethods.firstOrNull()?.label ?: "tidak diketahui"
    val msg = buildString {
        appendLine("🧾 *Konfirmasi Pembayaran - Aether Manager*")
        appendLine()
        appendLine("👤 *Nama:* $buyerName")
        appendLine("🆔 *Order ID:* `$orderId`")
        appendLine("💳 *Metode:* $methodNames")
        appendLine()
        appendLine("Saya sudah melakukan transfer. Mohon segera dikonfirmasi. Terima kasih!")
    }.trim()

    // Kirim ke WhatsApp
    try {
        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6285121390218?text=${Uri.encode(msg)}"))
        waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(waIntent)
    } catch (_: Exception) { /* WA tidak tersedia, lanjut ke Telegram */ }

    // Kirim ke Telegram (buka chat langsung)
    try {
        val tgMsg = msg.replace("*", "").replace("`", "")
        val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AetherDev22?text=${Uri.encode(tgMsg)}"))
        tgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(tgIntent)
    } catch (_: Exception) { /* Telegram tidak tersedia */ }
}

/**
 * Versi silent: hanya catat order, TIDAK membuka WA/Telegram.
 * WA/Telegram hanya dibuka manual oleh user jika perlu.
 */
private fun sendPaymentNotificationSilent(
    ctx: Context,
    buyerName: String,
    orderId: String,
    paymentMethods: List<PaymentManager.PaymentMethod>,
) {
    // No-op: konfirmasi dilakukan oleh admin via backend, tidak perlu buka WA/Telegram otomatis.
    // Fungsi ini sengaja dikosongkan agar flow tidak mengganggu user.
}

@Composable
private fun ContactAdminRow(ctx: Context) {
    val s = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick  = {
                val msg = "Halo Admin, saya ingin menanyakan status pembayaran Aether Manager Premium."
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/6285121390218?text=${Uri.encode(msg)}")))
            },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.Chat, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.licenseContactWhatsApp, style = MaterialTheme.typography.labelMedium)
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
                        Text("${inv.name} · ${inv.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val s     = LocalStrings.current
    val green = Color(0xFF4CAF50)
    val ctx   = LocalContext.current

    // Animasi masuk dialog
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dialogScale"
    )
    // Animasi pulse lingkaran icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── Animated icon ─────────────────────────────────────────────
                Box(contentAlignment = Alignment.Center) {
                    // Outer pulse ring
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                            .clip(CircleShape)
                            .background(green.copy(alpha = pulseAlpha))
                    )
                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(green.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.WorkspacePremium,
                            null,
                            tint = green,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // ── Checkmarks animation ──────────────────────────────────────
                var showBadge by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(300)
                    showBadge = true
                }
                AnimatedVisibility(
                    visible = showBadge,
                    enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = green.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = green, modifier = Modifier.size(16.dp))
                            Text(
                                "Pembayaran Berhasil!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = green
                            )
                        }
                    }
                }

                // ── Title ─────────────────────────────────────────────────────
                Text(
                    s.licenseSuccessTitle,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center
                )

                Text(
                    s.licenseSuccessBody,
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── License key card ──────────────────────────────────────────
                Surface(
                    shape    = RoundedCornerShape(16.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(s.licenseSuccessKeyLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            licenseKey,
                            style      = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                            textAlign  = TextAlign.Center
                        )
                        Text(s.licenseSuccessValidUntil.format(expLabel), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── Benefits list ─────────────────────────────────────────────
                var showBenefits by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(500); showBenefits = true }
                AnimatedVisibility(visible = showBenefits, enter = fadeIn(tween(400)) + slideInVertically { it / 2 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        s.licenseSuccessBenefits.forEachIndexed { i, label ->
                            var show by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { delay(600L + i * 120L); show = true }
                            AnimatedVisibility(visible = show, enter = fadeIn() + slideInHorizontally { -it / 2 }) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = green, modifier = Modifier.size(16.dp))
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // ── Action button ─────────────────────────────────────────────
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = green)
                ) {
                    Icon(Icons.Outlined.Rocket, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.licenseSuccessStartBtn, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
