package dev.aether.manager.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.metrics.performance.JankStats
import timber.log.Timber
import java.util.WeakHashMap

object UiPerformanceMonitor : Application.ActivityLifecycleCallbacks {
    private val stats = WeakHashMap<Activity, JankStats>()

    fun install(app: Application) {
        runCatching { app.registerActivityLifecycleCallbacks(this) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        runCatching {
            val jankStats = JankStats.createAndTrack(activity.window) { frameData ->
                if (frameData.isJank) {
                    Timber.tag("AetherJank").d("jank frame=%sms states=%s", frameData.frameDurationUiNanos / 1_000_000, frameData.states)
                }
            }
            jankStats.isTrackingEnabled = true
            stats[activity] = jankStats
        }
    }

    override fun onActivityResumed(activity: Activity) {
        stats[activity]?.isTrackingEnabled = true
    }

    override fun onActivityPaused(activity: Activity) {
        stats[activity]?.isTrackingEnabled = false
    }

    override fun onActivityStopped(activity: Activity) {
        stats[activity]?.isTrackingEnabled = false
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        stats.remove(activity)?.isTrackingEnabled = false
    }
}
