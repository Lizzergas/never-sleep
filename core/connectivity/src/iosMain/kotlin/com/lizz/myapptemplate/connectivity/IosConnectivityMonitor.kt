package com.lizz.myapptemplate.connectivity

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

class IosConnectivityMonitor : ConnectivityMonitor {
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
}

actual val connectivityPlatformKoinModule: Module =
    module {
        single<ConnectivityMonitor> { IosConnectivityMonitor() }
    }
