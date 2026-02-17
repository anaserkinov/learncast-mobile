package me.anasmusa.learncast.core.player.avplayer.cache

import androidx.room.execSQL
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteStatement
import me.anasmusa.learncast.data.local.db.AppDatabase
import kotlin.time.Clock

class CacheIndex(
    private val name: String,
    private val database: AppDatabase,
) {
    suspend fun insert(span: CacheSpan) {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.usePrepared(
                """
                INSERT OR REPLACE INTO $name (key, startOffset, endOffset, filePath, lastAccessedAt)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
            ) {
                it.bindText(1, span.key)
                it.bindLong(2, span.startOffset)
                it.bindLong(3, span.endOffset)
                it.bindText(4, span.filePath)
                it.bindLong(5, span.lastAccessedAt)
                it.step()
            }
        }
    }

    suspend fun get(key: String): CacheSpan? {
        AVCache.ensureInitialized()
        return database.useReaderConnection {
            it.usePrepared("SELECT * FROM $name WHERE key = ?") {
                it.bindText(1, key)
                if (it.step()) {
                    it.toCacheSpan()
                } else {
                    null
                }
            }
        }
    }

    suspend fun getRanges(
        key: String,
        from: Long,
        to: Long,
    ): List<CacheSpan> {
        AVCache.ensureInitialized()
        return database.useReaderConnection {
            // Find all spans that intersect with [from, to]
            // A span intersects if: span.start <= requestEnd AND span.end >= requestStart
            it.usePrepared("SELECT * FROM $name WHERE key = ? AND startOffset <= ? AND endOffset >= ?") {
                it.bindText(1, key)
                it.bindLong(2, to) // span must start before or at requested end
                it.bindLong(3, from) // span must end after or at requested start

                val list = ArrayList<CacheSpan>()
                while (it.step()) {
                    list.add(it.toCacheSpan())
                }
                list
            }
        }
    }

    suspend fun touch(span: CacheSpan) {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.usePrepared(
                "UPDATE $name SET lastAccessedAt = ? WHERE key = ? AND startOffset = ? AND endOffset = ?",
            ) {
                it.bindLong(1, Clock.System.now().toEpochMilliseconds())
                it.bindText(2, span.key)
                it.bindLong(3, span.startOffset)
                it.bindLong(4, span.endOffset)
                it.step()
            }
        }
    }

    suspend fun delete(key: String) {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.usePrepared(
                "DELETE FROM $name WHERE key = ?",
            ) {
                it.bindText(1, key)
                it.step()
            }
        }
    }

    suspend fun delete(span: CacheSpan) {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.usePrepared(
                "DELETE FROM $name WHERE key = ? AND startOffset = ? AND endOffset = ?",
            ) {
                it.bindText(1, span.key)
                it.bindLong(2, span.startOffset)
                it.bindLong(3, span.endOffset)
                it.step()
            }
        }
    }

    suspend fun clear() {
        AVCache.ensureInitialized()
        database.useWriterConnection {
            it.execSQL("DELETE FROM $name")
        }
    }

    suspend fun getOldestCacheEntries(limit: Int): List<CacheSpan> {
        AVCache.ensureInitialized()
        return database.useReaderConnection {
            it.usePrepared("SELECT * FROM $name ORDER BY lastAccessedAt ASC LIMIT ?") {
                it.bindInt(1, limit)

                val list = ArrayList<CacheSpan>()
                while (it.step()) {
                    list.add(it.toCacheSpan())
                }
                list
            }
        }
    }

    suspend fun getTotalLength(): Long {
        AVCache.ensureInitialized()
        return database.useReaderConnection {
            it.usePrepared("SELECT SUM(endOffset - startOffset + 1) FROM $name") {
                if (it.step()) {
                    it.getLong(0)
                } else {
                    0L
                }
            }
        }
    }

    private fun SQLiteStatement.toCacheSpan() =
        CacheSpan(
            key = getText(0),
            startOffset = getLong(1),
            endOffset = getLong(2),
            filePath = getText(3),
            lastAccessedAt = getLong(4),
        )
}
