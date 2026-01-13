package me.anasmusa.learncast.data.local.db

import androidx.room.TransactionScope
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

interface DBConnection {

    suspend fun <R> inWriteTransaction(run: suspend TransactionScope<R>.() -> R)

    suspend fun clearAllTables()
}

internal class DBConnectionImpl(
    private val db: AppDatabase
): DBConnection {
    override suspend fun <R> inWriteTransaction(run: suspend TransactionScope<R>.() -> R) {
        db.useWriterConnection {
            it.immediateTransaction(run)
        }
    }

    override suspend fun clearAllTables() {
        db.clearAllTables()
    }
}