package me.anasmusa.learncast.data.local.db.lesson

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.DownloadState
import kotlin.time.Duration

@Entity(tableName = TableNames.LESSON)
class LessonEntity(
    @PrimaryKey val id: Long,
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
    val createdAt: LocalDateTime
)

class LessonWithState(
    @Embedded
    val lesson: LessonEntity,
    @Embedded(prefix = "state_")
    val state: LessonStateEntity,
    val downloadState: DownloadState?,
    val percentDownloaded: Float?
)