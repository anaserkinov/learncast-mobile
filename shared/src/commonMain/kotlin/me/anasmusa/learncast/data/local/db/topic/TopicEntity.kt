package me.anasmusa.learncast.data.local.db.topic

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import kotlin.time.Duration

@Entity(tableName = TableNames.TOPIC)
class TopicEntity(
    @PrimaryKey val id: Long,
    val topicId: Long,
    val title: String,
    val description: String?,
    val coverImagePath: String?,
    val authorId: Long,
    val authorName: String,
    val createdAt: LocalDateTime,
    val lessonCount: Long,
    val totalDuration: Duration,
    val completedLessonCount: Long
)