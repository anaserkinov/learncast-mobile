package me.anasmusa.learncast.data.local

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getDataStore() = DataStoreFactory.create(
    storage = OkioStorage(
        fileSystem = FileSystem.SYSTEM,
        producePath = {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            (requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME").toPath()
        },
        serializer = PreferenceSerializer,
    )
)