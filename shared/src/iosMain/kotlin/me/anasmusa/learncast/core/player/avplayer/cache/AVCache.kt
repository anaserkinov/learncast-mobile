package me.anasmusa.learncast.core.player.avplayer.cache

import androidx.room.TransactionScope
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import me.anasmusa.learncast.data.local.db.AppDatabase
import org.koin.mp.KoinPlatform

object AVCache {
    private const val VERSION = 1L

    private const val TABLE_VERSION = "ios_version_table"
    const val TABLE_METADATA = "av_metadata"
    const val TABLE_CACHE = "av_cache"
    const val TABLE_DOWNLOAD = "av_download"

    private var isInitialized = false

    suspend fun ensureInitialized() {
        if (isInitialized) return
        val database = KoinPlatform.getKoin().get<AppDatabase>()
        database.useWriterConnection {
            if (isInitialized) return@useWriterConnection
            isInitialized = true
            it.immediateTransaction {
                execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_VERSION(
                        name TEXT NOT NULL PRIMARY KEY,
                        version INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                val version = getVersion("av_player_cache")

                if (VERSION != version) {
                    when (VERSION) {
                        1L -> {
                            execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS $TABLE_METADATA(
                                    key TEXT NOT NULL PRIMARY KEY,
                                    contentLength INTEGER NOT NULL,
                                    contentType TEXT NOT NULL
                                )
                                """.trimIndent(),
                            )

                            listOf(TABLE_CACHE, TABLE_DOWNLOAD).forEach { name ->
                                execSQL(
                                    """
                                    CREATE TABLE IF NOT EXISTS $name(
                                        key TEXT NOT NULL,
                                        startOffset INTEGER NOT NULL,
                                        endOffset INTEGER NOT NULL,
                                        filePath TEXT NOT NULL,
                                        lastAccessedAt INTEGER NOT NULL,
                                        PRIMARY KEY (key, startOffset, endOffset)
                                    )
                                    """.trimIndent(),
                                )
                            }

                            updateVersion("av_player_cache", VERSION)
                        }
                    }
                }
            }
        }
    }

    private suspend fun TransactionScope<Unit>.getVersion(name: String): Long =
        usePrepared("SELECT version FROM $TABLE_VERSION WHERE name = ?") {
            it.bindText(1, name)
            if (it.step()) {
                it.getLong(0)
            } else {
                -1
            }
        }

    private suspend fun TransactionScope<Unit>.updateVersion(
        name: String,
        version: Long,
    ) {
        usePrepared(
            """
            INSERT INTO $TABLE_VERSION (name, version)
            VALUES(?, ?)
            ON CONFLICT(name) DO UPDATE SET
                version = excluded.version
            """.trimIndent(),
        ) {
            it.bindText(1, name)
            it.bindLong(2, version)
            it.step()
        }
    }
}
