package com.lizz.myapptemplate

import com.lizz.myapptemplate.auth.AccountRoute
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.notes.NotesRoute
import com.lizz.myapptemplate.settings.SettingsRoute
import com.lizz.myapptemplate.showcase.DesignsystemGalleryRoute
import com.lizz.myapptemplate.showcase.NetworkDemoRoute
import com.lizz.myapptemplate.showcase.ShowcaseHomeRoute
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
            registry.resolve("myapptemplate://open/home"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = NotesRoute,
                stack = listOf(NotesRoute),
            ),
            registry.resolve("myapptemplate://open/notes"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = SettingsRoute,
                stack = listOf(SettingsRoute),
            ),
            registry.resolve("myapptemplate://open/settings"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = AccountRoute,
                stack = listOf(AccountRoute),
            ),
            registry.resolve("myapptemplate://open/account"),
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
            registry.resolve("myapptemplate://open/showcase/design-system"),
        )
        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = ShowcaseHomeRoute,
                stack = listOf(ShowcaseHomeRoute, NetworkDemoRoute),
            ),
            registry.resolve("myapptemplate://open/showcase/network"),
        )
    }

    @Test
    fun rejectsUnsupportedDeepLinks() {
        val registry = appDeepLinkRegistry()

        assertNull(registry.resolve("myapptemplate://open/notes/42"))
        assertNull(registry.resolve("myapptemplate://open/showcase"))
        assertNull(registry.resolve("https://example.com/open/home"))
    }
}
