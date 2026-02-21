package me.anasmusa.learncast.core.google

interface GoogleAuthManager {
    suspend fun signIn(): String?
}
