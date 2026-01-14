package me.anasmusa.learncast.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import kotlinx.coroutines.coroutineScope
import me.anasmusa.learncast.core.nowInstant
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.core.toUTCInstant
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateEntity
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.local.db.snip.SnipEntity
import me.anasmusa.learncast.data.mapper.toEntity
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.network.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.service.SnipService
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalPagingApi::class)
internal class SnipMediator(
    private val service: SnipService,
    private val snipDao: SnipDao,
    private val outboxDao: OutboxDao,
    private val pagingStateDao: PagingStateDao,
    private val request: PageRequestQuery,
) : CommonMediator<Int, SnipEntity>() {
    private var lastItemId: Long? = null
    override val hasItemLoaded: Boolean
        get() = lastItemId != null

    override fun isLastLoadedItem(item: SnipEntity): Boolean = item.id == lastItemId

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SnipEntity>,
    ): MediatorResult {
        val requestStringKey = request.toStringKey()
        var pagingState: PagingStateEntity? = null
        var deletedTime: Instant? = null
        var updatedTime: Instant? = null
        return try {
            pagingState = pagingStateDao.get(TableNames.SNIP, requestStringKey)
            when (loadType) {
                LoadType.REFRESH -> {
                    if (pagingState?.lastDeletionSync != null &&
                        nowInstant() - pagingState.lastDeletionSync.toUTCInstant() >= 10.toDuration(DurationUnit.MINUTES)
                    ) {
                        val response =
                            service.deleted(DeletedRequestQuery(pagingState.lastDeletionSync.toUTCInstant()))
                        val ids = response.data.map { it.id }
                        snipDao.delete(ids)

                        coroutineScope {
                            outboxDao.clearDeleteActions(ids, ReferenceType.SNIP)
                        }

                        deletedTime = response.time
                    }
                    request.cursor = null
                }

                LoadType.PREPEND -> return MediatorResult.Success(true)
                LoadType.APPEND -> state.lastItemOrNull() ?: return MediatorResult.Success(true)
            }

            val response = service.page(request)
            request.cursor = response.data.nextCursor

            val uuids = ArrayList<String>(response.data.items.size)

            snipDao.insert(
                response.data.items.map {
                    uuids.add(it.clientSnipId)
                    it.toEntity()
                },
            )
            response.data.items
                .lastOrNull()
                ?.let { lastItemId = it.id }
            updatedTime = response.time

            coroutineScope {
                outboxDao.clearCreateActions(uuids, nowLocalDateTime())
            }

            MediatorResult.Success(request.cursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        } finally {
            if (updatedTime != null && pagingState?.lastDeletionSync == null || deletedTime != null) {
                try {
                    pagingStateDao.upsert(
                        PagingStateEntity(
                            resourceType = TableNames.SNIP,
                            queryKey = requestStringKey,
                            lastDeletionSync =
                                deletedTime?.toDateTime()
                                    ?: pagingState?.lastDeletionSync ?: updatedTime!!.toDateTime(),
                        ),
                    )
                } catch (e: Exception) {
                }
            }
        }
    }
}
