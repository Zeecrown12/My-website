package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.auth.AuthValidation
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthManager {
    private const val TAG = "Auth"
    const val BOOTSTRAPPED_ADMIN_EMAIL = "olaademi.ZO@gmail.com"

    fun isBootstrappedAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.isEmailVerified &&
            user.email?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true
    }

    fun hasPasswordProvider(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }

    fun authStateFlow(auth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun registerWithEmailPassword(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
    ): Result<FirebaseUser> = try {
        val fullName = "${firstName.trim()} ${lastName.trim()}".trim()
        val result = Firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = checkNotNull(result.user) { "Account created but user session unavailable." }
        runCatching {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await()
        }
        runCatching {
            user.sendEmailVerification().await()
        }.onFailure { err ->
            Log.w(TAG, "Verification email dispatch skipped/rate-limited: ${err.message}")
        }
        Result.success(user)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Email registration failed", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = false)))
    }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = try {
        val result = Firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = checkNotNull(result.user) { "Signed in but user session unavailable." }
        Result.success(user)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Email sign-in failed", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = true)))
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        Firebase.auth.sendPasswordResetEmail(email.trim()).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Password reset error", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = false)))
    }

    suspend fun resendVerificationEmail(): Result<Unit> = try {
        val user = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("Please sign in first."))
        user.sendEmailVerification().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Verification resend failed", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = false)))
    }

    suspend fun reloadCurrentUser(): Result<FirebaseUser?> = try {
        val user = Firebase.auth.currentUser
        user?.reload()?.await()
        Result.success(Firebase.auth.currentUser)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "User reload failed", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = false)))
    }

    suspend fun updateDisplayNameAndPhoto(displayName: String? = null, photoUrl: String? = null): Result<Unit> = try {
        val user = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("Not signed in."))
        val builder = UserProfileChangeRequest.Builder()
        if (!displayName.isNullOrBlank()) builder.setDisplayName(displayName.trim())
        if (photoUrl != null) {
            builder.setPhotoUri(if (photoUrl.isBlank()) null else Uri.parse(photoUrl))
        }
        user.updateProfile(builder.build()).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e)))
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = try {
        val user = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("Please sign in first."))
        val email = user.email?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Account email is unavailable."))
        if (!hasPasswordProvider(user)) {
            return Result.failure(
                IllegalStateException("This account uses Google Sign-In. Use Forgot Password on the Sign-In screen if you want to add an email password.")
            )
        }
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Password update failed", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = true)))
    }

    suspend fun requestEmailChange(currentPassword: String, newEmail: String): Result<Unit> = try {
        val user = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("Please sign in first."))
        val existingEmail = user.email?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Current email unavailable."))
        if (hasPasswordProvider(user)) {
            require(currentPassword.isNotBlank()) { "Please enter your current password to confirm email change." }
            user.reauthenticate(EmailAuthProvider.getCredential(existingEmail, currentPassword)).await()
        }
        user.verifyBeforeUpdateEmail(newEmail.trim()).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Email change request failed", e)
        Result.failure(IllegalStateException(AuthValidation.mapFirebaseAuthError(e, isSignInFlow = false)))
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope,
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {},
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing.")
            return
        }
        val activity = context as? Activity
        if (activity == null) {
            onAuthError("Unable to start Google Sign-In from this context.")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {},
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
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
}
