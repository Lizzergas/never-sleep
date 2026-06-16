package com.lizz.myapptemplate

import androidx.compose.runtime.MutableState
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.Navigator

internal class AppNavigationController(
    private val topLevelBackStacks: Map<NavKey, NavBackStack<NavKey>>,
    private val selectedTopLevelRouteState: MutableState<NavKey>,
    private val defaultTopLevelRoute: NavKey,
    private val transientBackStack: NavBackStack<NavKey>? = null,
    private val transientActiveState: MutableState<Boolean>? = null,
) {
    val navigator: Navigator =
        object : Navigator {
            override fun navigate(route: NavKey) {
                this@AppNavigationController.navigate(route)
            }

            override fun goBack() {
                this@AppNavigationController.goBack()
            }

            override fun resetToStart() {
                this@AppNavigationController.resetToStart()
            }
        }

    val selectedTopLevelRoute: NavKey
        get() = selectedTopLevelRouteState.value

    val isTransientActive: Boolean
        get() = transientActiveState?.value == true && transientBackStack != null

    val currentBackStack: NavBackStack<NavKey>
        get() =
            if (isTransientActive) {
                transientBackStack ?: selectedTopLevelBackStack()
            } else {
                selectedTopLevelBackStack()
            }

    val currentRoute: NavKey?
        get() = currentBackStack.lastOrNull()

    val canHandleRootBack: Boolean
        get() =
            !isTransientActive &&
                currentBackStack.size == 1 &&
                selectedTopLevelRoute != defaultTopLevelRoute

    fun selectTopLevel(route: NavKey) {
        require(route in topLevelBackStacks) { "Unknown top-level route: $route" }
        if (isTransientActive) {
            transientActiveState?.value = false
        }
        if (selectedTopLevelRoute == route) {
            reselectTopLevel(route)
        } else {
            selectedTopLevelRouteState.value = route
        }
    }

    fun reselectTopLevel(route: NavKey) {
        require(route in topLevelBackStacks) { "Unknown top-level route: $route" }
        topLevelBackStacks.getValue(route).popToRoot()
    }

    fun navigate(route: NavKey) {
        currentBackStack.add(route)
    }

    fun goBack(): Boolean {
        if (currentBackStack.size > 1) {
            currentBackStack.removeLastOrNull()
            return true
        }
        if (isTransientActive) {
            transientActiveState?.value = false
            selectedTopLevelRouteState.value = defaultTopLevelRoute
            return true
        }
        if (selectedTopLevelRoute != defaultTopLevelRoute) {
            selectedTopLevelRouteState.value = defaultTopLevelRoute
            return true
        }
        return false
    }

    fun resetToStart() {
        transientActiveState?.value = false
        selectedTopLevelRouteState.value = defaultTopLevelRoute
        topLevelBackStacks.getValue(defaultTopLevelRoute).popToRoot()
    }

    private fun selectedTopLevelBackStack(): NavBackStack<NavKey> = topLevelBackStacks.getValue(selectedTopLevelRoute)

    private fun MutableList<NavKey>.popToRoot() {
        while (size > 1) {
            removeLastOrNull()
        }
    }
}
