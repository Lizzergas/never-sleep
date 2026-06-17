package com.lizz.myapptemplate

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppNavigationTransitionTest {
    @Test
    fun appOwnedNavigationTransitionTimingDocumentsFadeThroughPolicy() {
        assertEquals(180, APP_NAV_ENTER_MILLIS)
        assertEquals(90, APP_NAV_EXIT_MILLIS)
        assertEquals(90, APP_NAV_ENTER_DELAY_MILLIS)
    }

    @Test
    fun appOwnedNavigationTransitionsAreProvidedForAllNavigationDirections() {
        assertNotNull(appNavTransitionSpec<NavKey>())
        assertNotNull(appNavPopTransitionSpec<NavKey>())
        assertNotNull(appNavPredictivePopTransitionSpec<NavKey>())
    }
}
