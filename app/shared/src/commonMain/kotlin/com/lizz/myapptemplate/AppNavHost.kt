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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lizz.myapptemplate.auth.AuthFeature
import com.lizz.myapptemplate.designsystem.WindowWidthClass
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.StartRouteOverride
import com.lizz.myapptemplate.navigation.TopLevelDestination
import com.lizz.myapptemplate.notes.NotesFeature
import com.lizz.myapptemplate.onboarding.OnboardingFeature
import com.lizz.myapptemplate.settings.SettingsFeature
import com.lizz.myapptemplate.showcase.ShowcaseFeature
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
import com.lizz.myapptemplate.ui.LoadingContent
import com.lizz.myapptemplate.ui.rememberOptionalKoin
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * THE feature plug-in point for navigation. Each entry contributes its routes
 * (serializers), nav entries, showcase listing, and optional top-level
 * destination. To remove a feature, delete its line here, its Koin module in
 * di/Koin.kt, and its include in settings.gradle.kts.
 */
val featureRegistrations: List<FeatureRegistration> =
    listOf(
        ShowcaseFeature,
        NotesFeature,
        SettingsFeature,
        OnboardingFeature,
        AuthFeature,
    )

private val defaultStartRoute: NavKey = ShowcaseHomeRoute

@Composable
fun AppNavHost() {
    // Features may override the start destination (e.g. onboarding until its
    // seen-flag is set). The lookup is optional — without one we start at the
    // default immediately; with one we gate on the (suspend) resolution.
    val startRouteOverride = rememberOptionalKoin<StartRouteOverride>()
    var startRoute by remember {
        mutableStateOf(if (startRouteOverride == null) defaultStartRoute else null)
    }
    if (startRouteOverride != null && startRoute == null) {
        LaunchedEffect(Unit) {
            startRoute = startRouteOverride.startRoute() ?: defaultStartRoute
        }
    }

    val resolvedStartRoute = startRoute
    if (resolvedStartRoute == null) {
        LoadingContent()
        return
    }

    val configuration =
        remember {
            SavedStateConfiguration {
                serializersModule =
                    SerializersModule {
                        polymorphic(NavKey::class) {
                            featureRegistrations.forEach { it.registerRoutes(this) }
                        }
                    }
            }
        }
    val backStack = rememberNavBackStack(configuration, resolvedStartRoute)
    val navigator =
        remember(backStack) {
            object : Navigator {
                override fun navigate(route: NavKey) {
                    backStack.add(route)
                }

                override fun goBack() {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                }

                override fun resetToStart() {
                    backStack.add(defaultStartRoute)
                    while (backStack.size > 1) {
                        backStack.removeAt(0)
                    }
                }
            }
        }

    val topLevelDestinations =
        remember { featureRegistrations.mapNotNull { it.topLevelDestination } }
    val fullScreenRoutes =
        remember { featureRegistrations.flatMap { it.fullScreenRoutes }.toSet() }
    val currentRoute = backStack.lastOrNull()
    val showChrome = topLevelDestinations.isNotEmpty() && currentRoute !in fullScreenRoutes

    fun selectTopLevel(destination: TopLevelDestination) {
        if (currentRoute == destination.route) return
        backStack.add(destination.route)
        while (backStack.size > 1) {
            backStack.removeAt(0)
        }
    }

    // Remembered: rebuilding the 5-feature entry map on every back-stack
    // mutation would be wasted work on the hottest navigation path.
    val appEntryProvider =
        remember(navigator) {
            entryProvider {
                featureRegistrations.forEach { it.registerEntries(this, navigator) }
            }
        }
    val onBack = remember(navigator) { { navigator.goBack() } }
    val navDisplay: @Composable (Modifier) -> Unit = { modifier ->
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = onBack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Scopes ViewModels to nav entries, cleared when an entry is popped.
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider = appEntryProvider,
        )
    }

    AdaptiveShell(
        destinations = topLevelDestinations,
        currentRoute = currentRoute,
        showChrome = showChrome,
        onSelect = ::selectTopLevel,
        content = navDisplay,
    )
}

/** Bottom bar on compact widths, navigation rail on medium/expanded. */
@Composable
private fun AdaptiveShell(
    destinations: List<TopLevelDestination>,
    currentRoute: NavKey?,
    showChrome: Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        when {
            !showChrome -> content(Modifier.fillMaxSize())

            widthClass == WindowWidthClass.Compact ->
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = { onSelect(destination) },
                                    icon = { Icon(destination.icon, contentDescription = destination.label) },
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
                                selected = currentRoute == destination.route,
                                onClick = { onSelect(destination) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
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
