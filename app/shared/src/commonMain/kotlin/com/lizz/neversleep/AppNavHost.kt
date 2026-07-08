package com.lizz.neversleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lizz.neversleep.auth.AuthFeature
import com.lizz.neversleep.navigation.FeatureRegistration
import com.lizz.neversleep.notes.NotesFeature
import com.lizz.neversleep.onboarding.OnboardingFeature
import com.lizz.neversleep.settings.SettingsFeature
import com.lizz.neversleep.showcase.ShowcaseFeature
import com.lizz.neversleep.showcase.ShowcaseHomeRoute
import com.lizz.neversleep.ui.rememberOptionalKoin
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * THE feature plug-in point for navigation. Each entry contributes its routes
 * (serializers), route content, deep links, showcase listing, and destination
 * metadata. To remove a feature, delete its line here, its app/shared
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
    val configuration = rememberAppSavedStateConfiguration()
    val destinations = remember { appDestinations(featureRegistrations) }
    val deepLinkCoordinator = rememberOptionalKoin<DeepLinkCoordinator>()
    val navigationState = rememberAppNavigationState(
        startRoute = startRoute,
        configuration = configuration,
        destinations = destinations,
        deepLinkCoordinator = deepLinkCoordinator,
    )
    val controller = navigationState.controller
    val currentRoute = controller.currentRoute
    val currentDestination = currentRoute?.let(navigationState.destinationsByRoute::get)
    val showShell = navigationState.topLevelDestinations.isNotEmpty() &&
        currentRoute !in navigationState.fullScreenRoutes
    val onBack = remember(controller) {
        {
            controller.goBack()
            Unit
        }
    }

    HandleDeepLinkNavigation(
        coordinator = deepLinkCoordinator,
        controller = controller,
        initialRequest = navigationState.seededInitialRequest,
    )
    AppNavigationBackHandler(controller)

    ComposeAppShell(
        destinations = navigationState.topLevelDestinations,
        selectedTopLevelRoute = controller.selectedTopLevelRoute,
        currentDestination = currentDestination,
        canNavigateUp = controller.canNavigateUp,
        showShell = showShell,
        onNavigateUp = onBack,
        onSelect = { controller.selectTopLevel(it.route) },
    ) { modifier ->
        AppNavDisplay(
            controller = controller,
            topLevelBackStacks = navigationState.topLevelBackStacks,
            transientBackStack = navigationState.transientBackStack,
            modifier = modifier,
        )
    }
}

@Composable
private fun rememberAppSavedStateConfiguration(): SavedStateConfiguration =
    remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    featureRegistrations.forEach { it.registerRoutes(this) }
                }
            }
        }
    }
