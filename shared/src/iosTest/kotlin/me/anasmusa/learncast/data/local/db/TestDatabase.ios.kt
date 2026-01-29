package me.anasmusa.learncast.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun getInMemoryDatabase(): AppDatabase =
    Room
        .inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
