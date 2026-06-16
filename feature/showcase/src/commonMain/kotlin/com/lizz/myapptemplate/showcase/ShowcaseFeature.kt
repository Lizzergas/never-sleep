package com.lizz.myapptemplate.showcase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.TopLevelDestination
import com.lizz.myapptemplate.showcase.presentation.designsystem.DesignsystemGalleryScreen
import com.lizz.myapptemplate.showcase.presentation.home.ShowcaseHomeScreen
import com.lizz.myapptemplate.showcase.presentation.network.NetworkDemoScreen
import com.lizz.myapptemplate.showcase.presentation.network.NetworkDemoViewModel
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object ShowcaseFeature : FeatureRegistration {
    override val topLevelDestination =
        TopLevelDestination(route = ShowcaseHomeRoute, label = "Home", icon = Icons.Default.Home)

    // The home screen is the app's start destination, so the showcase lists
    // only its gallery as an openable feature.
    override val descriptors =
        listOf(
            FeatureDescriptor(
                id = "designsystem-gallery",
                title = "Design system gallery",
                description = "Colors, typography and spacing tokens rendered live",
                startRoute = DesignsystemGalleryRoute,
            ),
            FeatureDescriptor(
                id = "network-demo",
                title = "Network demo",
                description = "Typed API call to the template server via core:network",
                startRoute = NetworkDemoRoute,
            ),
        )

    override fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(ShowcaseHomeRoute::class)
        builder.subclass(DesignsystemGalleryRoute::class)
        builder.subclass(NetworkDemoRoute::class)
    }

    override fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    ) {
        scope.entry<ShowcaseHomeRoute> {
            ShowcaseHomeScreen(onOpenFeature = navigator::navigate)
        }
        scope.entry<DesignsystemGalleryRoute> {
            DesignsystemGalleryScreen(onBack = navigator::goBack)
        }
        scope.entry<NetworkDemoRoute> {
            NetworkDemoScreen(onBack = navigator::goBack)
        }
    }
}

val showcaseKoinModule: Module =
    module {
        viewModel {
            NetworkDemoViewModel(
                httpClient = get(),
                connectivityMonitor = getKoin().getOrNull(),
            )
        }
    }
