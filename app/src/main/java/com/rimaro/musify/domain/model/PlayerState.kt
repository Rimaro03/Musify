package com.rimaro.musify.domain.model

sealed class PlayerState {
    object Idle : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    data class Error(val message: String) : PlayerState()
}