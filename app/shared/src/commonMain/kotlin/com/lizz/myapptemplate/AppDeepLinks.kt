package com.lizz.myapptemplate

import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.navigation.DeepLinkAuthPolicy
import com.lizz.myapptemplate.navigation.DeepLinkRegistry
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.FeatureRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.mp.KoinPlatform

internal fun appDeepLinkRegistry(registrations: List<FeatureRegistration> = featureRegistrations): DeepLinkRegistry =
    DeepLinkRegistry(registrations.flatMap { it.deepLinks })

fun openAppDeepLink(url: String?): Boolean = runCatching {
    KoinPlatform.getKoin().get<DeepLinkCoordinator>().openUrl(url)
}.getOrDefault(false)

internal data class DeepLinkRequestEvent(
    val id: Long,
    val resolution: DeepLinkResolution,
)

internal class DeepLinkCoordinator(
    private val registry: DeepLinkRegistry,
) {
    private val _requests = MutableStateFlow<DeepLinkRequestEvent?>(null)
    val requests: StateFlow<DeepLinkRequestEvent?> = _requests.asStateFlow()
    private var nextRequestId = 0L

    fun currentRequest(): DeepLinkRequestEvent? = _requests.value

    fun openUrl(url: String?): Boolean {
        val resolution = registry.resolve(url) ?: return false
        nextRequestId += 1
        _requests.value = DeepLinkRequestEvent(
            id = nextRequestId,
            resolution = resolution,
        )
        return true
    }

    fun markHandled(request: DeepLinkRequestEvent) {
        if (_requests.value?.id == request.id) {
            _requests.value = null
        }
    }
}

internal class DeepLinkAuthGate(
    private val accountRoute: NavKey,
) {
    private var pendingResolution: DeepLinkResolution? = null

    fun resolveForSession(
        resolution: DeepLinkResolution,
        session: SessionState,
    ): DeepLinkResolution {
        if (resolution.authPolicy == DeepLinkAuthPolicy.Public || session is SessionState.LoggedIn) {
            return resolution
        }
        pendingResolution = resolution
        return DeepLinkResolution(
            selectedTopLevelRoute = accountRoute,
            stack = listOf(accountRoute),
        )
    }

    fun consumeAfterLogin(session: SessionState): DeepLinkResolution? {
        if (session !is SessionState.LoggedIn) return null
        return pendingResolution.also { pendingResolution = null }
    }
}
