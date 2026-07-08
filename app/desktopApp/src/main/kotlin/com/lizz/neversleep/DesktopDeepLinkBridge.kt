package com.lizz.neversleep

import java.awt.Desktop

private const val DESKTOP_DEEP_LINK_SCHEME = "neversleep"
private const val MAX_DESKTOP_DEEP_LINK_LENGTH = 2_048

internal fun interface DesktopUriHandlerInstaller {
    fun install(onUri: (String) -> Unit): Boolean
}

internal object AwtDesktopUriHandlerInstaller : DesktopUriHandlerInstaller {
    override fun install(onUri: (String) -> Unit): Boolean {
        val desktop = runCatching {
            if (!Desktop.isDesktopSupported()) return false
            Desktop.getDesktop()
        }.getOrElse { return false }

        if (!desktop.isSupported(Desktop.Action.APP_OPEN_URI)) return false

        return runCatching {
            desktop.setOpenURIHandler { event -> onUri(event.uri.toString()) }
        }.isSuccess
    }
}

internal class DesktopDeepLinkBridge(
    private val openUrl: (String) -> Boolean = ::openAppDeepLink,
    private val log: (String) -> Unit = ::logDesktopDeepLink,
    private val uriHandlerInstaller: DesktopUriHandlerInstaller = AwtDesktopUriHandlerInstaller,
) {
    fun installUriHandler(): Boolean {
        val installed = uriHandlerInstaller.install(::openIfCandidate)
        if (!installed) {
            log("Desktop URI handler is not supported on this platform.")
        }
        return installed
    }

    fun openStartupLinks(args: Array<String>): Int = args.count(::openIfCandidate)

    private fun openIfCandidate(rawUrl: String): Boolean {
        val url = rawUrl.trim()
        if (!url.isDesktopDeepLinkCandidate()) return false

        val opened = openUrl(url)
        if (!opened) {
            log("Ignored unsupported desktop deep link: $url")
        }
        return opened
    }

    private fun String.isDesktopDeepLinkCandidate(): Boolean =
        length <= MAX_DESKTOP_DEEP_LINK_LENGTH &&
            startsWith("$DESKTOP_DEEP_LINK_SCHEME://")
}

private fun logDesktopDeepLink(message: String) {
    System.err.println(message)
}
