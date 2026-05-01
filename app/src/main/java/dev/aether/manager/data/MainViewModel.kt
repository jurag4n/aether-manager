package dev.aether.manager.data

import android.app.Application
import android.app.NotificationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aether.manager.util.BackupManager
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.RootManager
import dev.aether.manager.util.RootEngine
import dev.aether.manager.util.SettingsPrefs
import dev.aether.manager.util.TweakApplier
import dev.aether.manager.ads.AdScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TweaksState(
    val schedboost: Boolean = false,
    val cpuBoost: Boolean = false,
    val gpuThrottleOff: Boolean = false,
    val cpusetOpt: Boolean = false,
    val mtkBoost: Boolean = false,
    val lmkAggressive: Boolean = false,
    val zram: Boolean = false,
    val zramSize: String = "1073741824",
    val zramAlgo: String = "lz4",
    val vmDirtyOpt: Boolean = false,
    val ioScheduler: String = "",
    val cpuGovernor: String = "",
    val ioLatencyOpt: Boolean = false,
    val tcpBbr: Boolean = false,
    val doh: Boolean = false,
    val netBuffer: Boolean = false,
    val doze: Boolean = false,
    val fastAnim: Boolean = false,
    val entropyBoost: Boolean = false,
    val clearCache: Boolean = false,
    val cpuFreqEnable: Boolean = false,
    val cpuFreqPrimeMin: String = "",
    val cpuFreqPrimeMax: String = "",
    val cpuFreqGoldMin: String  = "",
    val cpuFreqGoldMax: String  = "",
    val cpuFreqSilverMin: String = "",
    val cpuFreqSilverMax: String = "",
    val thermalProfile: String = "default",
    val gpuFreqLock: Boolean = false,
    val gpuFreqMax: String = "",
    val touchBoost: Boolean = false,
    val touchSampleRate: String = "default",
    val ksm: Boolean = false,
    val ksmAggressive: Boolean = false,

    // ─────────────────────────────────────────────────────────────────────────
    // New toggles for modern tweak engine
    //
    // networkStable     — enable network buffer tuning for more stable latency
    // swap              — enable swap tuning (swappiness/vm pressure)
    // killBackground    — aggressively drop caches & trim app caches
    // dnsProvider       — selected private DNS provider ("", "Cloudflare", "Google", "Quad9", "CleanBrowsing")
    // cpuFreqLock       — lock CPU frequencies to their current maximum
    // These fields default to off/blank.
    val networkStable: Boolean = false,
    val swap: Boolean = false,
    val killBackground: Boolean = false,
    val dnsProvider: String = "",
    val cpuFreqLock: Boolean = false,
)

data class MonitorState(
    val cpuUsage: Int = 0,
    val cpuFreq: String = "",
    val gpuUsage: Int = 0,
    val gpuFreq: String = "",
    val gpuName: String = "",
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val cpuTemp: Float = 0f,
    val gpuTemp: Float = 0f,
    val thermalTemp: Float = 0f,
    val batTemp: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val uptime: String = "",
    val batLevel: Int = 0,
    val cpuGovernor: String = "",
    val swapUsedMb: Long = 0L,
    val swapTotalMb: Long = 0L,
    val batCurrentMa: Long = 0L,
    val batVoltage: Long = 0L,
    val batStatus: String = "Unknown",
)

