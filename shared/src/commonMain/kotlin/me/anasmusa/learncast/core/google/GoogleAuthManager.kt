package me.anasmusa.learncast.core.google

internal interface GoogleAuthManager {
    suspend fun signIn(): String?
}

internal expect fun createGoogleAuthManager(): GoogleAuthManager
