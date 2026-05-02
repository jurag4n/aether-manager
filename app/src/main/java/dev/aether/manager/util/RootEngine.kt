package dev.aether.manager.util

import android.os.Build
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object RootEngine {
    const val CONF_DIR        = "/data/local/tmp/aether"
    const val TWEAKS_CONF     = "$CONF_DIR/tweaks.conf"
    const val PROFILE_FILE    = "$CONF_DIR/profile"
    const val SAFE_MODE_FILE  = "$CONF_DIR/safe_mode"
    const val BOOT_COUNT_FILE = "$CONF_DIR/boot_count"

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    suspend fun hasRoot(): Boolean {
        val rooted = RootManager.isRooted()
        if (rooted && !RootManager.isRootGranted) {
            RootManager.markGranted()
        }
        return rooted
    }

    suspend fun sh(script: String): ShellResult = withContext(Dispatchers.IO) {
        val appGranted = runCatching { Shell.isAppGrantedRoot() }.getOrNull()
        val canRun = appGranted == true || RootManager.isRootGranted

        if (!canRun) {
            return@withContext ShellResult(1, "", "Root not granted")
        }

        return@withContext try {
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

    private suspend fun shLocal(script: String, timeoutSec: Long = 4L): ShellResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val proc = ProcessBuilder("/system/bin/sh", "-c", script).start()
                val finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS)
                if (!finished) {
                    proc.destroyForcibly()
                    ShellResult(124, "", "Local shell timeout")
                } else {
                    ShellResult(
                        exitCode = proc.exitValue(),
                        stdout = proc.inputStream.bufferedReader().use { it.readText() },
                        stderr = proc.errorStream.bufferedReader().use { it.readText() },
                    )
                }
            }.getOrElse { e ->
                ShellResult(1, "", e.message ?: "Local shell error")
            }
        }

    suspend fun sh(vararg cmds: String): ShellResult =
        sh(cmds.joinToString("\n"))

    suspend fun ensureConfDir() {
        sh("mkdir -p $CONF_DIR")
    }

    suspend fun readFile(path: String): String =
        sh("cat $path 2>/dev/null").stdout

    suspend fun writeFile(path: String, content: String): Boolean {
        val escaped = content.replace("'", "'\\''")
        return sh("printf '%s' '$escaped' > $path").exitCode == 0
    }

    suspend fun fileExists(path: String): Boolean =
        sh("[ -f $path ] && echo yes").stdout.contains("yes")

    suspend fun getProp(key: String): String = withContext(Dispatchers.IO) {
        shLocal("getprop $key 2>/dev/null").stdout.trim()
    }

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

    suspend fun resetTweaks() {
        sh("rm -f $TWEAKS_CONF")
    }

    fun getDeviceInfoFallback(): DeviceInfo {
        val raw = listOf(
            Build.BOARD.orEmpty(),
            Build.HARDWARE.orEmpty(),
            Build.DEVICE.orEmpty(),
            Build.PRODUCT.orEmpty(),
            Build.MANUFACTURER.orEmpty(),
        ).joinToString(" ").trim()

        return DeviceInfo(
            model     = Build.MODEL ?: "Unknown Device",
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
            model=$(getprop ro.product.model 2>/dev/null | head -c 40)
            android=$(getprop ro.build.version.release 2>/dev/null)
            platform=$(getprop ro.board.platform 2>/dev/null)
            hardware=$(getprop ro.hardware 2>/dev/null)
            soc_model=$(getprop ro.soc.model 2>/dev/null)
            kernel=$(uname -r 2>/dev/null | head -c 50)
            selinux=$(getenforce 2>/dev/null)
            if [ -d /data/adb/ksu ]; then
              root=KernelSU
            elif [ -d /data/adb/ap ]; then
              root=APatch
            elif [ -d /data/adb/magisk ]; then
              root=Magisk
            else
              root=Unknown
            fi
            profile=$(cat '/data/local/tmp/aether/profile' 2>/dev/null || echo balance)
            safe=$([ -f '/data/local/tmp/aether/safe_mode' ] && echo 1 || echo 0)
            boot=$(cat '/data/local/tmp/aether/boot_count' 2>/dev/null || echo 0)
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
            shellRootType != null && shellRootType != "Unknown" -> shellRootType
            else -> RootManager.detectRootType()
        }

        return DeviceInfo(
            model     = map["model"]    ?: "Unknown Device",
            android   = map["android"]  ?: "?",
            kernel    = map["kernel"]   ?: "?",
            selinux   = map["selinux"]  ?: "?",
            rootType  = rootType,
            soc       = detectSoc(platform),
            socRaw    = map["platform"] ?: "",
            pid       = "",
            profile   = map["profile"]  ?: "balance",
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

    suspend fun readTweaksConf(): Map<String, String> =
        parseKv(readFile(TWEAKS_CONF))

    suspend fun writeTweakConf(key: String, value: String): Boolean {
        val script = """
            if grep -q '^$key=' $TWEAKS_CONF 2>/dev/null; then
              sed -i 's|^$key=.*|$key=$value|' $TWEAKS_CONF
            else
              echo '$key=$value' >> $TWEAKS_CONF
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

    private fun milliCtoFloat(mc: Int): Float {
        if (mc <= 0) return 0f
        val c = mc / 1000f
        return if (c in 15f..125f) c else 0f
    }

    private fun normRaw(raw: Long): Float {
        if (raw <= 0L) return 0f
        // 1) Millidegrees — paling umum di Android thermal zone (mis. 45000 → 45°C)
        if (raw >= 1_000L) {
            val c = raw / 1000f
            if (c in 15f..125f) return c
        }
        // 2) Tenths of degrees (mis. 450 → 45°C) — beberapa kernel lama
        if (raw >= 100L) {
            val c = raw / 10f
            if (c in 15f..125f) return c
        }
        // 3) Raw degrees (mis. 45 → 45°C)
        val c = raw.toFloat()
        return if (c in 15f..125f) c else 0f
    }

    private fun validTemp(v: Float) = v in 15f..125f

    suspend fun getMonitorState(): dev.aether.manager.data.MonitorState =
        withContext(Dispatchers.IO) {
            // ── CPU ──────────────────────────────────────────────────────────
            val cpuR = sh("""
                line1=$(grep -m1 "^cpu " /proc/stat); sleep 0.5; line2=$(grep -m1 "^cpu " /proc/stat)
                set -- $line1; u1=$2;n1=$3;s1=$4;i1=$5;w1=$6;hi1=$7;si1=$8
                total1=$((u1+n1+s1+i1+w1+hi1+si1)); idle1=$((i1+w1))
                set -- $line2; u2=$2;n2=$3;s2=$4;i2=$5;w2=$6;hi2=$7;si2=$8
                total2=$((u2+n2+s2+i2+w2+hi2+si2)); idle2=$((i2+w2))
                dt=$((total2-total1)); di=$((idle2-idle1))
                [ $dt -gt 0 ] && echo cpu_usage=$(((dt-di)*100/dt)) || echo cpu_usage=0
                total_freq=0; core_count=0
                for f in /sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_cur_freq; do
                  [ -f "$f" ] || continue
                  v=$(cat "$f" 2>/dev/null)
                  [ -n "$v" ] && [ "$v" -gt 0 ] 2>/dev/null && { total_freq=$((total_freq+v)); core_count=$((core_count+1)); }
                done
                [ $core_count -gt 0 ] && echo cpu_freq=$((total_freq/core_count/1000)) || echo cpu_freq=0
                echo cpu_gov=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo unknown)
                cpu_temp_raw=0
                for _z in /sys/class/thermal/thermal_zone*; do
                  [ -r "$_z/type" ] && [ -r "$_z/temp" ] || continue
                  _n=$(cat "$_z/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                  case "$_n" in *cpu*|*tsens*|*apc*|*cluster*)
                    _v=$(cat "$_z/temp" 2>/dev/null); [ -n "$_v" ] && { cpu_temp_raw=$_v; break; }
                  esac
                done
                echo cpu_temp=$cpu_temp_raw
            """.trimIndent())
            val cpuM      = parseKv(cpuR.stdout)
            val cpuUsage  = cpuM["cpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val cpuFreqMhz = cpuM["cpu_freq"]?.toLongOrNull() ?: 0L
            val cpuGovernor = cpuM["cpu_gov"] ?: ""
            val cpuTempC  = normRaw(cpuM["cpu_temp"]?.toLongOrNull() ?: 0L)

            // ── GPU ──────────────────────────────────────────────────────────
            val gpuR = sh("""
                gpu_usage=0
                if [ -f /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage ]; then
                  val=$(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                  [ -n "$val" ] && gpu_usage=$val
                elif [ -f /sys/class/kgsl/kgsl-3d0/gpubusy ]; then
                  read -r used total < /sys/class/kgsl/kgsl-3d0/gpubusy 2>/dev/null
                  [ -n "$used" ] && [ "$total" -gt 0 ] 2>/dev/null && gpu_usage=$((used*100/total))
                elif [ -f /sys/kernel/ged/hal/gpu_utilization ]; then
                  val=$(cat /sys/kernel/ged/hal/gpu_utilization 2>/dev/null | awk '{print $1}' | tr -cd '0-9')
                  [ -n "$val" ] && gpu_usage=$val
                elif [ -f /sys/class/misc/mali0/device/utilisation ]; then
                  val=$(cat /sys/class/misc/mali0/device/utilisation 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                  [ -n "$val" ] && gpu_usage=$val
                elif [ -f /sys/kernel/gpu/gpu_busy ]; then
                  val=$(cat /sys/kernel/gpu/gpu_busy 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                  [ -n "$val" ] && gpu_usage=$val
                fi
                [ "$gpu_usage" -gt 100 ] 2>/dev/null && gpu_usage=100
                echo gpu_usage=$gpu_usage
                gpu_freq=0
                if [ -f /sys/class/kgsl/kgsl-3d0/gpuclk ]; then
                  val=$(cat /sys/class/kgsl/kgsl-3d0/gpuclk 2>/dev/null | tr -cd '0-9')
                  [ "$val" -gt 0 ] 2>/dev/null && gpu_freq=$((val/1000000))
                elif [ -f /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq ]; then
                  val=$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq 2>/dev/null | tr -cd '0-9')
                  [ "$val" -gt 0 ] 2>/dev/null && gpu_freq=$((val/1000000))
                elif [ -f /sys/kernel/ged/hal/current_freqency ]; then
                  val=$(cat /sys/kernel/ged/hal/current_freqency 2>/dev/null | tr -cd '0-9')
                  [ "$val" -gt 0 ] 2>/dev/null && gpu_freq=$((val/1000000))
                elif [ -f /sys/kernel/gpu/gpu_clock ]; then
                  gpu_freq=$(cat /sys/kernel/gpu/gpu_clock 2>/dev/null | tr -cd '0-9')
                fi
                echo gpu_freq=$gpu_freq
                gpu_temp_raw=0
                for _z in /sys/class/thermal/thermal_zone*; do
                  [ -r "$_z/type" ] && [ -r "$_z/temp" ] || continue
                  _n=$(cat "$_z/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                  case "$_n" in *gpu*|*adreno*|*mali*|*g3d*|*mfg*|*ged*)
                    _v=$(cat "$_z/temp" 2>/dev/null); [ -n "$_v" ] && { gpu_temp_raw=$_v; break; }
                  esac
                done
                echo gpu_temp=$gpu_temp_raw
                gpu_name=""
                if [ -f /sys/class/kgsl/kgsl-3d0/gpu_model ]; then
                  gpu_name=$(cat /sys/class/kgsl/kgsl-3d0/gpu_model 2>/dev/null | tr -d '\n')
                elif [ -f /sys/class/kgsl/kgsl-3d0/gpu_tbl_name ]; then
                  gpu_name=$(cat /sys/class/kgsl/kgsl-3d0/gpu_tbl_name 2>/dev/null | tr -d '\n')
                elif [ -f /sys/class/misc/mali0/device/gpu_id ]; then
                  _id=$(cat /sys/class/misc/mali0/device/gpu_id 2>/dev/null | tr -cd '0-9')
                  [ -n "$_id" ] && gpu_name="Mali-G$_id"
                elif [ -d /sys/class/misc/mali0 ] || [ -d /sys/class/misc/g3d ]; then
                  gpu_name="Mali GPU"
                fi
                if [ -z "$gpu_name" ]; then
                  _vk=$(getprop ro.hardware.vulkan 2>/dev/null)
                  case "$_vk" in *adreno*) gpu_name="Adreno GPU";; *mali*) gpu_name="Mali GPU";; esac
                fi
                echo gpu_name=$gpu_name
            """.trimIndent())
            val gpuM      = parseKv(gpuR.stdout)
            val gpuUsage  = gpuM["gpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val gpuFreqMhz = gpuM["gpu_freq"]?.toLongOrNull() ?: 0L
            val gpuTempC  = normRaw(gpuM["gpu_temp"]?.toLongOrNull() ?: 0L)
            val gpuName   = gpuM["gpu_name"] ?: ""

            // ── Memory ───────────────────────────────────────────────────────
            val memR = sh("""
                mt=$(awk '/^MemTotal:/{print $2}' /proc/meminfo)
                ma=$(awk '/^MemAvailable:/{print $2}' /proc/meminfo)
                st=$(awk '/^SwapTotal:/{print $2}' /proc/meminfo)
                sf=$(awk '/^SwapFree:/{print $2}' /proc/meminfo)
                echo ram_total_mb=$((mt/1024))
                echo ram_used_mb=$(((mt-ma)/1024))
                echo swap_total_mb=$((st/1024))
                echo swap_used_mb=$(((st-sf)/1024))
            """.trimIndent())
            val memM       = parseKv(memR.stdout)
            val ramTotalMb = memM["ram_total_mb"]?.toLongOrNull()  ?: 0L
            val ramUsedMb  = memM["ram_used_mb"]?.toLongOrNull()   ?: 0L
            val swapTotalMb = memM["swap_total_mb"]?.toLongOrNull() ?: 0L
            val swapUsedMb  = memM["swap_used_mb"]?.toLongOrNull()  ?: 0L

            // ── Battery ──────────────────────────────────────────────────────
            val batR = sh("""
                echo bat_level=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 0)
                echo bat_temp=$(cat /sys/class/power_supply/battery/temp 2>/dev/null || cat /sys/class/power_supply/Battery/temp 2>/dev/null || echo 0)
                bat_ua=0
                for _p in /sys/class/power_supply/battery/current_now /sys/class/power_supply/Battery/current_now /sys/class/power_supply/bms/current_now; do
                  [ -f "$_p" ] || continue; _v=$(cat "$_p" 2>/dev/null | tr -cd '\-0-9'); [ -n "$_v" ] && { bat_ua=$_v; break; }
                done
                echo bat_ua=$bat_ua
                bat_uv=0
                for _p in /sys/class/power_supply/battery/voltage_now /sys/class/power_supply/Battery/voltage_now /sys/class/power_supply/bms/voltage_now; do
                  [ -f "$_p" ] || continue; _v=$(cat "$_p" 2>/dev/null | tr -cd '0-9'); [ -n "$_v" ] && { bat_uv=$_v; break; }
                done
                echo bat_uv=$bat_uv
                echo bat_status=$(cat /sys/class/power_supply/battery/status 2>/dev/null || echo Unknown)
            """.trimIndent())
            val batM       = parseKv(batR.stdout)
            val batLevel   = batM["bat_level"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val batTempC   = normRaw(batM["bat_temp"]?.toLongOrNull() ?: 0L)
            val rawUa      = batM["bat_ua"]?.toLongOrNull() ?: 0L
            val batCurrentMa = if (rawUa > 30_000L || rawUa < -30_000L) rawUa / 1000L else rawUa
            val rawUv      = batM["bat_uv"]?.toLongOrNull() ?: 0L
            val batVoltage = if (rawUv > 1_000_000L) rawUv / 1000L else rawUv
            val batStatus  = batM["bat_status"] ?: "Unknown"

            // ── Thermal ──────────────────────────────────────────────────────
            val thermalR = sh("""
                for _z in /sys/class/thermal/thermal_zone*; do
                  [ -r "$_z/type" ] && [ -r "$_z/temp" ] || continue
                  _n=$(cat "$_z/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                  case "$_n" in
                    *battery*|*batt*|*charger*|*usb*|*pa_therm*|*quiet_therm*) continue;;
                    *soc*|*xo*|*skin*|*shell*|*board*|*ambient*|*pmic*|*modem*)
                      _v=$(cat "$_z/temp" 2>/dev/null); [ -n "$_v" ] && { echo thermal_temp=$_v; exit 0; }
                  esac
                done
                echo thermal_temp=0
            """.trimIndent())
            val thermalRaw = parseKv(thermalR.stdout)["thermal_temp"]?.toLongOrNull() ?: 0L
            val thermalTempC = normRaw(thermalRaw).takeIf(::validTemp) ?: cpuTempC.takeIf(::validTemp) ?: 0f

            // ── Storage & Uptime ─────────────────────────────────────────────
            val storageR = sh("""
                df_out=$(df -k /data 2>/dev/null | tail -1)
                echo storage_total_kb=$(echo "$df_out" | awk '{print $2}')
                echo storage_used_kb=$(echo "$df_out" | awk '{print $3}')
                up=$(cut -d. -f1 /proc/uptime 2>/dev/null || echo 0)
                echo uptime=$(printf "%dd_%dh_%dm" $((up/86400)) $(((up%86400)/3600)) $(((up%3600)/60)))
            """.trimIndent())
            val sm = parseKv(storageR.stdout)

            dev.aether.manager.data.MonitorState(
                cpuUsage       = cpuUsage,
                cpuFreq        = if (cpuFreqMhz > 0) "$cpuFreqMhz MHz" else "",
                gpuUsage       = gpuUsage,
                gpuFreq        = if (gpuFreqMhz > 0) "$gpuFreqMhz MHz" else "",
                gpuName        = gpuName,
                ramUsedMb      = ramUsedMb,
                ramTotalMb     = ramTotalMb,
                cpuTemp        = cpuTempC,
                gpuTemp        = gpuTempC,
                thermalTemp    = thermalTempC,
                batTemp        = batTempC,
                storageUsedGb  = (sm["storage_used_kb"]?.toLongOrNull()  ?: 0L) / 1_048_576f,
                storageTotalGb = (sm["storage_total_kb"]?.toLongOrNull() ?: 0L) / 1_048_576f,
                uptime         = (sm["uptime"] ?: "").replace("_", " "),
                batLevel       = batLevel,
                cpuGovernor    = cpuGovernor,
                swapUsedMb     = swapUsedMb,
                swapTotalMb    = swapTotalMb,
                batCurrentMa   = batCurrentMa,
                batVoltage     = batVoltage,
                batStatus      = batStatus,
            )
        }

    private fun parseKv(raw: String): Map<String, String> =
        raw.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i > 0) line.substring(0, i).trim() to line.substring(i + 1).trim()
            else null
        }.toMap()
}
