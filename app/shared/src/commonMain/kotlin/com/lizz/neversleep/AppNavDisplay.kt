package com.lizz.neversleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
internal fun AppNavDisplay(
    controller: AppNavigationController,
    topLevelBackStacks: Map<NavKey, NavBackStack<NavKey>>,
    transientBackStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val appEntryProvider = remember(controller.navigator) {
        entryProvider {
            featureRegistrations.forEach { it.registerEntries(this, controller.navigator) }
        }
    }
    val entriesByTopLevel = topLevelBackStacks.mapValues { (_, backStack) ->
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = appEntryProvider,
        )
    }
    val transientEntries = rememberDecoratedNavEntries(
        backStack = transientBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = appEntryProvider,
    )
    val currentEntries = if (controller.isTransientActive) {
        transientEntries
    } else {
        entriesByTopLevel.getValue(controller.selectedTopLevelRoute)
    }
    val onBack = remember(controller) {
        {
            controller.goBack()
            Unit
        }
    }

    NavDisplay(
        entries = currentEntries,
        modifier = modifier,
        sizeTransform = null,
        transitionSpec = appNavTransitionSpec(),
        popTransitionSpec = appNavPopTransitionSpec(),
        predictivePopTransitionSpec = appNavPredictivePopTransitionSpec(),
        onBack = onBack,
    )
}
