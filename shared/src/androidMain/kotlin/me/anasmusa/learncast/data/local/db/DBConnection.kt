package me.anasmusa.learncast.data.local.db

internal class AndroidDBConnectionImpl(
    db: AppDatabase,
) : DBConnectionImpl(db) {
    override suspend fun clearAllTables() {
        db.clearAllTables()
    }
}

internal actual fun createDBConnection(db: AppDatabase): DBConnection = AndroidDBConnectionImpl(db)
