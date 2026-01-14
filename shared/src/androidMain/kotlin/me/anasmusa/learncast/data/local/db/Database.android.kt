package me.anasmusa.learncast.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.SupportSQLiteConnection
import me.anasmusa.learncast.ApplicationLoader
import org.koin.mp.KoinPlatform

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val database = KoinPlatform.getKoin().get<SQLiteDatabase>()
    return Room
        .databaseBuilder<AppDatabase>(
            context = ApplicationLoader.context,
            name = ApplicationLoader.context.getDatabasePath("app.db").absolutePath,
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
