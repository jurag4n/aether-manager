package com.aether.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

object RootEngine {
    const val CONF_DIR        = "/data/adb/aether"
    const val TWEAKS_CONF     = "$CONF_DIR/tweaks.conf"
    const val PROFILE_FILE    = "$CONF_DIR/profile"
    const val SAFE_MODE_FILE  = "$CONF_DIR/safe_mode"
    const val BOOT_COUNT_FILE = "$CONF_DIR/boot_count"

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    // ── Root shell (libsu) ────────────────────────────────────────────────────

    suspend fun hasRoot(): Boolean {
        val rooted = RootManager.isRooted()
        if (rooted && !RootManager.isRootGranted) RootManager.markGranted()
        return rooted
    }

    /**
     * Jalankan script dengan root shell (libsu).
     * Jika root belum granted, coba request sekali.
     */
    suspend fun sh(script: String): ShellResult = withContext(Dispatchers.IO) {
        val canRun = RootManager.ensureRootShellSync(requestIfNeeded = true)
        if (!canRun) return@withContext ShellResult(1, "", "Root not granted")

        try {
            val result = Shell.cmd(script).exec()
            ShellResult(
                exitCode = if (result.isSuccess) 0 else 1,
                stdout   = result.out.joinToString("\n"),
                stderr   = result.err.joinToString("\n"),
            )
        } catch (e: Exception) {
            ShellResult(1, "", e.message ?: "Shell error")
        }
    }

    suspend fun sh(vararg cmds: String): ShellResult =
        sh(cmds.joinToString("\n"))

    // ── Non-root local shell (baca /proc, /sys, dsb.) ─────────────────────────
    // Gunakan timeout yang cukup — baca multiple thermal zones bisa lambat.

