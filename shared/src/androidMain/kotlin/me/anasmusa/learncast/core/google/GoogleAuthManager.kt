package me.anasmusa.learncast.core.google

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

internal class AndroidGoogleAuthManager : GoogleAuthManager {
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
        val credentialManager = CredentialManager.create(ApplicationLoader.context)
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setServerClientId(appConfig.googleClientId)
                .build()

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(googleIdOption)
                .build()

        return try {
            val result =
                credentialManager.getCredential(
                    context = ApplicationLoader.currentActivity!!,
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
                val result =
                    CredentialManager
                        .create(ApplicationLoader.context)
                        .getCredential(
                            context = ApplicationLoader.currentActivity!!,
                            request = request,
                        )
                handleSignIn(result)
            } catch (e: Exception) {
                null
            }
        } catch (e: GetCredentialException) {
            null
        }
    }
}

internal actual fun createGoogleAuthManager(): GoogleAuthManager = AndroidGoogleAuthManager()
