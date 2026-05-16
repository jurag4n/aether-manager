package com.aether.shizuku

import com.aether.util.TweakApplier
import kotlin.system.measureTimeMillis

object ShizukuTweakApplier {
    private fun dnsHost(provider: String): String? = when (provider.trim().lowercase()) {
        "", "off" -> null
        "cloudflare" -> "one.one.one.one"
        "cloudflare malware" -> "security.cloudflare-dns.com"
        "google" -> "dns.google"
        "quad9" -> "dns.quad9.net"
        "cleanbrowsing" -> "family-filter-dns.cleanbrowsing.org"
        "opendns" -> "dns.opendns.com"
        "dns.sb" -> "dot.sb"
        else -> provider.trim().lowercase().filter { it.isLetterOrDigit() || it == '.' || it == '-' }.take(96).takeIf { it.contains('.') && !it.startsWith('.') && !it.endsWith('.') }
    }

    suspend fun apply(tweaks: Map<String, String>): TweakApplier.ApplyResult {
        var output = ""
        var exit = 1
        val elapsed = measureTimeMillis {
            val script = buildScript(tweaks)
            val result = ShizukuShell.sh(script)
            output = result.stdout + "\n" + result.stderr
            exit = result.exitCode
        }
        val subs = parseResults(output).ifEmpty {
            listOf(TweakApplier.SubsystemResult("shell", exit == 0, note = output.trim().take(120)))
        }
        return TweakApplier.ApplyResult(subs, elapsed)
    }

    private fun buildScript(t: Map<String, String>): String = buildString {
        append("""
_ok=0; _skip=0; _fail=0
mark_ok(){ _ok=$((_ok+1)); }
mark_skip(){ _skip=$((_skip+1)); }
mark_fail(){ _fail=$((_fail+1)); }
finish(){ _st=ok; [ ${'$'}_fail -gt 0 ] && [ ${'$'}_ok -eq 0 ] && _st=fail; echo "RESULT:${'$'}1:${'$'}_st:a=${'$'}_ok:s=${'$'}_skip:f=${'$'}_fail"; _ok=0; _skip=0; _fail=0; }
""".trimIndent()).append('\n')

        append("# ui animation\n")
        if (t["fast_anim"] == "1") {
            append("settings put global window_animation_scale 0.5 && mark_ok || mark_fail\n")
            append("settings put global transition_animation_scale 0.5 && mark_ok || mark_fail\n")
            append("settings put global animator_duration_scale 0.5 && mark_ok || mark_fail\n")
        } else {
            append("settings put global window_animation_scale 1.0 && mark_ok || mark_fail\n")
            append("settings put global transition_animation_scale 1.0 && mark_ok || mark_fail\n")
            append("settings put global animator_duration_scale 1.0 && mark_ok || mark_fail\n")
        }
        append("finish ui_anim\n")

        append("# private dns\n")
        val provider = t["dns_provider"].orEmpty()
        val host = dnsHost(provider)
        when {
            provider.equals("Off", true) -> {
                append("settings put global private_dns_mode off && mark_ok || mark_fail\n")
                append("settings delete global private_dns_specifier >/dev/null 2>&1; mark_ok\n")
            }
            host != null -> {
                append("settings put global private_dns_mode hostname && mark_ok || mark_fail\n")
                append("settings put global private_dns_specifier '$host' && mark_ok || mark_fail\n")
            }
            else -> append("mark_skip\n")
        }
        append("finish private_dns\n")

        append("# doze and cache\n")
        if (t["doze"] == "1") append("dumpsys deviceidle enable deep >/dev/null 2>&1 && mark_ok || mark_fail\n") else append("mark_skip\n")
        if (t["clear_cache"] == "1") append("cmd package trim-caches 1024M >/dev/null 2>&1 && mark_ok || mark_fail\n") else append("mark_skip\n")
        append("finish misc\n")

        append("# network user-space safe toggles\n")
        if (t["network_stable"] == "1") {
            append("settings put global wifi_scan_always_enabled 0 >/dev/null 2>&1 && mark_ok || mark_fail\n")
            append("settings put global ble_scan_always_enabled 0 >/dev/null 2>&1 && mark_ok || mark_fail\n")
        } else append("mark_skip\n")
        append("finish network_lite\n")
    }

    private fun parseResults(output: String): List<TweakApplier.SubsystemResult> = output.lines()
        .filter { it.startsWith("RESULT:") }
        .mapNotNull { line ->
            val p = line.split(":")
            if (p.size < 4) return@mapNotNull null
            val counts = p.drop(3).joinToString(":")
            TweakApplier.SubsystemResult(
                name = "shizuku_${p[1]}",
                ok = p[2] == "ok",
                applied = Regex("a=(\\d+)").find(counts)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                skipped = Regex("s=(\\d+)").find(counts)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                failed = Regex("f=(\\d+)").find(counts)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                note = "no-root via Shizuku"
            )
        }
}
