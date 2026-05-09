package dev.aether.manager.util

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AetherLog {
    private const val MAX_BYTES = 96 * 1024
    private const val LOG_NAME = "aether-local.log"

    private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun localFile(context: Context): File = File(context.filesDir, LOG_NAME)

    suspend fun append(context: Context, tag: String, message: String) = withContext(Dispatchers.IO) {
        runCatching {
            val f = localFile(context.applicationContext)
            val cleanMessage = message.replace('\n', ' ').replace('\r', ' ').take(500)
            val line = "${stamp()} [$tag] $cleanMessage\n"
            f.appendText(line)
            if (f.length() > MAX_BYTES) {
                val text = f.readText().takeLast(MAX_BYTES / 2)
                f.writeText(text)
            }
        }
    }

    suspend fun read(context: Context): String = withContext(Dispatchers.IO) {
        val local = runCatching { localFile(context.applicationContext).readText() }.getOrDefault("")
        val root = runCatching { RootEngine.readFile(RootEngine.LOG_FILE) }.getOrDefault("")
        listOf(local, root).filter { it.isNotBlank() }.joinToString("\n")
            .ifBlank { "Belum ada log." }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        runCatching { localFile(context.applicationContext).writeText("") }
        runCatching { RootEngine.clearRootLog() }
    }

    suspend fun export(context: Context): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(Environment.getExternalStorageDirectory(), "Aether")
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, "aether-log.txt")
            out.writeText(read(context))
            out
        }.getOrNull()
    }
}
