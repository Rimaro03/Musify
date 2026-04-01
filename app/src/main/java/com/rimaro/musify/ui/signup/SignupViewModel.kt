package com.rimaro.musify.ui.signup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val auth = Firebase.auth

    /**
     * Creates a new user with the given email and password.
     * If the username assignment fails, the user is deleted.
     */
    fun signUp(username: String, email: String, password: String) {
        _uiState.value = SignupUiState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = username
                    }

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d(TAG, "createUserWithEmail:success")
                                Log.d(TAG, "${user.email} ${user.displayName}" )
                                _uiState.value = SignupUiState.Success(user)
                            } else {
                                // remove user
                                user.delete()
                                Log.w(TAG, "createUserWithEmail:failure", profileTask.exception)
                                _uiState.value = SignupUiState.Error("Authentication failed")
                            }
                        }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    if(task.exception is FirebaseAuthUserCollisionException) {
                        _uiState.value = SignupUiState.Error("Email already registered")
                    } else {
                        _uiState.value = SignupUiState.Error("Authentication failed")
                    }
                }
            }
    }

    companion object {
        private const val TAG = "SignupEmailViewmodel"
    }
}