/** Per-apply status — ditampilkan di UI sebagai badge/toast */
data class ApplyStatus(
    val running:  Boolean = false,
    val lastOk:   Boolean = true,
    val summary:  String  = "",
    val totalMs:  Long    = 0L,
)

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val msg: String) : UiState<Nothing>()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    // ── Root & device info ────────────────────────────────────────────────────

    private val _rootGranted = MutableStateFlow<Boolean?>(null)
    val rootGranted: StateFlow<Boolean?> = _rootGranted.asStateFlow()

    private val _deviceInfo = MutableStateFlow<UiState<DeviceInfo>>(UiState.Loading)
    val deviceInfo: StateFlow<UiState<DeviceInfo>> = _deviceInfo.asStateFlow()

    // ── Tweaks ────────────────────────────────────────────────────────────────

    private val _tweaks = MutableStateFlow(TweaksState())
    val tweaks: StateFlow<TweaksState> = _tweaks.asStateFlow()

    // Apply status — dipakai UI untuk spinner/snackbar/badge
    private val _applyStatus = MutableStateFlow(ApplyStatus())
    val applyStatus: StateFlow<ApplyStatus> = _applyStatus.asStateFlow()

    // Legacy compat — beberapa composable masih pakai ini
    private val _applyingTweak = MutableStateFlow(false)
    val applyingTweak: StateFlow<Boolean> = _applyingTweak.asStateFlow()

    // ── Tweak Apply Queue ─────────────────────────────────────────────────────
    //
    // Mekanisme baru — tanpa debounce yang rawan bug:
    //
    // 1. setTweak() / setTweakStr():
    //    a. Update UI state INSTANT (optimistic)
    //    b. Kirim sinyal ke applyChannel (CONFLATED — kalau ada antrian, override)
    //
    // 2. applyWorker coroutine (loop seumur ViewModel):
    //    a. Tunggu sinyal dari channel
    //    b. Baca state UI saat itu (sudah include semua perubahan yang dikumpul)
    //    c. Tulis conf + apply kernel dalam satu Shell batch
    //
    // Efek: user toggle 5 switch cepat → hanya 1 Shell.cmd() yang dijalankan
    // karena channel CONFLATED membuang sinyal duplikat.
    // Tidak ada race, tidak ada debounce 150ms yang kadang hangfire.

    private val applyChannel = Channel<Unit>(capacity = Channel.CONFLATED)
    private var applyWorkerJob: Job? = null

    // ── Monitor ───────────────────────────────────────────────────────────────

    private val _monitorState = MutableStateFlow(MonitorState())
    val monitorState: StateFlow<MonitorState> = _monitorState.asStateFlow()

    private var monitorStarted = false
    private var monitorJob: Job? = null

    // ── Snack ─────────────────────────────────────────────────────────────────

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────────

    init {
        startApplyWorker()
        _deviceInfo.value = UiState.Success(RootEngine.getDeviceInfoFallback())
        viewModelScope.launch { initFromCachedRoot() }
    }

    private suspend fun initFromCachedRoot() {
        val fallback = RootEngine.getDeviceInfoFallback()
        _deviceInfo.value = UiState.Success(fallback)

        val hasRoot = RootEngine.hasRoot()
        _rootGranted.value = hasRoot
        if (hasRoot) {
            loadAll()
            startMonitorLoop()
        } else {
            _deviceInfo.value = UiState.Success(fallback)
        }
    }

    fun refresh() = viewModelScope.launch {
        val fallback = RootEngine.getDeviceInfoFallback()
        _deviceInfo.value = UiState.Success(fallback)

        val hasRoot = RootEngine.hasRoot()
        _rootGranted.value = hasRoot
        if (hasRoot) {
            loadAll()
            if (!monitorStarted) startMonitorLoop()
        } else {
            _deviceInfo.value = UiState.Success(fallback)
        }
    }

    /**
     * Refresh hanya jika state masih Loading atau root belum diketahui.
     * Dipanggil dari ON_RESUME agar Home tidak stuck skeleton setelah balik dari SetupActivity.
     */
    fun refreshIfNeeded() {
        val current = (_deviceInfo.value as? UiState.Success)?.data
        if (_deviceInfo.value is UiState.Loading || (RootManager.isRootGranted && current?.rootType == "Unknown")) {
            refresh()
        }
    }

    fun refreshMonitor() = viewModelScope.launch {
        _monitorState.value = RootEngine.getMonitorState()
    }

    private fun startMonitorLoop() {
        if (monitorStarted && monitorJob?.isActive == true) return
        monitorStarted = true
        monitorJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (!_applyStatus.value.running) {
                    try {
                        // Refresh CPU/GPU/memory/temperature stats every second instead of
                        // the previous three-second interval.  A shorter delay makes
                        // the real‑time monitor feel more responsive.
                        _monitorState.value = RootEngine.getMonitorState()
                    } catch (_: Exception) {}
                }
                delay(1000)
            }
            monitorStarted = false
        }
    }

    override fun onCleared() {
        monitorJob?.cancel()
        applyWorkerJob?.cancel()
        monitorStarted = false
        super.onCleared()
    }

    private suspend fun loadAll() {
        try {
            val info = RootEngine.getDeviceInfo()
            _deviceInfo.value = UiState.Success(info)
            loadTweaks()
        } catch (_: Exception) {
            _deviceInfo.value = UiState.Success(RootEngine.getDeviceInfoFallback())
        }
    }

    private suspend fun loadTweaks() {
        val map = RootEngine.readTweaksConf()
        _tweaks.value = mapToTweaksState(map)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Apply Worker — coroutine yang jalan seumur ViewModel
    // Baca channel CONFLATED → tidak ada antrian menumpuk
    // ─────────────────────────────────────────────────────────────────────────

    private fun startApplyWorker() {
        applyWorkerJob = viewModelScope.launch(Dispatchers.IO) {
            for (signal in applyChannel) {
                // Ambil state UI sekarang — sudah include semua toggle yang terkumpul
                val current = _tweaks.value
                val map = tweaksStateToMap(current)
                executeApply(map)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setTweak / setTweakStr — instant UI + enqueue apply
    // ─────────────────────────────────────────────────────────────────────────

    fun setTweak(key: String, value: Boolean) {
        // 1. Update UI state INSTANT (optimistic)
        _tweaks.value = applyTweakToState(_tweaks.value, key, value)
        // 2. Enqueue apply — CONFLATED: kalau worker belum selesai, sinyal baru override yang lama
        applyChannel.trySend(Unit)
    }

    fun setTweakStr(key: String, value: String) {
        _tweaks.value = applyTweakStrToState(_tweaks.value, key, value)
        applyChannel.trySend(Unit)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeApply — inti apply, dipanggil dari worker
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun executeApply(map: Map<String, String>) {
        _applyStatus.value = ApplyStatus(running = true)
        _applyingTweak.value = true

        try {
                val result = TweakApplier.writeAndApply(RootEngine.TWEAKS_CONF, map)

            _applyingTweak.value = false
            _applyStatus.value = ApplyStatus(
                running = false,
                lastOk  = result.success,
                summary = result.summary,
                totalMs = result.totalMs,
            )

            if (result.success) {
                // Trigger iklan setelah aksi penting selesai (subject to minIntervalMs guard)
                AdScheduler.tryShowAfterAction()
            }

            if (!result.success) {
                val failedSubs = result.subsystems.filter { !it.ok }
                val msg = when {
                    failedSubs.size == 1 && failedSubs[0].name == "shell" ->
                        "Apply gagal — shell tidak merespons"
                    else ->
                        "Apply partial — gagal: ${failedSubs.joinToString(", ") { it.name }}"
                }
                snack(msg)
            }

            // Kick off the auto‑backup after the apply has completed.  Launching
            // this asynchronously prevents the backup process (which may take
            // several hundred milliseconds) from blocking the apply operation.
            viewModelScope.launch {
                autoBackupIfEnabled()
            }

        } catch (e: Exception) {
            _applyingTweak.value = false
            _applyStatus.value = ApplyStatus(running = false, lastOk = false, summary = e.message ?: "error")
            snack("Gagal apply: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setProfile — apply langsung tanpa mengganggu worker
    // ─────────────────────────────────────────────────────────────────────────

    fun setProfile(profile: String) = viewModelScope.launch(Dispatchers.IO) {
        _applyingTweak.value = true
        _applyStatus.value = ApplyStatus(running = true)

        try {
            // Tulis profile file
            RootEngine.writeFile(RootEngine.PROFILE_FILE, profile)

            // Update state lokal dengan profile baru, lalu apply
            val newState = _tweaks.value.copy()
            val map = tweaksStateToMap(newState).toMutableMap()
            map["profile"] = profile

            val result = TweakApplier.writeAndApply(RootEngine.TWEAKS_CONF, map)

            // Update UI state dari map
            _tweaks.value = mapToTweaksState(map)

            _applyingTweak.value = false
            _applyStatus.value = ApplyStatus(
                running = false,
                lastOk  = result.success,
                summary = result.summary,
                totalMs = result.totalMs,
            )

            // Refresh device info so the profile badge updates
            val info = RootEngine.getDeviceInfo()
            _deviceInfo.value = UiState.Success(info)
            _monitorState.value = RootEngine.getMonitorState()
            // Do not show a success toast here; the UI will reflect the new profile

        } catch (e: Exception) {
            _applyingTweak.value = false
            _applyStatus.value = ApplyStatus(running = false, lastOk = false, summary = e.message ?: "error")
            snack("Gagal set profile: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // applyAll — force apply semua tweak sekarang (manual "Apply" button)
    // ─────────────────────────────────────────────────────────────────────────

    fun applyAll() = viewModelScope.launch(Dispatchers.IO) {
        val map = tweaksStateToMap(_tweaks.value)
        executeApply(map)
        // Skip success snack for apply; only errors will trigger a toast
    }

    // ─────────────────────────────────────────────────────────────────────────
    // tweaksStateToMap — TweaksState → Map<String,String> untuk Shell script
    // ─────────────────────────────────────────────────────────────────────────

    private fun tweaksStateToMap(s: TweaksState): Map<String, String> = buildMap {
        put("schedboost",        if (s.schedboost)      "1" else "0")
        put("cpu_boost",         if (s.cpuBoost)         "1" else "0")
        put("gpu_throttle_off",  if (s.gpuThrottleOff)  "1" else "0")
        put("cpuset_opt",        if (s.cpusetOpt)        "1" else "0")
        put("obb_noop",          if (s.mtkBoost)         "1" else "0")
        put("lmk_aggressive",    if (s.lmkAggressive)   "1" else "0")
        put("zram",              if (s.zram)             "1" else "0")
        put("zram_size",         s.zramSize)
        put("zram_algo",         s.zramAlgo)
        put("vm_dirty_opt",      if (s.vmDirtyOpt)      "1" else "0")
        put("io_scheduler",      s.ioScheduler)
        put("cpu_governor",      s.cpuGovernor)
        put("io_latency_opt",    if (s.ioLatencyOpt)    "1" else "0")
        put("tcp_bbr",           if (s.tcpBbr)          "1" else "0")
        put("doh",               if (s.doh)             "1" else "0")
        put("net_buffer",        if (s.netBuffer)       "1" else "0")
        put("doze",              if (s.doze)            "1" else "0")
        put("fast_anim",         if (s.fastAnim)        "1" else "0")
        put("entropy_boost",     if (s.entropyBoost)    "1" else "0")
        put("clear_cache",       if (s.clearCache)      "1" else "0")
        put("cpu_freq_enable",   if (s.cpuFreqEnable)   "1" else "0")
        put("cpu_freq_prime_min",  s.cpuFreqPrimeMin)
        put("cpu_freq_prime_max",  s.cpuFreqPrimeMax)
        put("cpu_freq_gold_min",   s.cpuFreqGoldMin)
        put("cpu_freq_gold_max",   s.cpuFreqGoldMax)
        put("cpu_freq_silver_min", s.cpuFreqSilverMin)
        put("cpu_freq_silver_max", s.cpuFreqSilverMax)
        put("thermal_profile",   s.thermalProfile)
        put("gpu_freq_lock",     if (s.gpuFreqLock)     "1" else "0")
        put("gpu_freq_max",      s.gpuFreqMax)
        put("touch_boost",       if (s.touchBoost)      "1" else "0")
        put("touch_sample_rate", s.touchSampleRate)
        put("ksm",               if (s.ksm)             "1" else "0")
        put("ksm_aggressive",    if (s.ksmAggressive)   "1" else "0")

        // New tweak keys for network, memory and CPU lock
        put("network_stable",    if (s.networkStable)    "1" else "0")
        put("swap",              if (s.swap)              "1" else "0")
        put("kill_background",   if (s.killBackground)   "1" else "0")
        put("dns_provider",      s.dnsProvider)
        put("cpu_freq_lock",     if (s.cpuFreqLock)      "1" else "0")
        // Profile dibaca dari RootEngine.PROFILE_FILE, tidak disimpan di TweaksState
        // tapi dibutuhkan script — baca dari disk saat ini
        put("profile", RootEngine.readProfileSync())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // mapToTweaksState
    // ─────────────────────────────────────────────────────────────────────────

    private fun mapToTweaksState(map: Map<String, String>) = TweaksState(
        schedboost       = map["schedboost"]        == "1",
        cpuBoost         = map["cpu_boost"]          == "1",
        gpuThrottleOff   = map["gpu_throttle_off"]   == "1",
        cpusetOpt        = map["cpuset_opt"]         == "1",
        mtkBoost         = map["obb_noop"]           == "1",
        lmkAggressive    = map["lmk_aggressive"]     == "1",
        zram             = map["zram"]               == "1",
        zramSize         = map["zram_size"]          ?: "1073741824",
        zramAlgo         = map["zram_algo"]          ?: "lz4",
        vmDirtyOpt       = map["vm_dirty_opt"]       == "1",
        ioScheduler      = map["io_scheduler"]       ?: "",
        cpuGovernor      = map["cpu_governor"]       ?: "",
        ioLatencyOpt     = map["io_latency_opt"]     == "1",
        tcpBbr           = map["tcp_bbr"]            == "1",
        doh              = map["doh"]                == "1",
        netBuffer        = map["net_buffer"]         == "1",
        doze             = map["doze"]               == "1",
        fastAnim         = map["fast_anim"]          == "1",
        entropyBoost     = map["entropy_boost"]      == "1",
        clearCache       = map["clear_cache"]        == "1",
        cpuFreqEnable    = map["cpu_freq_enable"]    == "1",
        cpuFreqPrimeMin  = map["cpu_freq_prime_min"] ?: "",
        cpuFreqPrimeMax  = map["cpu_freq_prime_max"] ?: "",
        cpuFreqGoldMin   = map["cpu_freq_gold_min"]  ?: "",
        cpuFreqGoldMax   = map["cpu_freq_gold_max"]  ?: "",
        cpuFreqSilverMin = map["cpu_freq_silver_min"]?: "",
        cpuFreqSilverMax = map["cpu_freq_silver_max"]?: "",
        thermalProfile   = map["thermal_profile"]    ?: "default",
        gpuFreqLock      = map["gpu_freq_lock"]      == "1",
        gpuFreqMax       = map["gpu_freq_max"]       ?: "",
        touchBoost       = map["touch_boost"]        == "1",
        touchSampleRate  = map["touch_sample_rate"]  ?: "default",
        ksm              = map["ksm"]                == "1",
        ksmAggressive    = map["ksm_aggressive"]     == "1",
        // New toggles
        networkStable    = map["network_stable"]    == "1",
        swap             = map["swap"]              == "1",
        killBackground   = map["kill_background"]   == "1",
        dnsProvider      = map["dns_provider"]      ?: "",
        cpuFreqLock      = map["cpu_freq_lock"]      == "1",
    )

    private fun applyTweakToState(state: TweaksState, key: String, value: Boolean): TweaksState =
        when (key) {
            // ── snake_case (canonical keys dari conf file) ────────────────
            "schedboost"       -> state.copy(schedboost = value)
            "cpu_boost"        -> state.copy(cpuBoost = value)
            "gpu_throttle_off" -> state.copy(gpuThrottleOff = value)
            "cpuset_opt"       -> state.copy(cpusetOpt = value)
            "obb_noop"         -> state.copy(mtkBoost = value)
            "lmk_aggressive"   -> state.copy(lmkAggressive = value)
            "zram"             -> state.copy(zram = value)
            "vm_dirty_opt"     -> state.copy(vmDirtyOpt = value)
            "io_latency_opt"   -> state.copy(ioLatencyOpt = value)
            "tcp_bbr"          -> state.copy(tcpBbr = value)
            "doh"              -> state.copy(doh = value)
            "net_buffer"       -> state.copy(netBuffer = value)
            "doze"             -> state.copy(doze = value)
            "fast_anim"        -> state.copy(fastAnim = value)
            "entropy_boost"    -> state.copy(entropyBoost = value)
            "clear_cache"      -> state.copy(clearCache = value)
            "cpu_freq_enable"  -> state.copy(cpuFreqEnable = value)
            "gpu_freq_lock"    -> state.copy(gpuFreqLock = value)
            "touch_boost"      -> state.copy(touchBoost = value)
            "ksm"              -> state.copy(ksm = value)
            "ksm_aggressive"   -> state.copy(ksmAggressive = value)
            "network_stable"   -> state.copy(networkStable = value)
            "swap"             -> state.copy(swap = value)
            "kill_background"  -> state.copy(killBackground = value)
            "cpu_freq_lock"    -> state.copy(cpuFreqLock = value)
            // ── camelCase aliases — digunakan oleh TweakScreen.kt ─────────
            // FIX: TweakScreen memanggil setTweak() dengan camelCase keys;
            // tanpa aliases ini semua toggle di TweakScreen tidak berpengaruh
            // karena jatuh ke `else -> state` dan apply channel tidak membawa
            // perubahan apapun.
            "cpuBoost"         -> state.copy(cpuBoost = value)
            "gpuThrottleOff"   -> state.copy(gpuThrottleOff = value)
            "tcpBbr"           -> state.copy(tcpBbr = value)
            "networkStable"    -> state.copy(networkStable = value)
            "killBackground"   -> state.copy(killBackground = value)
            "cpuFreqLock"      -> state.copy(cpuFreqLock = value)
            "lmkAggressive"    -> state.copy(lmkAggressive = value)
            "ioLatencyOpt"     -> state.copy(ioLatencyOpt = value)
            "vmDirtyOpt"       -> state.copy(vmDirtyOpt = value)
            "fastAnim"         -> state.copy(fastAnim = value)
            "entropyBoost"     -> state.copy(entropyBoost = value)
            "clearCache"       -> state.copy(clearCache = value)
            "cpuFreqEnable"    -> state.copy(cpuFreqEnable = value)
            "gpuFreqLock"      -> state.copy(gpuFreqLock = value)
            "touchBoost"       -> state.copy(touchBoost = value)
            "ksmAggressive"    -> state.copy(ksmAggressive = value)
            "cpusetOpt"        -> state.copy(cpusetOpt = value)
            "mtkBoost"         -> state.copy(mtkBoost = value)
            "ioScheduler"      -> state.copy(ioLatencyOpt = value)
            // privateDns: toggle DNS provider on/off (Cloudflare default)
            "privateDns"       -> state.copy(dnsProvider = if (value) "Cloudflare" else "")
            else               -> state
        }

    private fun applyTweakStrToState(state: TweaksState, key: String, value: String): TweaksState =
        when (key) {
            "cpu_governor"       -> state.copy(cpuGovernor = value)
            "io_scheduler"       -> state.copy(ioScheduler = value)
            "zram_size"          -> state.copy(zramSize = value)
            "zram_algo"          -> state.copy(zramAlgo = value)
            "thermal_profile"    -> state.copy(thermalProfile = value)
            "gpu_freq_max"       -> state.copy(gpuFreqMax = value)
            "touch_sample_rate"  -> state.copy(touchSampleRate = value)
            "cpu_freq_prime_min" -> state.copy(cpuFreqPrimeMin = value)
            "cpu_freq_prime_max" -> state.copy(cpuFreqPrimeMax = value)
            "cpu_freq_gold_min"  -> state.copy(cpuFreqGoldMin = value)
            "cpu_freq_gold_max"  -> state.copy(cpuFreqGoldMax = value)
            "cpu_freq_silver_min"-> state.copy(cpuFreqSilverMin = value)
            "cpu_freq_silver_max"-> state.copy(cpuFreqSilverMax = value)
            // New string-based toggles
            "dnsProvider", "dns_provider" -> state.copy(dnsProvider = value)
            else                 -> state
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Safe mode & reboot
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleSafeMode(enable: Boolean) = viewModelScope.launch {
        RootEngine.toggleSafeMode(enable)
        refresh()
    }

    fun reboot(mode: RootEngine.RebootMode) = viewModelScope.launch {
        RootEngine.reboot(mode)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backup & Reset
    // ─────────────────────────────────────────────────────────────────────────

    private val _backupList    = MutableStateFlow<List<BackupManager.BackupEntry>>(emptyList())
    val backupList: StateFlow<List<BackupManager.BackupEntry>> = _backupList.asStateFlow()

    private val _backupWorking = MutableStateFlow(false)
    val backupWorking: StateFlow<Boolean> = _backupWorking.asStateFlow()

    sealed class BackupEvent {
        data class Success(val msgKey: String) : BackupEvent()
        data class Failure(val msgKey: String) : BackupEvent()
    }

    private val _backupEvent = MutableStateFlow<BackupEvent?>(null)
    val backupEvent: StateFlow<BackupEvent?> = _backupEvent.asStateFlow()
    fun clearBackupEvent() { _backupEvent.value = null }

    fun loadBackups() = viewModelScope.launch(Dispatchers.IO) {
        _backupList.value = BackupManager.listBackups()
    }

    fun createBackup() = viewModelScope.launch {
        _backupWorking.value = true
        val name = BackupManager.createBackup()
        _backupEvent.value = if (name != null) {
            loadBackups()
            BackupEvent.Success("create")
        } else BackupEvent.Failure("create")
        _backupWorking.value = false
    }

    fun restoreBackup(filename: String) = viewModelScope.launch {
        _backupWorking.value = true
        val ok = BackupManager.restoreBackup(filename)
        _backupEvent.value = if (ok) {
            loadTweaks()
            applyChannel.trySend(Unit) // apply restored tweaks
            BackupEvent.Success("restore")
        } else BackupEvent.Failure("restore")
        _backupWorking.value = false
    }

    fun deleteBackup(filename: String) = viewModelScope.launch(Dispatchers.IO) {
        BackupManager.deleteBackup(filename)
        loadBackups()
    }

    fun resetTweaks() = viewModelScope.launch(Dispatchers.IO) {
        RootEngine.resetTweaks()
        loadTweaks()
        // Suppress success toast; UI will reflect default state
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auto-backup
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun autoBackupIfEnabled() {
        try {
            if (SettingsPrefs.getAutoBackup(getApplication())) BackupManager.createBackup()
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Snack helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun snack(msg: String) {
        _snackMessage.value = msg
    }

    fun clearSnack() {
        _snackMessage.value = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Settings
    // ─────────────────────────────────────────────────────────────────────────

    private val _darkModeOverride = MutableStateFlow(SettingsPrefs.isDarkModeOverride(getApplication()))
    private val _darkMode         = MutableStateFlow(SettingsPrefs.getDarkMode(getApplication()))
    private val _dynamicColor     = MutableStateFlow(SettingsPrefs.getDynamicColor(getApplication()))
    private val _autoBackup       = MutableStateFlow(SettingsPrefs.getAutoBackup(getApplication()))
    private val _applyOnBoot      = MutableStateFlow(SettingsPrefs.getApplyOnBoot(getApplication()))
    private val _notifications    = MutableStateFlow(SettingsPrefs.getNotifications(getApplication()))
    private val _debugLog         = MutableStateFlow(SettingsPrefs.getDebugLog(getApplication()))

    val darkModeOverride : StateFlow<Boolean> = _darkModeOverride.asStateFlow()
    val darkMode         : StateFlow<Boolean> = _darkMode.asStateFlow()
    val dynamicColor     : StateFlow<Boolean> = _dynamicColor.asStateFlow()
    val autoBackup       : StateFlow<Boolean> = _autoBackup.asStateFlow()
    val applyOnBoot      : StateFlow<Boolean> = _applyOnBoot.asStateFlow()
    val notifications    : StateFlow<Boolean> = _notifications.asStateFlow()
    val debugLog         : StateFlow<Boolean> = _debugLog.asStateFlow()

    fun setDarkMode(dark: Boolean) {
        SettingsPrefs.setDarkMode(getApplication(), dark)
        _darkMode.value = dark
        _darkModeOverride.value = true
    }

    fun clearDarkModeOverride() {
        SettingsPrefs.clearDarkModeOverride(getApplication())
        _darkModeOverride.value = false
    }

    fun setDynamicColor(e: Boolean) {
        SettingsPrefs.setDynamicColor(getApplication(), e)
        _dynamicColor.value = e
    }

    fun setAutoBackup(e: Boolean) {
        SettingsPrefs.setAutoBackup(getApplication(), e)
        _autoBackup.value = e
    }

    fun setApplyOnBoot(e: Boolean) {
        SettingsPrefs.setApplyOnBoot(getApplication(), e)
        _applyOnBoot.value = e
    }

    fun setNotifications(enabled: Boolean) {
        SettingsPrefs.setNotifications(getApplication(), enabled)
        _notifications.value = enabled
        if (!enabled) getApplication<Application>()
            .getSystemService(NotificationManager::class.java)?.cancelAll()
    }

    fun setDebugLog(e: Boolean) {
        SettingsPrefs.setDebugLog(getApplication(), e)
        _debugLog.value = e
    }

    fun resetToDefaults() = viewModelScope.launch(Dispatchers.IO) {
        RootEngine.resetTweaks()
        loadTweaks()
        // Suppress success toast; UI will reflect default state
    }

    fun clearAppCache() = viewModelScope.launch(Dispatchers.IO) {
        try {
            getApplication<Application>().cacheDir?.deleteRecursively()
            getApplication<Application>().externalCacheDir?.deleteRecursively()
            // Do not show a success toast when clearing cache
        } catch (e: Exception) {
            snack("Gagal hapus cache: ${e.message}")
        }
    }

    fun emitBackupSuccess(msgKey: String) {
        _backupEvent.value = BackupEvent.Success(msgKey)
    }

}