package com.lizz.myapptemplate.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkRegistryTest {
    @Test
    fun resolvesMatchingCustomSchemeUrl() {
        val registry = DeepLinkRegistry(
            specs = listOf(
                DeepLinkSpec(
                    pattern = DeepLinkPattern(
                        scheme = "myapptemplate",
                        host = "open",
                        pathSegments = listOf("home"),
                    ),
                    buildResolution = {
                        DeepLinkResolution(
                            selectedTopLevelRoute = TestHomeRoute,
                            stack = listOf(TestHomeRoute),
                        )
                    },
                ),
            ),
        )

        val resolution = registry.resolve("myapptemplate://open/home")

        assertEquals(
            DeepLinkResolution(
                selectedTopLevelRoute = TestHomeRoute,
                stack = listOf(TestHomeRoute),
            ),
            resolution,
        )
    }

    @Test
    fun rejectsMalformedUnknownDuplicateAndOversizedUrls() {
        val registry = DeepLinkRegistry(
            specs = listOf(
                DeepLinkSpec(
                    pattern = DeepLinkPattern(
                        scheme = "myapptemplate",
                        host = "open",
                        pathSegments = listOf("home"),
                    ),
                    buildResolution = {
                        DeepLinkResolution(
                            selectedTopLevelRoute = TestHomeRoute,
                            stack = listOf(TestHomeRoute),
                        )
                    },
                ),
                DeepLinkSpec(
                    pattern = DeepLinkPattern(
                        scheme = "myapptemplate",
                        host = "open",
                        pathSegments = listOf("home"),
                    ),
                    buildResolution = {
                        DeepLinkResolution(
                            selectedTopLevelRoute = TestSettingsRoute,
                            stack = listOf(TestSettingsRoute),
                        )
                    },
                ),
            ),
        )

        assertNull(registry.resolve("not a url"))
        assertNull(registry.resolve("myapptemplate://open/unknown"))
        assertNull(registry.resolve("myapptemplate://open/home"))
        assertNull(registry.resolve("myapptemplate://open/${"a".repeat(2048)}"))
    }

    @Serializable
    private data object TestHomeRoute : NavKey

    @Serializable
    private data object TestSettingsRoute : NavKey
}
