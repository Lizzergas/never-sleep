package com.lizz.myapptemplate.showcase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.AppDestination
import com.lizz.myapptemplate.navigation.DeepLinkPattern
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.DeepLinkSpec
import com.lizz.myapptemplate.navigation.DestinationKind
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.PrimaryNavigationItem
import com.lizz.myapptemplate.navigation.RouteContentRegistryBuilder
import com.lizz.myapptemplate.navigation.TopBarConfig
import com.lizz.myapptemplate.navigation.TopBarMode
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
    override val destinations = listOf(
        AppDestination(
            route = ShowcaseHomeRoute,
            id = "home",
            kind = DestinationKind.TopLevel,
            topBar = TopBarConfig(title = "Home", mode = TopBarMode.Large),
            primaryNavigation = PrimaryNavigationItem(
                label = "Home",
                materialIcon = Icons.Default.Home,
                systemImage = "house.fill",
            ),
        ),
        AppDestination(
            route = DesignsystemGalleryRoute,
            id = "design-system",
            topBar = TopBarConfig(title = "Design system", mode = TopBarMode.Inline),
        ),
        AppDestination(
            route = NetworkDemoRoute,
            id = "network",
            topBar = TopBarConfig(title = "Network demo", mode = TopBarMode.Inline),
        ),
    )

    override val deepLinks = listOf(
        DeepLinkSpec(
            pattern = DeepLinkPattern(
                scheme = "myapptemplate",
                host = "open",
                pathSegments = listOf("home"),
            ),
            buildResolution = {
                DeepLinkResolution(
                    selectedTopLevelRoute = ShowcaseHomeRoute,
                    stack = listOf(ShowcaseHomeRoute),
                )
            },
        ),
        DeepLinkSpec(
            pattern = DeepLinkPattern(
                scheme = "myapptemplate",
                host = "open",
                pathSegments = listOf("showcase", "design-system"),
            ),
            buildResolution = {
                DeepLinkResolution(
                    selectedTopLevelRoute = ShowcaseHomeRoute,
                    stack = listOf(ShowcaseHomeRoute, DesignsystemGalleryRoute),
                )
            },
        ),
        DeepLinkSpec(
            pattern = DeepLinkPattern(
                scheme = "myapptemplate",
                host = "open",
                pathSegments = listOf("showcase", "network"),
            ),
            buildResolution = {
                DeepLinkResolution(
                    selectedTopLevelRoute = ShowcaseHomeRoute,
                    stack = listOf(ShowcaseHomeRoute, NetworkDemoRoute),
                )
            },
        ),
    )

    // The home screen is the app's start destination, so the showcase lists
    // only its gallery as an openable feature.
    override val descriptors = listOf(
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
            DesignsystemGalleryScreen()
        }
        scope.entry<NetworkDemoRoute> {
            NetworkDemoScreen()
        }
    }

    override fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) {
        registry.entry<ShowcaseHomeRoute> {
            ShowcaseHomeScreen(onOpenFeature = navigator::navigate)
        }
        registry.entry<DesignsystemGalleryRoute> {
            DesignsystemGalleryScreen()
        }
        registry.entry<NetworkDemoRoute> {
            NetworkDemoScreen()
        }
    }
}

val showcaseKoinModule: Module = module {
    viewModel {
        NetworkDemoViewModel(
            httpClient = get(),
            connectivityMonitor = getKoin().getOrNull(),
        )
    }
}
