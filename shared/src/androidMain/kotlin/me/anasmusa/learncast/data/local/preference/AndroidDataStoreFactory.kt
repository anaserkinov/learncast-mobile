package me.anasmusa.learncast.data.local.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import me.anasmusa.learncast.PreferenceData
import me.anasmusa.learncast.data.local.PreferenceSerializer
import okio.FileSystem
import okio.Path.Companion.toPath

class AndroidDataStoreFactory(
    private val context: Context,
) : DataStoreFactory {
    override fun create(): DataStore<PreferenceData> =
        androidx.datastore.core.DataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    producePath = {
                        context.filesDir
                            .resolve(DATA_STORE_FILE_NAME)
                            .absolutePath
                            .toPath()
                    },
                    serializer = PreferenceSerializer,
                ),
        )
}
