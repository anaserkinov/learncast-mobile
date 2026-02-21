package me.anasmusa.learncast.core.player.avplayer.cache

import androidx.room.execSQL
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteStatement
import me.anasmusa.learncast.data.local.db.AppDatabase

class MetadataIndex(
    private val database: AppDatabase,
) {
    suspend fun insert(metadata: Metadata) {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.usePrepared(
                """
                INSERT OR REPLACE INTO ${AVCache.TABLE_METADATA} (key, contentLength, contentType)
                VALUES (?, ?, ?)
                """.trimIndent(),
            ) {
                it.bindText(1, metadata.key)
                it.bindLong(2, metadata.contentLength)
                it.bindText(3, metadata.contentType)
                it.step()
            }
        }
    }

    suspend fun get(key: String): Metadata? {
        AVCache.ensureInitialized()
        return database.useReaderConnection {
            it.usePrepared("SELECT * FROM ${AVCache.TABLE_METADATA} WHERE key = ?") {
                it.bindText(1, key)
                if (it.step()) {
                    it.toMetadata()
                } else {
                    null
                }
            }
        }
    }

    suspend fun clear() {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.execSQL("DELETE FROM ${AVCache.TABLE_METADATA}")
        }
    }

    private fun SQLiteStatement.toMetadata() =
        Metadata(
            key = getText(0),
            contentLength = getLong(1),
            contentType = getText(2),
        )
}
