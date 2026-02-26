package me.anasmusa.learncast.data.local.preference

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import kotlinx.cinterop.ExperimentalForeignApi
import me.anasmusa.learncast.PreferenceData
import me.anasmusa.learncast.data.local.PreferenceSerializer
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

class IosDataStoreFactory : DataStoreFactory {
    @OptIn(ExperimentalForeignApi::class)
    override fun create(): DataStore<PreferenceData> =
        androidx.datastore.core.DataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    producePath = {
                        val documentDirectory =
                            NSFileManager.defaultManager.URLForDirectory(
                                directory = NSDocumentDirectory,
                                inDomain = NSUserDomainMask,
                                appropriateForURL = null,
                                create = false,
                                error = null,
                            )
                        (requireNotNull(documentDirectory).path + "/${DATA_STORE_FILE_NAME}").toPath()
                    },
                    serializer = PreferenceSerializer,
                ),
        )
}
