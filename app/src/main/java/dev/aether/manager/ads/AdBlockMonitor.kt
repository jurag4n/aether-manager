package dev.aether.manager.ads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AdBlockMonitor {
    fun observe(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        var lastValue: Boolean? = null

        fun checkNow() {
            launch(Dispatchers.IO) {
                val detected = runCatching { AdBlockChecker.isAdblockActive(appContext) }.getOrDefault(false)
                if (lastValue != detected) {
                    lastValue = detected
                    trySend(detected)
                }
            }
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = checkNow()
            override fun onChange(selfChange: Boolean, uri: Uri?) = checkNow()
        }

        val resolver = appContext.contentResolver
        resolver.registerContentObserver(Settings.Global.getUriFor("private_dns_mode"), false, observer)
        resolver.registerContentObserver(Settings.Global.getUriFor("private_dns_specifier"), false, observer)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = checkNow()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        runCatching { appContext.registerReceiver(receiver, filter) }

        val ticker = launch {
            while (isActive) {
                checkNow()
                delay(2500L)
            }
        }

        checkNow()

        awaitClose {
            ticker.cancel()
            runCatching { resolver.unregisterContentObserver(observer) }
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
