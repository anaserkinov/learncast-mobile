package me.anasmusa.learncast.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun nowLocalDateTime() = Clock.System.now().toDateTime()
fun nowInstant() = Clock.System.now()

fun LocalDateTime.toUTCInstant(): Instant {
    return toInstant(TimeZone.currentSystemDefault())
}

@OptIn(ExperimentalTime::class)
fun Instant.toDateTime(): LocalDateTime {
    return toLocalDateTime(TimeZone.currentSystemDefault())
}

@OptIn(ExperimentalTime::class)
fun Instant.toDate(): LocalDate {
    return toDateTime().date
}

@OptIn(ExperimentalTime::class)
fun Instant.toTime(): LocalTime {
    return toDateTime().time
}

private val monthYear = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); year()
}

private val dayMonth = LocalDate.Format {
    day(); char('-'); monthName(MonthNames.ENGLISH_ABBREVIATED);
}

fun LocalDateTime.monthYear(): String {
    return monthYear.format(date)
}

fun LocalDateTime.dayMonth(): String {
    return dayMonth.format(date)
}