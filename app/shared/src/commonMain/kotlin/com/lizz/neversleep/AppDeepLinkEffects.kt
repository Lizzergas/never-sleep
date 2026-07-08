package com.lizz.neversleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lizz.neversleep.auth.AccountRoute
import com.lizz.neversleep.auth.domain.SessionRepository
import com.lizz.neversleep.auth.domain.SessionState
import com.lizz.neversleep.ui.rememberOptionalKoin

@Composable
internal fun HandleDeepLinkNavigation(
    coordinator: DeepLinkCoordinator?,
    controller: AppNavigationController,
    initialRequest: DeepLinkRequestEvent?,
) {
    val sessionRepository = rememberOptionalKoin<SessionRepository>()
    val session = sessionRepository
        ?.sessionState
        ?.collectAsStateWithLifecycle(initialValue = SessionState.Unknown)
        ?.value
        ?: SessionState.LoggedOut
    val pendingRequest = coordinator
        ?.requests
        ?.collectAsStateWithLifecycle(initialValue = coordinator.currentRequest())
        ?.value
    val authGate = remember { DeepLinkAuthGate(accountRoute = AccountRoute) }

    LaunchedEffect(initialRequest?.id) {
        if (initialRequest != null) {
            coordinator?.markHandled(initialRequest)
        }
    }
    LaunchedEffect(pendingRequest?.id, session) {
        if (pendingRequest != null && pendingRequest.id != initialRequest?.id) {
            controller.openDeepLink(authGate.resolveForSession(pendingRequest.resolution, session))
            coordinator.markHandled(pendingRequest)
        }
        authGate.consumeAfterLogin(session)?.let(controller::openDeepLink)
    }
}
