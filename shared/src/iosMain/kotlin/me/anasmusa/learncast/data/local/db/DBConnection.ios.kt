package me.anasmusa.learncast.data.local.db

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

internal class IosDBConnectionImpl(
    db: AppDatabase,
) : DBConnectionImpl(db) {
    override suspend fun clearAllTables() {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val tableNames = arrayListOf<String>()
                usePrepared(
                    """
                    SELECT name FROM sqlite_master
                    WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'
                    """.trimIndent(),
                ) {
                    while (it.step()) {
                        tableNames.add(it.getText(0))
                    }
                }
                tableNames.forEach {
                    execSQL("DELETE FROM $it")
                }
            }
        }
    }
}

internal actual fun createDBConnection(db: AppDatabase): DBConnection = IosDBConnectionImpl(db)
