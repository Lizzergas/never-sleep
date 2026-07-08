package com.lizz.neversleep

import com.lizz.neversleep.auth.AccountRoute
import com.lizz.neversleep.auth.domain.SessionState
import com.lizz.neversleep.auth.domain.User
import com.lizz.neversleep.navigation.DeepLinkAuthPolicy
import com.lizz.neversleep.navigation.DeepLinkPattern
import com.lizz.neversleep.navigation.DeepLinkRegistry
import com.lizz.neversleep.navigation.DeepLinkResolution
import com.lizz.neversleep.navigation.DeepLinkSpec
import com.lizz.neversleep.notes.NotesRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppDeepLinkCoordinatorTest {
    @Test
    fun coordinatorPublishesResolvedUrlsAndIgnoresInvalidUrls() {
        val coordinator = DeepLinkCoordinator(appDeepLinkRegistry())

        assertFalse(coordinator.openUrl("neversleep://open/missing"))
        assertNull(coordinator.currentRequest())

        assertTrue(coordinator.openUrl("neversleep://open/notes"))

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
                        scheme = "neversleep",
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

        assertTrue(coordinator.openUrl("neversleep://open/protected"))

        assertEquals(
            DeepLinkAuthPolicy.RequiresAuthenticatedSession,
            coordinator.currentRequest()?.resolution?.authPolicy,
        )
    }
}

private val USER = User(id = "42", email = "user@test.dev")
