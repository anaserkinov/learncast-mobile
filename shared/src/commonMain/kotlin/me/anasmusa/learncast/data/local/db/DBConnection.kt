package me.anasmusa.learncast.data.local.db

import androidx.room.TransactionScope
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

internal interface DBConnection {
    suspend fun <R> inWriteTransaction(run: suspend TransactionScope<R>.() -> R)

    suspend fun clearAllTables()
}

internal abstract class DBConnectionImpl(
    protected val db: AppDatabase,
) : DBConnection {
    override suspend fun <R> inWriteTransaction(run: suspend TransactionScope<R>.() -> R) {
        db.useWriterConnection {
            it.immediateTransaction(run)
        }
    }
}

internal expect fun createDBConnection(db: AppDatabase): DBConnection
