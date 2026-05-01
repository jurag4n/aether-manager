package dev.aether.manager

object CimolAgent {

    private var loaded = false

    /**
     * Attempt to load the native `libcimolagent.so` library with a
     * resilient fallback.  Normally [System.loadLibrary] suffices, but
     * certain install scenarios (for example split APKs) may place the
     * library in a non-standard location.  If the standard load fails
     * and a [Context] is supplied the loader will attempt to load the
     * library from the app's `nativeLibraryDir`.  The [context]
     * parameter is optional; when omitted only the standard lookup is
     * attempted.
     *
     * @param context optional application context to resolve the
     * native library directory for fallback loading
     * @return `true` if the library was loaded or is already loaded,
     *         `false` otherwise
     */
    fun tryLoad(context: android.content.Context? = null): Boolean {
        if (loaded) return true
        return runCatching {
            try {
                // Attempt the standard library load first.
                System.loadLibrary("cimolagent")
            } catch (err: UnsatisfiedLinkError) {
                // Fallback: if a context is available, build the absolute
                // path to libcimolagent.so in the nativeLibraryDir and load
                // it directly.  If no context is provided rethrow the
                // error to propagate failure.
                val dir = context?.applicationInfo?.nativeLibraryDir
                val fallback = dir?.let { "$it/libcimolagent.so" }
                if (fallback != null) {
                    System.load(fallback)
                } else {
                    throw err
                }
            }
            loaded = true
            true
        }.getOrElse { false }
    }

    val isAvailable: Boolean get() = loaded

    // CPU

    external fun getCpuFreqsNow(): IntArray

    external fun getCpuFreqMinMax(): IntArray

    external fun getCpuGovernors(): Array<String>

    external fun getCpuUsagePercent(intervalMs: Int): Int

    // Thermal

    data class ThermalZone(
        val index: Int,
        val type: String,
        val tempMilliC: Int,
    )

    external fun getThermalRaw(): IntArray

    external fun getThermalTypes(): Array<String>

    fun getThermalZones(): List<ThermalZone> {
        if (!loaded) return emptyList()
        return runCatching {
            val raw   = getThermalRaw()
            val types = getThermalTypes()
            val result = mutableListOf<ThermalZone>()
            var i = 0
            while (i + 1 < raw.size) {
                val idx  = raw[i]
                val temp = raw[i + 1]
                val type = if (idx < types.size) types[idx] else "zone$idx"
                result += ThermalZone(idx, type, temp)
                i += 2
            }
            result
        }.getOrDefault(emptyList())
    }

    external fun getCpuTempMilliC(): Int

    external fun getGpuTempMilliC(): Int

    // Memory

    external fun getMemInfoKb(): LongArray

    external fun getZramStats(): LongArray

    // Battery

    external fun getBatteryStats(): LongArray

    external fun getBatteryStatus(): String

    // IO

    external fun getIoSchedulers(): Array<String>

    external fun setIoScheduler(device: String, scheduler: String): Boolean

    // KSM

    external fun getKsmStats(): LongArray

    // Process

    external fun getProcessStats(): LongArray

    external fun getProcessName(pid: Int): String

    // GPU

    external fun getGpuFreqNow(): Int

    external fun getGpuBusyPercent(): Int

    // Executor

    external fun execWithTimeout(cmd: String, timeoutMs: Int): String

    external fun writeSysfs(path: String, value: String): Boolean

    external fun readSysfs(path: String): String?
}