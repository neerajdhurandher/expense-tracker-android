package com.example.data.repo

import android.content.Context
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("expense_tracker_auth", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Load persist status on creation
        val uid = sharedPrefs.getString("uid", null)
        val email = sharedPrefs.getString("email", null)
        val displayName = sharedPrefs.getString("display_name", null)
        val photoUrl = sharedPrefs.getString("photo_url", null)

        if (uid != null && email != null && displayName != null) {
            _currentUser.value = User(
                uid = uid,
                displayName = displayName,
                email = email,
                photoUrl = photoUrl
            )
        }
    }

    suspend fun signIn(email: String, displayName: String, photoUrl: String? = null): Result<User> {
        return try {
            val uid = "user_" + email.hashCode().toString()
            val user = User(
                uid = uid,
                displayName = displayName,
                email = email,
                photoUrl = photoUrl
            )
            
            sharedPrefs.edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("display_name", displayName)
                .putString("photo_url", photoUrl)
                .apply()

            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(displayName: String, photoUrl: String? = null): Result<User> {
        return try {
            val current = _currentUser.value ?: return Result.failure(Exception("No user signed in"))
            val updated = current.copy(displayName = displayName, photoUrl = photoUrl ?: current.photoUrl)

            sharedPrefs.edit()
                .putString("display_name", updated.displayName)
                .putString("photo_url", updated.photoUrl)
                .apply()

            _currentUser.value = updated
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            sharedPrefs.edit().clear().apply()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
