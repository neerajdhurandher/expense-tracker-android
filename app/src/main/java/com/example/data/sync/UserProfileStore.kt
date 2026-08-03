package com.example.data.sync

import com.example.data.model.AppThemePreference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Persists user metadata only in Firestore at users/{uid}.
 */
class UserProfileStore(private val firestore: FirebaseFirestore) {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    suspend fun upsertProfileFromGoogle(uid: String, displayName: String?, email: String) {
        val now = System.currentTimeMillis()
        val (firstName, lastName) = splitName(displayName, email)

        firestore.runTransaction { transaction ->
            val docRef = userDoc(uid)
            val snapshot = transaction.get(docRef)
            val joinedAt = snapshot.getLong("joinedAt") ?: now
            val themePreference = snapshot.getString("themePreference") ?: AppThemePreference.LIGHT.storageValue

            val profileData = mapOf(
                "uid" to uid,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "joinedAt" to joinedAt,
                "themePreference" to themePreference,
                "updatedAt" to now
            )

            transaction.set(docRef, profileData, SetOptions.merge())
        }.await()
    }

    suspend fun updateLastSyncAt(uid: String, timestamp: Long = System.currentTimeMillis()) {
        userDoc(uid)
            .set(
                mapOf(
                    "lastSyncAt" to timestamp,
                    "updatedAt" to timestamp
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun getThemePreference(uid: String): AppThemePreference {
        val snapshot = userDoc(uid).get().await()
        return AppThemePreference.fromStorage(snapshot.getString("themePreference"))
    }

    suspend fun updateThemePreference(uid: String, preference: AppThemePreference) {
        val now = System.currentTimeMillis()
        userDoc(uid)
            .set(
                mapOf(
                    "themePreference" to preference.storageValue,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun splitName(displayName: String?, email: String): Pair<String, String> {
        val trimmedName = displayName.orEmpty().trim()
        if (trimmedName.isNotEmpty()) {
            val parts = trimmedName.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val first = parts.firstOrNull().orEmpty()
            val last = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            return first to last
        }

        val fallback = email.substringBefore("@").trim()
        return fallback to ""
    }
}

