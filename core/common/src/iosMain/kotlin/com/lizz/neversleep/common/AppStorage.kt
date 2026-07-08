package com.lizz.neversleep.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * The app's Documents directory — the canonical place for user data on iOS.
 * Every module that persists files on the iOS target resolves paths through
 * here.
 */
@OptIn(ExperimentalForeignApi::class)
fun documentsDirectoryPath(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(requireNotNull(url) { "Documents directory unavailable" }.path)
}

/** An absolute path inside [documentsDirectoryPath]. */
fun documentsFilePath(name: String): String = documentsDirectoryPath() + "/" + name
