package com.lizz.myapptemplate

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.auth.AccountRoute
import com.lizz.myapptemplate.navigation.DeepLinkBackStackPolicy
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.notes.NotesRoute
import com.lizz.myapptemplate.onboarding.OnboardingRoute
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

    @Test
    fun topLevelDeepLinkSelectsAndPopsDestinationStackToRoot() {
        val notesStack = NavBackStack<NavKey>(NotesRoute, DesignsystemGalleryRoute)
        val settingsStack = NavBackStack<NavKey>(SettingsRoute, DesignsystemGalleryRoute)
        val controller =
            controller(
                selected = SettingsRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to NavBackStack(ShowcaseHomeRoute),
                        NotesRoute to notesStack,
                        SettingsRoute to settingsStack,
                    ),
            )

        controller.openDeepLink(
            DeepLinkResolution(
                selectedTopLevelRoute = NotesRoute,
                stack = listOf(NotesRoute),
            ),
        )

        assertEquals(NotesRoute, controller.selectedTopLevelRoute)
        assertEquals(listOf<NavKey>(NotesRoute), notesStack)
        assertEquals(listOf<NavKey>(SettingsRoute, DesignsystemGalleryRoute), settingsStack)
    }

    @Test
    fun detailDeepLinkReplacesOnlyOwningTopLevelStack() {
        val homeStack = NavBackStack<NavKey>(ShowcaseHomeRoute)
        val settingsStack = NavBackStack<NavKey>(SettingsRoute, DesignsystemGalleryRoute)
        val controller =
            controller(
                selected = SettingsRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to homeStack,
                        SettingsRoute to settingsStack,
                    ),
            )

        controller.openDeepLink(
            DeepLinkResolution(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(ShowcaseHomeRoute, DesignsystemGalleryRoute),
            ),
        )

        assertEquals(ShowcaseHomeRoute, controller.selectedTopLevelRoute)
        assertEquals(listOf<NavKey>(ShowcaseHomeRoute, DesignsystemGalleryRoute), homeStack)
        assertEquals(listOf<NavKey>(SettingsRoute, DesignsystemGalleryRoute), settingsStack)
    }

    @Test
    fun fullScreenDeepLinkUsesTransientStack() {
        val transientStack = NavBackStack<NavKey>(ShowcaseHomeRoute)
        val transientActiveState = mutableStateOf(false)
        val controller =
            controller(
                selected = SettingsRoute,
                stacks =
                    mapOf(
                        ShowcaseHomeRoute to NavBackStack(ShowcaseHomeRoute),
                        SettingsRoute to NavBackStack(SettingsRoute),
                    ),
                transientStack = transientStack,
                transientActiveState = transientActiveState,
            )

        controller.openDeepLink(
            DeepLinkResolution(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(OnboardingRoute),
                backStackPolicy = DeepLinkBackStackPolicy.Transient,
            ),
        )

        assertEquals(SettingsRoute, controller.selectedTopLevelRoute)
        assertTrue(controller.isTransientActive)
        assertEquals(listOf<NavKey>(OnboardingRoute), transientStack)
    }

    private fun controller(
        selected: NavKey,
        stacks: Map<NavKey, NavBackStack<NavKey>>,
        transientStack: NavBackStack<NavKey>? = null,
        transientActiveState: androidx.compose.runtime.MutableState<Boolean>? = null,
    ): AppNavigationController =
        AppNavigationController(
            topLevelBackStacks = stacks,
            selectedTopLevelRouteState = mutableStateOf(selected),
            defaultTopLevelRoute = ShowcaseHomeRoute,
            transientBackStack = transientStack,
            transientActiveState = transientActiveState,
        )
}
