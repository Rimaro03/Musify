package com.rimaro.musify.ui.common

sealed class PlayButtonState {
    object Idle : PlayButtonState()
    object Buffering : PlayButtonState()
    object PlayingThis : PlayButtonState()
    object PlayingOther : PlayButtonState()
}