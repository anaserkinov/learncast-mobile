package me.anasmusa.learncast.data.local.db.queue

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.lesson.LessonStateEntity
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType
import kotlin.time.Duration

@Entity(tableName = TableNames.QUEUE_ITEM)
class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val order: Int,
    val referenceId: Long,
    val referenceUuid: String,
    val referenceType: ReferenceType,
    // snip
    val startMs: Long?,
    val endMs: Long?,
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

@DatabaseView(
    """
            SELECT q.*,
        q.lessonId AS state_lessonId,
        COALESCE(s.listenCount, 0) AS state_listenCount,
        COALESCE(s.snipCount, 0) AS state_snipCount,
        COALESCE(s.userSnipCount, 0) AS state_userSnipCount,
        CASE
            WHEN o.actionType IS NOT NULL
            THEN o.actionType = 'FAVOURITE'
            ELSE COALESCE(s.isFavourite, 0)
            END AS state_isFavourite,
        COALESCE(s.startedAt, lo.startedAt) AS state_startedAt,
        COALESCE(lo.lastPositionMs, s.lastPositionMs) AS state_lastPositionMs,
        COALESCE(lo.status, s.status, 'NOT_STARTED') AS state_status,
        COALESCE(s.completedAt, lo.completedAt) AS state_completedAt,
        d.state as downloadState,
        d.percentDownloaded as downloadedPercent
        FROM ${TableNames.QUEUE_ITEM} AS q
        LEFT JOIN ${TableNames.DOWNLOAD_STATE} AS d ON d.referenceId = q.referenceId AND d.referenceUuid = q.referenceUuid AND d.referenceType = q.referenceType
        LEFT JOIN ${TableNames.LESSON_STATE} AS s ON s.lessonId = q.referenceId
        LEFT JOIN ${TableNames.LESSON_OUTBOX} AS lo ON lo.lessonId = q.referenceId
        LEFT JOIN ${TableNames.OUTBOX} AS o
             ON o.referenceId = q.referenceId AND 
             (o.actionType = 'FAVOURITE' OR o.actionType = 'REMOVE_FAVOURITE')
""",
    viewName = TableNames.QUEUE_ITEM_WITH_STATE_VIEW,
)
class QueueItemWithState(
    @Embedded
    val item: QueueItemEntity,
    @Embedded(prefix = "state_")
    val state: LessonStateEntity,
    val downloadState: DownloadState?,
    val percentDownloaded: Float?,
)
