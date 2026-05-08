package dev.aether.manager.util

import android.content.Context
import android.os.Looper
import androidx.tracing.trace
import java.util.concurrent.atomic.AtomicBoolean

object UiNativeBoost {
    private val loaded = AtomicBoolean(false)
    private val warmed = AtomicBoolean(false)

    fun init(context: Context? = null) {
        if (!loaded.get()) {
            runCatching {
                try {
                    System.loadLibrary("aetherui")
                } catch (err: UnsatisfiedLinkError) {
                    val dir = context?.applicationInfo?.nativeLibraryDir
                    val fallback = dir?.let { "$it/libaetherui.so" }
                    if (fallback != null) System.load(fallback) else throw err
                }
                loaded.set(true)
            }
        }
        if (loaded.get() && warmed.compareAndSet(false, true)) {
            runCatching { trace("AetherUiNativeWarmUp") { nativeWarmUp() } }
        }
    }

    fun isReady(): Boolean = loaded.get() && runCatching { nativeIsReady() }.getOrDefault(false)

    fun cpuCount(): Int = if (loaded.get()) {
        runCatching { nativeCpuCount().coerceAtLeast(1) }.getOrDefault(Runtime.getRuntime().availableProcessors())
    } else Runtime.getRuntime().availableProcessors()

    fun frameClockNanos(): Long = if (loaded.get()) {
        runCatching { nativeFrameClockNanos() }.getOrDefault(System.nanoTime())
    } else System.nanoTime()

    fun assertMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "UI operation must run on main thread" }
    }

    private external fun nativeWarmUp(): Boolean
    private external fun nativeCpuCount(): Int
    private external fun nativeFrameClockNanos(): Long
    private external fun nativeIsReady(): Boolean
}
