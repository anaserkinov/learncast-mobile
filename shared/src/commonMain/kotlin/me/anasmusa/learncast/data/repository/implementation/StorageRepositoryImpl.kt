package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.data.local.storage.StorageManager
import me.anasmusa.learncast.data.repository.abstraction.StorageRepository

internal class StorageRepositoryImpl(
    private val storageManager: StorageManager,
) : StorageRepository {
    override suspend fun getCacheSize(): Float =
        try {
            storageManager.getCacheSize()
        } catch (e: Exception) {
            0f
        }

    override suspend fun getDownloadSize(): Float =
        try {
            storageManager.getDownloadSize()
        } catch (e: Exception) {
            0f
        }

    override suspend fun clearCaches(): Float =
        withContext(Dispatchers.IO) {
            try {
                storageManager.clearCaches()
                storageManager.getCacheSize()
            } catch (e: Exception) {
                storageManager.getCacheSize()
            }
        }

    override suspend fun clearDownloads(): Float =
        withContext(Dispatchers.IO) {
            try {
                storageManager.clearDownloads()
                storageManager.getDownloadSize()
            } catch (e: Exception) {
                storageManager.getDownloadSize()
            }
        }
}
