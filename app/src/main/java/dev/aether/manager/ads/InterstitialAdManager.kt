package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Router iklan interstitial: Unity Ads ↔ AdMob, bergantian setiap tayangan.
 *
 * Urutan:
 *   Slot genap (0, 2, 4, …) → Unity Ads
 *   Slot ganjil (1, 3, 5, …) → AdMob
 *
 * Fallback otomatis:
 *   Jika provider giliran tidak siap → coba provider lain → skip (preload & jadwal lanjut).
 *   Tidak pernah lempar exception ke caller.
 *
 * Thread-safety: @Volatile untuk flag boolean; showCount diakses di Main thread saja.
 */
object InterstitialAdManager {

    private const val UNITY_PLACEMENT = "Interstitial_Android"

    // ── Unity load state ──────────────────────────────────────────────────────
    @Volatile private var unityLoaded  = false
    @Volatile private var unityLoading = false

    // ── Giliran: 0-based show counter, diakses dari Main thread saja ──────────
    private var showCount = 0

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

    // ── Preload kedua network sekaligus ───────────────────────────────────────

    fun preload(context: Context) {
        preloadUnity()
        AdMobInterstitialManager.preload(context)
    }

    private fun preloadUnity() {
        if (!UnityAds.isInitialized) return
        if (unityLoaded || unityLoading) return
        unityLoading = true
        UnityAds.load(UNITY_PLACEMENT, unityLoadListener)
    }

    // ── Show — router utama ───────────────────────────────────────────────────

    /**
     * Tampilkan iklan. Giliran ditentukan oleh [showCount]:
     *   genap → Unity, ganjil → AdMob, dengan fallback ke network lain jika tidak siap.
     */
    fun showIfReady(activity: Activity, onDone: (() -> Unit)? = null) {
        val preferUnity = (showCount % 2 == 0)
        showCount++

        if (preferUnity) {
            when {
                unityLoaded              -> showUnity(activity, onDone)
                AdMobInterstitialManager.isReady -> showAdMob(activity, onDone)
                else                     -> {
                    // Tidak ada yang siap — preload dan lanjut
                    preload(activity)
                    onDone?.invoke()
                }
            }
        } else {
            when {
                AdMobInterstitialManager.isReady -> showAdMob(activity, onDone)
                unityLoaded              -> showUnity(activity, onDone)
                else                     -> {
                    preload(activity)
                    onDone?.invoke()
                }
            }
        }
    }

    // ── Show Unity ────────────────────────────────────────────────────────────

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
                    preload(activity)
                }

                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String,
                ) {
                    onDone?.invoke()
                    preload(activity)
                }
            }
        )
    }

    // ── Show AdMob ────────────────────────────────────────────────────────────

    private fun showAdMob(activity: Activity, onDone: (() -> Unit)?) {
        AdMobInterstitialManager.showIfReady(activity) {
            onDone?.invoke()
            preload(activity)
        }
    }
}
