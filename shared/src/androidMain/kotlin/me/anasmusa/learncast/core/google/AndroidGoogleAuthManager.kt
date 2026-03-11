package me.anasmusa.learncast.core.google

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.core.appConfig

internal class AndroidGoogleAuthManager(
    private val context: Context,
) : GoogleAuthManager {
    private fun handleSignIn(result: GetCredentialResponse): String? {
        val credential = result.credential
        return if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return googleIdTokenCredential.idToken
        } else {
            null
        }
    }

    override suspend fun signIn(): String? {
        val activity = ApplicationLoader.currentActivity ?: return null
        return signIn(activity, true)
    }

    private suspend fun signIn(
        activity: Activity,
        firstTry: Boolean,
    ): String? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setServerClientId(appConfig.googleClientId)
                .setAutoSelectEnabled(true)
                .setFilterByAuthorizedAccounts(true)
                .build()

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(googleIdOption)
                .build()

        return try {
            val result =
                credentialManager.getCredential(
                    context = activity,
                    request = request,
                )

            handleSignIn(result)
        } catch (e: NoCredentialException) {
            try {
                val signInWithGoogleOption =
                    GetSignInWithGoogleOption
                        .Builder(
                            serverClientId = appConfig.googleClientId,
                        ).build()
                val request =
                    GetCredentialRequest
                        .Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()
                credentialManager.getCredential(
                    context = activity,
                    request = request,
                )
                if (firstTry) {
                    signIn(activity, false)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } catch (e: GetCredentialException) {
            null
        }
    }

    override suspend fun signOut() {
        CredentialManager
            .create(context)
            .clearCredentialState(
                ClearCredentialStateRequest(),
            )
    }
}
