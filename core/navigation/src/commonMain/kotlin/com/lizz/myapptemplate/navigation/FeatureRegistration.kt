package com.lizz.myapptemplate.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder

/**
 * The plug-in contract every feature module implements once.
 *
 * The app shell aggregates all registrations: routes are added to the
 * polymorphic NavKey serializers module (required for back-stack saved state),
 * entries to the Navigation3 entry provider, and listed descriptors to the
 * showcase feature catalog.
 *
 * Removing a feature = removing its settings.gradle include, its Koin module
 * registration, and its entry in the app shell's registration list.
 */
interface FeatureRegistration {
    /** Listed in the showcase feature catalog; empty hides the feature from it. */
    val descriptors: List<FeatureDescriptor> get() = emptyList()

    /**
     * Non-null places this feature in the adaptive shell's primary navigation
     * (bottom bar on compact, rail on medium/expanded). The catalog
     * automatically drops descriptors whose route is a top-level destination.
     */
    val topLevelDestination: TopLevelDestination? get() = null

    /** Register every route type this feature owns (serializer registration). */
    fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>)

    /** Register a Navigation3 entry for every route this feature owns. */
    fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    )
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

/** An entry in the adaptive shell's primary navigation. */
data class TopLevelDestination(
    val route: NavKey,
    val label: String,
    val icon: ImageVector,
)
