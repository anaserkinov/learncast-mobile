package me.anasmusa.learncast.data.local.db.snip

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import kotlin.time.Duration

@Entity(tableName = TableNames.SNIP)
internal data class SnipEntity(
    @PrimaryKey val clientSnipId: String,
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val note: String?,
    val createdAt: LocalDateTime,
    // lesson
    val lessonId: Long,
    val title: String,
    val description: String?,
    val coverImagePath: String?,
    val authorId: Long,
    val authorName: String,
    val topicId: Long?,
    val topicTitle: String?,
    val audioPath: String,
    val audioSize: Long,
    val audioDuration: Duration,
)
