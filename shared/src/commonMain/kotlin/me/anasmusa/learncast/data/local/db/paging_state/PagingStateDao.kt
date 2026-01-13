package me.anasmusa.learncast.data.local.db.paging_state

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import me.anasmusa.learncast.data.local.db.TableNames

@Dao
internal interface PagingStateDao {

    @Upsert
    suspend fun upsert(item: PagingStateEntity)

    @Query("SELECT * FROM ${TableNames.PAGING_STATE} WHERE resourceType = :resourceType AND queryKey = :queryKey")
    suspend fun get(resourceType: String, queryKey: String): PagingStateEntity?

}