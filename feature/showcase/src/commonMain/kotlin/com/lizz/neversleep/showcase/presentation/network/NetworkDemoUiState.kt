package com.lizz.neversleep.showcase.presentation.network

import com.lizz.neversleep.model.Item
import com.lizz.neversleep.ui.UiState

data class NetworkDemoUiState(
    val items: UiState<List<Item>>? = null,
)

sealed interface NetworkDemoEvent {
    data object Load : NetworkDemoEvent
}
