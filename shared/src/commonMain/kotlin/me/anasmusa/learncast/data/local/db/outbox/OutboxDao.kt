package me.anasmusa.learncast.data.local.db.outbox

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.OutboxStatus
import me.anasmusa.learncast.data.model.ReferenceType

@Dao
internal interface OutboxDao {

    @Insert
    suspend fun insert(entity: OutboxEntity): Long
    @Update
    suspend fun update(entity: OutboxEntity)

    @Insert
    suspend fun insert(entity: LessonOutboxEntity): Long
    @Update
    suspend fun update(entity: LessonOutboxEntity)

    @Insert
    suspend fun insert(entity: SnipOutboxEntity): Long
    @Update
    suspend fun update(entity: SnipOutboxEntity)

    @Insert
    suspend fun insert(entity: ListenOutboxEntity): Long

    @Query("DELETE FROM ${TableNames.OUTBOX} WHERE referenceType = :referenceType AND referenceId in (:ids)")
    suspend fun clearDeleteActions(ids: List<Long>, referenceType: ReferenceType)

    @Query("UPDATE ${TableNames.OUTBOX} SET actionType = 'UPDATE', updatedAt = :now, lastTriedAt = NULL WHERE referenceUuid IN (:uuids)")
    suspend fun clearCreateActions(uuids: List<String>, now: LocalDateTime)

    @Query("DELETE FROM ${TableNames.OUTBOX} WHERE id = :id")
    suspend fun deleteOutbox(id: Long)

    @Query("UPDATE ${TableNames.OUTBOX} SET lastTriedAt = :lastTriedAt, status = :status WHERE id = :outboxId")
    suspend fun update(outboxId: Long, lastTriedAt: LocalDateTime?, status: OutboxStatus)

    @Query("UPDATE ${TableNames.OUTBOX} SET actionType = :actionType, status = :status, lastTriedAt = NULL WHERE id = :outboxId")
    suspend fun update(outboxId: Long, actionType: ActionType, status: OutboxStatus)

    @Query("SELECT * FROM ${TableNames.LESSON_OUTBOX} WHERE outboxId = :outboxId")
    suspend fun getLessonOutbox(outboxId: Long): LessonOutboxEntity?

    @Query("SELECT * FROM ${TableNames.SNIP_OUTBOX} WHERE outboxId = :outboxId")
    suspend fun getSnipOutbox(outboxId: Long): SnipOutboxEntity?

    @Query("SELECT * FROM ${TableNames.LISTEN_OUTBOX} WHERE outboxId = :outboxId")
    suspend fun getListenOutbox(outboxId: Long): ListenOutboxEntity?


    @Query("""
        SELECT
           l.*,
           o.id              AS outbox_id,
           o.referenceId     AS outbox_referenceId,
           o.referenceUuid   AS outbox_referenceUuid,
           o.referenceType   AS outbox_referenceType,
           o.actionType      AS outbox_actionType,
           o.createdAt       AS outbox_createdAt,
           o.updatedAt       AS outbox_updatedAt,
           o.lastTriedAt     AS outbox_lastTriedAt,
           o.status          AS outbox_status
        FROM ${TableNames.LESSON_OUTBOX} l
        JOIN ${TableNames.OUTBOX} o ON o.id = l.outboxId
        WHERE l.lessonId = :lessonId AND actionType = 'UPDATE'
    """)
    suspend fun getLessonWithOutbox(lessonId: Long): LessonWithOutbox?

    @Query("""
        SELECT
           s.*,
           o.id              AS outbox_id,
           o.referenceId     AS outbox_referenceId,
           o.referenceUuid   AS outbox_referenceUuid,
           o.referenceType   AS outbox_referenceType,
           o.actionType      AS outbox_actionType,
           o.createdAt       AS outbox_createdAt,
           o.updatedAt       AS outbox_updatedAt,
           o.lastTriedAt     AS outbox_lastTriedAt,
           o.status          AS outbox_status
        FROM ${TableNames.SNIP_OUTBOX} s
        JOIN ${TableNames.OUTBOX} o ON o.id = s.outboxId
        WHERE s.clientSnipId = :clientSnipId
    """)
    suspend fun getSnipWithOutbox(clientSnipId: String): SnipWithOutbox?

    @Query("SELECT * FROM ${TableNames.OUTBOX} WHERE id = :outboxId")
    suspend fun getOutbox(outboxId: Long): OutboxEntity?

    @Query("SELECT * FROM ${TableNames.OUTBOX} WHERE referenceId = :referenceId AND referenceType = :referenceType AND actionType IN (:actionTypes)")
    suspend fun getOutbox(referenceId: Long, referenceType: ReferenceType, actionTypes: Array<ActionType>): OutboxEntity?

    @Query("""
        SELECT id
            FROM ${TableNames.OUTBOX} WHERE 
            (status = 'PENDING' AND (lastTriedAt IS NULL OR unixepoch() * 1000 - lastTriedAt >= 3600000) OR 
                status = 'IN_PROGRESS' AND lastTriedAt IS NOT NULL AND unixepoch() * 1000 - lastTriedAt >= 3600000) 
            ORDER BY lastTriedAt ASC, createdAt ASC
            LIMIT 1
        """)
    fun observeNextItemToSync(): Flow<Long?>

    @Query("""SELECT *
        FROM ${TableNames.OUTBOX} WHERE 
        (status = 'PENDING' AND (lastTriedAt IS NULL OR unixepoch() * 1000 - lastTriedAt >= 3600000) OR 
            status = 'IN_PROGRESS' AND lastTriedAt IS NOT NULL AND unixepoch() * 1000 - lastTriedAt >= 3600000) 
        ORDER BY lastTriedAt ASC, createdAt ASC
        LIMIT 1
        """)
    suspend fun _getToSync(): OutboxEntity?

    @Query("UPDATE ${TableNames.OUTBOX} SET status = :status, lastTriedAt = unixepoch() * 1000  WHERE id = :id")
    suspend fun changeStatus(id: Long, status: OutboxStatus)

    suspend fun getToSync(): OutboxEntity? {
        val entity = _getToSync() ?: return null
        changeStatus(entity.id, OutboxStatus.IN_PROGRESS)
        return entity
    }

}