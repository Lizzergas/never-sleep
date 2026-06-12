package com.lizz.myapptemplate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.StartRouteOverride
import com.lizz.myapptemplate.onboarding.OnboardingFeature
import com.lizz.myapptemplate.settings.SettingsFeature
import com.lizz.myapptemplate.showcase.ShowcaseFeature
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
import com.lizz.myapptemplate.ui.LoadingContent
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.mp.KoinPlatform

/**
 * THE feature plug-in point for navigation. Each entry contributes its routes
 * (serializers), nav entries, and showcase listing. To remove a feature,
 * delete its line here, its Koin module in di/Koin.kt, and its include in
 * settings.gradle.kts.
 */
val featureRegistrations: List<FeatureRegistration> =
    listOf(
        ShowcaseFeature,
        SettingsFeature,
        OnboardingFeature,
    )

private val defaultStartRoute: NavKey = ShowcaseHomeRoute

@Composable
fun AppNavHost() {
    // Features may override the start destination (e.g. onboarding until its
    // seen-flag is set). The lookup is optional — without one we start at the
    // default immediately; with one we gate on the (suspend) resolution.
    val startRouteOverride =
        remember {
            runCatching { KoinPlatform.getKoin().getOrNull<StartRouteOverride>() }.getOrNull()
        }
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
                    backStack.removeLastOrNull()
                }

                override fun resetToStart() {
                    backStack.add(defaultStartRoute)
                    while (backStack.size > 1) {
                        backStack.removeAt(0)
                    }
                }
            }
        }
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                // Scopes ViewModels to nav entries, cleared when an entry is popped.
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                featureRegistrations.forEach { it.registerEntries(this, navigator) }
            },
    )
}
