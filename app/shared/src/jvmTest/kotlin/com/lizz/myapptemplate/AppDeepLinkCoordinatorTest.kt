package com.lizz.myapptemplate

import com.lizz.myapptemplate.auth.AccountRoute
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.auth.domain.User
import com.lizz.myapptemplate.navigation.DeepLinkAuthPolicy
import com.lizz.myapptemplate.navigation.DeepLinkPattern
import com.lizz.myapptemplate.navigation.DeepLinkRegistry
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.DeepLinkSpec
import com.lizz.myapptemplate.notes.NotesRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppDeepLinkCoordinatorTest {
    @Test
    fun coordinatorPublishesResolvedUrlsAndIgnoresInvalidUrls() {
        val coordinator = DeepLinkCoordinator(appDeepLinkRegistry())

        assertFalse(coordinator.openUrl("myapptemplate://open/missing"))
        assertNull(coordinator.currentRequest())

        assertTrue(coordinator.openUrl("myapptemplate://open/notes"))

        val request = coordinator.currentRequest()
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = NotesRoute,
                stack = listOf(NotesRoute),
            ),
            request?.resolution,
        )

        coordinator.markHandled(request!!)

        assertNull(coordinator.currentRequest())
    }

    @Test
    fun authGateStoresProtectedLinksUntilLoginCompletes() {
        val protectedResolution = DeepLinkResolution(
            selectedTopLevelRoute = NotesRoute,
            stack = listOf(NotesRoute),
            authPolicy = DeepLinkAuthPolicy.RequiresAuthenticatedSession,
        )
        val gate = DeepLinkAuthGate(accountRoute = AccountRoute)

        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = AccountRoute,
                stack = listOf(AccountRoute),
            ),
            gate.resolveForSession(protectedResolution, SessionState.LoggedOut),
        )
        assertNull(gate.consumeAfterLogin(SessionState.LoggedOut))
        assertEquals(protectedResolution, gate.consumeAfterLogin(SessionState.LoggedIn(USER)))
        assertNull(gate.consumeAfterLogin(SessionState.LoggedIn(USER)))
    }

    @Test
    fun publicLinksBypassAuthGate() {
        val publicResolution = DeepLinkResolution(
            selectedTopLevelRoute = NotesRoute,
            stack = listOf(NotesRoute),
        )
        val gate = DeepLinkAuthGate(accountRoute = AccountRoute)

        assertEquals(publicResolution, gate.resolveForSession(publicResolution, SessionState.LoggedOut))
        assertNull(gate.consumeAfterLogin(SessionState.LoggedIn(USER)))
    }

    @Test
    fun coordinatorCanUseFeatureOwnedAuthRequiredSpecs() {
        val registry = DeepLinkRegistry(
            specs = listOf(
                DeepLinkSpec(
                    pattern = DeepLinkPattern(
                        scheme = "myapptemplate",
                        host = "open",
                        pathSegments = listOf("protected"),
                    ),
                    authPolicy = DeepLinkAuthPolicy.RequiresAuthenticatedSession,
                    buildResolution = {
                        DeepLinkResolution(
                            selectedTopLevelRoute = NotesRoute,
                            stack = listOf(NotesRoute),
                        )
                    },
                ),
            ),
        )
        val coordinator = DeepLinkCoordinator(registry)

        assertTrue(coordinator.openUrl("myapptemplate://open/protected"))

        assertEquals(
            DeepLinkAuthPolicy.RequiresAuthenticatedSession,
            coordinator.currentRequest()?.resolution?.authPolicy,
        )
    }
}

private val USER = User(id = "42", email = "user@test.dev")
