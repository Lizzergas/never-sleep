package com.lizz.myapptemplate.common

import java.io.File

/**
 * Per-user app data directory on desktop (`~/.myapptemplate`), created on
 * first use. Every module that persists files on the JVM target resolves
 * paths through here so the app's footprint stays in one folder.
 */
fun appStorageDir(): File = File(System.getProperty("user.home"), ".myapptemplate").apply { mkdirs() }

/** A file inside [appStorageDir]. */
fun appStorageFile(name: String): File = File(appStorageDir(), name)
