package com.lizz.myapptemplate.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

/** Feature-owned shared route content consumed by Compose and native platform hosts. */
class RouteContentRegistryBuilder {
    private val entries = mutableListOf<RouteContentEntry>()

    inline fun <reified T : NavKey> entry(noinline content: @Composable (T) -> Unit) {
        addEntry(
            routeClass = T::class,
            content = { route -> content(route as T) },
        )
    }

    fun build(): RouteContentRegistry = RouteContentRegistry(entries.toList())

    @PublishedApi
    internal fun addEntry(
        routeClass: KClass<out NavKey>,
        content: @Composable (NavKey) -> Unit,
    ) {
        entries += RouteContentEntry(routeClass = routeClass, content = content)
    }
}

class RouteContentRegistry internal constructor(
    private val entries: List<RouteContentEntry>,
) {
    fun canRender(route: NavKey): Boolean = entryFor(route) != null

    internal fun entryFor(route: NavKey): RouteContentEntry? = entries.lastOrNull { it.matches(route) }
}

@Composable
fun RouteContentHost(
    route: NavKey,
    registry: RouteContentRegistry,
    unsupported: @Composable () -> Unit,
) {
    val entry = registry.entryFor(route)
    if (entry == null) {
        unsupported()
    } else {
        entry.content(route)
    }
}

internal data class RouteContentEntry(
    private val routeClass: KClass<out NavKey>,
    val content: @Composable (NavKey) -> Unit,
) {
    fun matches(route: NavKey): Boolean = route::class == routeClass
}
