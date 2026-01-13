package me.anasmusa.learncast.data.repository.abstraction

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.model.Snip

interface SnipRepository {

    suspend fun save(
        clientSnipId: String,
        queueItemId: Long,
        startMs: Long,
        endMs: Long,
        note: String?
    ): Result<Unit>

    suspend fun get(clientSnipId: String): Result<Snip>

    suspend fun delete(clientSnipId: String)

    fun page(
        search: String? = null,
        lessonId: Long? = null,
        sort: QuerySort? = null,
        order: QueryOrder? = null
    ): Flow<PagingData<Snip>>

    suspend fun getSnipCount(lessonId: Long): Int


}