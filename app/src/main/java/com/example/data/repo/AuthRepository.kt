package com.example.data.repo

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.AppThemePreference
import com.example.data.model.User
import com.example.data.sync.UserProfileStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    companion object {
        private const val TAG = "ExpenseTracker.Auth"
    }

    private val firebaseAuth = runCatching {
        FirebaseAuth.getInstance()
    }.getOrElse {
        Log.w(TAG, "Firebase Auth unavailable: ${it.message}")
        null
    }
    private val userProfileStore = runCatching {
        UserProfileStore(FirebaseFirestore.getInstance())
    }.getOrElse {
        Log.w(TAG, "Firestore unavailable for profile/theme sync: ${it.message}")
        null
    }
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    private val _themePreference = MutableStateFlow(AppThemePreference.LIGHT)
    val themePreference: StateFlow<AppThemePreference> = _themePreference.asStateFlow()
    private val _isThemePreferenceLoading = MutableStateFlow(false)
    val isThemePreferenceLoading: StateFlow<Boolean> = _isThemePreferenceLoading.asStateFlow()

    init {
        val auth = firebaseAuth
        if (auth == null) {
            _currentUser.value = null
            _themePreference.value = AppThemePreference.LIGHT
            _isThemePreferenceLoading.value = false
        } else {
            // Restore Firebase auth state on creation
            val fbUser = auth.currentUser
            _currentUser.value = fbUser?.toUser()
            fbUser?.let { persistProfileAsync(it) }
            loadThemePreferenceAsync(fbUser)

            // Listen for auth state changes
            auth.addAuthStateListener { authState ->
                val current = authState.currentUser
                _currentUser.value = current?.toUser()
                current?.let { persistProfileAsync(it) }
                loadThemePreferenceAsync(current)
            }
        }
    }

    /**
     * Launch Google Sign-In using Credential Manager API.
     * [activityContext] must be an Activity context for the Credential Manager UI.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<User> {
        return try {
            val auth = firebaseAuth
                ?: return Result.failure(IllegalStateException("Firebase Auth is unavailable"))
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
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("Firebase authentication failed"))

            // Store identity metadata only in Firestore users/{uid}; never in local DB.
            userProfileStore?.upsertProfileFromGoogle(
                uid = user.uid,
                displayName = authResult.user?.displayName,
                email = user.email
            )
            _themePreference.value = userProfileStore?.getThemePreference(user.uid) ?: AppThemePreference.LIGHT
            _isThemePreferenceLoading.value = false

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
            firebaseAuth?.signOut()
            _currentUser.value = null
            _themePreference.value = AppThemePreference.LIGHT
            _isThemePreferenceLoading.value = false
            Log.i(TAG, "✅ Sign-out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign-out failed", e)
            Result.failure(e)
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth?.currentUser

    suspend fun updateThemePreference(preference: AppThemePreference): Result<Unit> {
        val firebaseUser = firebaseAuth?.currentUser
            ?: return Result.failure(IllegalStateException("No authenticated user"))

        val previousPreference = _themePreference.value
        _themePreference.value = preference

        return try {
            val store = userProfileStore
                ?: return Result.failure(IllegalStateException("Firestore is unavailable"))
            store.updateThemePreference(firebaseUser.uid, preference)
            Result.success(Unit)
        } catch (e: Exception) {
            _themePreference.value = previousPreference
            Log.e(TAG, "❌ Failed to update theme preference", e)
            Result.failure(e)
        }
    }

    private fun persistProfileAsync(firebaseUser: FirebaseUser) {
        val email = firebaseUser.email ?: return
        repoScope.launch {
            try {
                userProfileStore?.upsertProfileFromGoogle(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName,
                    email = email
                )
            } catch (e: Exception) {
                Log.w(TAG, "Profile sync skipped: ${e.message}")
            }
        }
    }

    private fun loadThemePreferenceAsync(firebaseUser: FirebaseUser?) {
        if (firebaseUser == null) {
            _themePreference.value = AppThemePreference.LIGHT
            _isThemePreferenceLoading.value = false
            return
        }

        _isThemePreferenceLoading.value = true
        repoScope.launch {
            try {
                _themePreference.value = userProfileStore?.getThemePreference(firebaseUser.uid) ?: AppThemePreference.LIGHT
            } catch (e: Exception) {
                _themePreference.value = AppThemePreference.LIGHT
                Log.w(TAG, "Theme preference fetch failed; defaulting to light: ${e.message}")
            } finally {
                _isThemePreferenceLoading.value = false
            }
        }
    }

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
