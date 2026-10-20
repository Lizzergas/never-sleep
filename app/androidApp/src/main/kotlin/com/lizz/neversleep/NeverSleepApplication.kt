package com.lizz.neversleep

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.MobileAds
import com.lizz.neversleep.review.ReviewPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Initializes the ads SDK only after [AdConsentManager] confirms ads may be requested.
 *
 * Keeping this explicit prevents SDK startup from racing ahead of the consent flow.
 */
class NeverSleepApplication : Application() {
    internal val reviewPolicy by lazy {
        ReviewPolicy(
            SharedPreferencesReviewStateStore(
                getSharedPreferences("never_sleep_prefs", MODE_PRIVATE),
            ),
        )
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mobileAdsInitializationStarted = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    val mobileAdsReadyState = mutableStateOf(false)

    fun initializeMobileAds() {
        if (!mobileAdsInitializationStarted.compareAndSet(false, true)) return

        applicationScope.launch {
            MobileAds.initialize(this@NeverSleepApplication) {
                mainHandler.post {
                    mobileAdsReadyState.value = true
                }
            }
        }
    }
}
