package com.lizz.neversleep.connectivity

import kotlinx.coroutines.flow.Flow
import org.koin.core.module.Module

/**
 * Online/offline awareness for the whole app. Collected by the app shell
 * (offline banner) and by screens that retry on reconnect.
 *
 * Platform implementations: ConnectivityManager callbacks (Android),
 * NWPathMonitor (iOS), socket-reachability polling (desktop JVM).
 */
interface ConnectivityMonitor {
    /** Emits on every connectivity change; distinct values only. */
    val isOnline: Flow<Boolean>
}

/** Provides [ConnectivityMonitor] as a singleton per platform. */
expect val connectivityPlatformKoinModule: Module
