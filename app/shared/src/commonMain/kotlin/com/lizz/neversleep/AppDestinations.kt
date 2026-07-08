package com.lizz.neversleep

import androidx.navigation3.runtime.NavKey
import com.lizz.neversleep.navigation.AppDestination
import com.lizz.neversleep.navigation.DestinationKind
import com.lizz.neversleep.navigation.FeatureRegistration
import com.lizz.neversleep.navigation.Navigator
import com.lizz.neversleep.navigation.RouteContentRegistry
import com.lizz.neversleep.navigation.RouteContentRegistryBuilder

fun appDestinations(): List<AppDestination> = appDestinations(featureRegistrations)

fun topLevelDestinations(): List<AppDestination> = appDestinations().filter { it.kind == DestinationKind.TopLevel }

fun destinationForRoute(route: NavKey): AppDestination? = appDestinations().firstOrNull { it.route == route }

fun destinationId(route: NavKey): String? = destinationForRoute(route)?.id

internal fun appDestinations(registrations: List<FeatureRegistration>): List<AppDestination> {
    val allDestinations = registrations.flatMap { it.destinations }
    val topLevelDestinations = allDestinations.filter { it.kind == DestinationKind.TopLevel }
    val topLevelRoutes = topLevelDestinations.map { it.route }.toSet()
    return topLevelDestinations + allDestinations.filterNot { it.route in topLevelRoutes }
}

internal fun appRouteContentRegistry(
    registrations: List<FeatureRegistration> = featureRegistrations,
    navigator: Navigator,
): RouteContentRegistry {
    val builder = RouteContentRegistryBuilder()
    registrations.forEach { it.registerRouteContent(builder, navigator) }
    return builder.build()
}
