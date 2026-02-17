package me.anasmusa.learncast.data.local.db

import androidx.sqlite.SQLiteStatement
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

internal fun SQLiteStatement.bind(data: List<Any?>) {
    data.forEachIndexed { index, entry ->
        val index = index + 1
        when (entry) {
            is String -> bindText(index, entry)
            is Boolean -> bindBoolean(index, entry)
            is Int -> bindInt(index, entry)
            is Long -> bindLong(index, entry)
            is LocalDateTime -> bindLong(index, entry.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
            is Enum<*> -> bindText(index, entry.name)
            else -> bindNull(index)
        }
    }
}
