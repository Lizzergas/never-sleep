package com.lizz.myapptemplate

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lizz.myapptemplate.auth.AccountRoute
import com.lizz.myapptemplate.auth.AuthFeature
import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.designsystem.WindowWidthClass
import com.lizz.myapptemplate.navigation.DeepLinkAuthPolicy
import com.lizz.myapptemplate.navigation.DeepLinkBackStackPolicy
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.TopLevelDestination
import com.lizz.myapptemplate.notes.NotesFeature
import com.lizz.myapptemplate.onboarding.OnboardingFeature
import com.lizz.myapptemplate.settings.SettingsFeature
import com.lizz.myapptemplate.showcase.ShowcaseFeature
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
import com.lizz.myapptemplate.ui.rememberOptionalKoin
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * THE feature plug-in point for navigation. Each entry contributes its routes
 * (serializers), nav entries, deep links, showcase listing, and optional
 * top-level destination. To remove a feature, delete its line here, its
 * app/shared dependency, its Koin module in di/Koin.kt, and its include in
 * settings.gradle.kts.
 */
val featureRegistrations: List<FeatureRegistration> = listOf(
    ShowcaseFeature,
    NotesFeature,
    SettingsFeature,
    OnboardingFeature,
    AuthFeature,
)

internal val defaultStartRoute: NavKey = ShowcaseHomeRoute

@Composable
fun AppNavHost(startRoute: NavKey = defaultStartRoute) {
    val configuration = remember {
        SavedStateConfiguration {
            serializersModule =
                SerializersModule {
                    polymorphic(NavKey::class) {
                        featureRegistrations.forEach { it.registerRoutes(this) }
                    }
                }
        }
    }
    val topLevelDestinations = remember {
        featureRegistrations.mapNotNull { it.topLevelDestination }
    }
    val fullScreenRoutes = remember { featureRegistrations.flatMap { it.fullScreenRoutes }.toSet() }
    val topLevelRoutes = remember(topLevelDestinations) {
        topLevelDestinations.map { it.route }.toSet()
    }
    val deepLinkCoordinator = rememberOptionalKoin<DeepLinkCoordinator>()
    val initialDeepLinkRequest =
        remember(deepLinkCoordinator) { deepLinkCoordinator?.currentRequest() }
    val initialDeepLinkResolution = initialDeepLinkRequest
        ?.resolution
        ?.takeIf { it.authPolicy == DeepLinkAuthPolicy.Public }
    val seededInitialRequest =
        if (initialDeepLinkResolution != null) initialDeepLinkRequest else null
    val initialRetainedDeepLink =
        initialDeepLinkResolution?.takeIf {
            it.backStackPolicy == DeepLinkBackStackPolicy.RetainedTopLevel &&
                it.selectedTopLevelRoute in topLevelRoutes
        }
    val initialTransientDeepLink =
        initialDeepLinkResolution?.takeIf {
            it.backStackPolicy == DeepLinkBackStackPolicy.Transient
        }
    val initialTopLevelRoute =
        initialRetainedDeepLink?.selectedTopLevelRoute
            ?: if (startRoute in topLevelRoutes) startRoute else defaultStartRoute
    val startsOutsideTopLevel = initialTransientDeepLink != null ||
        (initialRetainedDeepLink == null && startRoute !in topLevelRoutes)
    val selectedTopLevelRouteState = rememberSerializable(
        stateSerializer = PolymorphicSerializer(NavKey::class),
        configuration = configuration,
    ) {
        mutableStateOf(initialTopLevelRoute)
    }
    val transientActiveState = rememberSaveable(startRoute, initialDeepLinkRequest?.id) {
        mutableStateOf(startsOutsideTopLevel)
    }
    val initialTransientStack =
        when {
            initialTransientDeepLink != null -> initialTransientDeepLink.stack
            initialRetainedDeepLink == null && startRoute !in topLevelRoutes -> listOf(startRoute)
            else -> listOf(defaultStartRoute)
        }
    val transientBackStack =
        rememberNavBackStack(configuration, *initialTransientStack.toTypedArray())
    val topLevelBackStacks = rememberTopLevelBackStacks(
        configuration = configuration,
        destinations = topLevelDestinations,
        initialDeepLink = initialRetainedDeepLink,
    )
    val controller = remember(
        topLevelBackStacks,
        selectedTopLevelRouteState,
        transientBackStack,
        transientActiveState,
    ) {
        AppNavigationController(
            topLevelBackStacks = topLevelBackStacks,
            selectedTopLevelRouteState = selectedTopLevelRouteState,
            defaultTopLevelRoute = defaultStartRoute,
            transientBackStack = transientBackStack,
            transientActiveState = transientActiveState,
        )
    }
    HandleDeepLinks(
        coordinator = deepLinkCoordinator,
        controller = controller,
        initialRequest = seededInitialRequest,
    )
    val currentRoute = controller.currentRoute
    val showChrome = topLevelDestinations.isNotEmpty() && currentRoute !in fullScreenRoutes

    fun selectTopLevel(destination: TopLevelDestination) {
        controller.selectTopLevel(destination.route)
    }

    // Remembered: rebuilding the 5-feature entry map on every back-stack
    // mutation would be wasted work on the hottest navigation path.
    val appEntryProvider = remember(controller.navigator) {
        entryProvider {
            featureRegistrations.forEach { it.registerEntries(this, controller.navigator) }
        }
    }
    val entriesByTopLevel = topLevelBackStacks.mapValues { (_, backStack) ->
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = appEntryProvider,
        )
    }
    val transientEntries =
        rememberDecoratedNavEntries(
            backStack = transientBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = appEntryProvider,
        )
    val currentEntries = if (controller.isTransientActive) {
        transientEntries
    } else {
        entriesByTopLevel.getValue(controller.selectedTopLevelRoute)
    }
    val onBack = remember(controller) {
        {
            controller.goBack()
            Unit
        }
    }
    val rootBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = rootBackState,
        isBackEnabled = controller.canHandleRootBack,
        onBackCompleted = { controller.goBack() },
    )
    val navDisplay: @Composable (Modifier) -> Unit = { modifier ->
        key(if (controller.isTransientActive) currentRoute else controller.selectedTopLevelRoute) {
            NavDisplay(
                entries = currentEntries,
                modifier = modifier,
                sizeTransform = null,
                transitionSpec = appNavTransitionSpec(),
                popTransitionSpec = appNavPopTransitionSpec(),
                predictivePopTransitionSpec = appNavPredictivePopTransitionSpec(),
                onBack = onBack,
            )
        }
    }

    AdaptiveShell(
        destinations = topLevelDestinations,
        selectedTopLevelRoute = controller.selectedTopLevelRoute,
        showChrome = showChrome,
        onSelect = ::selectTopLevel,
        content = navDisplay,
    )
}

