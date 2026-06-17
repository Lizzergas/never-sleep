package com.lizz.myapptemplate

import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.StartRouteOverride
import org.koin.mp.KoinPlatform

suspend fun resolveAppStartRoute(): NavKey {
    val startRouteOverride = runCatching {
        KoinPlatform.getKoin().getOrNull<StartRouteOverride>()
    }.getOrNull()
    return startRouteOverride?.startRoute() ?: defaultStartRoute
}
