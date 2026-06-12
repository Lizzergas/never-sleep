package com.lizz.myapptemplate.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.StartRouteOverride
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
    override val fullScreenRoutes: Set<NavKey> = setOf(OnboardingRoute)

    override val descriptors =
        listOf(
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
            OnboardingScreen(onFinished = navigator::resetToStart)
        }
    }
}

val onboardingKoinModule: Module =
    module {
        single { DataStoreOnboardingRepository(get()) } binds
            arrayOf(OnboardingRepository::class, StartRouteOverride::class)
        viewModelOf(::OnboardingViewModel)
    }
