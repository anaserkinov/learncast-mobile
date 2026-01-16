package me.anasmusa.learncast.data.local.db.lesson

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.bind
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus

@Dao
internal interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<LessonEntity>)

    @Upsert(entity = LessonStateEntity::class)
    suspend fun upsertProgress(items: LessonProgressInput)

    @Upsert(entity = LessonStateEntity::class)
    suspend fun upsertStates(items: List<LessonStateInput>)

    @Query("UPDATE ${TableNames.LESSON_STATE} SET userSnipCount = :count WHERE lessonId = :lessonId")
    suspend fun updateUserSnipCount(
        lessonId: Long,
        count: Long,
    )

    @Query("UPDATE ${TableNames.LESSON_STATE} SET listenCount = :count WHERE lessonId = :lessonId")
    suspend fun updateListenCount(
        lessonId: Long,
        count: Long,
    )

    @Query(
        """
        SELECT 
            COALESCE(s.userSnipCount,0)
            + SUM(CASE WHEN o.actionType='CREATE' THEN 1 ELSE 0 END)
            - SUM(CASE WHEN o.actionType='DELETE' THEN 1 ELSE 0 END) AS userSnipCount
        FROM ${TableNames.LESSON_STATE} s
        LEFT JOIN ${TableNames.SNIP} snip
            ON snip.lessonId = :lessonId
        LEFT JOIN ${TableNames.OUTBOX} o
            ON o.referenceType = 'SNIP' AND o.referenceUuid = snip.clientSnipId
        WHERE s.lessonId = :lessonId
    """,
    )
    suspend fun getUserSnipCount(lessonId: Long): Long

    @Query("DELETE FROM ${TableNames.LESSON} WHERE id in (:ids)")
    suspend fun delete(ids: List<Long>)

    @Transaction
    suspend fun insert(
        lessons: List<LessonEntity>,
        states: List<LessonStateInput>,
    ) {
        insert(lessons)
        upsertStates(states)
    }

    @Query("SELECT * FROM ${TableNames.LESSON} WHERE topicId = :topicId AND authorId = :authorId ORDER BY createdAt ASC")
    suspend fun getLessons(
        topicId: Long,
        authorId: Long,
    ): List<LessonEntity>

    fun getLessons(
        search: String?,
        authorId: Long?,
        topicId: Long?,
        isFavourite: Boolean?,
        status: UserProgressStatus?,
        isDownloaded: Boolean?,
        sort: QuerySort?,
        order: QueryOrder?,
    ): PagingSource<Int, LessonWithState> {
        val query = StringBuilder("SElECT l.*")
        query.append(", s.lessonId AS state_lessonId")
        query
            .append(", s.listenCount AS state_listenCount")
            .append(", s.snipCount AS state_snipCount")
            .append(", s.userSnipCount AS state_userSnipCount")
            .append(
                """
                , CASE
                      WHEN o.actionType IS NOT NULL
                      THEN o.actionType = '${ActionType.FAVOURITE}'
                      ELSE s.isFavourite
                      END AS state_isFavourite
                """.trimIndent(),
            ).append(", COALESCE(s.startedAt, lo.startedAt) AS state_startedAt")
            .append(", COALESCE(lo.lastPositionMs, s.lastPositionMs) AS state_lastPositionMs")
            .append(", COALESCE(lo.status, s.status) AS state_status")
            .append(", COALESCE(s.completedAt, lo.completedAt) AS state_completedAt")

        query.append(" FROM ${TableNames.LESSON} AS l")
        query.append(" JOIN ${TableNames.LESSON_STATE} AS s ON s.lessonId = l.id")
        query.append(" LEFT JOIN ${TableNames.LESSON_OUTBOX} AS lo ON lo.lessonId = l.id")
        query.append(
            """ 
             LEFT JOIN ${TableNames.OUTBOX} AS o
             ON o.referenceId = l.id AND 
             (o.actionType = '${ActionType.FAVOURITE}' OR o.actionType = '${ActionType.REMOVE_FAVOURITE}')
        """,
        )

        val args = ArrayList<Any?>()
        var filtered = false

        query.append(" WHERE")

        if (authorId != null) {
            query.append(" l.authorId = ? AND")
            args.add(authorId)
            filtered = true
        }

        if (topicId != null) {
            query.append(" l.topicId = ? AND")
            args.add(topicId)
            filtered = true
        }

        if (status != null) {
            query.append(" state_status = ? AND")
            args.add(status.name)
            filtered = true
        }

        if (isFavourite != null) {
            query.append(" state_isFavourite = ? AND")
            args.add(isFavourite)
            filtered = true
        }

        if (search.isNullOrBlank()) {
            if (filtered) {
                query.deleteRange(query.length - 4, query.length)
            } else {
                query.deleteRange(query.length - 6, query.length)
            }
        } else {
            query.append(" l.title LIKE ?")
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
                QuerySort.CREATED_AT -> query.append(" ORDER BY l.createdAt $order, l.id $order")
                QuerySort.SNIP_COUNT -> query.append(" ORDER BY state_snipCount $order, l.id $order")
            }
        }

        return getLessons(
            RoomRawQuery(query.toString()) { it.bind(args) },
        )
    }

    fun getDownloadedLessons(
        search: String?,
        isDownloaded: Boolean?,
    ): PagingSource<Int, LessonWithState> {
        val query = StringBuilder("SElECT l.*")
        query.append(", s.lessonId AS state_lessonId")
        query
            .append(", s.listenCount AS state_listenCount")
            .append(", s.snipCount AS state_snipCount")
            .append(", s.userSnipCount AS state_userSnipCount")
            .append(
                """
                , CASE
                      WHEN o.actionType IS NOT NULL
                      THEN o.actionType = '${ActionType.FAVOURITE}'
                      ELSE s.isFavourite
                      END AS state_isFavourite
                """.trimIndent(),
            ).append(", COALESCE(s.startedAt, lo.startedAt) AS state_startedAt")
            .append(", COALESCE(lo.lastPositionMs, s.lastPositionMs) AS state_lastPositionMs")
            .append(", COALESCE(lo.status, s.status) AS state_status")
            .append(", COALESCE(s.completedAt, lo.completedAt) AS state_completedAt")
            .append(", d.state as downloadState")
            .append(", d.percentDownloaded as percentDownloaded")

        query
            .append(" FROM ${TableNames.DOWNLOAD_STATE} AS d")
            .append(" JOIN ${TableNames.LESSON} AS l ON l.id = d.referenceId")
            .append(" JOIN ${TableNames.LESSON_STATE} AS s ON s.lessonId = l.id")
            .append(" LEFT JOIN ${TableNames.LESSON_OUTBOX} AS lo ON lo.lessonId = l.id")
            .append(
                """ 
             LEFT JOIN ${TableNames.OUTBOX} AS o
             ON o.referenceId = l.id AND 
             (o.actionType = '${ActionType.FAVOURITE}' OR o.actionType = '${ActionType.REMOVE_FAVOURITE}')
        """,
            )

        val args = ArrayList<Any?>()
        var filtered = false

        query.append(" WHERE")
        query.append(" d.referenceType = ? AND d.state != ? AND")
        args.add(ReferenceType.LESSON)
        args.add(DownloadState.REMOVING)
        filtered = true

        if (search.isNullOrBlank()) {
            if (filtered) {
                query.deleteRange(query.length - 4, query.length)
            } else {
                query.deleteRange(query.length - 6, query.length)
            }
        } else {
            query.append(" l.title LIKE ?")
            args.add("%${search.lowercase()}%")
        }

        return getDownloadedLessons(
            RoomRawQuery(query.toString()) { it.bind(args) },
        )
    }

    @RawQuery(observedEntities = [LessonEntity::class])
    fun getLessons(query: RoomRawQuery): PagingSource<Int, LessonWithState>

    @RawQuery(observedEntities = [LessonEntity::class, DownloadStateEntity::class])
    fun getDownloadedLessons(query: RoomRawQuery): PagingSource<Int, LessonWithState>
}
