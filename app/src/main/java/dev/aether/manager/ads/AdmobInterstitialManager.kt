package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdmobInterstitialManager {

    // Ganti dengan Unit ID interstitial kamu dari AdMob dashboard
    // Format: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
    private const val AD_UNIT_ID = "ca-app-pub-5043818314955328/2382871354"

    @Volatile private var interstitialAd: InterstitialAd? = null
    @Volatile private var isLoading = false

    fun preload(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun isReady(): Boolean = interstitialAd != null

    fun showIfReady(activity: Activity, onDone: (() -> Unit)? = null) {
        val ad = interstitialAd
        if (ad == null) {
            preload(activity)
            onDone?.invoke()
            return
        }

        interstitialAd = null

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onDone?.invoke()
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                onDone?.invoke()
                preload(activity)
            }

            override fun onAdShowedFullScreenContent() {}
        }

        ad.show(activity)
    }
}
