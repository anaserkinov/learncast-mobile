package me.anasmusa.learncast.data.local.db

internal class AndroidDBConnectionImpl(
    db: AppDatabase,
) : DBConnectionImpl(db) {
    override suspend fun clearAllTables() {
        TODO()
    }
}

internal actual fun createDBConnection(db: AppDatabase): DBConnection = AndroidDBConnectionImpl(db)