@Composable
private fun rememberTopLevelBackStacks(
    configuration: SavedStateConfiguration,
    destinations: List<TopLevelDestination>,
    initialDeepLink: DeepLinkResolution?,
): Map<NavKey, NavBackStack<NavKey>> {
    val entries = destinations.map { destination ->
        val initialStack =
            if (initialDeepLink?.selectedTopLevelRoute == destination.route) {
                initialDeepLink.stack
            } else {
                listOf(destination.route)
            }
        destination.route to rememberNavBackStack(configuration, *initialStack.toTypedArray())
    }
    return remember(*entries.map { it.second }.toTypedArray()) {
        entries.toMap()
    }
}

@Composable
private fun HandleDeepLinks(
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

/** Bottom bar on compact widths, navigation rail on medium/expanded. */
@Composable
private fun AdaptiveShell(
    destinations: List<TopLevelDestination>,
    selectedTopLevelRoute: NavKey,
    showChrome: Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        when {
            !showChrome -> content(Modifier.fillMaxSize())
            widthClass == WindowWidthClass.Compact -> Scaffold(
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = selectedTopLevelRoute == destination.route,
                                onClick = { onSelect(destination) },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                content(Modifier.fillMaxSize().padding(padding))
            }

            else ->
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            NavigationRailItem(
                                selected = selectedTopLevelRoute == destination.route,
                                onClick = { onSelect(destination) },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        content(Modifier.fillMaxSize())
                    }
                }
        }
    }
}
