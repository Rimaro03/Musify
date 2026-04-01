package com.rimaro.musify.ui.signin

import com.google.firebase.auth.FirebaseUser

sealed class SigninUiState {
    object Idle : SigninUiState()
    object Loading : SigninUiState()
    data class Success(val user: FirebaseUser?) : SigninUiState()
    data class Error(val message: String) : SigninUiState()
}