package com.lizz.myapptemplate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDeepLinkBridgeTest {
    @Test
    fun startupArgsForwardSupportedDeepLinksOnly() {
        val opened = mutableListOf<String>()
        val logs = mutableListOf<String>()
        val bridge = DesktopDeepLinkBridge(
            openUrl = {
                opened += it
                true
            },
            log = logs::add,
            uriHandlerInstaller = DesktopUriHandlerInstaller { true },
        )

        val openedCount = bridge.openStartupLinks(
            arrayOf(
                "myapptemplate://open/notes",
                "--ignored",
                "https://example.com/open/notes",
                "myapptemplate://open/showcase/network",
            ),
        )

        assertEquals(2, openedCount)
        assertEquals(
            listOf(
                "myapptemplate://open/notes",
                "myapptemplate://open/showcase/network",
            ),
            opened,
        )
        assertEquals(emptyList(), logs)
    }

    @Test
    fun startupArgsIgnoreNonCandidatesBeforeForwarding() {
        val opened = mutableListOf<String>()
        val logs = mutableListOf<String>()
        val bridge = DesktopDeepLinkBridge(
            openUrl = {
                opened += it
                true
            },
            log = logs::add,
            uriHandlerInstaller = DesktopUriHandlerInstaller { true },
        )
        val oversized = "myapptemplate://" + "a".repeat(2_048)

        val openedCount = bridge.openStartupLinks(
            arrayOf(
                "",
                "   ",
                "MYAPPTEMPLATE://open/notes",
                "otherapp://open/notes",
                oversized,
            ),
        )

        assertEquals(0, openedCount)
        assertEquals(emptyList(), opened)
        assertEquals(emptyList(), logs)
    }

    @Test
    fun unsupportedSameSchemeUrlsAreForwardedAndLoggedAsIgnored() {
        val opened = mutableListOf<String>()
        val logs = mutableListOf<String>()
        val bridge = DesktopDeepLinkBridge(
            openUrl = {
                opened += it
                false
            },
            log = logs::add,
            uriHandlerInstaller = DesktopUriHandlerInstaller { true },
        )

        val openedCount = bridge.openStartupLinks(arrayOf("myapptemplate://open/unknown"))

        assertEquals(0, openedCount)
        assertEquals(listOf("myapptemplate://open/unknown"), opened)
        assertEquals(1, logs.size)
        assertTrue(logs.single().contains("Ignored unsupported desktop deep link"))
    }

    @Test
    fun installedUriHandlerUsesSameForwardingPath() {
        val installer = CapturingUriHandlerInstaller()
        val opened = mutableListOf<String>()
        val bridge = DesktopDeepLinkBridge(
            openUrl = {
                opened += it
                true
            },
            log = {},
            uriHandlerInstaller = installer,
        )

        assertTrue(bridge.installUriHandler())

        installer.open("myapptemplate://open/notes")
        installer.open("https://example.com/open/notes")
        installer.open("myapptemplate://open/showcase/network")

        assertEquals(
            listOf(
                "myapptemplate://open/notes",
                "myapptemplate://open/showcase/network",
            ),
            opened,
        )
    }

    @Test
    fun unsupportedUriHandlerInstallReturnsFalse() {
        val bridge = DesktopDeepLinkBridge(
            openUrl = { true },
            log = {},
            uriHandlerInstaller = DesktopUriHandlerInstaller { false },
        )

        assertFalse(bridge.installUriHandler())
    }

    private class CapturingUriHandlerInstaller : DesktopUriHandlerInstaller {
        private lateinit var handler: (String) -> Unit

        override fun install(onUri: (String) -> Unit): Boolean {
            handler = onUri
            return true
        }

        fun open(url: String) {
            handler(url)
        }
    }
}
