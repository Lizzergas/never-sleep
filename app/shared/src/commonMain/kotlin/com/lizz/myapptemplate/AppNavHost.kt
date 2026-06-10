package com.lizz.myapptemplate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.settings.SettingsFeature
import com.lizz.myapptemplate.showcase.ShowcaseFeature
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * THE feature plug-in point for navigation. Each entry contributes its routes
 * (serializers), nav entries, and showcase listing. To remove a feature,
 * delete its line here, its Koin module in di/Koin.kt, and its include in
 * settings.gradle.kts.
 */
val featureRegistrations: List<FeatureRegistration> = listOf(
    ShowcaseFeature,
    SettingsFeature,
)

@Composable
fun AppNavHost() {
    val configuration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    featureRegistrations.forEach { it.registerRoutes(this) }
                }
            }
        }
    }
    val backStack = rememberNavBackStack(configuration, ShowcaseHomeRoute)
    val navigator = remember(backStack) {
        object : Navigator {
            override fun navigate(route: NavKey) {
                backStack.add(route)
            }

            override fun goBack() {
                backStack.removeLastOrNull()
            }
        }
    }
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes ViewModels to nav entries, cleared when an entry is popped.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            featureRegistrations.forEach { it.registerEntries(this, navigator) }
        },
    )
}
