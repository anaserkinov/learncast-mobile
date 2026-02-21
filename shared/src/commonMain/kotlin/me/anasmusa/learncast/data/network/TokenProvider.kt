package me.anasmusa.learncast.data.network

interface TokenProvider {
    fun getTokens(): Pair<String, String>?

    fun refreshTokens(refreshToken: String)
}
