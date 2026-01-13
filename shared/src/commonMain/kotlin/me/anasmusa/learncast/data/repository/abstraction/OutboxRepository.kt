package me.anasmusa.learncast.data.repository.abstraction

import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.local.db.snip.SnipEntity
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.time.Duration

interface OutboxRepository {

    suspend fun getToSync(): OutboxEntity?

    suspend fun onOutboxSynced(
        id: Long,
        actionType: ActionType,
        time: LocalDateTime,
        success: Boolean,
        networkError: Boolean
    )

    suspend fun listen(lessonId: Long)

    suspend fun setFavourite(lessonId: Long, isFavourite: Boolean)

    suspend fun updateLessonProgress(
        lessonId: Long,
        startedAt: LocalDateTime,
        lastPositionMs: Duration,
        status: UserProgressStatus?,
        completedAt: LocalDateTime?
    )

    suspend fun createSnip(
        clientSnipId: String,
        lessonId: Long,
        startMs: Long,
        endMs: Long,
        note: String?
    )

    suspend fun updateSnip(
        clientSnipId: String,
        lessonId: Long,
        startMs: Long,
        endMs: Long,
        note: String?
    )

    suspend fun deleteSnip(
        clientSnipId: String
    )

}