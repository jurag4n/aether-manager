package dev.aether.manager.license

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LicenseViewModel(app: Application) : AndroidViewModel(app) {

    sealed class UiState {
        data object Idle    : UiState()
        data object Loading : UiState()
        data class  Success(val licenseKey: String, val message: String) : UiState()
        data class  Failure(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun activate(key: String) {
        if (key.isBlank()) return
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            when (val result = LicenseManager.activate(ctx, key)) {
                is LicenseManager.ActivateResult.Success ->
                    _uiState.value = UiState.Success(
                        licenseKey = result.licenseKey,
                        message    = result.message
                    )
                is LicenseManager.ActivateResult.Failure ->
                    _uiState.value = UiState.Failure(result.message)
            }
        }
    }

    fun deactivate() {
        val ctx = getApplication<Application>()
        LicenseManager.deactivate(ctx)
        _uiState.value = UiState.Idle
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }
}
