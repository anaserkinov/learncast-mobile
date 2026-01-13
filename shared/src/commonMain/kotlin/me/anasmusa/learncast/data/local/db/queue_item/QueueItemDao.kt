package me.anasmusa.learncast.data.local.db.queue_item

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.local.db.TableNames

@Dao
internal interface QueueItemDao {

    @Insert
    suspend fun insert(items: List<QueueItemEntity>)

    @Insert
    suspend fun insert(item: QueueItemEntity): Long

    @Transaction
    suspend fun replace(items: List<QueueItemEntity>) {
        clear()
        insert(items)
    }

    @Query("DELETE FROM ${TableNames.QUEUE_ITEM}")
    suspend fun clear()

    @Query("DELETE FROM ${TableNames.QUEUE_ITEM} WHERE `order` != 0")
    suspend fun clearExceptFirst()

    @Query("SELECT COUNT(id) FROM ${TableNames.QUEUE_ITEM}")
    suspend fun count(): Int

    @Transaction
    suspend fun addFirst(item: QueueItemEntity): Long {
        moveBy(1)
        return insert(item)
    }

    @Query("SELECT lessonId FROM ${TableNames.QUEUE_ITEM} WHERE id = :queueItemId AND referenceType = 'LESSON'")
    suspend fun getLessonId(queueItemId: Long): Long?

    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM} WHERE id = :id")
    suspend fun getById(id: Long): QueueItemEntity?

    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM_WITH_STATE_VIEW} WHERE id = :id")
    suspend fun getWithStateById(id: Long): QueueItemWithState?

    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM_WITH_STATE_VIEW} WHERE id = :id")
    fun observeWithStateById(id: Long): Flow<QueueItemWithState?>

    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM_WITH_STATE_VIEW} WHERE referenceId = :id AND referenceType = 'LESSON'")
    suspend fun getByLessonReferenceId(id: Long): QueueItemWithState?
    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM_WITH_STATE_VIEW} WHERE referenceUuid = :clientSnipUuid AND referenceType = 'SNIP'")
    suspend fun getBySnipReferenceUuid(clientSnipUuid: String): QueueItemWithState?

    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM} WHERE `order` = :order LIMIT 1")
    suspend fun getByOrder(order: Int): QueueItemEntity?

    @Query("""
            UPDATE ${TableNames.QUEUE_ITEM}
            SET startMs = :startMs,
                endMs = :endMs,
                title = :title
            WHERE id = :id
    """)
    suspend fun updateSnipQueueItem(
        id: Long,
        startMs: Long,
        endMs: Long,
        title: String
    )

    @Query("DELETE FROM ${TableNames.QUEUE_ITEM} WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
            DELETE FROM ${TableNames.QUEUE_ITEM}
            WHERE `order` < (
                  SELECT `order` FROM ${TableNames.QUEUE_ITEM} WHERE id = :id
            )
    """)
    suspend fun deleteBefore(id: Long): Int

    @Query("UPDATE ${TableNames.QUEUE_ITEM} SET `order` = `order` + :by")
    suspend fun moveBy(by: Int)

    @Query(
        """
            UPDATE ${TableNames.QUEUE_ITEM}
            SET `order` = `order` + :by
            WHERE `order` >= (
                  SELECT `order` FROM ${TableNames.QUEUE_ITEM} WHERE id = :id
            )
    """)
    suspend fun moveByAfter(id: Long, by: Int)

    @Query("UPDATE ${TableNames.QUEUE_ITEM} SET `order` = `order` + :by WHERE `order` >= :from AND `order` <= :to")
    suspend fun moveBy(from: Int, to: Int, by: Int)

    @Query("UPDATE ${TableNames.QUEUE_ITEM} SET `order` = :to WHERE id = :id")
    suspend fun moveTo(id: Long, to: Int)

    @Transaction
    suspend fun move(from: Int, to: Int) {
        val item = getByOrder(from)
        if (from < to)
            moveBy(
                from + 1,
                to,
                -1
            )
        else
            moveBy(
                to,
                from - 1,
                1
            )
        if (item != null) moveTo(item.id, to)
    }

    @Transaction
    suspend fun remove(id: Long) {
        moveByAfter(id, -1)
        delete(id)
    }

    @Transaction
    suspend fun ensureItemIsFirst(id: Long){
        val deletedCount = deleteBefore(id)
        moveBy(-deletedCount)
    }

    @Query("SELECT * FROM ${TableNames.QUEUE_ITEM_WITH_STATE_VIEW} ORDER BY `order` ASC")
    suspend fun getAll(): List<QueueItemWithState>

}