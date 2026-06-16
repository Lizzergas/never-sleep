package com.lizz.myapptemplate.showcase.presentation.network

import com.lizz.myapptemplate.model.Item
import com.lizz.myapptemplate.ui.UiState

data class NetworkDemoUiState(
    val items: UiState<List<Item>>? = null,
)

sealed interface NetworkDemoEvent {
    data object Load : NetworkDemoEvent
}
