package com.rimaro.musify.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val _searchbarFocused = MutableSharedFlow<Boolean>(replay = 1)
    val searchbarFocused = _searchbarFocused.asSharedFlow()

    fun triggerSearchbar() {
        val currentFocus = _searchbarFocused.replayCache.firstOrNull() ?: false
        viewModelScope.launch { _searchbarFocused.emit(!currentFocus) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consumeSearchbarFocused() {
        _searchbarFocused.resetReplayCache()
    }
}