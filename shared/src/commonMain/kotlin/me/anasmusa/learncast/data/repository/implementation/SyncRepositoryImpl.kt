package me.anasmusa.learncast.data.repository.implementation

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.io.IOException
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.core.toUTCInstant
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.mapper.toEntity
import me.anasmusa.learncast.data.mapper.toInput
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.network.model.lesson.ListenSessionCreateRequest
import me.anasmusa.learncast.data.network.model.lesson.UpdateProgressRequest
import me.anasmusa.learncast.data.network.model.snip.SnipCURequest
import me.anasmusa.learncast.data.network.service.LessonService
import me.anasmusa.learncast.data.network.service.SnipService
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import me.anasmusa.learncast.data.repository.abstraction.SyncRepository
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class SyncRepositoryImpl(
    private val outboxRepository: OutboxRepository,
    private val outboxDao: OutboxDao,
    private val lessonService: LessonService,
    private val lessonDao: LessonDao,
    private val snipService: SnipService,
    private val snipDao: SnipDao
) : SyncRepository {

    override suspend fun sync(finishWhenDrained: Boolean) = withContext(Dispatchers.IO) {
        if (finishWhenDrained) {
            while (true) {
                val outbox = outboxRepository.getToSync() ?: return@withContext
                if (syncOutbox(outbox)) break
            }
        } else outboxDao.observeNextItemToSync()
            .buffer(Channel.CONFLATED)
            .collect {
                val outbox = outboxRepository.getToSync() ?: return@collect
                val abort = syncOutbox(outbox)
                if (abort) {
                    delay(2.toDuration(DurationUnit.MINUTES))
                    return@collect
                }
            }
    }

    private suspend fun syncOutbox(outbox: OutboxEntity): Boolean {
        return when (outbox.referenceType) {
            ReferenceType.LESSON -> {
                syncLessonOutbox(outbox)
            }

            ReferenceType.SNIP -> {
                syncSnipOutbox(outbox)
            }
        }
    }

    private suspend fun syncLessonOutbox(outbox: OutboxEntity): Boolean {
        return when (outbox.actionType) {
            ActionType.UPDATE -> updateUserProgress(outbox)
            ActionType.LISTEN -> createListenSession(outbox)
            ActionType.FAVOURITE, ActionType.REMOVE_FAVOURITE -> favourite(outbox)
            else -> {
                false
            }
        }
    }

    private suspend fun syncSnipOutbox(outbox: OutboxEntity): Boolean {
        return when (outbox.actionType) {
            ActionType.CREATE -> createSnip(outbox)
            ActionType.UPDATE -> updateSnip(outbox)
            ActionType.DELETE -> deleteSnip(outbox)
            else -> {
                false
            }
        }
    }

    private suspend fun onOutboxSynced(
        id: Long,
        actionType: ActionType,
        time: LocalDateTime,
        exception: Exception?
    ): Boolean {
        val networkError = when (exception) {
            is IOException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            is UnresolvedAddressException -> {
                true
            }

            else -> false
        }
        outboxRepository.onOutboxSynced(
            id = id,
            actionType = actionType,
            time = time,
            success = exception == null,
            networkError = networkError
        )
        return networkError
    }

    private suspend fun updateUserProgress(outbox: OutboxEntity): Boolean {
        return try {
            val progress = outboxDao.getLessonOutbox(outbox.id)
                ?: run {
                    outboxDao.deleteOutbox(outbox.id)
                    return false
                }

            val response = lessonService.updateProgress(
                lessonId = progress.lessonId,
                request = UpdateProgressRequest(
                    status = progress.status,
                    startedAt = progress.startedAt.toUTCInstant(),
                    completedAt = progress.completedAt?.toUTCInstant(),
                    lastPositionMs = progress.lastPositionMs.inWholeMilliseconds
                )
            )
            lessonDao.upsertProgress(response.data.toInput())
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = response.time.toDateTime(),
                exception = null
            )
        } catch (e: Exception) {
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = nowLocalDateTime(),
                exception = e
            )
        }
    }

    private suspend fun createListenSession(outbox: OutboxEntity): Boolean {
        return try {
            val session = outboxDao.getListenOutbox(outbox.id)
                ?: run {
                    outboxDao.deleteOutbox(outbox.id)
                    return false
                }
            val response = lessonService.listen(
                lessonId = outbox.referenceId,
                request = ListenSessionCreateRequest(
                    sessionId = session.sessionId,
                    createdAt = outbox.createdAt.toUTCInstant()
                )
            )
            lessonDao.updateListenCount(outbox.referenceId, response.data.listenCount)
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = response.time.toDateTime(),
                exception = null
            )
        } catch (e: Exception) {
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = nowLocalDateTime(),
                exception = e
            )
        }
    }

    private suspend fun favourite(outbox: OutboxEntity): Boolean {
        return try {
            val response = if (outbox.actionType == ActionType.FAVOURITE)
                lessonService.setFavourite(outbox.referenceId)
            else
                lessonService.removeFavourite(outbox.referenceId)
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = response.time.toDateTime(),
                exception = null
            )
        } catch (e: Exception) {
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = nowLocalDateTime(),
                exception = e
            )
        }
    }

    private suspend fun createSnip(outbox: OutboxEntity): Boolean {
        return try {
            val snip = outboxDao.getSnipOutbox(outbox.id)
                ?: run {
                    outboxDao.deleteOutbox(outbox.id)
                    return false
                }
            val response = snipService.create(
                lessonId = snip.lessonId,
                request = SnipCURequest(
                    clientSnipId = snip.clientSnipId,
                    startMs = snip.startMs,
                    endMs = snip.endMs,
                    note = snip.note,
                    createdAt = outbox.createdAt.toUTCInstant()
                )
            )
            response.data.let { snipResponse ->
                snipDao.insert(snipResponse.toEntity())
                snipResponse.userSnipCount?.let { snipCount ->
                    lessonDao.updateUserSnipCount(snipResponse.lesson.id, snipCount)
                }
            }
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = response.time.toDateTime(),
                exception = null
            )
        } catch (e: Exception) {
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = nowLocalDateTime(),
                exception = e
            )
        }
    }

    private suspend fun updateSnip(outbox: OutboxEntity): Boolean {
        return try {
            val snip = outboxDao.getSnipOutbox(outbox.id)
                ?: run {
                    outboxDao.deleteOutbox(outbox.id)
                    return false
                }
            val response = snipService.update(
                clientSnipId = outbox.referenceUuid,
                request = SnipCURequest(
                    clientSnipId = snip.clientSnipId,
                    startMs = snip.startMs,
                    endMs = snip.endMs,
                    note = snip.note,
                    createdAt = outbox.createdAt.toUTCInstant()
                )
            )
            snipDao.insert(response.data.toEntity())
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = response.time.toDateTime(),
                exception = null
            )
        } catch (e: Exception) {
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = nowLocalDateTime(),
                exception = e
            )
        }
    }

    private suspend fun deleteSnip(outbox: OutboxEntity): Boolean {
        return try {
            val response = snipService.delete(outbox.referenceUuid)
            snipDao.delete(outbox.referenceUuid)
            response.data?.let {
                lessonDao.updateUserSnipCount(it.lessonId, it.count)
            }
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = response.time.toDateTime(),
                exception = null
            )
        } catch (e: Exception) {
            onOutboxSynced(
                id = outbox.id,
                actionType = outbox.actionType,
                time = nowLocalDateTime(),
                exception = e
            )
        }
    }

}