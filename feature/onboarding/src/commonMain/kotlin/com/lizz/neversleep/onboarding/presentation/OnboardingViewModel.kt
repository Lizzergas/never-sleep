package com.lizz.neversleep.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizz.neversleep.onboarding.domain.OnboardingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface OnboardingEvent {
    data object Finish : OnboardingEvent
}

/** One-off effects: navigate only after the repository marks onboarding as seen. */
sealed interface OnboardingEffect {
    data object Done : OnboardingEffect
}

class OnboardingViewModel(
    private val repository: OnboardingRepository,
) : ViewModel() {
    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Finish ->
                viewModelScope.launch {
                    repository.markSeen()
                    _effects.send(OnboardingEffect.Done)
                }
        }
    }
}
