package me.anasmusa.learncast.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import me.anasmusa.learncast.core.nowInstant
import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.core.toUTCInstant
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateEntity
import me.anasmusa.learncast.data.local.db.topic.TopicDao
import me.anasmusa.learncast.data.local.db.topic.TopicEntity
import me.anasmusa.learncast.data.mapper.toEntity
import me.anasmusa.learncast.data.network.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.service.TopicService
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalPagingApi::class)
internal class TopicMediator(
    private val service: TopicService,
    private val topicDao: TopicDao,
    private val pagingStateDao: PagingStateDao,
    private val request: PageRequestQuery,
) : CommonMediator<Int, TopicEntity>() {
    private var lastItemId: Long? = null
    override val hasItemLoaded: Boolean
        get() = lastItemId != null

    override fun isLastLoadedItem(item: TopicEntity): Boolean = item.id == lastItemId

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TopicEntity>,
    ): MediatorResult {
        val requestStringKey = request.toStringKey()
        var pagingState: PagingStateEntity? = null
        var deletedTime: Instant? = null
        var updatedTime: Instant? = null
        return try {
            pagingState = pagingStateDao.get(TableNames.TOPIC, requestStringKey)
            when (loadType) {
                LoadType.REFRESH -> {
                    if (pagingState?.lastDeletionSync != null &&
                        nowInstant() - pagingState.lastDeletionSync.toUTCInstant() >= 10.toDuration(DurationUnit.MINUTES)
                    ) {
                        val response = service.deleted(DeletedRequestQuery(pagingState.lastDeletionSync.toUTCInstant()))
                        topicDao.delete(response.data.map { it.id })
                        deletedTime = response.time
                    }
                    request.cursor = null
                }
                LoadType.PREPEND -> return MediatorResult.Success(true)
                LoadType.APPEND -> state.lastItemOrNull() ?: return MediatorResult.Success(true)
            }

            val response = service.page(request)
            request.cursor = response.data.nextCursor

            topicDao.insert(
                response.data.items.map { it.toEntity() },
            )
            response.data.items
                .lastOrNull()
                ?.let { lastItemId = it.id }
            updatedTime = response.time

            MediatorResult.Success(request.cursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        } finally {
            if (updatedTime != null && pagingState?.lastDeletionSync == null || deletedTime != null) {
                try {
                    pagingStateDao.upsert(
                        PagingStateEntity(
                            resourceType = TableNames.TOPIC,
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
