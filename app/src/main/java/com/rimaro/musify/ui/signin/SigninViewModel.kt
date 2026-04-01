package com.rimaro.musify.ui.signin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SigninViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application)   {
    private val _uiState = MutableStateFlow<SigninUiState>(SigninUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val auth = Firebase.auth

    fun signin(email: String, password: String) {
        _uiState.value = SigninUiState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _uiState.value = SigninUiState.Success(user)
                } else {
                    _uiState.value = SigninUiState.Error("Authentication failed")
                }
            }
    }
}