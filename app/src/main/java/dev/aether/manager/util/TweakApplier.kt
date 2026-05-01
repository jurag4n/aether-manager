package dev.aether.manager.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TweakApplier — engine apply tweak bergaya encore_profiler.
 *
 * Filosofi (persis encore):
 *   apply(val, node) = chmod 644 → echo val → chmod 444   (sysfs 444-protected)
 *   write(val, node)  = chmod 644 → echo val              (procfs / dynamic nodes)
 *
 * Satu Shell.cmd() batch per subsistem, tidak ada file tmp, tidak ada round-trip.
 * Semua subshell $(...) dalam heredoc sudah di-escape dengan benar.
 */
object TweakApplier {

    // ─────────────────────────────────────────────────────────────────────────
    // Result model
    // ─────────────────────────────────────────────────────────────────────────

    data class SubsystemResult(
        val name:    String,
        val ok:      Boolean,
        val applied: Int    = 0,
        val skipped: Int    = 0,
        val failed:  Int    = 0,
        val note:    String = "",
    )

    data class ApplyResult(
        val subsystems: List<SubsystemResult>,
        val totalMs:    Long,
    ) {
        val success: Boolean get() = subsystems.all { it.ok }
        val summary: String  get() {
            val ok  = subsystems.count { it.ok }
            val all = subsystems.size
            return "$ok/$all subsystems OK · ${totalMs}ms"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shell helper — jalankan script langsung via Shell.cmd() tanpa file tmp
    // Tidak perlu /data/local/tmp lagi karena kita pakai Shell interactive root
    // ─────────────────────────────────────────────────────────────────────────

    private fun runScript(script: String): String {
        // Pastikan shell interactive sudah siap
        if (Shell.isAppGrantedRoot() != true) {
             // Jika belum granted di session ini, coba init
             Shell.getShell()
        }
        val result = Shell.cmd(script).exec()
        return result.out.joinToString("\n")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun apply(tweaks: Map<String, String>): ApplyResult = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val output = runScript(buildFullScript(tweaks))
        ApplyResult(
            subsystems = parseResults(output),
            totalMs    = System.currentTimeMillis() - t0,
        )
    }

    suspend fun writeAndApply(
        confPath: String,
        tweaks:   Map<String, String>,
    ): ApplyResult = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()

        // Tulis conf + apply dalam SATU Shell batch
        val dir = confPath.substringBeforeLast('/')
        val confLines = tweaks.entries.joinToString("\n") { (k, v) -> "$k=$v" }
        val writeConf = buildString {
            appendLine("mkdir -p $dir")
            appendLine("cat > \$confPath << 'AEOF'")
            appendLine(confLines)
            appendLine("AEOF")
        }

        val output = runScript(writeConf + "\n" + buildFullScript(tweaks))
        ApplyResult(
            subsystems = parseResults(output),
            totalMs    = System.currentTimeMillis() - t0,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Script builder — susun semua subsistem
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildFullScript(t: Map<String, String>): String {
        val profile = t["profile"] ?: "balance"
        return buildString {
            append(SHELL_HELPERS)
            append(buildCpuGovernor(t, profile))
            append(buildCpuFreq(t, profile))
            append(buildCpuBoost(t, profile))
            // We intentionally omit MTK boost, cpuset, ksm, touch, UI animation, misc
            append(buildSched(t, profile))
            append(buildThermal(t))
            append(buildGpu(t, profile))
            append(buildGpuFreq(t))
            append(buildMemory(t))
            append(buildIo(t))
            append(buildNetwork(t))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHELL HELPERS — persis encore_profiler
    //
    // apply val node  = chmod 644 → echo → chmod 444   (sysfs protected)
    // write val node  = chmod 644 → echo               (procfs/dynamic)
    // forall val glob = write ke semua node yang match
    // gov name        = change_cpu_gov — tee ke semua policy*/cpu*
    //
    // Semua menggunakan single-quote heredoc agar $ tidak diekspansi Kotlin.
    // ─────────────────────────────────────────────────────────────────────────

    private val SHELL_HELPERS = """
_A=0; _S=0; _F=0

# apply val node — encore style: chmod 644 → echo → chmod 444
apply() {
  local val="${'$'}1" node="${'$'}2"
  [ -f "${'$'}node" ] || { _S=${'$'}((_S+1)); return 1; }
  chmod 644 "${'$'}node" 2>/dev/null
  if echo "${'$'}val" > "${'$'}node" 2>/dev/null; then
    _A=${'$'}((_A+1))
    chmod 444 "${'$'}node" 2>/dev/null
  else
    _F=${'$'}((_F+1))
    chmod 444 "${'$'}node" 2>/dev/null
  fi
}

# write val node — encore style: chmod 644 → echo (tanpa restore)
write() {
  local val="${'$'}1" node="${'$'}2"
  [ -f "${'$'}node" ] || { _S=${'$'}((_S+1)); return 1; }
  chmod 644 "${'$'}node" 2>/dev/null
  if echo "${'$'}val" > "${'$'}node" 2>/dev/null; then
    _A=${'$'}((_A+1))
  else
    _F=${'$'}((_F+1))
  fi
}

# forall val glob — write ke semua node yang match glob
forall() {
  local val="${'$'}1"; shift
  for node in ${'$'}@; do
    [ -f "${'$'}node" ] && write "${'$'}val" "${'$'}node"
  done
}

# change_cpu_gov — persis encore change_cpu_gov()
change_cpu_gov() {
  local gov="${'$'}1"
  chmod 644 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor 2>/dev/null
  chmod 644 /sys/devices/system/cpu/cpufreq/policy*/scaling_governor 2>/dev/null
  chown 0:0 /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor 2>/dev/null
  chown 0:0 /sys/devices/system/cpu/cpufreq/policy*/scaling_governor 2>/dev/null
  echo "${'$'}gov" | tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor >/dev/null 2>&1
  echo "${'$'}gov" | tee /sys/devices/system/cpu/cpufreq/policy*/scaling_governor >/dev/null 2>&1
  _A=${'$'}((_A+1))
}

# devfreq helpers — encore style
devfreq_max() {
  local path="${'$'}1"
  [ -f "${'$'}path/available_frequencies" ] || return
  local maxf; maxf=${'$'}(tr ' ' '\n' < "${'$'}path/available_frequencies" | sort -n | tail -1)
  apply "${'$'}maxf" "${'$'}path/max_freq"
  apply "${'$'}maxf" "${'$'}path/min_freq"
}

devfreq_unlock() {
  local path="${'$'}1"
  [ -f "${'$'}path/available_frequencies" ] || return
  local maxf minf
  maxf=${'$'}(tr ' ' '\n' < "${'$'}path/available_frequencies" | sort -n | tail -1)
  minf=${'$'}(tr ' ' '\n' < "${'$'}path/available_frequencies" | sort -n | head -1)
  write "${'$'}maxf" "${'$'}path/max_freq"
  write "${'$'}minf" "${'$'}path/min_freq"
}

# marker — diparse Kotlin untuk SubsystemResult
_begin() { _A=0; _S=0; _F=0; }
_end()   {
  if [ ${'$'}_F -eq 0 ]; then _st=ok; else _st=fail; fi
  echo "RESULT:${'$'}1:${'$'}_st:a=${'$'}_A:s=${'$'}_S:f=${'$'}_F"
}

""".trimIndent() + "\n"

    // ─────────────────────────────────────────────────────────────────────────
    // CPU Governor
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildCpuGovernor(t: Map<String, String>, profile: String): String {
        val target = when (profile) {
            "performance" -> "performance"
            "battery"     -> "powersave"
            "gaming"      -> t["cpu_governor"]?.takeIf { it.isNotBlank() } ?: "performance"
            else          -> t["cpu_governor"]?.takeIf { it.isNotBlank() } ?: "schedutil"
        }
        // Gunakan single-quote agar $ shell tidak diekspansi Kotlin
        return """
_begin
# cpu_governor — target: $target
AVAIL=${'$'}(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>/dev/null || echo "")
if [ -n "${'$'}AVAIL" ]; then
  _GOV=$target
  _OK=0
  for g in ${'$'}AVAIL; do
    [ "${'$'}g" = "${'$'}_GOV" ] && { _OK=1; break; }
  done
  if [ ${'$'}_OK -eq 0 ]; then
    for fb in schedutil interactive ondemand performance; do
      for g in ${'$'}AVAIL; do
        [ "${'$'}g" = "${'$'}fb" ] && { _GOV=${'$'}fb; _OK=1; break 2; }
      done
    done
  fi
  change_cpu_gov "${'$'}_GOV"
fi
_end "cpu_governor"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CPU Frequency
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildCpuFreq(t: Map<String, String>, profile: String): String {
        return when {
            // Lock CPU frequencies to their maximum when cpu_freq_lock is enabled
            t["cpu_freq_lock"] == "1" -> """
_begin
# cpu_freq lock — force max freq on all policies
for pol in /sys/devices/system/cpu/cpufreq/policy*/; do
  [ -d "${'$'}pol" ] || continue
  maxf=${'$'}(cat "${'$'}{pol}cpuinfo_max_freq" 2>/dev/null || echo 0)
  echo "${'$'}maxf" | grep -qE '^[0-9]+' || continue
  # apply both min and max to lock the freq
  apply "${'$'}maxf" "${'$'}{pol}scaling_max_freq"
  apply "${'$'}maxf" "${'$'}{pol}scaling_min_freq"
done
_end "cpu_freq"
"""
            profile == "gaming" || profile == "performance" -> """
_begin
# cpu_freq — max perf (safe: write only, no chmod 444 lock)
for pol in /sys/devices/system/cpu/cpufreq/policy*/; do
  [ -d "${'$'}pol" ] || continue
  maxf=${'$'}(cat "${'$'}{pol}cpuinfo_max_freq" 2>/dev/null)
  echo "${'$'}maxf" | grep -qE '^[0-9]+$' || continue
  # Tulis max dulu, baru min — urutan penting agar tidak invalid range
  write "${'$'}maxf" "${'$'}{pol}scaling_max_freq"
  write "${'$'}maxf" "${'$'}{pol}scaling_min_freq"
done
# TIDAK chmod 444 — mengunci freq nodes secara paksa dapat menyebabkan
# kernel hang saat thermal atau scheduler mencoba mengubah nilai tersebut
_end "cpu_freq"
"""
            profile == "battery" -> """
_begin
# cpu_freq — battery: cap max 60% (encore cpufreq_unlock style)
for pol in /sys/devices/system/cpu/cpufreq/policy*/; do
  [ -d "${'$'}pol" ] || continue
  maxf=${'$'}(cat "${'$'}{pol}cpuinfo_max_freq" 2>/dev/null)
  minf=${'$'}(cat "${'$'}{pol}cpuinfo_min_freq" 2>/dev/null)
  echo "${'$'}maxf" | grep -qE '^[0-9]+$' || continue
  echo "${'$'}minf" | grep -qE '^[0-9]+$' || continue
  apply "${'$'}(( maxf * 60 / 100 ))" "${'$'}{pol}scaling_max_freq"
  write "${'$'}minf" "${'$'}{pol}scaling_min_freq"
done
_end "cpu_freq"
"""
            t["cpu_freq_enable"] == "1" -> {
                val pMin  = t["cpu_freq_prime_min"]  ?: ""
                val pMax  = t["cpu_freq_prime_max"]  ?: ""
                val gMin  = t["cpu_freq_gold_min"]   ?: ""
                val gMax  = t["cpu_freq_gold_max"]   ?: ""
                val sMin  = t["cpu_freq_silver_min"] ?: ""
                val sMax  = t["cpu_freq_silver_max"] ?: ""
                """
_begin
# cpu_freq limiter — tri-cluster via cpu_capacity
for pol in /sys/devices/system/cpu/cpufreq/policy*/; do
  [ -d "${'$'}pol" ] || continue
  max_cap=0
  for c in ${'$'}(cat "${'$'}{pol}related_cpus" 2>/dev/null); do
    cap=${'$'}(cat "/sys/devices/system/cpu/cpu${'$'}{c}/cpu_capacity" 2>/dev/null || echo 0)
    [ "${'$'}cap" -gt "${'$'}max_cap" ] 2>/dev/null && max_cap=${'$'}cap
  done
  if [ "${'$'}max_cap" -eq 0 ] 2>/dev/null; then
    # cpu_capacity tidak tersedia — fallback: pilih cluster berdasar urutan policy
    # policy0=little, policy4=gold, policy7=prime (heuristic umum)
    pol_num=${'$'}(basename "${'$'}pol" | grep -oE '[0-9]+' | head -1)
    if [ "${'$'}pol_num" -ge 7 ] 2>/dev/null; then
      cmin="$pMin"; cmax="$pMax"
    elif [ "${'$'}pol_num" -ge 4 ] 2>/dev/null; then
      cmin="$gMin"; cmax="$gMax"
    else
      cmin="$sMin"; cmax="$sMax"
    fi
  elif [ "${'$'}max_cap" -gt 900 ] 2>/dev/null; then
    cmin="$pMin"; cmax="$pMax"
  elif [ "${'$'}max_cap" -gt 700 ] 2>/dev/null; then
    cmin="$gMin"; cmax="$gMax"
  else
    cmin="$sMin"; cmax="$sMax"
  fi
  cpu_min=${'$'}(cat "${'$'}{pol}cpuinfo_min_freq" 2>/dev/null || echo 0)
  cpu_max=${'$'}(cat "${'$'}{pol}cpuinfo_max_freq" 2>/dev/null || echo 9999999)
  if echo "${'$'}cmin" | grep -qE '^[0-9]+$' && [ "${'$'}cmin" -ge "${'$'}cpu_min" ] 2>/dev/null; then
    apply "${'$'}cmin" "${'$'}{pol}scaling_min_freq"
  else
    [ -n "${'$'}cmin" ] && _F=${'$'}((_F+1)) || _S=${'$'}((_S+1))
  fi
  if echo "${'$'}cmax" | grep -qE '^[0-9]+$' && [ "${'$'}cmax" -le "${'$'}cpu_max" ] 2>/dev/null; then
    apply "${'$'}cmax" "${'$'}{pol}scaling_max_freq"
  else
    [ -n "${'$'}cmax" ] && _F=${'$'}((_F+1)) || _S=${'$'}((_S+1))
  fi
done
_end "cpu_freq"
"""
            }
            else -> """
_begin
# cpu_freq — unlock ke cpuinfo limits (encore cpufreq_unlock)
for pol in /sys/devices/system/cpu/cpufreq/policy*/; do
  [ -d "${'$'}pol" ] || continue
  maxf=${'$'}(cat "${'$'}{pol}cpuinfo_max_freq" 2>/dev/null)
  minf=${'$'}(cat "${'$'}{pol}cpuinfo_min_freq" 2>/dev/null)
  echo "${'$'}maxf" | grep -qE '^[0-9]+$' || continue
  echo "${'$'}minf" | grep -qE '^[0-9]+$' || continue
  write "${'$'}maxf" "${'$'}{pol}scaling_max_freq"
  write "${'$'}minf" "${'$'}{pol}scaling_min_freq"
done
chmod -f 644 /sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq 2>/dev/null
_end "cpu_freq"
"""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CPU Boost
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildCpuBoost(t: Map<String, String>, profile: String): String {
        val on = t["cpu_boost"] == "1" || profile == "gaming" || profile == "performance"
        return if (on) """
_begin
# cpu_boost — enable
write 1 /sys/module/cpu_boost/parameters/input_boost_enabled
write 64 /sys/module/cpu_boost/parameters/input_boost_ms
write 1 /sys/module/msm_performance/parameters/touchboost
# Konstruksi "cpu:freq cpu:freq ..." lalu write sekali (atomic, tidak append)
node=/sys/module/cpu_boost/parameters/input_boost_freq
if [ -f "${'$'}node" ]; then
  _boost_str=""
  for pol in /sys/devices/system/cpu/cpufreq/policy*/; do
    [ -d "${'$'}pol" ] || continue
    maxf=${'$'}(cat "${'$'}{pol}cpuinfo_max_freq" 2>/dev/null || echo 0)
    boostf=${'$'}(( maxf * 80 / 100 ))
    [ "${'$'}boostf" -gt 0 ] 2>/dev/null || continue
    for c in ${'$'}(cat "${'$'}{pol}related_cpus" 2>/dev/null); do
      _boost_str="${'$'}_boost_str ${'$'}c:${'$'}boostf"
    done
  done
  [ -n "${'$'}_boost_str" ] && write "${'$'}(echo ${'$'}_boost_str | xargs)" "${'$'}node"
fi
_end "cpu_boost"
""" else """
_begin
# cpu_boost — disable
write 0 /sys/module/cpu_boost/parameters/input_boost_enabled
write 0 /sys/module/msm_performance/parameters/touchboost
_end "cpu_boost"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MTK EAS / HPS Boost
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildMtkBoost(t: Map<String, String>): String {
        if (t["obb_noop"] != "1") return """
_begin
# mtk_boost — skipped
_S=${'$'}((_S+1))
_end "mtk_boost"
"""
        return """
_begin
# mtk_boost — MTK EAS / HPS
# EAS: prefer big cluster
write 1   /proc/cpufreq/cpufreq_eas_mode 2>/dev/null
# HPS: hotplug boost
write 1   /proc/hps/enabled 2>/dev/null
write 4   /proc/hps/up_threshold 2>/dev/null
write 1   /proc/hps/down_threshold 2>/dev/null
write 8   /proc/hps/rush_boost_enabled 2>/dev/null
# CCI boost (MediaTek interconnect)
write 1   /proc/cci_pmu/enable 2>/dev/null
write max /proc/cpufreq/cpufreq_power_mode 2>/dev/null
# MTK GPU DVFS boost (ged)
write 0   /sys/kernel/ged/hal/dvfs_loading_mode 2>/dev/null
_end "mtk_boost"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cpuset
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildCpuset(t: Map<String, String>): String {
        if (t["cpuset_opt"] != "1") return """
_begin
# cpuset — skipped
_S=${'$'}((_S+1))
_end "cpuset"
"""
        return """
_begin
# cpuset — optimise berdasarkan nproc
ncpu=${'$'}(nproc 2>/dev/null || echo 8)
last=${'$'}(( ncpu - 1 ))
perf=${'$'}(( ncpu * 3 / 4 ))
[ -d /dev/cpuset/top-app ]           && write "${'$'}{perf}-${'$'}{last}" /dev/cpuset/top-app/cpus
[ -d /dev/cpuset/foreground ]        && write "0-${'$'}{last}"       /dev/cpuset/foreground/cpus
[ -d /dev/cpuset/background ]        && write "0-1"             /dev/cpuset/background/cpus
[ -d /dev/cpuset/system-background ] && write "0-3"             /dev/cpuset/system-background/cpus
[ -d /dev/cpuset/restricted ]        && write "0-3"             /dev/cpuset/restricted/cpus
_end "cpuset"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduler
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildSched(t: Map<String, String>, profile: String): String {
        val boost = if (t["schedboost"] == "1" || profile == "gaming" || profile == "performance") "1" else "0"
        val (upmig, downmig) = when (profile) {
            "gaming", "performance" -> "85" to "75"
            "battery"               -> "99" to "95"
            else                    -> "95" to "85"
        }
        return """
_begin
# sched
if [ -f /proc/sys/kernel/sched_boost ]; then
  write $boost /proc/sys/kernel/sched_boost
fi
write $upmig /proc/sys/kernel/sched_upmigrate
write $downmig /proc/sys/kernel/sched_downmigrate
write $upmig /proc/sys/kernel/sched_group_upmigrate
write $downmig /proc/sys/kernel/sched_group_downmigrate
_end "sched"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Thermal
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildThermal(t: Map<String, String>): String {
        return when (t["thermal_profile"] ?: "default") {
            "performance" -> """
_begin
# thermal — performance: raise limits
for tz in /sys/class/thermal/thermal_zone*/mode; do
  apply enabled "${'$'}tz"
done
write 85 /sys/module/msm_thermal/parameters/temp_threshold
write 90 /sys/module/msm_thermal/parameters/core_limit_temp_degC
write Y  /sys/module/msm_thermal/parameters/enabled
write 1  /sys/module/msm_thermal/core_control/enabled
write 2  /proc/mtk_cl_objthermal/thermal_policy
_end "thermal"
"""
            "extreme" -> """
_begin
# thermal — extreme: raise limits only (TIDAK disable total — disable thermal
# dapat menyebabkan chip overheat → kernel panic → reboot paksa)
write 90 /sys/module/msm_thermal/parameters/temp_threshold
write 95 /sys/module/msm_thermal/parameters/core_limit_temp_degC
write Y  /sys/module/msm_thermal/parameters/enabled
write 1  /sys/module/msm_thermal/core_control/enabled
write 3  /proc/mtk_cl_objthermal/thermal_policy
# MTK: raise trip point tapi tidak disable
for tz in /sys/class/thermal/thermal_zone*/trip_point_0_temp; do
  [ -f "${'$'}tz" ] || continue
  cur=${'$'}(cat "${'$'}tz" 2>/dev/null)
  [ "${'$'}cur" -lt 90000 ] 2>/dev/null && write 90000 "${'$'}tz"
done
_end "thermal"
"""
            else -> """
_begin
# thermal — default: restore
for tz in /sys/class/thermal/thermal_zone*/mode; do
  apply enabled "${'$'}tz"
done
write Y  /sys/module/msm_thermal/parameters/enabled
write 1  /sys/module/msm_thermal/core_control/enabled
write 60 /sys/module/msm_thermal/parameters/temp_threshold
write 0  /proc/mtk_cl_objthermal/thermal_policy
_end "thermal"
"""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GPU
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildGpu(t: Map<String, String>, profile: String): String {
        val perf = t["gpu_throttle_off"] == "1" || profile == "gaming" || profile == "performance"
        return if (perf) """
_begin
# gpu — performance (safe: tanpa force_clk_on/idle_timer paksa)
if [ -d /sys/class/kgsl/kgsl-3d0 ]; then
  apply 0 /sys/class/kgsl/kgsl-3d0/throttling
  # force_clk_on dan idle_timer=1000 dihapus: menyebabkan GPU driver hang
  # saat app keluar dan driver mencoba power-gate GPU
  apply 0 /sys/class/kgsl/kgsl-3d0/bus_split
  # Qcom devfreq GPU
  devfreq_max /sys/class/devfreq/kgsl-3d0
fi
write 0 /sys/kernel/ged/hal/dvfs_enable
apply always_on /sys/class/misc/mali0/device/power_policy
apply on        /sys/devices/platform/mali.0/power/control
apply 10        /sys/class/misc/mali0/device/js_scheduling_period
_end "gpu"
""" else """
_begin
# gpu — restore throttle / auto DVFS
apply 1 /sys/class/kgsl/kgsl-3d0/throttling
apply 0 /sys/class/kgsl/kgsl-3d0/force_clk_on
apply 0 /sys/class/kgsl/kgsl-3d0/bus_split
write 1 /sys/kernel/ged/hal/dvfs_enable
apply coarse_demand /sys/class/misc/mali0/device/power_policy
apply auto          /sys/devices/platform/mali.0/power/control
# Qcom devfreq GPU
devfreq_unlock /sys/class/devfreq/kgsl-3d0
_end "gpu"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GPU Freq Lock
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildGpuFreq(t: Map<String, String>): String {
        val freq = t["gpu_freq_max"]?.takeIf { it.isNotBlank() && t["gpu_freq_lock"] == "1" }
        return if (freq != null) """
_begin
# gpu_freq — lock $freq Hz
apply $freq /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
apply $freq /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
write $freq /proc/gpufreq/gpufreq_opp_freq
apply $freq /sys/class/misc/mali0/device/dvfs_max_lock
apply $freq /sys/class/misc/mali0/device/dvfs_min_lock
_end "gpu_freq"
""" else """
_begin
# gpu_freq — unlock
devfreq_unlock /sys/class/devfreq/kgsl-3d0
apply 0 /sys/class/misc/mali0/device/dvfs_max_lock
apply 0 /sys/class/misc/mali0/device/dvfs_min_lock
_end "gpu_freq"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Memory
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildMemory(t: Map<String, String>): String = buildString {
        append("_begin\n# memory\n")

        // swap tuning — adjust swappiness and cache pressure when swap enabled
        if (t["swap"] == "1") {
            append("write 100 /proc/sys/vm/swappiness\n")
            append("write 200 /proc/sys/vm/vfs_cache_pressure\n")
        }

        if (t["lmk_aggressive"] == "1") {
            append("write 18432,23040,27648,32256,55296,80640 /sys/module/lowmemorykiller/parameters/minfree\n")
            append("write 0,100,200,300,900,906 /sys/module/lowmemorykiller/parameters/adj\n")
            append("setprop ro.lmk.low 1001 2>/dev/null; _A=\$((_A+1))\n")
            append("setprop ro.lmk.medium 800 2>/dev/null; _A=\$((_A+1))\n")
            append("setprop ro.lmk.critical 0 2>/dev/null; _A=\$((_A+1))\n")
        }

        if (t["zram"] == "1") {
            val size = t["zram_size"] ?: "1073741824"
            val algo = t["zram_algo"] ?: "lz4"
            append("""
# zram setup — TIDAK pakai write() helper karena [ -f ] gagal pada sysfs special nodes
# Urutan wajib: swapoff → reset (disksize=0) → comp_algorithm → disksize → mkswap → swapon
# Perbaikan: Tambahkan pengecekan error di setiap langkah untuk mencegah bootloop
swapoff /dev/block/zram0 2>/dev/null || true
write 0 /sys/block/zram0/disksize 2>/dev/null || true
write $algo /sys/block/zram0/comp_algorithm 2>/dev/null || true
write $size /sys/block/zram0/disksize 2>/dev/null || true
mkswap /dev/block/zram0 2>/dev/null || true
swapon /dev/block/zram0 2>/dev/null || true
_zram_ok=0
for _zdev in /dev/zram0 /dev/zram1; do
  [ -b "${'$'}_zdev" ] || continue
  _zblk=${'$'}(basename "${'$'}_zdev")
  _zsys="/sys/block/${'$'}_zblk"
  [ -d "${'$'}_zsys" ] || continue

  # Step 1: swapoff — paksa meski gagal (lanjut ke reset)
  swapoff "${'$'}_zdev" 2>/dev/null || true

  # Step 2: reset — cara kernel modern: tulis 0 ke disksize dulu
  # Beberapa kernel pakai /reset, beberapa pakai disksize=0, support keduanya
  if [ -f "${'$'}_zsys/reset" ]; then
    chmod 644 "${'$'}_zsys/reset" 2>/dev/null
    echo 1 > "${'$'}_zsys/reset" 2>/dev/null || true
  else
    chmod 644 "${'$'}_zsys/disksize" 2>/dev/null
    echo 0 > "${'$'}_zsys/disksize" 2>/dev/null || true
  fi
  # Beri waktu kernel selesaikan reset
  sleep 0.1 2>/dev/null || true

  # Step 3: set comp_algorithm — cek dulu apakah algo tersedia
  if [ -f "${'$'}_zsys/comp_algorithm" ]; then
    chmod 644 "${'$'}_zsys/comp_algorithm" 2>/dev/null
    if grep -qw "$algo" "${'$'}_zsys/comp_algorithm" 2>/dev/null; then
      echo "$algo" > "${'$'}_zsys/comp_algorithm" 2>/dev/null || true
    else
      # Fallback algo: lz4 → lzo → deflate
      for _fb in lz4 lzo deflate; do
        grep -qw "${'$'}_fb" "${'$'}_zsys/comp_algorithm" 2>/dev/null && {
          echo "${'$'}_fb" > "${'$'}_zsys/comp_algorithm" 2>/dev/null || true
          break
        }
      done
    fi
  fi

  # Step 4: set disksize
  chmod 644 "${'$'}_zsys/disksize" 2>/dev/null
  echo $size > "${'$'}_zsys/disksize" 2>/dev/null || { _F=${'$'}((_F+1)); continue; }

  # Step 5: mkswap + swapon
  if mkswap "${'$'}_zdev" >/dev/null 2>&1 && swapon -p 5 "${'$'}_zdev" 2>/dev/null; then
    _A=${'$'}((_A+1))
    _zram_ok=1
    break  # sukses, tidak perlu coba zram1
  else
    _F=${'$'}((_F+1))
  fi
done
[ "${'$'}_zram_ok" = "0" ] && [ "${'$'}_F" = "0" ] && _S=${'$'}((_S+1))
""")
        }

        // kill background — drop caches and trim app caches
        if (t["kill_background"] == "1") {
            append("sync && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null && _A=\$((_A+1)) || _F=\$((_F+1))\n")
            append("pm trim-caches 0 2>/dev/null && _A=\$((_A+1)) || _S=\$((_S+1))\n")
        }

        append("_end \"memory\"\n")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // I/O
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildIo(t: Map<String, String>): String = buildString {
        append("_begin\n# io\n")

        val sched = t["io_scheduler"]?.takeIf { it.isNotBlank() }
        if (sched != null) {
            append("""
for dev in /sys/block/*/queue/scheduler; do
  [ -f "${'$'}dev" ] || continue
  if grep -q "$sched" "${'$'}dev" 2>/dev/null; then
    write "$sched" "${'$'}dev"
  else
    for fb in none mq-deadline deadline cfq; do
      grep -q "${'$'}fb" "${'$'}dev" 2>/dev/null && { write "${'$'}fb" "${'$'}dev"; break; }
    done
  fi
done
""")
        }

        if (t["io_latency_opt"] == "1") {
            append("""
# block queue tuning (encore style)
for q in /sys/block/*/queue/; do
  [ -d "${'$'}q" ] || continue
  write 512 "${'$'}{q}read_ahead_kb"
  write 0   "${'$'}{q}add_random"
  write 2   "${'$'}{q}rq_affinity"
  write 0   "${'$'}{q}nomerges"
  write 256 "${'$'}{q}nr_requests"
  write 0   "${'$'}{q}iostats"
done
""")
        }

        append("_end \"io\"\n")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Network
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildNetwork(t: Map<String, String>): String {
        // Determine provider from dns_provider or legacy keys
        val provider = when {
            t["dns_provider"]?.isNotBlank() == true -> t["dns_provider"]!!
            t["doh"] == "1" -> "Cloudflare"
            else -> ""
        }
        val dnsHost = if (provider.isNotEmpty() && provider != "Off") {
            when (provider.lowercase()) {
                "cloudflare"      -> "one.one.one.one"
                "google"          -> "dns.google"
                "quad9"           -> "dns.quad9.net"
                "cleanbrowsing"   -> "family-filter-dns.cleanbrowsing.org"
                "control d", "controld" -> "p2.freedns.controld.com"
                "nextdns"         -> "dns.nextdns.io"
                else               -> provider
            }
        } else null
        return buildString {
            append("_begin\n# network\n")
            // TCP congestion control (BBR/bbr2) and fair queueing
            if (t["tcp_bbr"] == "1") {
                append("""
for algo in bbr bbr2 westwood cubic; do
  if grep -q "${'$'}algo" /proc/sys/net/ipv4/tcp_available_congestion_control 2>/dev/null; then
    write "${'$'}algo" /proc/sys/net/ipv4/tcp_congestion_control
    break
  fi
done
write fq /proc/sys/net/core/default_qdisc
""")
            }
            // Network stabiliser: tune socket buffers, enable low latency & scaling
            if (t["network_stable"] == "1") {
                append("printf '4096 87380 16777216' > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null && _A=\$((_A+1)) || _F=\$((_F+1))\n")
                append("printf '4096 65536 16777216' > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null && _A=\$((_A+1)) || _F=\$((_F+1))\n")
                append("write 16777216 /proc/sys/net/core/rmem_max\n")
                append("write 16777216 /proc/sys/net/core/wmem_max\n")
                append("write 3 /proc/sys/net/ipv4/tcp_fastopen\n")
                append("write 1 /proc/sys/net/ipv4/tcp_window_scaling\n")
                append("write 1 /proc/sys/net/ipv4/tcp_low_latency\n")
                append("write 1 /proc/sys/net/ipv4/tcp_sack\n")
            }
            // Private DNS provider
            if (dnsHost != null) {
                append("settings put global private_dns_mode hostname 2>/dev/null && _A=\$((_A+1))\n")
                append("settings put global private_dns_specifier $dnsHost 2>/dev/null && _A=\$((_A+1))\n")
            } else {
                append("settings put global private_dns_mode off 2>/dev/null; _A=\$((_A+1))\n")
            }
            append("_end \"network\"\n")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KSM
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildKsm(t: Map<String, String>): String {
        if (t["ksm"] != "1") return """
_begin
# ksm — disable
write 0 /sys/kernel/mm/ksm/run
_end "ksm"
"""
        val agr = t["ksm_aggressive"] == "1"
        return """
_begin
# ksm — enable${'$'}{if (agr) " aggressive" else ""}
write 1 /sys/kernel/mm/ksm/run
write ${'$'}{if (agr) "200" else "1000"}  /sys/kernel/mm/ksm/sleep_millisecs
write ${'$'}{if (agr) "1000" else "256"}  /sys/kernel/mm/ksm/pages_to_scan
write ${'$'}{if (agr) "1" else "0"}       /sys/kernel/mm/ksm/merge_across_nodes
_end "ksm"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Touch
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildTouch(t: Map<String, String>): String {
        if (t["touch_boost"] != "1") return """
_begin
# touch — skipped
_S=${'$'}((_S+1))
_end "touch"
"""
        val hz = when (t["touch_sample_rate"]) { "high" -> "180"; "max" -> "240"; else -> "120" }
        return """
_begin
# touch — ${hz}Hz
forall $hz /sys/bus/platform/drivers/synaptics_dsx_*/*/*/report_rate
forall $hz /sys/bus/i2c/drivers/synaptics_rmi4_i2c/*/report_rate
write $hz /proc/goodix_tp/report_rate
write $hz /proc/tp_debug/report_rate
write $hz /sys/kernel/debug/mtk_touch/report_rate_ctrl
write 64  /sys/module/cpu_boost/parameters/input_boost_ms
_end "touch"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Animation
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildUiAnim(t: Map<String, String>): String {
        val scale = if (t["fast_anim"] == "1") "0.5" else "1.0"
        return """
_begin
# ui_anim — scale $scale
settings put global window_animation_scale $scale 2>/dev/null && _A=${'$'}((_A+1)) || _F=${'$'}((_F+1))
settings put global transition_animation_scale $scale 2>/dev/null && _A=${'$'}((_A+1)) || _F=${'$'}((_F+1))
settings put global animator_duration_scale $scale 2>/dev/null && _A=${'$'}((_A+1)) || _F=${'$'}((_F+1))
_end "ui_anim"
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misc — encore perfcommon() style
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildMisc(t: Map<String, String>): String = buildString {
        append("_begin\n# misc (encore perfcommon style)\n")

        // sched_rt_runtime_us = -1 menonaktifkan RT throttling.
        // BERBAHAYA: beberapa kernel tidak support nilai -1 dan dapat menyebabkan deadlock.
        // Fix: cek dulu sebelum menulis, fallback ke 950000 jika tidak support.
        append("""
if [ -f /proc/sys/kernel/sched_rt_runtime_us ]; then
  if echo -1 > /proc/sys/kernel/sched_rt_runtime_us 2>/dev/null; then
    _A=$((_A+1))
  else
    echo 950000 > /proc/sys/kernel/sched_rt_runtime_us 2>/dev/null && _A=$((_A+1)) || _F=$((_F+1))
  fi
fi
""")
        append("write 1  /proc/sys/kernel/perf_event_paranoid\n")
        append("write 3  /proc/sys/kernel/perf_cpu_time_max_percent\n")
        append("write 0  /proc/sys/kernel/sched_schedstats\n")
        append("write 1  /proc/sys/kernel/sched_child_runs_first\n")
        append("write 32 /proc/sys/kernel/sched_nr_migrate\n")
        append("write 0  /proc/sys/vm/page-cluster\n")
        append("write 0  /proc/sys/vm/compaction_proactiveness\n")
        // Disable Oplus/Realme bloats (aman, skip jika tidak ada)
        append("write 0 /sys/module/opchain/parameters/chain_on\n")
        append("write 0 /sys/module/cpufreq_bouncing/parameters/enable\n")

        if (t["entropy_boost"] == "1") {
            append("write 256 /proc/sys/kernel/random/write_wakeup_threshold\n")
            append("write 64  /proc/sys/kernel/random/read_wakeup_threshold\n")
        }

        if (t["doze"] == "1") {
            append("dumpsys deviceidle enable deep 2>/dev/null; _A=\$((_A+1))\n")
        }

        append("_end \"misc\"\n")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse RESULT: markers
    // Format: RESULT:<name>:<ok|fail>:a=<n>:s=<n>:f=<n>
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseResults(output: String): List<SubsystemResult> {
        val results = output.lines()
            .filter { it.startsWith("RESULT:") }
            .mapNotNull { line ->
                val p = line.split(":")
                if (p.size < 6) return@mapNotNull null
                SubsystemResult(
                    name    = p[1],
                    ok      = p[2] == "ok",
                    applied = p[3].removePrefix("a=").toIntOrNull() ?: 0,
                    skipped = p[4].removePrefix("s=").toIntOrNull() ?: 0,
                    failed  = p[5].removePrefix("f=").toIntOrNull() ?: 0,
                )
            }

        return results.ifEmpty {
            listOf(
                SubsystemResult(
                    name = "shell",
                    ok   = false,
                    note = "Tidak ada output — pastikan root aktif dan Shell.cmd() terhubung",
                )
            )
        }
    }
}