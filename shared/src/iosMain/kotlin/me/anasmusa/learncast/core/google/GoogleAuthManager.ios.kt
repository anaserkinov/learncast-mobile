package me.anasmusa.learncast.core.google

private class IosGoogleAuthManager : GoogleAuthManager {
    override suspend fun signIn(): String? {
        TODO("Not yet implemented")
    }
}

internal actual fun createGoogleAuthManager(): GoogleAuthManager = IosGoogleAuthManager()
