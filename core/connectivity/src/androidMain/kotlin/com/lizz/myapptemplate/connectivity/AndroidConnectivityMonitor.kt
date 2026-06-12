package com.lizz.myapptemplate.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Requires android.permission.ACCESS_NETWORK_STATE (declared in androidApp). */
class AndroidConnectivityMonitor(
    private val context: Context,
) : ConnectivityMonitor {
    override val isOnline: Flow<Boolean> =
        callbackFlow {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            fun currentlyOnline(): Boolean {
                val capabilities = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
                return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            }

            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(true)
                    }

                    override fun onLost(network: Network) {
                        trySend(currentlyOnline())
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                    }
                }

            trySend(currentlyOnline())
            manager.registerDefaultNetworkCallback(callback)
            awaitClose { manager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()
}

actual val connectivityPlatformKoinModule: Module =
    module {
        single<ConnectivityMonitor> { AndroidConnectivityMonitor(androidContext()) }
    }
