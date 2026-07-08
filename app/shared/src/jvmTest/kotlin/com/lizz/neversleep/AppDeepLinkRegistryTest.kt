package com.lizz.neversleep

import com.lizz.neversleep.auth.AccountRoute
import com.lizz.neversleep.navigation.DeepLinkResolution
import com.lizz.neversleep.notes.NotesRoute
import com.lizz.neversleep.settings.SettingsRoute
import com.lizz.neversleep.showcase.DesignsystemGalleryRoute
import com.lizz.neversleep.showcase.NetworkDemoRoute
import com.lizz.neversleep.showcase.ShowcaseHomeRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppDeepLinkRegistryTest {
    @Test
    fun resolvesTemplateV1TopLevelLinks() {
        val registry = appDeepLinkRegistry()

        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(ShowcaseHomeRoute),
            ),
            registry.resolve("neversleep://open/home"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = NotesRoute,
                stack = listOf(NotesRoute),
            ),
            registry.resolve("neversleep://open/notes"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = SettingsRoute,
                stack = listOf(SettingsRoute),
            ),
            registry.resolve("neversleep://open/settings"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = AccountRoute,
                stack = listOf(AccountRoute),
            ),
            registry.resolve("neversleep://open/account"),
        )
    }

    @Test
    fun resolvesTemplateV1ShowcaseDetailLinksIntoHomeStack() {
        val registry = appDeepLinkRegistry()

        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(ShowcaseHomeRoute, DesignsystemGalleryRoute),
            ),
            registry.resolve("neversleep://open/showcase/design-system"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(ShowcaseHomeRoute, NetworkDemoRoute),
            ),
            registry.resolve("neversleep://open/showcase/network"),
        )
    }

    @Test
    fun rejectsUnsupportedDeepLinks() {
        val registry = appDeepLinkRegistry()

        assertNull(registry.resolve("neversleep://open/notes/42"))
        assertNull(registry.resolve("neversleep://open/showcase"))
        assertNull(registry.resolve("https://example.com/open/home"))
    }
}
