package dev.aether.manager.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.aether.manager.i18n.AppStrings
import dev.aether.manager.i18n.getStringsForContext

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
        /**
         * Validasi nomor WhatsApp global.
         * Support:
         * - Indonesia: 08xxxxxxxxxx, 62xxxxxxxxxx, +62xxxxxxxxxx
         * - International: +<country-code><number>, 8-15 digit sesuai gaya E.164
         */
        fun validatePhone(phone: String, strings: AppStrings? = null): String? {
            val raw = phone.trim()
            if (raw.isBlank()) return strings?.paymentPhoneRequired ?: "WhatsApp number is required"

            val compact = raw.replace(Regex("[\\s\\-()]"), "")
            if (!compact.matches(Regex("^\\+?\\d+$"))) {
                return strings?.paymentPhoneInvalid ?: "Invalid number format. Example: 08123456789 or +14155552671"
            }

            val internationalDigits = when {
                compact.startsWith("+") -> compact.drop(1)
                compact.startsWith("00") -> compact.drop(2)
                compact.startsWith("08") -> "62" + compact.drop(1)
                compact.startsWith("62") -> compact
                else -> compact
            }

            if (internationalDigits.length < 8) return strings?.paymentPhoneTooShort ?: "Number is too short"
            if (internationalDigits.length > 15) return strings?.paymentPhoneTooLong ?: "Number is too long"
            return null
        }

        fun normalizePhone(phone: String): String {
            val compact = phone.trim().replace(Regex("[\\s\\-()]"), "")
            return when {
                compact.startsWith("+") -> compact
                compact.startsWith("00") -> "+" + compact.drop(2)
                compact.startsWith("08") -> "+62" + compact.drop(1)
                compact.startsWith("62") -> "+" + compact
                else -> "+" + compact
            }
        }

        fun isInternationalBuyer(phone: String): Boolean {
            val normalized = normalizePhone(phone)
            return normalized.isNotBlank() && !normalized.startsWith("+62")
        }
    }

    fun createOrder(name: String, phone: String) {
        val strings = getStringsForContext(ctx)
        if (name.isBlank()) { _uiState.value = UiState.Failure(strings.paymentNameRequired); return }
        val phoneError = validatePhone(phone, strings)
        if (phoneError != null) { _uiState.value = UiState.Failure(phoneError); return }
        viewModelScope.launch {
            _uiState.value = UiState.CreatingOrder
            when (val r = PaymentManager.createOrder(ctx, name, normalizePhone(phone))) {
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
        val strings = getStringsForContext(ctx)
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
                    UiState.Failure(strings.paymentStatusFailed.format(result.status))
                is PaymentManager.PollResult.Error ->
                    UiState.Failure(result.message)
                is PaymentManager.PollResult.Pending ->
                    UiState.Failure(strings.paymentPollTimeoutShort)
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
