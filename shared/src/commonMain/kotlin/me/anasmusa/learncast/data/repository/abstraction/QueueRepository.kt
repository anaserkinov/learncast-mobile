package me.anasmusa.learncast.data.repository.abstraction

import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.QueueItem

interface QueueRepository {
    suspend fun getById(id: Long): QueueItem?

    fun observe(id: Long): Flow<QueueItem?>

    suspend fun getLessonId(queueItemId: Long): Long?

    suspend fun addToQueue(queueItem: QueueItem): Triple<QueueItem, Int, Int>?

    suspend fun addToQueue(
        topicId: Long,
        authorId: Long,
    ): List<QueueItem>

    suspend fun getQueuedItems(): List<QueueItem>

    suspend fun move(
        from: Int,
        to: Int,
    )

    suspend fun remove(id: Long)

    suspend fun ensureItemIsFirst(id: Long)

    suspend fun clear(completely: Boolean)

    suspend fun getQueueItem(id: Long): QueueItem?

    suspend fun refreshQueueItem(
        id: Long,
        referenceUuid: String,
    ): QueueItem?
}
