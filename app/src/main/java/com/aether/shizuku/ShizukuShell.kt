package com.aether.shizuku

import android.content.pm.PackageManager
import android.os.Build
import com.aether.util.RootEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

object ShizukuShell {
    private const val REQUEST_CODE = 7431

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        isAvailable() && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            )
    }.getOrDefault(false)

    fun requestPermissionIfNeeded(): Boolean {
        if (!isAvailable()) return false
        if (hasPermission()) return true
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
        return false
    }

    suspend fun sh(script: String, timeoutSec: Long = 8L): RootEngine.ShellResult = withContext(Dispatchers.IO) {
        if (!requestPermissionIfNeeded()) {
            return@withContext RootEngine.ShellResult(1, "", "Shizuku permission not granted")
        }

        runCatching {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", script), null, null)
            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@runCatching RootEngine.ShellResult(124, "", "Shizuku shell timeout")
            }
            RootEngine.ShellResult(
                exitCode = process.exitValue(),
                stdout = process.inputStream.bufferedReader().readText(),
                stderr = process.errorStream.bufferedReader().readText()
            )
        }.getOrElse { e ->
            RootEngine.ShellResult(1, "", e.message ?: "Shizuku shell error")
        }
    }
}
