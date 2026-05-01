package dev.aether.manager.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RootEngine — modern root helper and device info provider.
 *
 * This replaces the old RootUtils. It uses libsu via Shell APIs to execute
 * commands as root and exposes helper methods to manage tweak configuration
 * files, read device state and apply tweaks. All functionality previously
 * provided by RootUtils is preserved so existing code can switch to this
 * class. The engine works across Snapdragon, MediaTek, Exynos and other
 * chipsets and applies tweaks immediately without requiring a reboot.
 */
object RootEngine {
    // Configuration directory and files. The same paths are kept to preserve
    // compatibility with existing backups and tweak configurations.
    const val CONF_DIR        = "/data/local/tmp/aether"
    const val TWEAKS_CONF     = "$CONF_DIR/tweaks.conf"
    const val PROFILE_FILE    = "$CONF_DIR/profile"
    const val SAFE_MODE_FILE  = "$CONF_DIR/safe_mode"
    const val BOOT_COUNT_FILE = "$CONF_DIR/boot_count"

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    /**
     * Determine whether the application has root access. Uses RootManager
     * for caching and detection. When root is granted the cached flag in
     * RootManager will be updated automatically.
     */
    suspend fun hasRoot(): Boolean {
        val rooted = RootManager.isRooted()
        // Synchronise cache if root is detected but not yet marked granted.
        if (rooted && !RootManager.isRootGranted) {
            RootManager.markGranted()
        }
        return rooted
    }

    /**
     * Execute a shell script as root. Returns a ShellResult containing
     * exit code, standard output and standard error. This helper will
     * ensure the root shell is initialised before execution.
     */
    suspend fun sh(script: String): ShellResult = withContext(Dispatchers.IO) {
        // Initialise the shell if not already. libsu handles prompting the
        // user for root only once.
        try {
            if (Shell.isAppGrantedRoot() != true) {
                Shell.getShell()
            }
        } catch (_: Exception) {
            // fallback: let exec handle root prompt
        }
        val result = Shell.cmd(script).exec()
        return@withContext ShellResult(
            exitCode = if (result.isSuccess) 0 else 1,
            stdout   = result.out.joinToString("\n"),
            stderr   = result.err.joinToString("\n"),
        )
    }

    /**
     * Execute multiple commands as a single script. Each element in
     * the argument list becomes its own line in the script.
     */
    suspend fun sh(vararg cmds: String): ShellResult =
        sh(cmds.joinToString("\n"))

    /**
     * Create the configuration directory if it does not already exist.
     */
    suspend fun ensureConfDir() = withContext(Dispatchers.IO) {
        Shell.cmd("mkdir -p $CONF_DIR").exec()
    }

    /**
     * Read the contents of a file as root. If the file does not exist
     * an empty string is returned.
     */
    suspend fun readFile(path: String): String =
        sh("cat $path 2>/dev/null").stdout

    /**
     * Write content to a file atomically. Content is escaped to ensure it
     * is written verbatim. Returns true when the write succeeds.
     */
    suspend fun writeFile(path: String, content: String): Boolean {
        val escaped = content.replace("'", "'\\''")
        return sh("printf '%s' '$escaped' > $path").exitCode == 0
    }

    /**
     * Test if a file exists. Returns true if the file is present.
     */
    suspend fun fileExists(path: String): Boolean =
        sh("[ -f $path ] && echo yes").stdout.contains("yes")

    /**
     * Query a system property via getprop. An empty string is returned
     * when the property does not exist.
     */
    suspend fun getProp(key: String): String = withContext(Dispatchers.IO) {
        Shell.cmd("getprop $key 2>/dev/null").exec().out.joinToString("").trim()
    }

    /**
     * Read the current active profile synchronously. If the profile file
     * does not exist or is empty the default "balance" profile is returned.
     */
    fun readProfileSync(): String =
        Shell.cmd("cat $PROFILE_FILE 2>/dev/null || echo balance")
            .exec().out.joinToString("").trim()
            .ifBlank { "balance" }

    /**
     * Remove the tweaks configuration file. Useful to reset all tweaks.
     */
    suspend fun resetTweaks() = withContext(Dispatchers.IO) {
        Shell.cmd("rm -f $TWEAKS_CONF").exec()
    }

    /**
     * Read device information such as model, Android version and chipset.
     * Additional fields like root type and thermal state are also included.
     */
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
            profile=$(cat ${'$'}{PROFILE_FILE} 2>/dev/null || echo balance)
            safe=$([ -f ${'$'}{SAFE_MODE_FILE} ] && echo 1 || echo 0)
            boot=$(cat ${'$'}{BOOT_COUNT_FILE} 2>/dev/null || echo 0)
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
        val platform = "${'$'}{map["platform"]} ${'$'}{map["hardware"]} ${'$'}{map["soc_model"]}".lowercase()

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

