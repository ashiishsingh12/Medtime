package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.notifications.MedicationReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "EmailAuth"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

// ईमेल और पासवर्ड से नया अकाउंट बनाने के लिए (Sign Up)
suspend fun signUpWithEmail(email: String, pass: String): Result<Boolean> {
    return try {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).await()
        Result.success(true)
    } catch (e: Exception) {
        Log.e(TAG, "Sign up failed: ${e.message}", e)
        Result.failure(e)
    }
}

// पहले से बने अकाउंट से लॉगिन करने के लिए (Sign In)
suspend fun signInWithEmail(email: String, pass: String): Result<Boolean> {
    return try {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass).await()
        Result.success(true)
    } catch (e: Exception) {
        Log.e(TAG, "Sign in failed: ${e.message}", e)
        Result.failure(e)
    }
}

fun signOutWithCleanup(
    appContext: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                MedicationReminderScheduler.cancelAllAlarms(appContext)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
