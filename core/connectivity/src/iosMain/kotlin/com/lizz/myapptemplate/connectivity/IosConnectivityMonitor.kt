package com.lizz.myapptemplate.connectivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

private const val STOP_TIMEOUT_MS = 5_000L

class IosConnectivityMonitor : ConnectivityMonitor {
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isOnline: Flow<Boolean> =
        callbackFlow {
            val monitor = nw_path_monitor_create()
            nw_path_monitor_set_update_handler(monitor) { path ->
                trySend(nw_path_get_status(path) == nw_path_status_satisfied)
            }
            nw_path_monitor_set_queue(
                monitor,
                dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u),
            )
            nw_path_monitor_start(monitor)
            awaitClose { nw_path_monitor_cancel(monitor) }
        }.distinctUntilChanged()
            // One upstream regardless of collector count: the shell banner and
            // any screen share a single poll loop / system callback.
            .shareIn(monitorScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)
}

actual val connectivityPlatformKoinModule: Module =
    module {
        single<ConnectivityMonitor> { IosConnectivityMonitor() }
    }