    /**
     * Parse the tweaks configuration file into a map of key → value.
     */
    suspend fun readTweaksConf(): Map<String, String> =
        parseKv(readFile(TWEAKS_CONF))

    /**
     * Update a single tweak entry in the configuration file. If the key
     * already exists its value is replaced, otherwise the key/value pair
     * is appended. Returns true on success.
     */
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

    /**
     * Write the profile file. Returns true on success.
     */
    suspend fun setProfile(profile: String): Boolean = writeFile(PROFILE_FILE, profile)

    /**
     * Apply a set of tweaks immediately using TweakApplier.writeAndApply.
     * Returns true when all subsystems apply successfully.
     */
    suspend fun applyTweaksDirect(tweaks: Map<String, String>): Boolean =
        TweakApplier.writeAndApply(TWEAKS_CONF, tweaks).success

    /**
     * Change profile and apply. The profile file is written and the
     * provided tweaks map is updated with the new profile. Returns true
     * when all subsystems apply successfully.
     */
    suspend fun setProfileDirect(profile: String): Boolean {
        writeFile(PROFILE_FILE, profile)
        val tweaks = readTweaksConf().toMutableMap()
        tweaks["profile"] = profile
        return TweakApplier.writeAndApply(TWEAKS_CONF, tweaks).success
    }

    /**
     * Toggle safe mode by creating or removing the safe mode file.
     */
    suspend fun toggleSafeMode(enable: Boolean): Boolean =
        if (enable) sh("touch $SAFE_MODE_FILE").exitCode == 0
        else        sh("rm -f $SAFE_MODE_FILE").exitCode == 0

    /**
     * Request a reboot. Depending on the mode the device will reboot to
     * normal system, recovery or bootloader. Returns true on success.
     */
    suspend fun reboot(mode: RebootMode = RebootMode.NORMAL): Boolean =
        sh(when (mode) {
            RebootMode.NORMAL     -> "reboot"
            RebootMode.RECOVERY   -> "reboot recovery"
            RebootMode.BOOTLOADER -> "reboot bootloader"
        }).exitCode == 0

    enum class RebootMode { NORMAL, RECOVERY, BOOTLOADER }

    /**
     * Gather monitor data such as CPU/GPU usage, memory state and
     * temperatures. The collected values are parsed into a MonitorState.
     * The implementation consolidates multiple vendor-specific paths so
     * it works across Snapdragon, MediaTek, Exynos and other chipsets.
     */
    suspend fun getMonitorState(): dev.aether.manager.data.MonitorState =
        withContext(Dispatchers.IO) {
            // Build two scripts: one for CPU/GPU/memory and one for storage/uptime.
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

                normalize_temp() {
                  _raw=$(echo "${'$'}1" | tr -cd '0-9')
                  [ -n "${'$'}_raw" ] || { echo 0; return; }
                  if [ "${'$'}_raw" -gt 100000 ] 2>/dev/null; then
                    echo 0
                  elif [ "${'$'}_raw" -gt 1000 ] 2>/dev/null; then
                    echo ${'$'}((_raw/1000))
                  elif [ "${'$'}_raw" -gt 200 ] 2>/dev/null; then
                    echo ${'$'}((_raw/10))
                  else
                    echo ${'$'}_raw
                  fi
                }

                read_temp_by_name() {
                  _keys="${'$'}1"
                  _best=0
                  for _zone in /sys/class/thermal/thermal_zone*; do
                    [ -d "${'$'}_zone" ] || continue
                    [ -r "${'$'}_zone/type" ] && [ -r "${'$'}_zone/temp" ] || continue
                    _name=$(cat "${'$'}_zone/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                    _raw=$(cat "${'$'}_zone/temp" 2>/dev/null)
                    _norm=$(normalize_temp "${'$'}_raw")
                    [ "${'$'}_norm" -ge 15 ] 2>/dev/null && [ "${'$'}_norm" -le 125 ] 2>/dev/null || continue
                    for _key in ${'$'}_keys; do
                      case "${'$'}_name" in
                        *"${'$'}_key"*) echo "${'$'}_raw"; return ;;
                      esac
                    done
                    [ "${'$'}_best" -eq 0 ] 2>/dev/null && _best=${'$'}_raw
                  done
                  echo ${'$'}_best
                }

                read_temp_any() {
                  _best=0
                  _best_norm=0
                  for _zone in /sys/class/thermal/thermal_zone*; do
                    [ -d "${'$'}_zone" ] || continue
                    [ -r "${'$'}_zone/temp" ] || continue
                    _name=$(cat "${'$'}_zone/type" 2>/dev/null | tr '[:upper:]' '[:lower:]')
                    case "${'$'}_name" in
                      *battery*|*batt*|*charger*|*usb*|*pa_therm*|*quiet_therm*) continue ;;
                    esac
                    _raw=$(cat "${'$'}_zone/temp" 2>/dev/null)
                    _norm=$(normalize_temp "${'$'}_raw")
                    [ "${'$'}_norm" -ge 15 ] 2>/dev/null && [ "${'$'}_norm" -le 125 ] 2>/dev/null || continue
                    if [ "${'$'}_norm" -gt "${'$'}_best_norm" ] 2>/dev/null; then
                      _best_norm=${'$'}_norm
                      _best=${'$'}_raw
                    fi
                  done
                  echo ${'$'}_best
                }

