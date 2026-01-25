package me.anasmusa.learncast.data.local.preference

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.data.local.PreferenceSerializer
import okio.FileSystem
import okio.Path.Companion.toPath

actual fun getDataStore() =
    DataStoreFactory.create(
        storage =
            OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                producePath = {
                    ApplicationLoader.context.filesDir
                        .resolve(DATA_STORE_FILE_NAME)
                        .absolutePath
                        .toPath()
                },
                serializer = PreferenceSerializer,
            ),
    )
