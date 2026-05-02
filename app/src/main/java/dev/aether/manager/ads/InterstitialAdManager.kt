package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Iklan interstitial via Unity Ads saja.
 * AdMob dihapus.
 */
object InterstitialAdManager {

    private const val UNITY_PLACEMENT = "Interstitial_Android"

    @Volatile private var unityLoaded  = false
    @Volatile private var unityLoading = false

    private val unityLoadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(placementId: String) {
            unityLoaded  = true
            unityLoading = false
        }
        override fun onUnityAdsFailedToLoad(
            placementId: String,
            error: UnityAds.UnityAdsLoadError,
            message: String,
        ) {
            unityLoaded  = false
            unityLoading = false
        }
    }

    fun preload(context: Context) {
        preloadUnity()
    }

    private fun preloadUnity() {
        if (!UnityAds.isInitialized) return
        if (unityLoaded || unityLoading) return
        unityLoading = true
        UnityAds.load(UNITY_PLACEMENT, unityLoadListener)
    }

    fun showIfReady(activity: Activity, onDone: (() -> Unit)? = null) {
        if (unityLoaded) {
            showUnity(activity, onDone)
        } else {
            preloadUnity()
            onDone?.invoke()
        }
    }

    private fun showUnity(activity: Activity, onDone: (() -> Unit)?) {
        unityLoaded = false
        UnityAds.show(
            activity,
            UNITY_PLACEMENT,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}
                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState,
                ) {
                    onDone?.invoke()
                    preloadUnity()
                }
                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String,
                ) {
                    onDone?.invoke()
                    preloadUnity()
                }
            }
        )
    }
}
