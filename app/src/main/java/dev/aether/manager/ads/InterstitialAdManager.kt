package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

object InterstitialAdManager {

    private const val PLACEMENT = "Interstitial_Android"

    @Volatile private var isLoaded  = false
    @Volatile private var isLoading = false

    private val loadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(placementId: String) {
            isLoaded  = true
            isLoading = false
        }

        override fun onUnityAdsFailedToLoad(
            placementId: String,
            error: UnityAds.UnityAdsLoadError,
            message: String
        ) {
            isLoaded  = false
            isLoading = false
        }
    }

    fun preload(context: Context) {
        if (!UnityAds.isInitialized) return
        if (isLoaded || isLoading) return
        // nativeCheckUnityIntact TIDAK dipanggil di sini.
        // Check ini hanya valid setelah onInitializationComplete (dipanggil dari AetherApplication).
        // Memanggil di sini menyebabkan false kill karena class Unity belum terdaftar
        // saat preload() dipicu dari showIfReady() (iklan belum siap → reload → cek terlalu dini).
        isLoading = true
        UnityAds.load(PLACEMENT, loadListener)
    }

    fun showIfReady(activity: Activity, onDone: (() -> Unit)? = null) {
        if (!UnityAds.isInitialized) {
            onDone?.invoke()
            return
        }
        if (!isLoaded) {
            preload(activity)
            onDone?.invoke()
            return
        }

        isLoaded = false

        UnityAds.show(activity, PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}

            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                onDone?.invoke()
                preload(activity)
            }

            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                onDone?.invoke()
                preload(activity)
            }
        })
    }
}
