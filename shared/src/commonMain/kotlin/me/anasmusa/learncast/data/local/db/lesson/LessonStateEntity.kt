package me.anasmusa.learncast.data.local.db.lesson

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.time.Duration

@Entity(tableName = TableNames.LESSON_STATE)
class LessonStateEntity(
    @PrimaryKey val lessonId: Long,
    @ColumnInfo(defaultValue = "0") val listenCount: Long,
    @ColumnInfo(defaultValue = "0") val snipCount: Long,
    @ColumnInfo(defaultValue = "0") val userSnipCount: Long,
    @ColumnInfo(defaultValue = "0") val isFavourite: Boolean,
    val startedAt: LocalDateTime?,
    val lastPositionMs: Duration?,
    val status: UserProgressStatus,
    val completedAt: LocalDateTime?
)

class LessonStateInput(
    val lessonId: Long,
    val listenCount: Long,
    val snipCount: Long,
    val isFavourite: Boolean,
    val startedAt: LocalDateTime?,
    val lastPositionMs: Duration?,
    val status: UserProgressStatus,
    val completedAt: LocalDateTime?
)

class LessonProgressInput(
    val lessonId: Long,
    val startedAt: LocalDateTime?,
    val lastPositionMs: Duration?,
    val status: UserProgressStatus,
    val completedAt: LocalDateTime?
)