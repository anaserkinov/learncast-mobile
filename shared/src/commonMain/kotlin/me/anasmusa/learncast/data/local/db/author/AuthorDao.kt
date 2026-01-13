package me.anasmusa.learncast.data.local.db.author

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
internal interface AuthorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<AuthorEntity>)

    @Query("DELETE FROM ${TableNames.AUTHOR} WHERE id in (:ids)")
    suspend fun delete(ids: List<Long>)

    fun getAuthors(
        search: String?
    ): PagingSource<Int, AuthorEntity>{
        val query = StringBuilder("SElECT * FROM ${TableNames.AUTHOR} WHERE ")
        val args = ArrayList<Any?>()

        if (search.isNullOrBlank())
            query.deleteRange(query.length - 6, query.length)
        else {
            query.append("name LIKE ?")
            args.add("%${search}%")
        }

        query.append(" ORDER BY lessonCount DESC, id DESC")

        return getAuthors(
            RoomRawQuery(query.toString()){ it.bind(args) }
        )
    }

    @RawQuery(observedEntities = [AuthorEntity::class])
    fun getAuthors(query: RoomRawQuery): PagingSource<Int, AuthorEntity>

}