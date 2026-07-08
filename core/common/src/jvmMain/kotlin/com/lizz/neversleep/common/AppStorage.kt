package com.lizz.neversleep.common

import java.io.File

/**
 * Per-user app data directory on desktop (`~/.neversleep`), created on
 * first use. Every module that persists files on the JVM target resolves
 * paths through here so the app's footprint stays in one folder.
 */
fun appStorageDir(): File = File(System.getProperty("user.home"), ".neversleep").apply { mkdirs() }

/** A file inside [appStorageDir]. */
fun appStorageFile(name: String): File = File(appStorageDir(), name)
