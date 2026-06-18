package com.lizz.myapptemplate

import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.navigation.DeepLinkBackStackPolicy
import com.lizz.myapptemplate.navigation.DeepLinkRegistry
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import org.koin.mp.KoinPlatform

data class IosNavigationCommand(
    val selectedTopLevelRoute: NavKey,
    val stack: List<NavKey>,
    val isFullScreen: Boolean,
)

fun nativeDeepLinkCommand(url: String?): IosNavigationCommand? =
    runCatching {
        val koin = KoinPlatform.getKoin()
        val bridge = koin.get<IosDeepLinkCommandBridge>()
        val session = koin
            .getOrNull<SessionRepository>()
            ?.sessionState
            ?.value
            ?: SessionState.LoggedOut
        bridge.commandForUrl(url, session)
    }.getOrNull()

fun consumePendingNativeDeepLinkAfterLogin(): IosNavigationCommand? =
    runCatching {
        val koin = KoinPlatform.getKoin()
        val bridge = koin.get<IosDeepLinkCommandBridge>()
        val session = koin
            .getOrNull<SessionRepository>()
            ?.sessionState
            ?.value
            ?: SessionState.LoggedOut
        bridge.consumeAfterLogin(session)
    }.getOrNull()

class IosDeepLinkCommandBridge(
    private val registry: DeepLinkRegistry,
    accountRoute: NavKey,
) {
    private val authGate = DeepLinkAuthGate(accountRoute = accountRoute)

    fun commandForUrl(
        url: String?,
        session: SessionState,
    ): IosNavigationCommand? =
        registry
            .resolve(url)
            ?.let { authGate.resolveForSession(it, session) }
            ?.toIosCommand()

    fun consumeAfterLogin(session: SessionState): IosNavigationCommand? =
        authGate.consumeAfterLogin(session)?.toIosCommand()
}

private fun DeepLinkResolution.toIosCommand(): IosNavigationCommand =
    IosNavigationCommand(
        selectedTopLevelRoute = selectedTopLevelRoute,
        stack = stack,
        isFullScreen = backStackPolicy == DeepLinkBackStackPolicy.Transient,
    )