    private suspend fun shLocal(script: String): ShellResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val proc = ProcessBuilder("/system/bin/sh", "-c", script)
                    .redirectErrorStream(true)
                    .start()
                val finished = proc.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)
                if (!finished) {
                    proc.destroyForcibly()
                    return@runCatching ShellResult(124, "", "Local shell timeout")
                }
                ShellResult(
                    proc.exitValue(),
                    proc.inputStream.bufferedReader().readText(),
                    ""
                )
            }.getOrElse { e -> ShellResult(1, "", e.message ?: "Local shell error") }
        }

    /** Root fallback khusus monitor: tidak pernah memunculkan dialog root. */
    private suspend fun shRootCached(script: String): ShellResult =
        withContext(Dispatchers.IO) {
            if (!RootManager.ensureRootShellSync(requestIfNeeded = false)) {
                return@withContext ShellResult(1, "", "Root shell not cached")
            }
            runCatching {
                val result = Shell.cmd(script).exec()
                ShellResult(
                    if (result.isSuccess) 0 else 1,
                    result.out.joinToString("\n"),
                    result.err.joinToString("\n")
                )
            }.getOrElse { e -> ShellResult(1, "", e.message ?: "Root monitor shell error") }
        }

    // ── File helpers (butuh root) ─────────────────────────────────────────────

    suspend fun ensureConfDir() {
        sh("mkdir -p '$CONF_DIR' && chmod 700 '$CONF_DIR' && chown 0:0 '$CONF_DIR'")
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    suspend fun readFile(path: String): String =
        sh("cat ${shellQuote(path)} 2>/dev/null").stdout

    suspend fun writeFile(path: String, content: String): Boolean {
        val escaped = content.replace("'", "'\\''")
        val quotedPath = shellQuote(path)
        val dir = path.substringBeforeLast('/', "")
        val mkdir = if (dir.isNotBlank()) "mkdir -p ${shellQuote(dir)} && " else ""
        return sh("${mkdir}printf '%s' '$escaped' > $quotedPath && chmod 600 $quotedPath").exitCode == 0
    }

    suspend fun fileExists(path: String): Boolean =
        sh("[ -f ${shellQuote(path)} ] && echo yes").stdout.contains("yes")

    // ── getprop — tidak butuh root ────────────────────────────────────────────

    suspend fun getProp(key: String): String = withContext(Dispatchers.IO) {
        shLocal("getprop $key 2>/dev/null").stdout.trim()
    }

    // ── readProfileSync — fallback sync (dipanggil dari non-coroutine) ────────

    fun readProfileSync(): String {
        val granted = Shell.isAppGrantedRoot() == true || RootManager.isRootGranted
        if (!granted) return "balance"
        return try {
            Shell.cmd("cat $PROFILE_FILE 2>/dev/null || echo balance")
                .exec().out.joinToString("").trim()
                .ifBlank { "balance" }
        } catch (_: Exception) {
            "balance"
        }
    }

    suspend fun resetTweaks() = sh("rm -f $TWEAKS_CONF")

    // ── Device info ───────────────────────────────────────────────────────────

    fun getDeviceInfoFallback(): DeviceInfo {
        val raw = listOf(
            Build.BOARD.orEmpty(),
            Build.HARDWARE.orEmpty(),
            Build.DEVICE.orEmpty(),
            Build.PRODUCT.orEmpty(),
            Build.MANUFACTURER.orEmpty(),
        ).joinToString(" ").trim()

        return DeviceInfo(
            model     = Build.MODEL    ?: "Unknown Device",
            android   = Build.VERSION.RELEASE ?: "?",
            kernel    = System.getProperty("os.version") ?: "?",
            selinux   = "?",
            rootType  = RootManager.detectRootType(),
            soc       = detectSoc(raw.lowercase()),
            socRaw    = raw,
            pid       = "",
            profile   = readProfileSafe(),
            safeMode  = false,
            bootCount = 0,
        )
    }

    private fun readProfileSafe(): String = try {
        if (RootManager.isRootGranted) readProfileSync() else "balance"
    } catch (_: Exception) {
        "balance"
    }

    suspend fun getDeviceInfo(): DeviceInfo {
        val script = """
            model=${'$'}(getprop ro.product.model 2>/dev/null | head -c 40)
            android=${'$'}(getprop ro.build.version.release 2>/dev/null)
            platform=${'$'}(getprop ro.board.platform 2>/dev/null)
            hardware=${'$'}(getprop ro.hardware 2>/dev/null)
            soc_model=${'$'}(getprop ro.soc.model 2>/dev/null)
            kernel=${'$'}(uname -r 2>/dev/null | head -c 50)
            selinux=${'$'}(getenforce 2>/dev/null)
            if [ -d /data/adb/ksu ]; then
              root=KernelSU
            elif [ -d /data/adb/ap ]; then
              root=APatch
            elif [ -d /data/adb/magisk ]; then
              root=Magisk
            else
              root=Unknown
            fi
            profile=${'$'}(cat '$PROFILE_FILE' 2>/dev/null || echo balance)
            safe=${'$'}([ -f '$SAFE_MODE_FILE' ] && echo 1 || echo 0)
            boot=${'$'}(cat '$BOOT_COUNT_FILE' 2>/dev/null || echo 0)
            echo model=${'$'}model
            echo android=${'$'}android
            echo platform=${'$'}platform
            echo hardware=${'$'}hardware
            echo soc_model=${'$'}soc_model
            echo kernel=${'$'}kernel
            echo selinux=${'$'}selinux
            echo root=${'$'}root
            echo profile=${'$'}profile
            echo safe=${'$'}safe
            echo boot=${'$'}boot
        """.trimIndent()

        val result = sh(script)
        val map = parseKv(result.stdout)
        val platform = "${map["platform"]} ${map["hardware"]} ${map["soc_model"]}".lowercase()

        val shellRootType = map["root"]
        val rootType = when {
            !shellRootType.isNullOrBlank() && shellRootType != "Unknown" -> shellRootType
            else -> RootManager.detectRootType()
        }

        return DeviceInfo(
            model     = map["model"]   ?: "Unknown Device",
            android   = map["android"] ?: "?",
            kernel    = map["kernel"]  ?: "?",
            selinux   = map["selinux"] ?: "?",
            rootType  = rootType,
            soc       = detectSoc(platform),
            socRaw    = map["platform"] ?: "",
            pid       = "",
            profile   = map["profile"] ?: "balance",
            safeMode  = map["safe"] == "1",
            bootCount = map["boot"]?.toIntOrNull() ?: 0
        )
    }

    private fun detectSoc(platform: String): SocType = when {
        Regex("sm\\d|msm|qcom|snapdragon|sdm|lahaina|shima|taro|kalama|crow|parrot|neo|bengal|khaje|qualcomm|kryo|krait")
            .containsMatchIn(platform) -> SocType.SNAPDRAGON
        Regex("mt\\d|mediatek|helio|dimensity")
            .containsMatchIn(platform) -> SocType.MEDIATEK
        Regex("exynos|universal|s5e")
            .containsMatchIn(platform) -> SocType.EXYNOS
        Regex("kirin|hi36|hi37|emily|monica")
            .containsMatchIn(platform) -> SocType.KIRIN
        else -> SocType.OTHER
    }

    // ── Tweaks conf ───────────────────────────────────────────────────────────

    suspend fun readTweaksConf(): Map<String, String> =
        parseKv(readFile(TWEAKS_CONF))

    suspend fun writeTweakConf(key: String, value: String): Boolean {
        val safeKey   = key.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        val safeValue = value.replace("'", "'\\''")
        val script = """
            mkdir -p '$CONF_DIR'
            touch '$TWEAKS_CONF'
            if grep -q '^$safeKey=' '$TWEAKS_CONF' 2>/dev/null; then
              sed -i 's|^$safeKey=.*|$safeKey=$safeValue|' '$TWEAKS_CONF'
            else
              printf '%s\n' '$safeKey=$safeValue' >> '$TWEAKS_CONF'
            fi
        """.trimIndent()
        return sh(script).exitCode == 0
    }

    suspend fun setProfile(profile: String): Boolean = writeFile(PROFILE_FILE, profile)

    suspend fun applyTweaksDirect(tweaks: Map<String, String>): Boolean =
        TweakApplier.writeAndApply(TWEAKS_CONF, tweaks).success

    suspend fun setProfileDirect(profile: String): Boolean {
        writeFile(PROFILE_FILE, profile)
        val tweaks = readTweaksConf().toMutableMap()
        tweaks["profile"] = profile
        return TweakApplier.writeAndApply(TWEAKS_CONF, tweaks).success
    }

    suspend fun toggleSafeMode(enable: Boolean): Boolean =
        if (enable) sh("touch $SAFE_MODE_FILE").exitCode == 0
        else        sh("rm -f $SAFE_MODE_FILE").exitCode == 0

    suspend fun reboot(mode: RebootMode = RebootMode.NORMAL): Boolean =
        sh(when (mode) {
            RebootMode.NORMAL     -> "reboot"
            RebootMode.RECOVERY   -> "reboot recovery"
            RebootMode.BOOTLOADER -> "reboot bootloader"
        }).exitCode == 0

    enum class RebootMode { NORMAL, RECOVERY, BOOTLOADER }

    // ── Monitor state ───────────────────────────────────────────────────────────

    @Volatile private var lastCpuTotal: Long = 0L
    @Volatile private var lastCpuIdle: Long = 0L
    @Volatile private var lastCpuUsage: Int = 0
    @Volatile private var lastGpuBusyUsed: Long = 0L
    @Volatile private var lastGpuBusyTotal: Long = 0L
    @Volatile private var lastGpuUsage: Int = 0

    private fun readTextFast(path: String): String = runCatching {
        File(path).takeIf { it.canRead() }?.readText()?.trim().orEmpty()
    }.getOrDefault("")

    private fun readLongFast(path: String): Long = readTextFast(path).filter { it.isDigit() || it == '-' }.toLongOrNull() ?: 0L

    private fun powerSupplyPaths(fileName: String): List<String> = runCatching {
        val preferred = setOf("battery", "bms", "fg_battery", "main", "main-battery")
        File("/sys/class/power_supply").listFiles()
            ?.filter { ps -> preferred.any { key -> ps.name.contains(key, ignoreCase = true) } }
            ?.flatMap { ps ->
                listOf("${ps.path}/$fileName") + when (fileName) {
                    "temp" -> listOf("${ps.path}/temperature", "${ps.path}/batt_temp")
                    "current_now" -> listOf("${ps.path}/current_avg", "${ps.path}/batt_current_now")
                    "voltage_now" -> listOf("${ps.path}/batt_vol", "${ps.path}/voltage_avg")
                    else -> emptyList()
                }
            }
            .orEmpty()
            .distinct()
    }.getOrDefault(emptyList())

    private fun firstReadableLong(paths: List<String>): Long =
        paths.firstNotNullOfOrNull { readLongFast(it).takeIf { value -> value != 0L } } ?: 0L

    private fun glob(prefix: String, suffix: String = ""): List<File> = runCatching {
        val root = File(prefix.substringBefore('*').ifBlank { "/" })
        val parent = if (prefix.contains('*')) root.parentFile ?: File("/") else File(prefix)
        val namePrefix = prefix.substringAfterLast('/').substringBefore('*')
        parent.listFiles()?.filter { it.name.startsWith(namePrefix) && (suffix.isBlank() || it.path.endsWith(suffix)) }.orEmpty()
    }.getOrDefault(emptyList())

    private fun normRaw(raw: Long): Float {
        if (raw <= 0L) return 0f
        if (raw >= 1_000L) { val c = raw / 1000f; if (c in 15f..125f) return c }
        if (raw >= 100L)   { val c = raw / 10f;   if (c in 15f..125f) return c }
        val c = raw.toFloat()
        return if (c in 15f..125f) c else 0f
    }

    private fun validTemp(v: Float) = v in 15f..125f

    private fun readCpuUsage(): Int {
        val parts = readTextFast("/proc/stat").lineSequence().firstOrNull { it.startsWith("cpu ") }
            ?.trim()?.split(Regex("\\s+")) ?: return lastCpuUsage
        if (parts.size < 8) return lastCpuUsage
        val nums = parts.drop(1).map { it.toLongOrNull() ?: 0L }
        val idle = nums.getOrElse(3) { 0L } + nums.getOrElse(4) { 0L }
        val total = nums.sum()
        val oldTotal = lastCpuTotal
        val oldIdle = lastCpuIdle
        lastCpuTotal = total
        lastCpuIdle = idle
        if (oldTotal <= 0L || total <= oldTotal) return lastCpuUsage
        val dt = total - oldTotal
        val di = idle - oldIdle
        lastCpuUsage = (((dt - di).coerceAtLeast(0L) * 100L) / dt).toInt().coerceIn(0, 100)
        return lastCpuUsage
    }

    private fun readCpuFreqMhz(): Long {
        val policyFreqs = File("/sys/devices/system/cpu/cpufreq").listFiles()
            ?.filter { it.name.startsWith("policy") }
            ?.mapNotNull { policy ->
                val cur = readLongFast("${policy.path}/scaling_cur_freq")
                    .takeIf { it > 0L } ?: readLongFast("${policy.path}/cpuinfo_cur_freq")
                cur.takeIf { it > 0L }
            }.orEmpty()

        val cpuFreqs = File("/sys/devices/system/cpu").listFiles()
            ?.filter { it.name.matches(Regex("cpu[0-9]+")) }
            ?.mapNotNull { cpu ->
                val cur = readLongFast("${cpu.path}/cpufreq/scaling_cur_freq")
                    .takeIf { it > 0L } ?: readLongFast("${cpu.path}/cpufreq/cpuinfo_cur_freq")
                cur.takeIf { it > 0L }
            }.orEmpty()

        val freqs = (policyFreqs + cpuFreqs).distinct().filter { it > 0L }
        if (freqs.isEmpty()) {
            val procCpuMhz = readTextFast("/proc/cpuinfo")
                .lineSequence()
                .firstOrNull { it.contains("cpu MHz", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.toFloatOrNull()
                ?.roundToInt()
                ?.toLong()
            return procCpuMhz ?: 0L
        }

        val valueKhz = freqs.average().roundToInt().toLong()
        return when {
            valueKhz > 1_000_000L -> valueKhz / 1000L
            valueKhz > 10_000L -> valueKhz / 1000L
            else -> valueKhz
        }
    }

    private fun readCpuGovernor(): String {
        File("/sys/devices/system/cpu/cpufreq").listFiles()
            ?.firstOrNull { it.name.startsWith("policy") && File(it, "scaling_governor").canRead() }
            ?.let { return readTextFast("${it.path}/scaling_governor") }
        return File("/sys/devices/system/cpu").listFiles()
            ?.firstOrNull { it.name.matches(Regex("cpu[0-9]+")) && File(it, "cpufreq/scaling_governor").canRead() }
            ?.let { readTextFast("${it.path}/cpufreq/scaling_governor") }.orEmpty()
    }

    private fun readThermalByName(vararg keys: String, excludeBattery: Boolean = false): Float {
        val zones = File("/sys/class/thermal").listFiles()?.filter { it.name.startsWith("thermal_zone") }.orEmpty()
        var best = 0f
        for (z in zones) {
            val type = readTextFast("${z.path}/type").lowercase()
            if (type.isBlank()) continue
            if (excludeBattery && listOf("battery", "batt", "charger", "usb").any { type.contains(it) }) continue
            if (keys.any { type.contains(it) }) {
                val t = normRaw(readLongFast("${z.path}/temp"))
                if (validTemp(t)) return t
            }
        }
        for (z in zones) {
            val type = readTextFast("${z.path}/type").lowercase()
            if (excludeBattery && listOf("battery", "batt", "charger", "usb").any { type.contains(it) }) continue
            val t = normRaw(readLongFast("${z.path}/temp"))
            if (validTemp(t) && t > best) best = t
        }
        return best
    }

    private fun normalizeFreqToMhz(raw: Long): Long = when {
        raw <= 0L -> 0L
        raw > 100_000_000L -> raw / 1_000_000L
        raw > 100_000L -> raw / 1_000L
        else -> raw
    }

    private fun firstPercentFrom(text: String): Int? = text
        .split(Regex("\\s+|%|,"))
        .firstNotNullOfOrNull { part -> part.filter { it.isDigit() }.toIntOrNull()?.takeIf { v -> v in 0..100 } }

    private fun readGpuUsageLocal(): Int {
        firstPercentFrom(readTextFast("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"))?.let {
            lastGpuUsage = it
            return it
        }

        val busy = readTextFast("/sys/class/kgsl/kgsl-3d0/gpubusy").split(Regex("\\s+"))
        val used = busy.getOrNull(0)?.toLongOrNull() ?: 0L
        val total = busy.getOrNull(1)?.toLongOrNull() ?: 0L
        if (used > 0L && total > 0L) {
            val oldUsed = lastGpuBusyUsed
            val oldTotal = lastGpuBusyTotal
            lastGpuBusyUsed = used
            lastGpuBusyTotal = total
            val pct = if (oldTotal > 0L && total > oldTotal && used >= oldUsed) {
                val du = used - oldUsed
                val dt = total - oldTotal
                if (dt > 0L) ((du.coerceAtLeast(0L) * 100L) / dt).toInt() else 0
            } else {
                ((used.coerceAtMost(total) * 100L) / total).toInt()
            }.coerceIn(0, 100)
            if (pct > 0) {
                lastGpuUsage = pct
                return pct
            }
        }

        val paths = listOf(
            "/sys/kernel/ged/hal/gpu_utilization",
            "/sys/class/misc/mali0/device/utilisation",
            "/sys/class/devfreq/mtk-gpu/load",
            "/sys/kernel/gpu/gpu_busy",
            "/sys/kernel/gpu/gpu_utilization",
            "/proc/gpufreq/gpufreq_var_dump"
        )
        for (path in paths) {
            firstPercentFrom(readTextFast(path))?.let {
                lastGpuUsage = it
                return it
            }
        }
        return lastGpuUsage.takeIf { it > 0 } ?: 0
    }


    private fun readGpuLocal(): Triple<Int, Long, String> {
        val usage = readGpuUsageLocal()
        var freq = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
            "/sys/kernel/ged/hal/current_freqency",
            "/sys/kernel/ged/hal/current_freqency_by_pid",
            "/sys/kernel/gpu/gpu_clock"
        ).firstNotNullOfOrNull { readLongFast(it).takeIf { v -> v > 0L } } ?: 0L
        if (freq <= 0L) {
            freq = File("/sys/class/devfreq").listFiles()?.firstNotNullOfOrNull { d ->
                if (d.name.contains("gpu", true) || d.name.contains("mali", true) || d.name.contains("kgsl", true) || d.name.contains("g3d", true) || d.name.contains("mfg", true)) {
                    readLongFast("${d.path}/cur_freq").takeIf { it > 0L }
                } else null
            } ?: 0L
        }
        val name = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_model",
            "/sys/class/kgsl/kgsl-3d0/gpu_tbl_name",
            "/proc/gpuinfo"
        ).firstNotNullOfOrNull { readTextFast(it).takeIf { v -> v.isNotBlank() } } ?: when {
            File("/sys/class/kgsl/kgsl-3d0").exists() -> "Adreno GPU"
            File("/sys/class/misc/mali0").exists() || File("/sys/class/misc/g3d").exists() -> "Mali GPU"
            else -> ""
        }
        return Triple(usage.coerceIn(0, 100), normalizeFreqToMhz(freq), name.lineSequence().firstOrNull().orEmpty().take(48))
    }

    private data class BatterySnapshot(
        val level: Int = 0,
        val tempC: Float = 0f,
        val currentMa: Long = 0L,
        val voltageMv: Long = 0L,
        val status: String = "Unknown",
    )

    private fun normalizeBatteryCurrentMa(raw: Long): Long {
        if (raw == 0L || raw == Long.MIN_VALUE) return 0L
        return if (raw > 30_000L || raw < -30_000L) raw / 1000L else raw
    }

    private fun readBatterySnapshot(context: Context?): BatterySnapshot {
        if (context == null) return BatterySnapshot()
        return runCatching {
            val app = context.applicationContext
            val intent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val levelRaw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
            val level = if (levelRaw >= 0) ((levelRaw * 100f) / scale).roundToInt().coerceIn(0, 100) else 0
            val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = (tempRaw / 10f).takeIf(::validTemp) ?: 0f
            val voltageMv = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0).toLong().coerceAtLeast(0L)
            val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
                else -> "Unknown"
            }
            val bm = app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val currentRaw = listOf(
                bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0L,
                bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: 0L
            ).firstOrNull { it != 0L && it != Long.MIN_VALUE } ?: 0L
            BatterySnapshot(
                level = level,
                tempC = tempC,
                currentMa = normalizeBatteryCurrentMa(currentRaw),
                voltageMv = voltageMv,
                status = status,
            )
        }.getOrDefault(BatterySnapshot())
    }

    suspend fun getMonitorState(context: Context? = null): com.aether.data.MonitorState = withContext(Dispatchers.IO) {
        val cpuUsage = readCpuUsage()
        var cpuFreqMhz = readCpuFreqMhz()
        val cpuGovernor = readCpuGovernor()
        var cpuTempC = readThermalByName("cpu", "cpu0", "cpu-", "tsens", "apc", "cluster", "big", "little", "silver", "gold", "socd", "cpuss", excludeBattery = true)

        var (gpuUsage, gpuFreqMhz, gpuName) = readGpuLocal()
        var gpuTempC = readThermalByName("gpu", "gpu0", "gpu-", "adreno", "mali", "g3d", "mfg", "ged", excludeBattery = true)

        if ((gpuUsage <= 0 || gpuFreqMhz <= 0L || gpuName.isBlank() || !validTemp(gpuTempC)) && RootManager.isRootGranted) {
            val rootGpuR = shRootCached("""
                gpu_usage=0
                if [ -r /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage ]; then
                  gpu_usage=${'$'}(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                elif [ -r /sys/class/kgsl/kgsl-3d0/gpubusy ]; then
                  set -- ${'$'}(cat /sys/class/kgsl/kgsl-3d0/gpubusy 2>/dev/null)
                  [ -n "${'$'}1" ] && [ -n "${'$'}2" ] && [ "${'$'}2" -gt 0 ] 2>/dev/null && gpu_usage=${'$'}(( ${'$'}1 * 100 / ${'$'}2 ))
                fi
                if [ -z "${'$'}gpu_usage" ] || [ "${'$'}gpu_usage" -le 0 ] 2>/dev/null; then
                  for p in /sys/kernel/ged/hal/gpu_utilization /sys/class/misc/mali0/device/utilisation /sys/class/devfreq/*gpu*/load /sys/kernel/gpu/gpu_busy /sys/kernel/gpu/gpu_utilization; do
                    [ -r "${'$'}p" ] || continue
                    v=${'$'}(cat "${'$'}p" 2>/dev/null | tr -cd '0-9 ' | awk '{print $1}' | cut -c1-3)
                    [ -n "${'$'}v" ] && { gpu_usage=${'$'}v; break; }
                  done
                fi
                [ "${'$'}gpu_usage" -gt 100 ] 2>/dev/null && gpu_usage=100
                echo gpu_usage=${'$'}gpu_usage
                gpu_freq=0
                for p in /sys/class/kgsl/kgsl-3d0/gpuclk /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq /sys/kernel/ged/hal/current_freqency /sys/kernel/gpu/gpu_clock /sys/class/devfreq/*gpu*/cur_freq /sys/class/devfreq/*mali*/cur_freq /sys/class/devfreq/*kgsl*/cur_freq; do
                  [ -r "${'$'}p" ] || continue
                  v=${'$'}(cat "${'$'}p" 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}v" ] && { gpu_freq=${'$'}v; break; }
                done
                echo gpu_freq=${'$'}gpu_freq
                gpu_temp=0
                for z in /sys/class/thermal/thermal_zone*; do
                  [ -r "${'$'}z/type" ] && [ -r "${'$'}z/temp" ] || continue
                  n=${'$'}(cat "${'$'}z/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                  case "${'$'}n" in *gpu*|*adreno*|*mali*|*g3d*|*mfg*|*ged*) gpu_temp=${'$'}(cat "${'$'}z/temp" 2>/dev/null); break;; esac
                done
                echo gpu_temp=${'$'}gpu_temp
                gpu_name=""
                for p in /sys/class/kgsl/kgsl-3d0/gpu_model /sys/class/kgsl/kgsl-3d0/gpu_tbl_name /proc/gpuinfo; do
                  [ -r "${'$'}p" ] || continue
                  gpu_name=${'$'}(cat "${'$'}p" 2>/dev/null | head -n1 | tr -d '\r')
                  [ -n "${'$'}gpu_name" ] && break
                done
                echo gpu_name=${'$'}gpu_name
            """.trimIndent())
            val rootGpuM = parseKv(rootGpuR.stdout)
            if (gpuUsage <= 0) gpuUsage = rootGpuM["gpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: gpuUsage
            if (gpuFreqMhz <= 0L) gpuFreqMhz = normalizeFreqToMhz(rootGpuM["gpu_freq"]?.toLongOrNull() ?: 0L)
            if (!validTemp(gpuTempC)) gpuTempC = normRaw(rootGpuM["gpu_temp"]?.toLongOrNull() ?: 0L)
            if (gpuName.isBlank()) gpuName = rootGpuM["gpu_name"].orEmpty()
        }

        if ((cpuFreqMhz <= 0L || !validTemp(cpuTempC)) && RootManager.isRootGranted) {
            val rootCpuR = shRootCached("""
                total=0; count=0
                for p in /sys/devices/system/cpu/cpufreq/policy*/scaling_cur_freq /sys/devices/system/cpu/cpufreq/policy*/cpuinfo_cur_freq /sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_cur_freq /sys/devices/system/cpu/cpu[0-9]*/cpufreq/cpuinfo_cur_freq; do
                  [ -r "${'$'}p" ] || continue
                  v=${'$'}(cat "${'$'}p" 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}v" ] && [ "${'$'}v" -gt 0 ] 2>/dev/null && { total=${'$'}((total+v)); count=${'$'}((count+1)); }
                done
                [ "${'$'}count" -gt 0 ] && echo cpu_freq=${'$'}((total/count/1000)) || echo cpu_freq=0
                cpu_temp=0
                for z in /sys/class/thermal/thermal_zone*; do
                  [ -r "${'$'}z/type" ] && [ -r "${'$'}z/temp" ] || continue
                  n=${'$'}(cat "${'$'}z/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                  case "${'$'}n" in *cpu*|*tsens*|*apc*|*cluster*|*big*|*little*|*silver*|*gold*) cpu_temp=${'$'}(cat "${'$'}z/temp" 2>/dev/null); break;; esac
                done
                echo cpu_temp=${'$'}cpu_temp
            """.trimIndent())
            val rootCpuM = parseKv(rootCpuR.stdout)
            if (cpuFreqMhz <= 0L) cpuFreqMhz = rootCpuM["cpu_freq"]?.toLongOrNull() ?: 0L
            if (!validTemp(cpuTempC)) cpuTempC = normRaw(rootCpuM["cpu_temp"]?.toLongOrNull() ?: 0L)
        }

        val memInfo = readTextFast("/proc/meminfo").lines().associate { line ->
            val key = line.substringBefore(':', "")
            val value = line.substringAfter(':', "0").trim().substringBefore(' ').toLongOrNull() ?: 0L
            key to value
        }
        val ramTotalMb = (memInfo["MemTotal"] ?: 0L) / 1024L
        val ramUsedMb = (((memInfo["MemTotal"] ?: 0L) - (memInfo["MemAvailable"] ?: 0L)).coerceAtLeast(0L)) / 1024L
        val swapTotalMb = (memInfo["SwapTotal"] ?: 0L) / 1024L
        val swapUsedMb = (((memInfo["SwapTotal"] ?: 0L) - (memInfo["SwapFree"] ?: 0L)).coerceAtLeast(0L)) / 1024L

        val battery = readBatterySnapshot(context)
        val sysBatteryLevel = firstReadableLong(
            listOf("/sys/class/power_supply/battery/capacity", "/sys/class/power_supply/Battery/capacity") + powerSupplyPaths("capacity")
        ).toInt().coerceIn(0, 100)
        val batLevel = if (sysBatteryLevel > 0) sysBatteryLevel else battery.level
        val batTempC = (
            listOf(
                "/sys/class/power_supply/battery/temp",
                "/sys/class/power_supply/Battery/temp",
                "/sys/class/power_supply/bms/temp",
                "/sys/class/power_supply/battery/batt_temp",
                "/sys/class/power_supply/fg_battery/temp"
            ) + powerSupplyPaths("temp")
        ).firstNotNullOfOrNull { normRaw(readLongFast(it)).takeIf(::validTemp) }
            ?: battery.tempC.takeIf(::validTemp)
            ?: readThermalByName("battery", "batt", "charger")
        val rawUa = firstReadableLong(
            listOf(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/Battery/current_now",
                "/sys/class/power_supply/battery/current_avg",
                "/sys/class/power_supply/bms/current_now",
                "/sys/class/power_supply/bms/current_avg",
                "/sys/class/power_supply/fg_battery/current_now"
            ) + powerSupplyPaths("current_now")
        )
        val batCurrentMa = normalizeBatteryCurrentMa(rawUa).takeIf { it != 0L } ?: battery.currentMa
        val rawUv = firstReadableLong(
            listOf(
                "/sys/class/power_supply/battery/voltage_now",
                "/sys/class/power_supply/Battery/voltage_now",
                "/sys/class/power_supply/battery/voltage_avg",
                "/sys/class/power_supply/bms/voltage_now",
                "/sys/class/power_supply/fg_battery/voltage_now"
            ) + powerSupplyPaths("voltage_now")
        )
        val sysVoltage = if (rawUv > 1_000_000L) rawUv / 1000L else rawUv
        val batVoltage = sysVoltage.takeIf { it > 0L } ?: battery.voltageMv
        val batStatus = (
            listOf("/sys/class/power_supply/battery/status", "/sys/class/power_supply/Battery/status") + powerSupplyPaths("status")
        ).firstNotNullOfOrNull { readTextFast(it).takeIf { v -> v.isNotBlank() } } ?: battery.status

        val thermalTempC = readThermalByName("soc", "xo", "skin", "shell", "board", "ambient", "pmic", "modem", "quiet", "therm", excludeBattery = true)
            .takeIf(::validTemp)
            ?: cpuTempC.takeIf(::validTemp)
            ?: gpuTempC.takeIf(::validTemp)
            ?: batTempC.takeIf(::validTemp)
            ?: 0f

        val ramTotalForEstimate = ramTotalMb.takeIf { it > 0L } ?: 1L
        val ramUsageEstimate = ((ramUsedMb * 100L) / ramTotalForEstimate).toInt().coerceIn(0, 100)
        if (gpuUsage <= 0 && gpuFreqMhz <= 0L && gpuName.isBlank()) {
            gpuUsage = ((cpuUsage * 0.62f) + (ramUsageEstimate * 0.18f)).roundToInt().coerceIn(0, 100)
            gpuName = "System estimate"
        }
        val displayCpuTempC = if (validTemp(cpuTempC)) cpuTempC else thermalTempC
        val displayGpuTempC = if (validTemp(gpuTempC)) gpuTempC else thermalTempC

        val (storageTotalKb, storageUsedKb) = runCatching {
            val dataFs = StatFs(Environment.getDataDirectory().absolutePath)
            val blockSize = dataFs.blockSizeLong
            val total = dataFs.blockCountLong
            val free = dataFs.availableBlocksLong
            Pair((blockSize * total) / 1024L, (blockSize * (total - free)) / 1024L)
        }.getOrElse { Pair(0L, 0L) }

        val upSec = readTextFast("/proc/uptime").substringBefore(' ').toLongOrNull() ?: 0L
        val uptime = "%dd %dh %dm".format(upSec / 86400L, (upSec % 86400L) / 3600L, (upSec % 3600L) / 60L)

        com.aether.data.MonitorState(
            cpuUsage = cpuUsage,
            cpuFreq = if (cpuFreqMhz > 0L) "$cpuFreqMhz MHz" else "",
            gpuUsage = gpuUsage,
            gpuFreq = if (gpuFreqMhz > 0L) "$gpuFreqMhz MHz" else "",
            gpuName = gpuName.trim(),
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            cpuTemp = displayCpuTempC,
            gpuTemp = displayGpuTempC,
            thermalTemp = thermalTempC,
            batTemp = batTempC,
            storageUsedGb = storageUsedKb / 1_048_576f,
            storageTotalGb = storageTotalKb / 1_048_576f,
            uptime = uptime,
            batLevel = batLevel,
            cpuGovernor = cpuGovernor,
            swapUsedMb = swapUsedMb,
            swapTotalMb = swapTotalMb,
            batCurrentMa = batCurrentMa,
            batVoltage = batVoltage,
            batStatus = batStatus,
        )
    }

    private fun parseKv(raw: String): Map<String, String> =
        raw.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i > 0) line.substring(0, i).trim() to line.substring(i + 1).trim()
            else null
        }.toMap()
}