                cpu_temp=$(read_temp_by_name "cpu cpu-0 cpu0 cpu1 cpu2 cpu3 cpu4 cpu5 cpu6 cpu7 cpuss xcpu little big prime cluster tsens_tz_sensor apc0")
                [ "$(normalize_temp "${'$'}cpu_temp")" -le 0 ] 2>/dev/null && cpu_temp=$(read_temp_any)
                echo cpu_temp=${'$'}cpu_temp

                bat_temp=$(cat /sys/class/power_supply/battery/temp 2>/dev/null || cat /sys/class/power_supply/Battery/temp 2>/dev/null || echo 0)
                echo bat_temp=${'$'}bat_temp

                gpu_temp=$(read_temp_by_name "gpu gpuss gpu0 gpu-0 adreno mali g3d mfg ged")
                [ "$(normalize_temp "${'$'}gpu_temp")" -le 0 ] 2>/dev/null && gpu_temp=${'$'}cpu_temp
                echo gpu_temp=${'$'}gpu_temp

                thermal_temp=$(read_temp_by_name "soc xo skin shell board ambient pmic quiet case modem ap md pa therm thermal")
                [ "$(normalize_temp "${'$'}thermal_temp")" -le 0 ] 2>/dev/null && thermal_temp=${'$'}cpu_temp
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

