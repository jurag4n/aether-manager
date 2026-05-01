package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Mengelola lifecycle AdMob Interstitial:
 * load → ready → show → dismissed → load lagi.
 *
 * Thread-safety: semua callback AdMob sudah di-deliver ke Main thread
 * oleh SDK, jadi @Volatile sudah cukup tanpa synchronized block.
 */
object AdMobInterstitialManager {

    // Unit iklan AdMob — ambil dari konstanta agar mudah diganti via NativeAether nanti
    private const val UNIT_ID = "ca-app-pub-5043818314955328/4052266582"

    @Volatile private var ad        : InterstitialAd? = null
    @Volatile private var isLoading : Boolean         = false

    val isReady: Boolean get() = ad != null

    fun preload(context: Context) {
        if (isReady || isLoading) return
        isLoading = true

        val req = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            UNIT_ID,
            req,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    ad        = interstitialAd
                    isLoading = false
                    setupCallbacks()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad        = null
                    isLoading = false
                    // Jangan retry langsung — biarkan scheduler/showIfReady yang trigger
                }
            }
        )
    }

    fun showIfReady(activity: Activity, onDone: (() -> Unit)? = null) {
        val interstitial = ad
        if (interstitial == null) {
            preload(activity)
            onDone?.invoke()
            return
        }

        // Invalidasi dulu agar tidak bisa di-show dua kali
        ad = null

        // Set pendingOnDone SEBELUM show() agar tidak race dengan
        // onAdFailedToShowFullScreenContent yang bisa fire sebelum baris berikutnya
        pendingOnDone = onDone
        interstitial.show(activity)
    }

    // Simpan onDone sementara antara show() dan callback dismiss
    @Volatile private var pendingOnDone: (() -> Unit)? = null

    private fun setupCallbacks() {
        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                pendingOnDone?.invoke()
                pendingOnDone = null
                // Preload berikutnya dipanggil dari InterstitialAdManager.showIfReady
                // agar sinkron dengan giliran Unity/AdMob
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                ad = null
                pendingOnDone?.invoke()
                pendingOnDone = null
            }

            override fun onAdShowedFullScreenContent() {
                // ad sudah dipakai — null-kan agar isReady jadi false
                ad = null
            }
        }
    }
}
