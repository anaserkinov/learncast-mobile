package me.anasmusa.learncast.data.local.db

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import me.anasmusa.learncast.core.toDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

class LocalDateTimeConverter {
    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun encode(value: LocalDateTime?): Long? = value?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds()

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun decode(value: Long?): LocalDateTime? = value?.let { Instant.fromEpochMilliseconds(it).toDateTime() }
}

class DurationConverter {
    @TypeConverter
    fun encode(value: Duration?): Long? = value?.inWholeMilliseconds

    @TypeConverter
    fun decode(value: Long?): Duration? = value?.toDuration(DurationUnit.MILLISECONDS)
}
