package dev.aether.manager.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aether.manager.util.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class AppsUiState {
    object Loading : AppsUiState()
    data class Ready(
        val apps: List<AppInfo>,
        val profiles: Map<String, AppProfile>,
        val monitorRunning: Boolean,
    ) : AppsUiState()
    data class Error(val msg: String) : AppsUiState()
}

class AppProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    private val _snack = MutableStateFlow<String?>(null)
    val snack: StateFlow<String?> = _snack.asStateFlow()

    private val _editingProfile = MutableStateFlow<AppProfile?>(null)
    val editingProfile: StateFlow<AppProfile?> = _editingProfile.asStateFlow()

    private val _savingPkg = MutableStateFlow<String?>(null)
    val savingPkg: StateFlow<String?> = _savingPkg.asStateFlow()

    @Volatile private var loading = false
    @Volatile private var profileSyncing = false

    init { load() }

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        if (loading) return@launch
        loading = true
        _state.value = AppsUiState.Loading

        try {
            val apps = withTimeout(8_000L) {
                AppProfileRepository.loadUserApps(getApplication())
            }

            // Tampilkan daftar aplikasi secepat mungkin. Jangan tunggu root/shell,
            // karena bagian itu yang sering bikin tab App Profile stuck Loading.
            _state.value = AppsUiState.Ready(apps, emptyMap(), false)
            syncProfilesIfRootReady()
        } catch (e: TimeoutCancellationException) {
            _state.value = AppsUiState.Error("Timeout memuat daftar aplikasi")
        } catch (e: Exception) {
            _state.value = AppsUiState.Error(e.message ?: "Gagal memuat aplikasi")
        } finally {
            loading = false
        }
    }

    private fun syncProfilesIfRootReady() = viewModelScope.launch(Dispatchers.IO) {
        if (profileSyncing || !RootManager.isRootGranted) return@launch
        val ready = _state.value as? AppsUiState.Ready ?: return@launch
        profileSyncing = true
        try {
            val profiles = withTimeout(5_000L) { AppProfileRepository.loadAllProfiles() }
            val running = runCatching {
                withTimeout(2_000L) { AppProfileRepository.isMonitorRunning() }
            }.getOrDefault(false)
            _state.value = ready.copy(profiles = profiles, monitorRunning = running)
        } catch (_: Exception) {
            // UI sudah Ready dengan daftar app; kegagalan sync root tidak boleh
            // mengembalikan layar ke Loading/Error.
        } finally {
            profileSyncing = false
        }
    }

    fun openEditor(app: AppInfo) = viewModelScope.launch(Dispatchers.IO) {
        val current = if (RootManager.isRootGranted) {
            runCatching { AppProfileRepository.loadProfile(app.packageName) }
                .getOrDefault(AppProfile(app.packageName))
        } else {
            AppProfile(app.packageName)
        }
        _editingProfile.value = current
    }

    fun closeEditor() { _editingProfile.value = null }

    fun saveProfile(profile: AppProfile) = viewModelScope.launch(Dispatchers.IO) {
        if (!RootManager.isRootGranted) {
            snack("Root belum aktif — buka Setup dan grant root dulu")
            return@launch
        }

        _savingPkg.value = profile.packageName
        try {
            AppProfileRepository.saveProfile(profile)
            val s = _state.value
            if (s is AppsUiState.Ready) {
                val updated = s.profiles.toMutableMap()
                updated[profile.packageName] = profile
                val hasEnabled = updated.values.any { it.enabled }
                val monitorWasOff = !s.monitorRunning

                if (hasEnabled && monitorWasOff) {
                    AppProfileRepository.startMonitor()
                    _state.value = s.copy(profiles = updated, monitorRunning = true)
                } else {
                    if (s.monitorRunning) AppProfileRepository.startMonitor()
                    _state.value = s.copy(profiles = updated)
                }
            }
            _editingProfile.value = null
        } catch (e: Exception) {
            snack("Gagal simpan: ${e.message}")
        } finally {
            _savingPkg.value = null
        }
    }

    fun toggleMonitor(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        if (!RootManager.isRootGranted) {
            snack("Root belum aktif — buka Setup dan grant root dulu")
            return@launch
        }

        runCatching {
            if (enable) AppProfileRepository.startMonitor() else AppProfileRepository.stopMonitor()
        }.onFailure { e ->
            snack("Gagal mengubah monitor: ${e.message}")
            return@launch
        }

        val s = _state.value
        if (s is AppsUiState.Ready) {
            _state.value = s.copy(monitorRunning = enable)
        }
    }

    fun deleteProfile(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        if (!RootManager.isRootGranted) {
            snack("Root belum aktif — buka Setup dan grant root dulu")
            return@launch
        }
        AppProfileRepository.deleteProfile(packageName)
        val s = _state.value
        if (s is AppsUiState.Ready) {
            val updated = s.profiles.toMutableMap()
            updated.remove(packageName)
            _state.value = s.copy(profiles = updated)
        }
    }

    fun resetMonitor() = viewModelScope.launch(Dispatchers.IO) {
        if (!RootManager.isRootGranted) return@launch
        AppProfileRepository.resetMonitor()
        val s = _state.value
        if (s is AppsUiState.Ready) {
            _state.value = s.copy(monitorRunning = false)
        }
    }

    fun resetAllProfiles() = viewModelScope.launch(Dispatchers.IO) {
        if (!RootManager.isRootGranted) return@launch
        AppProfileRepository.resetAllProfiles()
        val s = _state.value
        if (s is AppsUiState.Ready) {
            _state.value = s.copy(profiles = emptyMap(), monitorRunning = false)
        }
    }

    fun snack(msg: String) { _snack.value = msg }
    fun clearSnack() { _snack.value = null }

    fun loadIfNeeded() {
        val s = _state.value
        when {
            s is AppsUiState.Loading && !loading -> load()
            s is AppsUiState.Error -> load()
            s is AppsUiState.Ready && s.apps.isEmpty() -> load()
            s is AppsUiState.Ready && RootManager.isRootGranted && s.profiles.isEmpty() -> syncProfilesIfRootReady()
        }
    }
}
