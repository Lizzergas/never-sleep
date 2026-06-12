package com.lizz.myapptemplate.connectivity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

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
    override val isOnline: Flow<Boolean> =
        flow {
            while (true) {
                emit(probe())
                delay(pollIntervalMs)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)

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

actual val connectivityPlatformKoinModule: Module =
    module {
        single<ConnectivityMonitor> { JvmConnectivityMonitor() }
    }
