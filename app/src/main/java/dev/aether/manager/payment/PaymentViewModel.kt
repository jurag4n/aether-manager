package dev.aether.manager.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
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
            val deviceId:       String,
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

    fun createOrder(name: String) {
        if (name.isBlank()) {
            _uiState.value = UiState.Failure("Nama harus diisi")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.CreatingOrder
            when (val r = PaymentManager.createOrder(ctx, name)) {
                is PaymentManager.CreateOrderResult.Success -> {
                    activeOrderId = r.order.orderId
                    _uiState.value = UiState.WaitingTransfer(
                        orderId        = r.order.orderId,
                        paymentMethods = r.order.paymentMethods,
                        nominal        = r.order.nominal,
                        deviceId       = r.order.deviceId,
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
        activeOrderId = null
        _showTimeoutWarning.value = false
        _uiState.value = UiState.Idle
    }
}
