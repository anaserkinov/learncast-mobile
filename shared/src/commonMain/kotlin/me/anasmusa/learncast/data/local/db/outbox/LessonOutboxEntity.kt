package me.anasmusa.learncast.data.local.db.outbox

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.time.Duration

@Entity(
    tableName = TableNames.LESSON_OUTBOX,
    foreignKeys = [
        ForeignKey(
            entity = OutboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["outboxId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class LessonOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val outboxId: Long,
    val lessonId: Long,
    val startedAt: LocalDateTime,
    val lastPositionMs: Duration,
    val status: UserProgressStatus?,
    val completedAt: LocalDateTime?
)

class LessonWithOutbox(
    @Embedded
    val lesson: LessonOutboxEntity,
    @Embedded(prefix = "outbox_")
    val outbox: OutboxEntity
)