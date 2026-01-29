package me.anasmusa.learncast.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider

actual fun getInMemoryDatabase(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room
        .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .setDriver(AndroidSQLiteDriver())
        .build()
}