                gpu_name=""
                # Adreno (Qualcomm/Snapdragon) — kgsl
                if [ -f /sys/class/kgsl/kgsl-3d0/gpu_model ]; then
                  gpu_name=$(cat /sys/class/kgsl/kgsl-3d0/gpu_model 2>/dev/null | tr -d '\n')
                elif [ -f /sys/class/kgsl/kgsl-3d0/gpu_tbl_name ]; then
                  gpu_name=$(cat /sys/class/kgsl/kgsl-3d0/gpu_tbl_name 2>/dev/null | tr -d '\n')
                elif [ -f /sys/class/kgsl/kgsl-3d0/device/name ]; then
                  gpu_name=$(cat /sys/class/kgsl/kgsl-3d0/device/name 2>/dev/null | tr -d '\n')
                fi
                # Mali (MediaTek/Exynos/Kirin) — mali0
                if [ -z "${'$'}gpu_name" ]; then
                  if [ -f /sys/class/misc/mali0/device/gpu_id ]; then
                    _id=$(cat /sys/class/misc/mali0/device/gpu_id 2>/dev/null | tr -cd '0-9')
                    [ -n "${'$'}_id" ] && gpu_name="Mali-G${'$'}_id"
                  elif [ -d /sys/class/misc/mali0 ]; then
                    gpu_name="Mali GPU"
                  fi
                fi
                # Exynos fallback
                if [ -z "${'$'}gpu_name" ] && [ -d /sys/class/misc/g3d ]; then
                  gpu_name="Mali GPU"
                fi
                # PowerVR (older Intel/Imagination)
                if [ -z "${'$'}gpu_name" ] && [ -d /sys/bus/platform/drivers/pvrsrvkm ]; then
                  gpu_name="PowerVR GPU"
                fi
                # Vulkan / OpenGL ES fallback via ro.hardware.vulkan
                if [ -z "${'$'}gpu_name" ]; then
                  _vk=$(getprop ro.hardware.vulkan 2>/dev/null)
                  case "${'$'}_vk" in
                    *adreno*) gpu_name="Adreno GPU" ;;
                    *mali*)   gpu_name="Mali GPU" ;;
                    *powervr*)gpu_name="PowerVR GPU" ;;
                  esac
                fi
                # ro.hardware fallback
                if [ -z "${'$'}gpu_name" ]; then
                  _hw=$(getprop ro.hardware 2>/dev/null)
                  case "${'$'}_hw" in
                    *adreno*) gpu_name="Adreno GPU" ;;
                    *mali*)   gpu_name="Mali GPU" ;;
                    *powervr*)gpu_name="PowerVR GPU" ;;
                  esac
                fi
                echo gpu_name=${'$'}gpu_name
            """.trimIndent()

            val storageScript = """
                df_out=$(df -k /data 2>/dev/null | tail -1)
                storage_total=$(echo "${'$'}df_out" | awk '{print ${'$'}2}')
                storage_used=$(echo "${'$'}df_out" | awk '{print ${'$'}3}')
                echo storage_total_kb=${'$'}storage_total
                echo storage_used_kb=${'$'}storage_used
                # Use kernel's uptime. The first field is seconds since boot.
                uptime_seconds=${'$'}(cut -d. -f1 /proc/uptime 2>/dev/null || echo 0)
                days=${'$'}((uptime_seconds/86400)); hours=${'$'}(((uptime_seconds%86400)/3600)); mins=${'$'}(((uptime_seconds%3600)/60))
                echo uptime=${'$'}(printf "%dd_%dh_%dm" ${'$'}days ${'$'}hours ${'$'}mins)
            """.trimIndent()

            // Run two scripts concurrently and combine the output lines
            val r1 = sh(cpuScript + "\n" + statScript)
            val r2 = sh(storageScript)
            val map = parseKv(r1.stdout + "\n" + r2.stdout)

            val cpuTempC = run {
                val raw = map["cpu_temp"]?.toLongOrNull() ?: 0L
                when {
                    raw > 100_000L -> 0f
                    raw > 1_000L   -> raw / 1000f
                    raw > 200L     -> raw / 10f
                    else           -> raw.toFloat()
                }
            }

            val batTempC = run {
                val raw = map["bat_temp"]?.toLongOrNull() ?: 0L
                when {
                    raw > 100_000L -> 0f
                    raw > 1_000L   -> raw / 1000f
                    raw > 200L     -> raw / 10f
                    else           -> raw.toFloat()
                }
            }

            val gpuTempC = run {
                val raw = map["gpu_temp"]?.toLongOrNull() ?: 0L
                when {
                    raw > 100_000L -> 0f
                    raw > 1_000L   -> raw / 1000f
                    raw > 200L     -> raw / 10f
                    else           -> raw.toFloat()
                }
            }

            val thermalTempC = run {
                val raw = map["thermal_temp"]?.toLongOrNull() ?: 0L
                fun validTemp(x: Float) = x >= 15 && x <= 125
                val shellThermalTemp = when {
                    raw > 100_000L -> 0f
                    raw > 1_000L   -> raw / 1000f
                    raw > 200L     -> raw / 10f
                    else           -> raw.toFloat()
                }
                shellThermalTemp.takeIf(::validTemp)
                    ?: cpuTempC.takeIf(::validTemp)
                    ?: gpuTempC.takeIf(::validTemp)
                    ?: batTempC.takeIf(::validTemp)
                    ?: 0f
            }

            dev.aether.manager.data.MonitorState(
                cpuUsage       = map["cpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                cpuFreq        = map["cpu_freq"]?.toLongOrNull()?.takeIf { it > 0 }?.let { "$it MHz" } ?: "",
                gpuUsage       = map["gpu_usage"]?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                gpuFreq        = map["gpu_freq"]?.toLongOrNull()?.takeIf { it > 0 }?.let { "$it MHz" } ?: "",
                gpuName        = map["gpu_name"]?.takeIf { it.isNotBlank() } ?: "",
                ramUsedMb      = map["ram_used_mb"]?.toLongOrNull()  ?: 0L,
                ramTotalMb     = map["ram_total_mb"]?.toLongOrNull() ?: 0L,
                cpuTemp        = cpuTempC,
                gpuTemp        = gpuTempC,
                thermalTemp    = thermalTempC,
                batTemp        = batTempC,
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
                    else if (ua > 30_000L || ua < -30_000L) ua / 1000L
                    else ua
                },
                batVoltage     = run {
                    val uv = map["bat_uv"]?.toLongOrNull() ?: 0L
                    if (uv > 1_000_000L) uv / 1000L else uv
                },
                batStatus      = map["bat_status"] ?: "Unknown",
            )
        }

    /**
     * Convert a key=value string into a map. Lines without '=' are ignored.
     */
    private fun parseKv(raw: String): Map<String, String> =
        raw.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i > 0) line.substring(0, i).trim() to line.substring(i + 1).trim()
            else null
        }.toMap()
}

/**
 * Immutable data describing the device. This mirrors the original
 * DeviceInfo defined in RootUtils so the UI and ViewModel code do
 * not need any changes. The soc enum indicates the chipset type.
 */
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

/**
 * Enumeration of supported System-on-Chip (SoC) families. Additional
 * families can be added as needed.
 */
enum class SocType(val label: String) {
    SNAPDRAGON("Snapdragon"),
    MEDIATEK  ("MediaTek"),
    EXYNOS    ("Exynos"),
    KIRIN     ("Kirin"),
    OTHER     ("Universal")
}