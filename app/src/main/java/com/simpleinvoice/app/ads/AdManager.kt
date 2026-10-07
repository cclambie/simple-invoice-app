package com.simpleinvoice.app.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.simpleinvoice.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Loads and shows pop-up (interstitial) ads. One ad is kept preloaded so it can appear straight
 * away; if none is ready the user's action just goes ahead, so an ad never blocks the app.
 */
class AdManager(private val context: Context, private val scope: CoroutineScope) {
    private val ready = MutableStateFlow<InterstitialAd?>(null)
    private var loading = false

    fun initialize() {
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {}
            withContext(Dispatchers.Main) { preload() }
        }
    }

    private fun preload() {
        if (ready.value != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loading = false
                    ready.value = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            }
        )
    }

    /** Shows a pop-up ad if one is loaded, then runs [then] once it's closed (or straight away). */
    fun showInterstitial(activity: Activity, then: () -> Unit) {
        val ad = ready.value ?: run {
            preload()
            then()
            return
        }
        ready.value = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
                Toast.makeText(context, UPGRADE_HINT, Toast.LENGTH_LONG).show()
                then()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preload()
                then()
            }
        }
        ad.show(activity)
    }

    /** For app-open ads: waits briefly for the first ad to load, then shows it if it arrived. */
    suspend fun showInterstitialWhenReady(activity: Activity, timeoutMs: Long = 5_000) {
        withTimeoutOrNull(timeoutMs) { ready.first { it != null } } ?: return
        showInterstitial(activity) {}
    }

    private companion object {
        const val UPGRADE_HINT = "Remove pop-up ads with Premium: \$1/month"
    }
}
