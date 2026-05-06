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

    private val appContext = application.applicationContext

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

        val cachedApps = AppProfileRepository.loadCachedApps(appContext)
        val cachedProfiles = AppProfileRepository.loadCachedProfiles(appContext)
        val cachedMonitor = AppProfileRepository.loadMonitorCache(appContext)

        if (cachedApps.isNotEmpty()) {
            _state.value = AppsUiState.Ready(cachedApps, cachedProfiles, cachedMonitor)
        } else {
            _state.value = AppsUiState.Loading
        }

        try {
            val apps = withTimeout(6_000L) {
                AppProfileRepository.loadUserApps(appContext)
            }
            val latestProfiles = AppProfileRepository.loadCachedProfiles(appContext)
            val latestMonitor = AppProfileRepository.loadMonitorCache(appContext)
            _state.value = AppsUiState.Ready(apps, latestProfiles, latestMonitor)
            syncProfilesIfRootReady()
        } catch (e: TimeoutCancellationException) {
            if (cachedApps.isEmpty()) {
                _state.value = AppsUiState.Error("Timeout memuat daftar aplikasi")
            } else {
                snack("Daftar aplikasi memakai cache, refresh terlalu lama")
            }
        } catch (e: Exception) {
            if (cachedApps.isEmpty()) {
                _state.value = AppsUiState.Error(e.message ?: "Gagal memuat aplikasi")
            } else {
                snack("Daftar aplikasi memakai cache")
            }
        } finally {
            loading = false
        }
    }

    private fun syncProfilesIfRootReady() = viewModelScope.launch(Dispatchers.IO) {
        if (profileSyncing) return@launch
        val hasRoot = RootManager.isRootGranted || RootManager.isRooted()
        if (!hasRoot) return@launch
        val ready = _state.value as? AppsUiState.Ready ?: return@launch
        profileSyncing = true
        try {
            val rootProfiles = withTimeout(4_000L) { AppProfileRepository.loadAllProfiles() }
            val cachedProfiles = AppProfileRepository.loadCachedProfiles(appContext)
            val mergedProfiles = if (rootProfiles.isEmpty()) cachedProfiles else cachedProfiles + rootProfiles
            AppProfileRepository.saveProfilesCache(appContext, mergedProfiles)

            val running = runCatching {
                withTimeout(1_500L) { AppProfileRepository.isMonitorRunning() }
            }.getOrDefault(ready.monitorRunning)
            AppProfileRepository.setMonitorCache(appContext, running)

            val current = _state.value as? AppsUiState.Ready ?: return@launch
            _state.value = current.copy(profiles = mergedProfiles, monitorRunning = running)
        } catch (_: Exception) {
            // UI tetap pakai cache lokal. Root/shell tidak boleh bikin layar balik Loading.
        } finally {
            profileSyncing = false
        }
    }

    private suspend fun ensureRootReady(showSnack: Boolean = true): Boolean {
        val ready = RootManager.isRootGranted || RootManager.isRooted() || RootManager.requestRoot()
        if (!ready && showSnack) snack("Root belum aktif — profile tersimpan lokal, grant root agar bisa diterapkan")
        return ready
    }

    fun openEditor(app: AppInfo) {
        val s = _state.value as? AppsUiState.Ready
        _editingProfile.value = s?.profiles?.get(app.packageName)
            ?: AppProfileRepository.loadCachedProfiles(appContext)[app.packageName]
            ?: AppProfile(app.packageName)
    }

    fun closeEditor() { _editingProfile.value = null }

    fun saveProfile(profile: AppProfile) {
        AppProfileRepository.saveProfileCache(appContext, profile)

        val s = _state.value
        if (s is AppsUiState.Ready) {
            val updated = s.profiles.toMutableMap()
            updated[profile.packageName] = profile
            val monitor = if (updated.values.any { it.enabled }) true else s.monitorRunning
            if (monitor != s.monitorRunning) AppProfileRepository.setMonitorCache(appContext, monitor)
            _state.value = s.copy(profiles = updated, monitorRunning = monitor)
        }
        _editingProfile.value = null

        viewModelScope.launch(Dispatchers.IO) {
            _savingPkg.value = profile.packageName
            try {
                if (!ensureRootReady()) return@launch
                AppProfileRepository.saveProfile(profile)
                val ready = _state.value as? AppsUiState.Ready
                if (ready != null && ready.profiles.values.any { it.enabled }) {
                    AppProfileRepository.startMonitor()
                    AppProfileRepository.setMonitorCache(appContext, true)
                    _state.value = ready.copy(monitorRunning = true)
                } else if (ready?.monitorRunning == true) {
                    AppProfileRepository.startMonitor()
                }
            } catch (e: Exception) {
                snack("Profile sudah tersimpan lokal, gagal apply root: ${e.message}")
            } finally {
                _savingPkg.value = null
            }
        }
    }

    fun toggleMonitor(enable: Boolean) {
        AppProfileRepository.setMonitorCache(appContext, enable)
        val s = _state.value
        if (s is AppsUiState.Ready) _state.value = s.copy(monitorRunning = enable)

        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureRootReady()) return@launch
            runCatching {
                if (enable) AppProfileRepository.startMonitor() else AppProfileRepository.stopMonitor()
            }.onFailure { e ->
                snack("Gagal mengubah monitor: ${e.message}")
            }
        }
    }

    fun deleteProfile(packageName: String) {
        AppProfileRepository.deleteProfileCache(appContext, packageName)
        val s = _state.value
        if (s is AppsUiState.Ready) {
            val updated = s.profiles.toMutableMap()
            updated.remove(packageName)
            _state.value = s.copy(profiles = updated)
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureRootReady(showSnack = false)) return@launch
            runCatching { AppProfileRepository.deleteProfile(packageName) }
        }
    }

    fun resetMonitor() {
        AppProfileRepository.setMonitorCache(appContext, false)
        val s = _state.value
        if (s is AppsUiState.Ready) _state.value = s.copy(monitorRunning = false)

        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureRootReady(showSnack = false)) return@launch
            runCatching { AppProfileRepository.resetMonitor() }
        }
    }

    fun resetAllProfiles() {
        AppProfileRepository.clearLocalCache(appContext, keepApps = true)
        val s = _state.value
        if (s is AppsUiState.Ready) {
            _state.value = s.copy(profiles = emptyMap(), monitorRunning = false)
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureRootReady(showSnack = false)) return@launch
            runCatching { AppProfileRepository.resetAllProfiles() }
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
            s is AppsUiState.Ready && s.profiles.isEmpty() -> syncProfilesIfRootReady()
        }
    }
}
