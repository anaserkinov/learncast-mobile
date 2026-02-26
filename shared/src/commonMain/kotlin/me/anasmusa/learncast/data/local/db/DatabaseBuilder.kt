package me.anasmusa.learncast.data.local.db

import androidx.room.RoomDatabase

interface DatabaseBuilder {
    fun get(): RoomDatabase.Builder<AppDatabase>
}
