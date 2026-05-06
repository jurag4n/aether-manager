package dev.aether.manager.update

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

sealed class UpdateState {
    object Idle      : UpdateState()
    object Checking  : UpdateState()
    object UpToDate  : UpdateState()
    data class Available(val info: ReleaseInfo) : UpdateState()
    data class Error(val msg: String)           : UpdateState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state     = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _dismissed = MutableStateFlow(false)
    val dismissed: StateFlow<Boolean> = _dismissed.asStateFlow()

    val installedVersionCode: Int by lazy {
        try {
            val pm  = app.packageManager
            val pkg = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(app.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode.toInt()
            else @Suppress("DEPRECATION") pkg.versionCode
        } catch (_: Exception) { 0 }
    }

    val installedVersionName: String by lazy {
        try {
            val pm  = app.packageManager
            val pkg = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(app.packageName, 0)
            }
            pkg.versionName ?: "?"
        } catch (_: Exception) { "?" }
    }

    /** Alias yang dipakai UpdateDialogHost di UpdateDialog.kt */
    val currentVersionName: String get() = installedVersionName

    fun checkUpdate() {
        if (_state.value is UpdateState.Checking) return
        _dismissed.value = false
        _state.value = UpdateState.Checking
        viewModelScope.launch {
            val release = UpdateChecker.fetchLatest()
            _state.value = when {
                release == null                             -> UpdateState.Error("Gagal cek update")
                release.versionCode > installedVersionCode -> UpdateState.Available(release)
                else                                       -> UpdateState.UpToDate
            }
        }
    }

    fun dismiss() {
        _dismissed.value = true
        _state.value = UpdateState.Idle
    }

    // Konversi ke UpdateUiState untuk kompatibilitas MainActivity lama
    fun currentUiState(): UpdateUiState = _state.value.toUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
// UpdateUiState — alias yang dipakai MainActivity
// ─────────────────────────────────────────────────────────────────────────────

sealed class UpdateUiState {
    object Idle     : UpdateUiState()
    object Checking : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateUiState()
    data class Error(val msg: String)                : UpdateUiState()
}

fun UpdateState.toUiState(): UpdateUiState = when (this) {
    is UpdateState.Idle      -> UpdateUiState.Idle
    is UpdateState.Checking  -> UpdateUiState.Checking
    is UpdateState.UpToDate  -> UpdateUiState.UpToDate
    is UpdateState.Available -> UpdateUiState.UpdateAvailable(
        UpdateInfo(
            latestVersion = info.versionName,
            versionCode   = info.versionCode,
            releaseNotes  = info.changelog,
            downloadUrl   = info.downloadUrl,
            htmlUrl       = info.htmlUrl,
        )
    )
    is UpdateState.Error     -> UpdateUiState.Error(msg)
}

// UpdateDialogHost ada di UpdateDialog.kt
