package dev.aether.manager.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    sealed class UiState {
        data object Idle : UiState()
        data object CreatingOrder : UiState()
        data class WaitingTransfer(
            val orderId:        String,
            val paymentMethods: List<PaymentManager.PaymentMethod>,
            val nominal:        Int,
            // backward-compat fields (dipakai InvoicePrefs lama)
            val gopay: String = paymentMethods.firstOrNull { it.id == "gopay" }?.number ?: "-",
            val dana:  String = paymentMethods.firstOrNull { it.id == "dana"  }?.number ?: "-",
        ) : UiState()
        data object Polling : UiState()
        data class Success(val licenseKey: String, val orderId: String) : UiState()
        data class Failure(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _showTimeoutWarning = MutableStateFlow(false)
    val showTimeoutWarning = _showTimeoutWarning.asStateFlow()

    private var activeOrderId: String? = null

    companion object {
        /** Validasi nomor HP Indonesia */
        fun validatePhone(phone: String): String? {
            val digits = phone.trim().replace(Regex("[\\s\\-()]"), "")
            if (digits.isBlank()) return "Nomor HP wajib diisi"
            if (!digits.matches(Regex("^(\\+62|62|08)\\d+"))) return "Format nomor tidak valid (contoh: 08123456789)"
            val normalized = when {
                digits.startsWith("+62") -> "0" + digits.removePrefix("+62")
                digits.startsWith("62")  -> "0" + digits.removePrefix("62")
                else                     -> digits
            }
            if (normalized.length < 10) return "Nomor terlalu pendek (minimal 10 digit)"
            if (normalized.length > 15) return "Nomor terlalu panjang (maksimal 15 digit)"
            return null
        }
    }

    fun createOrder(name: String, phone: String) {
        if (name.isBlank()) { _uiState.value = UiState.Failure("Nama harus diisi"); return }
        val phoneError = validatePhone(phone)
        if (phoneError != null) { _uiState.value = UiState.Failure(phoneError); return }
        viewModelScope.launch {
            _uiState.value = UiState.CreatingOrder
            when (val r = PaymentManager.createOrder(ctx, name, phone)) {
                is PaymentManager.CreateOrderResult.Success -> {
                    activeOrderId  = r.order.orderId
                    _uiState.value = UiState.WaitingTransfer(
                        orderId        = r.order.orderId,
                        paymentMethods = r.order.paymentMethods,
                        nominal        = r.order.nominal,
                    )
                }
                is PaymentManager.CreateOrderResult.Error ->
                    _uiState.value = UiState.Failure(r.message)
            }
        }
    }

    fun confirmAndPoll() {
        val orderId = activeOrderId ?: return
        startPolling(orderId)
    }

    fun resumePoll(orderId: String) {
        activeOrderId = orderId
        startPolling(orderId)
    }

    private fun startPolling(orderId: String) {
        _showTimeoutWarning.value = false
        viewModelScope.launch {
            // 2-minute warning timer
            delay(2 * 60 * 1_000L)
            if (_uiState.value is UiState.Polling) {
                _showTimeoutWarning.value = true
            }
        }
        viewModelScope.launch {
            _uiState.value = UiState.Polling
            val result = PaymentManager.pollUntilDone(ctx, orderId) { }
            _showTimeoutWarning.value = false
            _uiState.value = when (result) {
                is PaymentManager.PollResult.Completed ->
                    UiState.Success(licenseKey = result.licenseKey, orderId = orderId)
                is PaymentManager.PollResult.Failed ->
                    UiState.Failure("Pembayaran ${result.status}. Hubungi admin.")
                is PaymentManager.PollResult.Error ->
                    UiState.Failure(result.message)
                is PaymentManager.PollResult.Pending ->
                    UiState.Failure("Timeout – belum ada konfirmasi")
            }
        }
    }

    fun dismissTimeoutWarning() {
        _showTimeoutWarning.value = false
    }

    fun reset() {
        activeOrderId  = null
        _showTimeoutWarning.value = false
        _uiState.value = UiState.Idle
    }
}
