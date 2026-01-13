package me.anasmusa.learncast.data.local.storage

interface StorageManager {
    suspend fun getCacheSize(): Float
    suspend fun getDownloadSize(): Float
    suspend fun clearCaches()
    suspend fun clearHttpCaches()
    suspend fun clearDownloads()
}

expect fun createStorageManager(): StorageManager