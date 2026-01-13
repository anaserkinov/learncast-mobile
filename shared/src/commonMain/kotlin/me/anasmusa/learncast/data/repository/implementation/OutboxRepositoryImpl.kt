package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.data.local.db.DBConnection
import me.anasmusa.learncast.data.local.db.outbox.LessonOutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.ListenOutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.SnipOutboxEntity
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.OutboxStatus
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class OutboxRepositoryImpl(
    private val dbConnection: DBConnection,
    private val dao: OutboxDao,
) : OutboxRepository {

    private val mutex = Mutex()

    override suspend fun getToSync(): OutboxEntity? {
        return try {
            mutex.withLock {
                dao.getToSync()
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun onOutboxSynced(
        id: Long,
        actionType: ActionType,
        time: LocalDateTime,
        success: Boolean,
        networkError: Boolean
    ) {
        try {
            mutex.withLock {
                val entity = dao.getOutbox(id) ?: return
                if (entity.updatedAt < time) {
                    if (success) dao.deleteOutbox(id)
                    else dao.update(
                        outboxId = id,
                        lastTriedAt = if (networkError) null else time,
                        status = OutboxStatus.PENDING
                    )
                } else {
                    when (actionType) {
                        ActionType.FAVOURITE if (success && actionType == entity.actionType) ->
                                dao.deleteOutbox(id)
                        ActionType.REMOVE_FAVOURITE if (success && actionType == entity.actionType) ->
                            dao.deleteOutbox(id)

                        ActionType.CREATE if (success) ->
                            dao.update(
                                outboxId = id,
                                actionType = ActionType.UPDATE,
                                status = OutboxStatus.PENDING
                            )

                        else -> dao.update(
                            outboxId = id,
                            lastTriedAt = if (success || networkError) null else time,
                            status = OutboxStatus.PENDING
                        )
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun listen(lessonId: Long) {
        try {
            dbConnection.inWriteTransaction {
                val outboxId = dao.insert(
                    OutboxEntity(
                        id = 0,
                        referenceId = lessonId,
                        referenceUuid = "",
                        referenceType = ReferenceType.LESSON,
                        actionType = ActionType.LISTEN,
                        updatedAt = nowLocalDateTime(),
                        createdAt = nowLocalDateTime(),
                        lastTriedAt = null,
                        status = OutboxStatus.PENDING
                    )
                )
                dao.insert(
                    ListenOutboxEntity(
                        id = 0L,
                        outboxId = outboxId,
                        sessionId = Uuid.random().toHexString()
                    )
                )
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun setFavourite(lessonId: Long, isFavourite: Boolean) {
        mutex.withLock {
            val outbox = dao.getOutbox(
                referenceId = lessonId,
                referenceType = ReferenceType.LESSON,
                actionTypes = arrayOf(ActionType.FAVOURITE, ActionType.REMOVE_FAVOURITE)
            )
            if (
                outbox?.status == OutboxStatus.PENDING &&
                (isFavourite && outbox.actionType == ActionType.REMOVE_FAVOURITE ||
                        !isFavourite && outbox.actionType == ActionType.FAVOURITE)
            )
                dao.deleteOutbox(outbox.id)
            else {
                val entity = OutboxEntity(
                    id = outbox?.id ?: 0,
                    referenceId = lessonId,
                    referenceUuid = "",
                    referenceType = ReferenceType.LESSON,
                    actionType = if (isFavourite) ActionType.FAVOURITE else ActionType.REMOVE_FAVOURITE,
                    updatedAt = nowLocalDateTime(),
                    createdAt = outbox?.createdAt ?: nowLocalDateTime(),
                    lastTriedAt = null,
                    status = outbox?.status ?: OutboxStatus.PENDING
                )
                if (outbox == null)
                    dao.insert(entity)
                else
                    dao.update(entity)
            }
        }
    }

    override suspend fun updateLessonProgress(
        lessonId: Long,
        startedAt: LocalDateTime,
        lastPositionMs: Duration,
        status: UserProgressStatus?,
        completedAt: LocalDateTime?
    ) {
        try {
            mutex.withLock {
                val entity = dao.getLessonWithOutbox(lessonId)
                dbConnection.inWriteTransaction {
                    val outbox = entity?.outbox.let {
                        OutboxEntity(
                            id = it?.id ?: 0,
                            referenceId = lessonId,
                            referenceUuid = "",
                            referenceType = ReferenceType.LESSON,
                            actionType = ActionType.UPDATE,
                            updatedAt = nowLocalDateTime(),
                            createdAt = it?.createdAt ?: nowLocalDateTime(),
                            lastTriedAt = null,
                            status = it?.status ?: OutboxStatus.PENDING
                        )
                    }
                    val outboxId = if (entity?.outbox != null) {
                        dao.update(outbox)
                        outbox.id
                    } else dao.insert(outbox)

                    val lesson = entity?.lesson.let {
                        LessonOutboxEntity(
                            id = it?.id ?: 0,
                            outboxId = outboxId,
                            lessonId = lessonId,
                            startedAt = it?.startedAt ?: startedAt,
                            lastPositionMs = lastPositionMs,
                            status = it?.status ?: status,
                            completedAt = it?.completedAt ?: completedAt
                        )
                    }
                    if (entity?.lesson != null)
                        dao.update(lesson)
                    else
                        dao.insert(lesson)
                }
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun createSnip(
        clientSnipId: String,
        lessonId: Long,
        startMs: Long,
        endMs: Long,
        note: String?
    ) {
        try {
            dbConnection.inWriteTransaction {
                val outboxId = dao.insert(
                    OutboxEntity(
                        id = 0,
                        referenceId = 0,
                        referenceUuid = clientSnipId,
                        referenceType = ReferenceType.SNIP,
                        actionType = ActionType.CREATE,
                        createdAt = nowLocalDateTime(),
                        updatedAt = nowLocalDateTime(),
                        lastTriedAt = null,
                        status = OutboxStatus.PENDING
                    )
                )
                dao.insert(
                    SnipOutboxEntity(
                        0L,
                        outboxId,
                        clientSnipId,
                        lessonId,
                        startMs,
                        endMs,
                        note
                    )
                )
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun updateSnip(
        clientSnipId: String,
        lessonId: Long,
        startMs: Long,
        endMs: Long,
        note: String?
    ) {
        try {
            mutex.withLock {
                val entity = dao.getSnipWithOutbox(clientSnipId)
                dbConnection.inWriteTransaction {
                    val outbox = entity?.outbox.let {
                        OutboxEntity(
                            id = it?.id ?: 0L,
                            referenceId = 0,
                            referenceUuid = clientSnipId,
                            referenceType = ReferenceType.SNIP,
                            actionType = it?.actionType ?: ActionType.UPDATE,
                            updatedAt = nowLocalDateTime(),
                            createdAt = it?.createdAt ?: nowLocalDateTime(),
                            lastTriedAt = null,
                            status = it?.status ?: OutboxStatus.PENDING
                        )
                    }

                    val outboxId = if (entity?.outbox != null) {
                        dao.update(outbox)
                        outbox.id
                    } else dao.insert(outbox)

                    val snip = entity?.snip.let {
                        SnipOutboxEntity(
                            id = it?.id ?: 0,
                            outboxId = outboxId,
                            clientSnipId = clientSnipId,
                            lessonId = lessonId,
                            startMs = startMs,
                            endMs = endMs,
                            note = note
                        )
                    }

                    if (entity?.snip != null)
                        dao.update(snip)
                    else
                        dao.insert(snip)
                }
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun deleteSnip(clientSnipId: String) {
        try {
            mutex.withLock {
                val entity = dao.getSnipWithOutbox(clientSnipId)
                if (entity?.outbox?.status == OutboxStatus.PENDING && entity.outbox.actionType == ActionType.CREATE){
                    dao.deleteOutbox(entity.outbox.id)
                    return
                }
                val outbox = entity?.outbox.let {
                    OutboxEntity(
                        it?.id ?: 0L,
                        0L,
                        clientSnipId,
                        ReferenceType.SNIP,
                        ActionType.DELETE,
                        it?.createdAt ?: nowLocalDateTime(),
                        nowLocalDateTime(),
                        null,
                        it?.status ?: OutboxStatus.PENDING
                    )
                }
                if (entity?.outbox != null) {
                    dao.update(outbox)
                } else dao.insert(outbox)
            }
        } catch (e: Exception) {
        }
    }

}