package com.lizz.myapptemplate.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.StartRouteOverride
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

@Serializable
data object OnboardingRoute : NavKey

object OnboardingFeature : FeatureRegistration {
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
        singleOf(::OnboardingRepository) bind StartRouteOverride::class
    }
