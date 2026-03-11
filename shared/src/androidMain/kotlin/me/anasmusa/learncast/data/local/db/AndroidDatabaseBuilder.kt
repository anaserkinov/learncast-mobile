package me.anasmusa.learncast.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.SupportSQLiteConnection
import org.koin.mp.KoinPlatform

class AndroidDatabaseBuilder(
    private val context: Context,
) : DatabaseBuilder {
    override fun get(): RoomDatabase.Builder<AppDatabase> {
        val database = KoinPlatform.getKoin().get<SQLiteDatabase>()
        return Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = context.getDatabasePath("app.db").absolutePath,
            ).setDriver(
                object : SQLiteDriver {
                    override val hasConnectionPool: Boolean
                        get() = true

                    override fun open(fileName: String): SQLiteConnection =
                        SupportSQLiteConnection(
                            FrameworkSQLiteDatabase(database),
                        )
                },
            )
    }
}
