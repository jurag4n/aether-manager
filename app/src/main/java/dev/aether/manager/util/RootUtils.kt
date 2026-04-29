package dev.aether.manager.util

import com.topjohnwu.superuser.Shell
import dev.aether.manager.CimolAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootUtils {

    const val CONF_DIR        = "/data/local/tmp/aether"
    const val TWEAKS_CONF     = "$CONF_DIR/tweaks.conf"
    const val PROFILE_FILE    = "$CONF_DIR/profile"
    const val SAFE_MODE_FILE  = "$CONF_DIR/safe_mode"
    const val BOOT_COUNT_FILE = "$CONF_DIR/boot_count"

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    suspend fun hasRoot(): Boolean {
        val rooted = RootManager.isRooted()
        // Sinkronisasi state jika terdeteksi root tapi cache belum update
        if (rooted && !RootManager.isRootGranted) {
            RootManager.markGranted()
        }
        return rooted
    }

    suspend fun sh(script: String): ShellResult = withContext(Dispatchers.IO) {
        val result = Shell.cmd(script).exec()
        ShellResult(
            exitCode = if (result.isSuccess) 0 else 1,
            stdout   = result.out.joinToString("\n"),
            stderr   = result.err.joinToString("\n"),
        )
    }

    suspend fun ensureConfDir() = withContext(Dispatchers.IO) {
        Shell.cmd("mkdir -p $CONF_DIR").exec()
    }

    suspend fun sh(vararg cmds: String): ShellResult =
        sh(cmds.joinToString("\n"))

    suspend fun readFile(path: String): String =
        sh("cat $path 2>/dev/null").stdout

    suspend fun writeFile(path: String, content: String): Boolean {
        val escaped = content.replace("'", "'\\''")
        return sh("printf '%s' '$escaped' > $path").exitCode == 0
    }

    suspend fun fileExists(path: String): Boolean =
        sh("[ -f $path ] && echo yes").stdout.contains("yes")

    suspend fun getProp(key: String): String = withContext(Dispatchers.IO) {
        Shell.cmd("getprop $key 2>/dev/null").exec().out.joinToString("").trim()
    }

    fun readProfileSync(): String =
        Shell.cmd("cat $PROFILE_FILE 2>/dev/null || echo balance")
            .exec().out.joinToString("").trim()
            .ifBlank { "balance" }

    suspend fun resetTweaks() = withContext(Dispatchers.IO) {
        Shell.cmd("rm -f $TWEAKS_CONF").exec()
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
            profile=$(cat ${PROFILE_FILE} 2>/dev/null || echo balance)
            safe=$([ -f ${SAFE_MODE_FILE} ] && echo 1 || echo 0)
            boot=$(cat ${BOOT_COUNT_FILE} 2>/dev/null || echo 0)
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

    suspend fun getMonitorState(): dev.aether.manager.data.MonitorState =
        withContext(Dispatchers.IO) {
            val cpuScript = """
                line1=$(grep -m1 "^cpu " /proc/stat)
                sleep 0.5
                line2=$(grep -m1 "^cpu " /proc/stat)
                set -- ${'$'}line1
                u1=${'$'}2; n1=${'$'}3; s1=${'$'}4; i1=${'$'}5; w1=${'$'}6; hi1=${'$'}7; si1=${'$'}8
                total1=${'$'}((u1+n1+s1+i1+w1+hi1+si1))
                idle1=${'$'}((i1+w1))
                set -- ${'$'}line2
                u2=${'$'}2; n2=${'$'}3; s2=${'$'}4; i2=${'$'}5; w2=${'$'}6; hi2=${'$'}7; si2=${'$'}8
                total2=${'$'}((u2+n2+s2+i2+w2+hi2+si2))
                idle2=${'$'}((i2+w2))
                dtotal=${'$'}((total2-total1))
                didle=${'$'}((idle2-idle1))
                if [ ${'$'}dtotal -gt 0 ]; then
                  usage=${'$'}(( (dtotal-didle)*100/dtotal ))
                else
                  usage=0
                fi
                echo cpu_usage=${'$'}usage
            """.trimIndent()

            val statScript = """
                total_freq=0; core_count=0
                for f in /sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_cur_freq; do
                  [ -f "${'$'}f" ] || continue
                  v=$(cat "${'$'}f" 2>/dev/null)
                  [ -n "${'$'}v" ] && [ "${'$'}v" -gt 0 ] 2>/dev/null && {
                    total_freq=${'$'}((total_freq+v))
                    core_count=${'$'}((core_count+1))
                  }
                done
                if [ ${'$'}core_count -gt 0 ]; then
                  echo cpu_freq=${'$'}((total_freq/core_count/1000))
                else
                  echo cpu_freq=0
                fi

                gpu_usage=0
                if [ -f /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage ]; then
                  val=$(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                  [ -n "${'$'}val" ] && gpu_usage=${'$'}val
                elif [ -f /sys/class/kgsl/kgsl-3d0/gpubusy ]; then
                  read -r used total < /sys/class/kgsl/kgsl-3d0/gpubusy 2>/dev/null
                  [ -n "${'$'}used" ] && [ -n "${'$'}total" ] && [ "${'$'}total" -gt 0 ] 2>/dev/null && \
                    gpu_usage=${'$'}((used*100/total))
                elif [ -f /sys/kernel/ged/hal/gpu_utilization ]; then
                  val=$(cat /sys/kernel/ged/hal/gpu_utilization 2>/dev/null | awk '{print $1}' | tr -cd '0-9')
                  [ -n "${'$'}val" ] && gpu_usage=${'$'}val
                elif [ -f /sys/kernel/debug/ged/hal/gpu_utilization ]; then
                  val=$(cat /sys/kernel/debug/ged/hal/gpu_utilization 2>/dev/null | awk '{print $1}' | tr -cd '0-9')
                  [ -n "${'$'}val" ] && gpu_usage=${'$'}val
                elif [ -f /sys/class/misc/mali0/device/utilisation ]; then
                  val=$(cat /sys/class/misc/mali0/device/utilisation 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                  [ -n "${'$'}val" ] && gpu_usage=${'$'}val
                elif [ -f /sys/kernel/gpu/gpu_busy ]; then
                  val=$(cat /sys/kernel/gpu/gpu_busy 2>/dev/null | tr -cd '0-9' | cut -c1-3)
                  [ -n "${'$'}val" ] && gpu_usage=${'$'}val
                fi
                [ "${'$'}gpu_usage" -gt 100 ] 2>/dev/null && gpu_usage=100
                echo gpu_usage=${'$'}gpu_usage

                gpu_freq=0
                if [ -f /sys/class/kgsl/kgsl-3d0/gpuclk ]; then
                  val=$(cat /sys/class/kgsl/kgsl-3d0/gpuclk 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}val" ] && [ "${'$'}val" -gt 0 ] 2>/dev/null && gpu_freq=${'$'}((val/1000000))
                elif [ -f /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq ]; then
                  val=$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}val" ] && [ "${'$'}val" -gt 0 ] 2>/dev/null && gpu_freq=${'$'}((val/1000000))
                elif [ -f /sys/kernel/ged/hal/current_freqency ]; then
                  val=$(cat /sys/kernel/ged/hal/current_freqency 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}val" ] && [ "${'$'}val" -gt 0 ] 2>/dev/null && gpu_freq=${'$'}((val/1000000))
                elif [ -f /sys/kernel/gpu/gpu_clock ]; then
                  val=$(cat /sys/kernel/gpu/gpu_clock 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}val" ] && [ "${'$'}val" -gt 0 ] 2>/dev/null && gpu_freq=${'$'}val
                fi
                echo gpu_freq=${'$'}gpu_freq

                mem_total=$(awk '/^MemTotal:/{print $2}' /proc/meminfo)
                mem_avail=$(awk '/^MemAvailable:/{print $2}' /proc/meminfo)
                mem_used=${'$'}((mem_total-mem_avail))
                echo ram_total_mb=${'$'}((mem_total/1024))
                echo ram_used_mb=${'$'}((mem_used/1024))

                sw_total=$(awk '/^SwapTotal:/{print $2}' /proc/meminfo)
                sw_free=$(awk '/^SwapFree:/{print $2}' /proc/meminfo)
                echo swap_total_mb=${'$'}((sw_total/1024))
                echo swap_used_mb=${'$'}(((sw_total-sw_free)/1024))

                cpu_temp=0
                for zone in 4 0 1 2 3 5 6 7; do
                  path="/sys/class/thermal/thermal_zone${'$'}zone/temp"
                  [ -f "${'$'}path" ] || continue
                  t=$(cat "${'$'}path" 2>/dev/null)
                  [ -n "${'$'}t" ] && [ "${'$'}t" -gt 0 ] 2>/dev/null && { cpu_temp=${'$'}t; break; }
                done
                echo cpu_temp=${'$'}cpu_temp

                echo bat_temp=$(cat /sys/class/power_supply/battery/temp 2>/dev/null || echo 0)

                gpu_temp=-1
                for _gkw in gpu adreno-lowf gpuss mali; do
                  for _zi in ${'$'}(seq 0 49); do
                    _tp="/sys/class/thermal/thermal_zone${'$'}{_zi}/type"
                    _tv="/sys/class/thermal/thermal_zone${'$'}{_zi}/temp"
                    [ -f "${'$'}_tp" ] && [ -f "${'$'}_tv" ] || continue
                    _t=${'$'}(cat "${'$'}_tp" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                    case "${'$'}_t" in *"${'$'}_gkw"*) gpu_temp=${'$'}(cat "${'$'}_tv" 2>/dev/null || echo -1); break 2;; esac
                  done
                done
                echo gpu_temp=${'$'}gpu_temp

                thermal_temp=-1
                for _tzone in /sys/class/thermal/thermal_zone*/temp; do
                  _tv=${'$'}(cat "${'$'}_tzone" 2>/dev/null)
                  [ -n "${'$'}_tv" ] && [ "${'$'}_tv" -gt 0 ] 2>/dev/null && { thermal_temp=${'$'}_tv; break; }
                done
                echo thermal_temp=${'$'}thermal_temp
                echo bat_level=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 0)

                bat_ua=0
                for _cp in \
                  /sys/class/power_supply/battery/current_now \
                  /sys/class/power_supply/Battery/current_now \
                  /sys/class/power_supply/bms/current_now; do
                  [ -f "${'$'}_cp" ] || continue
                  _v=$(cat "${'$'}_cp" 2>/dev/null | tr -cd '\-0-9')
                  [ -n "${'$'}_v" ] && { bat_ua=${'$'}_v; break; }
                done
                echo bat_ua=${'$'}bat_ua

                bat_uv=0
                for _vp in \
                  /sys/class/power_supply/battery/voltage_now \
                  /sys/class/power_supply/Battery/voltage_now \
                  /sys/class/power_supply/bms/voltage_now; do
                  [ -f "${'$'}_vp" ] || continue
                  _v=$(cat "${'$'}_vp" 2>/dev/null | tr -cd '0-9')
                  [ -n "${'$'}_v" ] && { bat_uv=${'$'}_v; break; }
                done
                echo bat_uv=${'$'}bat_uv

                echo bat_status=$(cat /sys/class/power_supply/battery/status 2>/dev/null || echo Unknown)
                echo cpu_gov=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo unknown)

                read -r _ stotal sused _ <<< $(df /data 2>/dev/null | tail -1)
                echo storage_used_kb=${'$'}{sused:-0}
                echo storage_total_kb=${'$'}{stotal:-0}

                up=$(awk '{printf "%d", $1}' /proc/uptime 2>/dev/null || echo 0)
                echo uptime=${'$'}((up/3600))h_${'$'}((up%3600/60))m
            """.trimIndent()

            val r1 = sh(cpuScript)
            val r2 = sh(statScript)
            val map = parseKv(r1.stdout + "\n" + r2.stdout)

            val cpuTempRaw = map["cpu_temp"]?.toLongOrNull() ?: 0L
            val batTempRaw = map["bat_temp"]?.toLongOrNull() ?: 0L

            fun normTemp(raw: Long): Float = when {
                raw > 1000 -> raw / 1000f
                raw > 200  -> raw / 10f
                raw > 0    -> raw.toFloat()
                else       -> 0f
            }

            val gpuTempC: Float = if (CimolAgent.isAvailable) {
                val mc = CimolAgent.getGpuTempMilliC()
                if (mc > 0) mc / 1000f else normTemp(map["gpu_temp"]?.toLongOrNull() ?: -1L)
            } else {
                normTemp(map["gpu_temp"]?.toLongOrNull() ?: -1L)
            }

            val thermalTempC: Float = if (CimolAgent.isAvailable) {
                val zones = CimolAgent.getThermalZones()
                val best  = zones.firstOrNull { it.type.contains("soc", ignoreCase = true) }
                    ?: zones.firstOrNull { it.type.contains("thermal", ignoreCase = true) }
                    ?: zones.firstOrNull()
                if (best != null && best.tempMilliC > 0) best.tempMilliC / 1000f
                else normTemp(map["thermal_temp"]?.toLongOrNull() ?: -1L)
            } else {
                normTemp(map["thermal_temp"]?.toLongOrNull() ?: -1L)
            }

            dev.aether.manager.data.MonitorState(
                cpuUsage       = map["cpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                cpuFreq        = map["cpu_freq"]?.toLongOrNull()?.takeIf { it > 0 }?.let { "$it MHz" } ?: "",
                gpuUsage       = map["gpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                gpuFreq        = map["gpu_freq"]?.toLongOrNull()?.takeIf { it > 0 }?.let { "$it MHz" } ?: "",
                ramUsedMb      = map["ram_used_mb"]?.toLongOrNull()  ?: 0L,
                ramTotalMb     = map["ram_total_mb"]?.toLongOrNull() ?: 0L,
                cpuTemp        = when {
                    cpuTempRaw > 1000 -> cpuTempRaw / 1000f
                    cpuTempRaw > 200  -> cpuTempRaw / 10f
                    else              -> cpuTempRaw.toFloat()
                },
                gpuTemp        = gpuTempC,
                thermalTemp    = thermalTempC,
                batTemp        = when {
                    batTempRaw > 1000 -> batTempRaw / 1000f
                    batTempRaw > 200  -> batTempRaw / 10f
                    else              -> batTempRaw.toFloat()
                },
                storageUsedGb  = (map["storage_used_kb"]?.toLongOrNull()  ?: 0L) / 1_048_576f,
                storageTotalGb = (map["storage_total_kb"]?.toLongOrNull() ?: 0L) / 1_048_576f,
                uptime         = (map["uptime"] ?: "").replace("_", " "),
                batLevel       = map["bat_level"]?.toIntOrNull() ?: 0,
                cpuGovernor    = map["cpu_gov"] ?: "",
                swapUsedMb     = map["swap_used_mb"]?.toLongOrNull()  ?: 0L,
                swapTotalMb    = map["swap_total_mb"]?.toLongOrNull() ?: 0L,
                batCurrentMa   = run {
                    val ua = map["bat_ua"]?.toLongOrNull() ?: 0L
                    if (ua == 0L) 0L
                    else if (ua > 30000L || ua < -30000L) ua / 1000L
                    else ua
                },
                batVoltage     = run {
                    val uv = map["bat_uv"]?.toLongOrNull() ?: 0L
                    if (uv > 1_000_000L) uv / 1000L else uv
                },
                batStatus      = map["bat_status"] ?: "Unknown",
            )
        }

    private fun parseKv(raw: String): Map<String, String> =
        raw.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i > 0) line.substring(0, i).trim() to line.substring(i + 1).trim()
            else null
        }.toMap()
}

data class DeviceInfo(
    val model     : String,
    val android   : String,
    val kernel    : String,
    val selinux   : String,
    val rootType  : String,
    val soc       : SocType,
    val socRaw    : String,
    val pid       : String,
    val profile   : String,
    val safeMode  : Boolean,
    val bootCount : Int
)

enum class SocType(val label: String) {
    SNAPDRAGON("Snapdragon"),
    MEDIATEK  ("MediaTek"),
    EXYNOS    ("Exynos"),
    KIRIN     ("Kirin"),
    OTHER     ("Universal")
}
