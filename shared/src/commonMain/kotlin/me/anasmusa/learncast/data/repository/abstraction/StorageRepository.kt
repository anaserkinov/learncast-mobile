package me.anasmusa.learncast.data.repository.abstraction

interface StorageRepository {

    suspend fun getCacheSize(): Float
    suspend fun getDownloadSize(): Float

    suspend fun clearCaches(): Float
    suspend fun clearDownloads(): Float

}