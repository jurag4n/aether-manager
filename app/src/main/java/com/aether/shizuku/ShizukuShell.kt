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

    enum class State(val label: String, val detail: String) {
        READY("Shizuku Ready", "Permission Shizuku aktif dan shell bisa digunakan."),
        NOT_RUNNING("Shizuku Off", "Aplikasi/daemon Shizuku belum berjalan."),
        DENIED("Shizuku Denied", "Permission Shizuku belum diberikan untuk Aether."),
        ERROR("Shizuku Error", "Status Shizuku tidak bisa dibaca.")
    }

    fun state(): State = runCatching {
        if (!Shizuku.pingBinder()) return@runCatching State.NOT_RUNNING
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        if (granted) State.READY else State.DENIED
    }.getOrDefault(State.ERROR)

    fun isAvailable(): Boolean = state() != State.NOT_RUNNING && state() != State.ERROR

    fun hasPermission(): Boolean = state() == State.READY

    fun requestPermissionIfNeeded(): Boolean {
        return when (state()) {
            State.READY -> true
            State.DENIED -> {
                runCatching { Shizuku.requestPermission(REQUEST_CODE) }
                false
            }
            else -> false
        }
    }

    suspend fun sh(script: String, timeoutSec: Long = 8L): RootEngine.ShellResult = withContext(Dispatchers.IO) {
        when (state()) {
            State.READY -> Unit
            State.DENIED -> {
                runCatching { Shizuku.requestPermission(REQUEST_CODE) }
                return@withContext RootEngine.ShellResult(1, "", "Shizuku permission belum diberikan")
            }
            State.NOT_RUNNING -> return@withContext RootEngine.ShellResult(1, "", "Shizuku belum berjalan")
            State.ERROR -> return@withContext RootEngine.ShellResult(1, "", "Status Shizuku error")
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
