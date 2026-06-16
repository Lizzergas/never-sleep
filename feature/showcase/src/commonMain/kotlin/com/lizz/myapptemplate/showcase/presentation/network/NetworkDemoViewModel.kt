package com.lizz.myapptemplate.showcase.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.myapptemplate.connectivity.ConnectivityMonitor
import com.lizz.myapptemplate.model.AppError
import com.lizz.myapptemplate.model.Item
import com.lizz.myapptemplate.network.safeGet
import com.lizz.myapptemplate.ui.UiState
import com.lizz.myapptemplate.ui.toUiState
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkDemoViewModel(
    private val httpClient: HttpClient,
    connectivityMonitor: ConnectivityMonitor?,
) : ViewModel() {
    private val local = MutableStateFlow(NetworkDemoUiState())
    val state: StateFlow<NetworkDemoUiState> = local.asStateFlow()

    init {
        connectivityMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isOnline.collect { online ->
                    if (online && local.value.items.failedOnNetwork()) {
                        load()
                    }
                }
            }
        }
    }

    fun onEvent(event: NetworkDemoEvent) {
        when (event) {
            NetworkDemoEvent.Load -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            local.update { it.copy(items = UiState.Loading) }
            local.update {
                it.copy(
                    items = httpClient.safeGet<List<Item>>("/api/items").toUiState { items ->
                        items.isEmpty()
                    },
                )
            }
        }
    }

    private fun UiState<List<Item>>?.failedOnNetwork(): Boolean =
        this is UiState.Error && (error is AppError.Network || error is AppError.Timeout)
}
