package com.lizz.myapptemplate

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.auth.AccountRoute
import com.lizz.myapptemplate.notes.NotesRoute
import com.lizz.myapptemplate.settings.SettingsRoute
import com.lizz.myapptemplate.showcase.DesignsystemGalleryRoute
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationControllerTest {
    @Test
    fun switchingTopLevelDestinationsPreservesPreviousStack() {
        val settingsStack = NavBackStack<NavKey>(SettingsRoute, DesignsystemGalleryRoute)
        val controller =
            controller(
                selected = SettingsRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to NavBackStack(ShowcaseHomeRoute),
                        SettingsRoute to settingsStack,
                        AccountRoute to NavBackStack(AccountRoute),
                    ),
            )

        controller.selectTopLevel(AccountRoute)

        assertEquals(AccountRoute, controller.selectedTopLevelRoute)
        assertEquals(AccountRoute, controller.currentRoute)
        assertEquals(listOf<NavKey>(SettingsRoute, DesignsystemGalleryRoute), settingsStack)
    }

    @Test
    fun switchingBackToTopLevelDestinationRestoresItsStack() {
        val controller =
            controller(
                selected = AccountRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to NavBackStack(ShowcaseHomeRoute),
                        SettingsRoute to NavBackStack(SettingsRoute, DesignsystemGalleryRoute),
                        AccountRoute to NavBackStack(AccountRoute),
                    ),
            )

        controller.selectTopLevel(SettingsRoute)

        assertEquals(SettingsRoute, controller.selectedTopLevelRoute)
        assertEquals(DesignsystemGalleryRoute, controller.currentRoute)
    }

    @Test
    fun reselectingSelectedTopLevelDestinationPopsOnlyThatStackToRoot() {
        val notesStack = NavBackStack<NavKey>(NotesRoute, DesignsystemGalleryRoute)
        val homeStack = NavBackStack<NavKey>(ShowcaseHomeRoute, DesignsystemGalleryRoute)
        val controller =
            controller(
                selected = NotesRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to homeStack,
                        NotesRoute to notesStack,
                    ),
            )

        controller.selectTopLevel(NotesRoute)

        assertEquals(listOf<NavKey>(NotesRoute), notesStack)
        assertEquals(listOf<NavKey>(ShowcaseHomeRoute, DesignsystemGalleryRoute), homeStack)
    }

    @Test
    fun resetToStartSelectsHomeAndLeavesHomeAtRoot() {
        val homeStack = NavBackStack<NavKey>(ShowcaseHomeRoute, DesignsystemGalleryRoute)
        val controller =
            controller(
                selected = SettingsRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to homeStack,
                        SettingsRoute to NavBackStack(SettingsRoute, DesignsystemGalleryRoute),
                    ),
            )

        controller.resetToStart()

        assertEquals(ShowcaseHomeRoute, controller.selectedTopLevelRoute)
        assertEquals(listOf<NavKey>(ShowcaseHomeRoute), homeStack)
    }

    @Test
    fun goBackPopsDetailFirstThenSwitchesNonHomeRootToHome() {
        val controller =
            controller(
                selected = SettingsRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to NavBackStack(ShowcaseHomeRoute),
                        SettingsRoute to NavBackStack(SettingsRoute, DesignsystemGalleryRoute),
                    ),
            )

        assertTrue(controller.goBack())
        assertEquals(SettingsRoute, controller.selectedTopLevelRoute)
        assertEquals(SettingsRoute, controller.currentRoute)

        assertTrue(controller.goBack())
        assertEquals(ShowcaseHomeRoute, controller.selectedTopLevelRoute)
        assertEquals(ShowcaseHomeRoute, controller.currentRoute)

        assertFalse(controller.goBack())
    }

    private fun controller(
        selected: NavKey,
        stacks: Map<NavKey, NavBackStack<NavKey>>,
    ): AppNavigationController =
        AppNavigationController(
            topLevelBackStacks = stacks,
            selectedTopLevelRouteState = mutableStateOf(selected),
            defaultTopLevelRoute = ShowcaseHomeRoute,
        )
}
