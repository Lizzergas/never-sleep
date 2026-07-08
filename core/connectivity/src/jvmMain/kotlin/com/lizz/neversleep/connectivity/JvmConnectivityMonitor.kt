package com.lizz.neversleep.connectivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration.Companion.milliseconds

private const val STOP_TIMEOUT_MS = 5_000L
private const val DEFAULT_POLL_INTERVAL_MS = 5_000L
private const val CONNECT_TIMEOUT_MS = 1_500
private const val PROBE_HOST = "1.1.1.1"
private const val PROBE_PORT = 53

/**
 * Desktop has no system connectivity callback — poll reachability of a
 * well-known endpoint instead.
 */
class JvmConnectivityMonitor(
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : ConnectivityMonitor {
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isOnline: Flow<Boolean> = flow {
        while (true) {
            emit(probe())
            delay(pollIntervalMs.milliseconds)
        }
    }.distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        // One upstream regardless of collector count: the shell banner and
        // any screen share a single poll loop.
        .shareIn(monitorScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    private suspend fun probe(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use {
                    it.connect(InetSocketAddress(PROBE_HOST, PROBE_PORT), CONNECT_TIMEOUT_MS)
                    true
                }
            } catch (_: IOException) {
                false
            }
        }
}

actual val connectivityPlatformKoinModule: Module = module {
    single<ConnectivityMonitor> { JvmConnectivityMonitor() }
}
