package me.anasmusa.learncast.data.local.db.snip

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.bind
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort

@Dao
internal interface SnipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SnipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<SnipEntity>)

    @Query(
        """
        SELECT 
        s.clientSnipId,
        s.id,
        s.lessonId,
        s.title,
        s.description,
        s.coverImagePath,
        s.authorId,
        s.authorName,
        s.topicId,
        s.topicTitle,
        s.audioPath,
        s.audioSize,
        s.audioDuration,       
        s.createdAt,
        COALESCE(so.startMs, s.startMs) AS startMs,
        COALESCE(so.endMs, s.endMs) AS endMs,
        COALESCE(so.note, s.note) AS note
        FROM ${TableNames.SNIP} AS s 
        LEFT JOIN ${TableNames.SNIP_OUTBOX} AS so ON so.clientSnipId = s.clientSnipId
        WHERE s.clientSnipId = :clientSnipId
    """,
    )
    suspend fun getByClientSnipId(clientSnipId: String): SnipEntity?

    @Query("DELETE FROM ${TableNames.SNIP} WHERE clientSnipId = :clientSnipId")
    suspend fun delete(clientSnipId: String)

    @Query("DELETE FROM ${TableNames.SNIP} WHERE id in (:ids)")
    suspend fun delete(ids: List<Long>)

    fun getSnips(
        search: String?,
        authorId: Long?,
        topicId: Long?,
        lessonId: Long?,
        sort: QuerySort?,
        order: QueryOrder?,
    ): PagingSource<Int, SnipEntity> {
        val query =
            StringBuilder(
                """
                SElECT 
                s.clientSnipId,
                s.id,
                s.lessonId,
                s.title,
                s.description,
                s.coverImagePath,
                s.authorId,
                s.authorName,
                s.topicId,
                s.topicTitle,
                s.audioPath,
                s.audioSize,
                s.audioDuration,       
                s.createdAt,
                COALESCE(so.startMs, s.startMs) AS startMs,
                COALESCE(so.endMs, s.endMs) AS endMs,
                COALESCE(so.note, s.note) AS note                
                """.trimIndent(),
            )

        query
            .append(" FROM ${TableNames.SNIP} AS s")
            .append(" LEFT JOIN ${TableNames.SNIP_OUTBOX} AS so")
            .append(" ON so.clientSnipId = s.clientSnipId")
            .append(" LEFT JOIN ${TableNames.OUTBOX} AS o")
            .append(" ON o.referenceUuid = s.clientSnipId AND o.actionType = 'DELETE'")

        val args = ArrayList<Any?>()
        var filtered = false

        query.append(" WHERE")

        filtered = true
        query.append(" o.id IS NULL AND")

        if (authorId != null) {
            query.append(" s.authorId = ? AND")
            args.add(authorId)
            filtered = true
        }

        if (topicId != null) {
            query.append(" s.topicId = ? AND")
            args.add(topicId)
            filtered = true
        }

        if (lessonId != null) {
            query.append(" s.lessonId = ? AND")
            args.add(lessonId)
            filtered = true
        }

        if (search.isNullOrBlank()) {
            if (filtered) {
                query.deleteRange(query.length - 4, query.length)
            } else {
                query.deleteRange(query.length - 6, query.length)
            }
        } else {
            query.append(" note LIKE ?")
            args.add("%${search.lowercase()}%")
        }

        if (sort != null) {
            val order =
                if (order == QueryOrder.ASC) {
                    "ASC"
                } else {
                    "DESC"
                }
            when (sort) {
                QuerySort.CREATED_AT -> query.append(" ORDER BY createdAt $order, id $order")
                else -> {}
            }
        }

        return getSnips(
            RoomRawQuery(query.toString()) { it.bind(args) },
        )
    }

    @RawQuery(observedEntities = [SnipEntity::class, OutboxEntity::class])
    fun getSnips(query: RoomRawQuery): PagingSource<Int, SnipEntity>
}
