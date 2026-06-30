package com.example.data.repo

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    companion object {
        private const val TAG = "ExpenseTracker.Auth"
    }

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Restore Firebase auth state on creation
        val fbUser = firebaseAuth.currentUser
        _currentUser.value = fbUser?.toUser()

        // Listen for auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser?.toUser()
        }
    }

    /**
     * Launch Google Sign-In using Credential Manager API.
     * [activityContext] must be an Activity context for the Credential Manager UI.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<User> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getWebClientId())
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Authenticate with Firebase
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("Firebase authentication failed"))

            Log.i(TAG, "✅ Google Sign-In successful: ${user.email}")
            _currentUser.value = user
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user")
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            _currentUser.value = null
            Log.i(TAG, "✅ Sign-out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign-out failed", e)
            Result.failure(e)
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth.currentUser

    private fun FirebaseUser.toUser(): User = User(
        uid = uid,
        displayName = displayName ?: email ?: "User",
        email = email ?: "",
        photoUrl = photoUrl?.toString()
    )

    /**
     * Get the Web Client ID from resources.
     * This is auto-populated by google-services.json → R.string.default_web_client_id.
     */
    private fun getWebClientId(): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            // Fallback — should never happen if google-services.json is correctly set up
            throw IllegalStateException("default_web_client_id not found. Ensure google-services.json is in app/ directory.")
        }
    }
}
