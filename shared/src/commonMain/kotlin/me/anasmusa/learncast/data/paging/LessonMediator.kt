package me.anasmusa.learncast.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import me.anasmusa.learncast.core.nowInstant
import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.core.toUTCInstant
import me.anasmusa.learncast.data.local.db.TableNames
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.lesson.LessonStateInput
import me.anasmusa.learncast.data.local.db.lesson.LessonWithState
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateEntity
import me.anasmusa.learncast.data.mapper.toEntity
import me.anasmusa.learncast.data.mapper.toInput
import me.anasmusa.learncast.data.network.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.model.lesson.LessonResponse
import me.anasmusa.learncast.data.network.service.LessonService
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalPagingApi::class)
internal class LessonMediator(
    private val service: LessonService,
    private val lessonDao: LessonDao,
    private val pagingStateDao: PagingStateDao,
    private val request: PageRequestQuery,
    private val isDownloaded: Boolean?,
) : CommonMediator<Int, LessonWithState>() {
    private var lastItemId: Long? = null
    override val hasItemLoaded: Boolean
        get() = lastItemId != null

    override fun isLastLoadedItem(item: LessonWithState): Boolean = item.lesson.id == lastItemId

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, LessonWithState>,
    ): MediatorResult {
        if (isDownloaded != null) {
            return MediatorResult.Success(true)
        }

        val requestStringKey = request.toStringKey()
        var pagingState: PagingStateEntity? = null
        var deletedTime: Instant? = null
        var updatedTime: Instant? = null
        return try {
            pagingState = pagingStateDao.get(TableNames.LESSON, requestStringKey)
            when (loadType) {
                LoadType.REFRESH -> {
                    if (pagingState?.lastDeletionSync != null &&
                        nowInstant() - pagingState.lastDeletionSync.toUTCInstant() >= 10.toDuration(DurationUnit.MINUTES)
                    ) {
                        val response = service.deleted(DeletedRequestQuery(pagingState.lastDeletionSync.toUTCInstant()))
                        lessonDao.delete(response.data.map { it.id })
                        deletedTime = response.time
                    }
                    request.cursor = null
                }

                LoadType.PREPEND -> return MediatorResult.Success(true)
                LoadType.APPEND -> state.lastItemOrNull() ?: return MediatorResult.Success(true)
            }

            val lessons = mutableListOf<LessonResponse>()

            var responseTime: Instant? = null
            if (request.authorId != null && request.topicId != null) {
                while (true) {
                    val response = service.page(request)
                    if (responseTime == null) responseTime = response.time
                    request.cursor = response.data.nextCursor
                    lessons.addAll(response.data.items)
                    if (request.cursor == null) {
                        break
                    }
                }
            } else {
                val response = service.page(request)
                responseTime = response.time
                request.cursor = response.data.nextCursor
                lessons.addAll(response.data.items)
            }

            val states = mutableListOf<LessonStateInput>()
            lessonDao.insert(
                lessons =
                    lessons.map {
                        states.add(it.toInput())
                        it.toEntity()
                    },
                states = states,
            )
            lessons.lastOrNull()?.let { lastItemId = it.id }
            updatedTime = responseTime

            MediatorResult.Success(request.cursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        } finally {
            if (updatedTime != null && pagingState?.lastDeletionSync == null || deletedTime != null) {
                try {
                    pagingStateDao.upsert(
                        PagingStateEntity(
                            resourceType = TableNames.LESSON,
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
