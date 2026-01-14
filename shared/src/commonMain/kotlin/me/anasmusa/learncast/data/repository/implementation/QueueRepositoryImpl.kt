package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.queue.QueueItemDao
import me.anasmusa.learncast.data.local.db.queue.QueueItemEntity
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.mapper.toEntity
import me.anasmusa.learncast.data.mapper.toUI
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository

internal class QueueRepositoryImpl(
    private val queueItemDao: QueueItemDao,
    private val lessonDao: LessonDao,
    private val snipDao: SnipDao,
) : QueueRepository {
    override suspend fun getById(id: Long): QueueItem? =
        try {
            queueItemDao.getWithStateById(id)?.toUI()
        } catch (e: Exception) {
            null
        }

    override fun observe(id: Long): Flow<QueueItem?> =
        queueItemDao
            .observeWithStateById(id)
            .map { it?.toUI() }

    override suspend fun getLessonId(queueItemId: Long): Long? =
        try {
            queueItemDao.getLessonId(queueItemId)
        } catch (e: Exception) {
            null
        }

    override suspend fun addToQueue(queueItem: QueueItem): Triple<QueueItem, Int, Int>? =
        try {
            var entity =
                if (queueItem.referenceId != 0L) {
                    queueItemDao.getByLessonReferenceId(queueItem.referenceId)
                } else {
                    queueItemDao.getBySnipReferenceUuid(queueItem.referenceUuid)
                }
            var order = entity?.item?.order ?: 0
            if (entity == null) {
                order = -1
                val entityId = queueItemDao.addFirst(queueItem.toEntity())
                entity = queueItemDao.getWithStateById(entityId)!!
            } else {
                queueItemDao.move(entity.item.order, 0)
            }
            Triple(
                entity.toUI(),
                order,
                queueItemDao.count(),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    override suspend fun addToQueue(
        topicId: Long,
        authorId: Long,
    ): List<QueueItem> =
        try {
            queueItemDao.replace(
                lessonDao.getLessons(topicId, authorId).mapIndexed { index, lesson ->
                    QueueItemEntity(
                        id = 0L,
                        order = index,
                        referenceId = lesson.id,
                        referenceUuid = "",
                        referenceType = ReferenceType.LESSON,
                        startMs = null,
                        endMs = null,
                        lessonId = lesson.id,
                        title = lesson.title,
                        description = lesson.description,
                        coverImagePath = lesson.coverImagePath,
                        authorId = lesson.authorId,
                        authorName = lesson.authorName,
                        topicId = lesson.topicId,
                        topicTitle = lesson.topicTitle,
                        audioPath = lesson.audioPath,
                        audioSize = lesson.audioSize,
                        audioDuration = lesson.audioDuration,
                    )
                },
            )
            queueItemDao.getAll().map {
                it.toUI()
            }
        } catch (e: Exception) {
            emptyList()
        }

    override suspend fun getQueuedItems(): List<QueueItem> =
        try {
            queueItemDao.getAll().map { it.toUI() }
        } catch (e: Exception) {
            emptyList()
        }

    override suspend fun move(
        from: Int,
        to: Int,
    ) {
        try {
            queueItemDao.move(from, to)
        } catch (e: Exception) {
        }
    }

    override suspend fun clear(completely: Boolean) {
        try {
            if (completely) {
                queueItemDao.clear()
            } else {
                queueItemDao.clearExceptFirst()
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun remove(id: Long) {
        try {
            queueItemDao.remove(id)
        } catch (e: Exception) {
        }
    }

    override suspend fun ensureItemIsFirst(id: Long) {
        try {
            queueItemDao.ensureItemIsFirst(id)
        } catch (e: Exception) {
        }
    }

    override suspend fun getQueueItem(id: Long): QueueItem? =
        try {
            queueItemDao.getWithStateById(id)?.toUI()
        } catch (e: Exception) {
            null
        }

    override suspend fun refreshQueueItem(
        id: Long,
        referenceUuid: String,
    ): QueueItem? {
        return try {
            val snip =
                snipDao.getByClientSnipId(referenceUuid)
                    ?: return null
            queueItemDao.updateSnipQueueItem(
                id,
                snip.startMs,
                snip.endMs,
                if (!snip.note.isNullOrBlank()) {
                    "${snip.note} - ${snip.title}"
                } else {
                    snip.title
                },
            )
            queueItemDao.getWithStateById(id)?.toUI()
        } catch (e: Exception) {
            null
        }
    }
}
