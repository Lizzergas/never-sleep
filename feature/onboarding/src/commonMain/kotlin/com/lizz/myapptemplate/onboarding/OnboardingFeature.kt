package com.lizz.myapptemplate.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.AppDestination
import com.lizz.myapptemplate.navigation.DestinationKind
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.RouteContentRegistryBuilder
import com.lizz.myapptemplate.navigation.StartRouteOverride
import com.lizz.myapptemplate.navigation.TopBarConfig
import com.lizz.myapptemplate.navigation.TopBarMode
import com.lizz.myapptemplate.onboarding.data.DataStoreOnboardingRepository
import com.lizz.myapptemplate.onboarding.domain.OnboardingRepository
import com.lizz.myapptemplate.onboarding.presentation.OnboardingScreen
import com.lizz.myapptemplate.onboarding.presentation.OnboardingViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

@Serializable
data object OnboardingRoute : NavKey

object OnboardingFeature : FeatureRegistration {
    override val destinations = listOf(
        AppDestination(
            route = OnboardingRoute,
            id = "onboarding",
            kind = DestinationKind.FullScreen,
            topBar = TopBarConfig(title = "Onboarding", mode = TopBarMode.Hidden),
        ),
    )

    override val descriptors = listOf(
        FeatureDescriptor(
            id = "onboarding",
            title = "Onboarding",
            description = "First-launch intro flow (re-run it any time)",
            startRoute = OnboardingRoute,
        ),
    )

    override fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(OnboardingRoute::class)
    }

    override fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    ) {
        scope.entry<OnboardingRoute> {
            OnboardingRouteContent(navigator)
        }
    }

    override fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) {
        registry.entry<OnboardingRoute> {
            OnboardingRouteContent(navigator)
        }
    }
}

@Composable
private fun OnboardingRouteContent(navigator: Navigator) {
    OnboardingScreen(onFinished = navigator::resetToStart)
}

val onboardingKoinModule: Module = module {
    single { DataStoreOnboardingRepository(get()) } binds
        arrayOf(OnboardingRepository::class, StartRouteOverride::class)
    viewModelOf(::OnboardingViewModel)
}
