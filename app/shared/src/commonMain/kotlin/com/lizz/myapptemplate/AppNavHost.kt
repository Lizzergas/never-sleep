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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
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
import com.lizz.myapptemplate.auth.AuthFeature
import com.lizz.myapptemplate.designsystem.WindowWidthClass
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.TopLevelDestination
import com.lizz.myapptemplate.notes.NotesFeature
import com.lizz.myapptemplate.onboarding.OnboardingFeature
import com.lizz.myapptemplate.settings.SettingsFeature
import com.lizz.myapptemplate.showcase.ShowcaseFeature
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * THE feature plug-in point for navigation. Each entry contributes its routes
 * (serializers), nav entries, showcase listing, and optional top-level
 * destination. To remove a feature, delete its line here, its app/shared
 * dependency, its Koin module in di/Koin.kt, and its include in
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
    val initialTopLevelRoute =
        if (startRoute in topLevelRoutes) startRoute else defaultStartRoute
    val startsOutsideTopLevel = startRoute !in topLevelRoutes
    val selectedTopLevelRouteState = rememberSerializable(
        stateSerializer = PolymorphicSerializer(NavKey::class),
        configuration = configuration,
    ) {
        mutableStateOf(initialTopLevelRoute)
    }
    val transientActiveState = rememberSaveable(startRoute) {
        mutableStateOf(startsOutsideTopLevel)
    }
    val transientBackStack =
        if (startsOutsideTopLevel) {
            rememberNavBackStack(configuration, startRoute)
        } else {
            null
        }
    val topLevelBackStacks = rememberTopLevelBackStacks(configuration, topLevelDestinations)
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
    val transientEntries = transientBackStack?.let { backStack ->
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = appEntryProvider,
        )
    }
    val currentEntries = if (controller.isTransientActive && transientEntries != null) {
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
): Map<NavKey, NavBackStack<NavKey>> {
    val entries = destinations.map { destination ->
        destination.route to rememberNavBackStack(configuration, destination.route)
    }
    return remember(*entries.map { it.second }.toTypedArray()) {
        entries.toMap()
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
