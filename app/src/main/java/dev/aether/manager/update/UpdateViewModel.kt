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

sealed class UpdateState {
    object Idle       : UpdateState()
    object Checking   : UpdateState()
    object UpToDate   : UpdateState()
    data class Available(val info: ReleaseInfo) : UpdateState()
    data class Error(val msg: String)           : UpdateState()
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** versionCode APK yang sedang terinstall */
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

    /** versionName APK yang sedang terinstall */
    val installedVersionName: String by lazy {
        try {
            val pm = app.packageManager
            val pkg = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(app.packageName, 0)
            }
            pkg.versionName ?: "?"
        } catch (_: Exception) { "?" }
    }

    // ─────────────────────────────────────────────────────────────────────────

    fun checkUpdate() {
        if (_state.value is UpdateState.Checking) return
        _state.value = UpdateState.Checking

        viewModelScope.launch {
            val release = UpdateChecker.fetchLatest()
            _state.value = when {
                release == null -> UpdateState.Error("Gagal cek update — coba lagi nanti")

                // Update tersedia kalau versionCode remote > installed
                release.versionCode > installedVersionCode ->
                    UpdateState.Available(release)

                else -> UpdateState.UpToDate
            }
        }
    }

    fun dismiss() {
        // Reset ke Idle supaya bisa di-trigger ulang nanti
        _state.value = UpdateState.Idle
    }
}
