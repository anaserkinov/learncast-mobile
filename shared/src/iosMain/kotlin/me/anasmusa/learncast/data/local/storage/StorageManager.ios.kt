package me.anasmusa.learncast.data.local.storage

class IosStorageManager : StorageManager {
    override suspend fun getCacheSize(): Float {
        TODO("Not yet implemented")
    }

    override suspend fun getDownloadSize(): Float {
        TODO("Not yet implemented")
    }

    override suspend fun clearCaches() {
        TODO("Not yet implemented")
    }

    override suspend fun clearDownloads() {
        TODO("Not yet implemented")
    }
}

actual fun createStorageManager(): StorageManager = IosStorageManager()
