package com.lizz.neversleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    var failed by remember { mutableStateOf(false) }

    if (failed) {
        Box(
            modifier = modifier
                .background(Color(0xFF1A1A2E))
                .height(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Ad unavailable",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        failed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView ->
            adView.destroy()
        },
        update = { adView ->
            if (adView.adUnitId != BuildConfig.ADMOB_BANNER_UNIT_ID) {
                adView.adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                adView.loadAd(AdRequest.Builder().build())
            }
        },
    )
}
