package com.rimaro.musify.ui.signup

import com.google.firebase.auth.FirebaseUser

sealed class SignupUiState {
    object Idle : SignupUiState()
    object Loading : SignupUiState()
    data class Success(val user: FirebaseUser?) : SignupUiState()
    data class Error(val message: String) : SignupUiState()
}