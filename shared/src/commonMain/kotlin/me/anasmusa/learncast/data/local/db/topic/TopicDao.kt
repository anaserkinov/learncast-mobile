package me.anasmusa.learncast.data.local.db.topic

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.bind

@Dao
internal interface TopicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<TopicEntity>)

    @Query("DELETE FROM ${TableNames.TOPIC} WHERE id in (:ids)")
    suspend fun delete(ids: List<Long>)

    fun getTopics(
        search: String?,
        authorId: Long?
    ): PagingSource<Int, TopicEntity>{

        val query = StringBuilder("SElECT * FROM ${TableNames.TOPIC} WHERE ")
        val args = ArrayList<Any?>()
        var filtered = false

        if (authorId != null){
            query.append(" authorId = ? AND")
            args.add(authorId)
            filtered = true
        }

        if (search.isNullOrBlank()) {
            if (filtered) query.deleteRange(query.length - 4, query.length)
            else query.deleteRange(query.length - 6, query.length)
        } else {
            query.append(" title LIKE ?")
            args.add("%${search}%")
        }

        query.append(" ORDER BY id DESC")

        return getTopics(
            RoomRawQuery(query.toString()){ it.bind(args) }
        )
    }

    @RawQuery(observedEntities = [TopicEntity::class])
    fun getTopics(query: RoomRawQuery): PagingSource<Int, TopicEntity>

}