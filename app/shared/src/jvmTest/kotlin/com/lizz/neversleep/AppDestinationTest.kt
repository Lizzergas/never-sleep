package com.lizz.neversleep

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.lizz.neversleep.auth.AccountRoute
import com.lizz.neversleep.auth.domain.SessionState
import com.lizz.neversleep.auth.domain.User
import com.lizz.neversleep.di.initKoin
import com.lizz.neversleep.navigation.AppDestination
import com.lizz.neversleep.navigation.DeepLinkAuthPolicy
import com.lizz.neversleep.navigation.DeepLinkPattern
import com.lizz.neversleep.navigation.DeepLinkRegistry
import com.lizz.neversleep.navigation.DeepLinkResolution
import com.lizz.neversleep.navigation.DeepLinkSpec
import com.lizz.neversleep.navigation.DestinationKind
import com.lizz.neversleep.navigation.TopBarMode
import com.lizz.neversleep.notes.NotesRoute
import com.lizz.neversleep.onboarding.OnboardingRoute
import com.lizz.neversleep.settings.SettingsRoute
import com.lizz.neversleep.showcase.DesignsystemGalleryRoute
import com.lizz.neversleep.showcase.NetworkDemoRoute
import com.lizz.neversleep.showcase.ShowcaseHomeRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppDestinationTest {
    @get:Rule
    val rule = createComposeRule()

    private val dataStoreScope = CoroutineScope(Job() + Dispatchers.Default)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) initKoin()
        loadKoinModules(listOf(testDataStoreModule(dataStoreScope), testDatabaseModule()))
        skipOnboardingForTests()
    }

    @After
    fun tearDown() {
        stopKoin()
        dataStoreScope.cancel()
    }

    @Test
    fun destinationMetadataCoversTopLevelAndDeepLinkRoutes() {
        val destinations = appDestinations()

        assertEquals(
            listOf("home", "notes", "settings", "account", "design-system", "network", "onboarding"),
            destinations.map { it.id },
        )
        assertEquals(destinations.map { it.id }.toSet().size, destinations.size)
        assertEquals(listOf("home", "notes", "settings", "account"), topLevelDestinations().map { it.id })
        assertTrue(destinations.topLevelDestinations().all { it.primaryNavigation != null })
        assertTrue(destinations.fullScreenDestinations().all { it.topBar.mode == TopBarMode.Hidden })
        assertTrue(destinations.fullScreenDestinations().all { it.primaryNavigation == null })

        assertDestination(
            ShowcaseHomeRoute,
            "home",
            "Home",
            "house.fill",
            DestinationKind.TopLevel,
            TopBarMode.Large,
            hasPrimaryNavigation = true,
        )
        assertDestination(
            NotesRoute,
            "notes",
            "Notes",
            "square.and.pencil",
            DestinationKind.TopLevel,
            TopBarMode.Large,
            hasPrimaryNavigation = true,
        )
        assertDestination(
            SettingsRoute,
            "settings",
            "Settings",
            "gearshape.fill",
            DestinationKind.TopLevel,
            TopBarMode.Large,
            hasPrimaryNavigation = true,
        )
        assertDestination(
            AccountRoute,
            "account",
            "Account",
            "person.fill",
            DestinationKind.TopLevel,
            TopBarMode.Large,
            hasPrimaryNavigation = true,
        )
        assertDestination(
            OnboardingRoute,
            "onboarding",
            "Onboarding",
            null,
            DestinationKind.FullScreen,
            TopBarMode.Hidden,
            hasPrimaryNavigation = false,
        )
        assertDestination(
            DesignsystemGalleryRoute,
            "design-system",
            "Design system",
            null,
            DestinationKind.Detail,
            TopBarMode.Inline,
            hasPrimaryNavigation = false,
        )
        assertDestination(
            NetworkDemoRoute,
            "network",
            "Network demo",
            null,
            DestinationKind.Detail,
            TopBarMode.Inline,
            hasPrimaryNavigation = false,
        )
    }

    @Test
    fun routeContentRegistryCoversEveryDestination() {
        val registry = appRouteContentRegistry(
            navigator = TestNavigator(),
        )

        appDestinations().forEach { destination ->
            assertTrue(
                registry.canRender(destination.route),
                "Missing route content registration for ${destination.id}",
            )
        }
    }

    @Test
    fun deepLinkTargetsHaveDestinationMetadataAndRouteContent() {
        val deepLinks = listOf(
            "neversleep://open/home",
            "neversleep://open/notes",
            "neversleep://open/settings",
            "neversleep://open/account",
            "neversleep://open/showcase/design-system",
            "neversleep://open/showcase/network",
        )
        val deepLinkRegistry = appDeepLinkRegistry()
        val routeContentRegistry = appRouteContentRegistry(
            navigator = TestNavigator(),
        )

        deepLinks.forEach { url ->
            val resolution = assertNotNull(deepLinkRegistry.resolve(url), "Expected $url to resolve")
            val routes = (listOf(resolution.selectedTopLevelRoute) + resolution.stack).distinct()
            routes.forEach { route ->
                assertNotNull(destinationForRoute(route), "Missing destination metadata for $route from $url")
                assertTrue(
                    routeContentRegistry.canRender(route),
                    "Missing route content for $route from $url",
                )
            }
        }
    }

    @Test
    fun singleScreenAppRendersRoutesWithoutComposeTitleOrBackControls() {
        rule.setContent {
            TestAppOwner {
                SingleScreenApp(
                    route = NetworkDemoRoute,
                    onNavigate = {},
                    onGoBack = {},
                    onSet = {},
                    onActivate = {},
                )
            }
        }

        rule
            .onNodeWithText(
                "GET /api/items from the template server, decoded into shared " +
                    "core:model DTOs. Failures map to typed AppError values " +
                    "rendered by core:ui state components.",
            ).assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(16.dp)
        rule.onNodeWithText("Load items").assertIsDisplayed()
        rule.onAllNodesWithText("Network demo").assertCountEquals(0)
        rule.onAllNodesWithText("Back").assertCountEquals(0)
    }

    @Test
    fun nativeDeepLinkBridgeMapsPublicLinksToNativeCommands() {
        val bridge = IosDeepLinkCommandBridge(
            registry = appDeepLinkRegistry(),
            accountRoute = AccountRoute,
        )

        assertEquals(
            IosNavigationCommand(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(ShowcaseHomeRoute, NetworkDemoRoute),
                isFullScreen = false,
            ),
            bridge.commandForUrl("neversleep://open/showcase/network", SessionState.LoggedOut),
        )
        assertNull(bridge.commandForUrl("neversleep://open/missing", SessionState.LoggedOut))
    }

    @Test
    fun nativeDeepLinkBridgeStoresProtectedLinksUntilLoginCompletes() {
        val protectedRoute = NotesRoute
        val bridge = IosDeepLinkCommandBridge(
            registry = DeepLinkRegistry(
                listOf(
                    DeepLinkSpec(
                        pattern = DeepLinkPattern(
                            scheme = "neversleep",
                            host = "open",
                            pathSegments = listOf("protected"),
                        ),
                        authPolicy = DeepLinkAuthPolicy.RequiresAuthenticatedSession,
                        buildResolution = {
                            DeepLinkResolution(
                                selectedTopLevelRoute = protectedRoute,
                                stack = listOf(protectedRoute),
                            )
                        },
                    ),
                ),
            ),
            accountRoute = AccountRoute,
        )

        assertEquals(
            IosNavigationCommand(
                selectedTopLevelRoute = AccountRoute,
                stack = listOf(AccountRoute),
                isFullScreen = false,
            ),
            bridge.commandForUrl("neversleep://open/protected", SessionState.LoggedOut),
        )
        assertNull(bridge.consumeAfterLogin(SessionState.LoggedOut))
        assertEquals(
            IosNavigationCommand(
                selectedTopLevelRoute = NotesRoute,
                stack = listOf(NotesRoute),
                isFullScreen = false,
            ),
            bridge.consumeAfterLogin(SessionState.LoggedIn(User("42", "user@test.dev"))),
        )
        assertNull(bridge.consumeAfterLogin(SessionState.LoggedIn(User("42", "user@test.dev"))))
    }

    private fun assertDestination(
        route: Any,
        id: String,
        title: String,
        systemImage: String?,
        kind: DestinationKind,
        topBarMode: TopBarMode,
        hasPrimaryNavigation: Boolean,
    ) {
        val destination = destinationForRoute(route as androidx.navigation3.runtime.NavKey)
        assertEquals(id, destination?.id)
        assertEquals(title, destination?.topBar?.title)
        assertEquals(systemImage, destination?.systemImage)
        assertEquals(kind, destination?.kind)
        assertEquals(topBarMode, destination?.topBar?.mode)
        assertEquals(hasPrimaryNavigation, destination?.primaryNavigation != null)
    }

    private fun List<AppDestination>.topLevelDestinations() = filter { it.kind == DestinationKind.TopLevel }

    private fun List<AppDestination>.fullScreenDestinations() = filter { it.kind == DestinationKind.FullScreen }

    private class TestNavigator : com.lizz.neversleep.navigation.Navigator {
        override fun navigate(route: androidx.navigation3.runtime.NavKey) = Unit

        override fun goBack() = Unit

        override fun resetToStart() = Unit
    }
}
