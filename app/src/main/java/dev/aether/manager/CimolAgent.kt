package dev.aether.manager

object CimolAgent {

    private var loaded = false

    fun tryLoad(): Boolean {
        if (loaded) return true
        return runCatching {
            System.loadLibrary("cimolagent")
            loaded = true
            true
        }.getOrDefault(false)
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