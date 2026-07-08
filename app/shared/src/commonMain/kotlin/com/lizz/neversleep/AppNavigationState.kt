package com.lizz.neversleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lizz.neversleep.navigation.AppDestination
import com.lizz.neversleep.navigation.DeepLinkAuthPolicy
import com.lizz.neversleep.navigation.DeepLinkBackStackPolicy
import com.lizz.neversleep.navigation.DeepLinkResolution
import com.lizz.neversleep.navigation.DestinationKind
import kotlinx.serialization.PolymorphicSerializer

internal data class AppNavigationState(
    val controller: AppNavigationController,
    val topLevelDestinations: List<AppDestination>,
    val destinationsByRoute: Map<NavKey, AppDestination>,
    val fullScreenRoutes: Set<NavKey>,
    val topLevelBackStacks: Map<NavKey, NavBackStack<NavKey>>,
    val transientBackStack: NavBackStack<NavKey>,
    val seededInitialRequest: DeepLinkRequestEvent?,
)

@Composable
internal fun rememberAppNavigationState(
    startRoute: NavKey,
    configuration: SavedStateConfiguration,
    destinations: List<AppDestination>,
    deepLinkCoordinator: DeepLinkCoordinator?,
): AppNavigationState {
    val destinationsByRoute = remember(destinations) {
        destinations.associateBy { it.route }
    }
    val topLevelDestinations = remember(destinations) {
        destinations.filter { it.kind == DestinationKind.TopLevel }
    }
    val fullScreenRoutes = remember(destinations) {
        destinations
            .filter { it.kind == DestinationKind.FullScreen }
            .map { it.route }
            .toSet()
    }
    val topLevelRoutes = remember(topLevelDestinations) {
        topLevelDestinations.map { it.route }.toSet()
    }
    val initialDeepLinkRequest = remember(deepLinkCoordinator) { deepLinkCoordinator?.currentRequest() }
    val initialDeepLinkResolution = initialDeepLinkRequest
        ?.resolution
        ?.takeIf { it.authPolicy == DeepLinkAuthPolicy.Public }
    val seededInitialRequest = if (initialDeepLinkResolution != null) initialDeepLinkRequest else null
    val initialRetainedDeepLink = initialDeepLinkResolution?.takeIf {
        it.backStackPolicy == DeepLinkBackStackPolicy.RetainedTopLevel &&
            it.selectedTopLevelRoute in topLevelRoutes
    }
    val initialTransientDeepLink = initialDeepLinkResolution?.takeIf {
        it.backStackPolicy == DeepLinkBackStackPolicy.Transient
    }
    val initialTopLevelRoute = initialRetainedDeepLink?.selectedTopLevelRoute
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
    val initialTransientStack = when {
        initialTransientDeepLink != null -> initialTransientDeepLink.stack
        initialRetainedDeepLink == null && startRoute !in topLevelRoutes -> listOf(startRoute)
        else -> listOf(defaultStartRoute)
    }
    val transientBackStack = rememberNavBackStack(configuration, *initialTransientStack.toTypedArray())
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

    return AppNavigationState(
        controller = controller,
        topLevelDestinations = topLevelDestinations,
        destinationsByRoute = destinationsByRoute,
        fullScreenRoutes = fullScreenRoutes,
        topLevelBackStacks = topLevelBackStacks,
        transientBackStack = transientBackStack,
        seededInitialRequest = seededInitialRequest,
    )
}

@Composable
internal fun AppNavigationBackHandler(controller: AppNavigationController) {
    val rootBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = rootBackState,
        isBackEnabled = controller.canHandleRootBack,
        onBackCompleted = { controller.goBack() },
    )
}

@Composable
private fun rememberTopLevelBackStacks(
    configuration: SavedStateConfiguration,
    destinations: List<AppDestination>,
    initialDeepLink: DeepLinkResolution?,
): Map<NavKey, NavBackStack<NavKey>> {
    val entries = destinations.map { destination ->
        val initialStack = if (initialDeepLink?.selectedTopLevelRoute == destination.route) {
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
