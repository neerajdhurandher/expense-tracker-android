package com.example.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repo.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser

    /**
     * Trigger Google Sign-In flow using Credential Manager.
     * [activityContext] is required for the Credential Manager to show the Google account picker.
     */
    fun signInWithGoogle(activityContext: Context, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activityContext)
            onResult(result)
        }
    }

    fun signOut(onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = authRepository.signOut()
            onResult(result)
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
