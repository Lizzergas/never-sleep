package com.lizz.neversleep.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder

enum class DestinationKind {
    TopLevel,
    Detail,
    FullScreen,
}

enum class TopBarMode {
    Large,
    Inline,
    Hidden,
}

data class TopBarConfig(
    val title: String,
    val mode: TopBarMode = TopBarMode.Inline,
)

data class PrimaryNavigationItem(
    val label: String,
    val materialIcon: ImageVector,
    val systemImage: String,
)

/** Shared route destination metadata consumed by Compose and native platform shells. */
data class AppDestination(
    val route: NavKey,
    val id: String,
    val kind: DestinationKind = DestinationKind.Detail,
    val topBar: TopBarConfig,
    val primaryNavigation: PrimaryNavigationItem? = null,
) {
    init {
        require(kind != DestinationKind.TopLevel || primaryNavigation != null) {
            "Top-level destinations require a primaryNavigation item: $id"
        }
    }

    val title: String get() = topBar.title

    val systemImage: String? get() = primaryNavigation?.systemImage

    val isTopLevel: Boolean get() = kind == DestinationKind.TopLevel

    val isFullScreen: Boolean get() = kind == DestinationKind.FullScreen

    val usesLargeTitle: Boolean get() = topBar.mode == TopBarMode.Large
}

/**
 * The plug-in contract every feature module implements once.
 *
 * The app shell aggregates all registrations: routes are added to the
 * polymorphic NavKey serializers module (required for back-stack saved state),
 * entries to the Navigation3 entry provider, and listed descriptors to the
 * showcase feature catalog.
 *
 * Removing a feature = removing its settings.gradle include, app/shared
 * dependency, Koin module registration, and app-shell registration.
 */
interface FeatureRegistration {
    /** Listed in the showcase feature catalog; empty hides the feature from it. */
    val descriptors: List<FeatureDescriptor> get() = emptyList()

    /** Destination metadata owned by this feature and consumed by app hosts. */
    val destinations: List<AppDestination> get() = emptyList()

    /** Public URL contracts owned by this feature. */
    val deepLinks: List<DeepLinkSpec> get() = emptyList()

    /** Register every route type this feature owns (serializer registration). */
    fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>)

    /** Register a Navigation3 entry for every route this feature owns. */
    fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    )

    /** Register shared route content for hosts that own top bars/navigation UI. */
    fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) = Unit
}

/** A feature as shown in the showcase catalog. */
data class FeatureDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val startRoute: NavKey,
)

/** All listed features, provided via DI by the app shell. */
data class FeatureCatalog(
    val features: List<FeatureDescriptor>,
)
