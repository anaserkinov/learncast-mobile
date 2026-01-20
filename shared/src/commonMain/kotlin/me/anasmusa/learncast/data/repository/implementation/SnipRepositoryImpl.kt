package me.anasmusa.learncast.data.repository.implementation

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.anasmusa.learncast.Resource.string
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.core.toResult
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.queue.QueueItemDao
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.local.db.snip.SnipEntity
import me.anasmusa.learncast.data.mapper.toUI
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.model.Snip
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.service.SnipService
import me.anasmusa.learncast.data.paging.CommonPager
import me.anasmusa.learncast.data.paging.SnipMediator
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import me.anasmusa.learncast.data.repository.abstraction.SnipRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class SnipRepositoryImpl(
    private val snipService: SnipService,
    private val snipDao: SnipDao,
    private val outboxDao: OutboxDao,
    private val lessonDao: LessonDao,
    private val queueItemDao: QueueItemDao,
    private val pagingStateDao: PagingStateDao,
    private val outboxRepository: OutboxRepository,
) : SnipRepository {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun save(
        clientSnipId: String,
        queueItemId: Long,
        startMs: Long,
        endMs: Long,
        note: String?,
    ): Result<Unit> {
        return try {
            val queueItem =
                queueItemDao.getById(queueItemId)
                    ?: return Result.Fail(Strings.NOT_FOUND.string())

            if (clientSnipId == "") {
                val snip =
                    SnipEntity(
                        clientSnipId = if (clientSnipId == "") Uuid.random().toHexString() else clientSnipId,
                        id = 0,
                        startMs = startMs,
                        endMs = endMs,
                        note = note?.trim()?.takeUnless { it.isEmpty() },
                        createdAt = nowLocalDateTime(),
                        lessonId = queueItem.lessonId,
                        title = queueItem.title,
                        description = queueItem.description,
                        coverImagePath = queueItem.coverImagePath,
                        authorId = queueItem.authorId,
                        authorName = queueItem.authorName,
                        topicId = queueItem.topicId,
                        topicTitle = queueItem.topicTitle,
                        audioPath = queueItem.audioPath,
                        audioSize = queueItem.audioSize,
                        audioDuration = queueItem.audioDuration,
                    )
                snipDao.insert(snip)
                outboxRepository.createSnip(
                    snip.clientSnipId,
                    snip.lessonId,
                    snip.startMs,
                    snip.endMs,
                    snip.note,
                )
            } else {
                outboxRepository.updateSnip(
                    clientSnipId,
                    queueItem.lessonId,
                    startMs,
                    endMs,
                    note,
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            e.toResult()
        }
    }

    override suspend fun get(clientSnipId: String): Result<Snip> {
        return try {
            val entity =
                snipDao.getByClientSnipId(clientSnipId)
                    ?: return Result.Fail(Strings.NOT_FOUND.string())
            Result.Success(entity.toUI())
        } catch (e: Exception) {
            e.toResult()
        }
    }

    override suspend fun delete(clientSnipId: String) {
        outboxRepository.deleteSnip(clientSnipId)
    }

    override fun page(
        search: String?,
        lessonId: Long?,
        sort: QuerySort?,
        order: QueryOrder?,
    ): Flow<PagingData<Snip>> =
        CommonPager(
            config =
                PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                ),
            commonMediator =
                SnipMediator(
                    service = snipService,
                    snipDao = snipDao,
                    outboxDao = outboxDao,
                    pagingStateDao = pagingStateDao,
                    request =
                        PageRequestQuery(
                            search = search,
                            lessonId = lessonId,
                            sort = sort,
                            order = order,
                        ),
                ),
            pagingSourceFactory = {
                snipDao.getSnips(
                    search = search,
                    authorId = null,
                    topicId = null,
                    lessonId = lessonId,
                    sort = sort,
                    order = order,
                )
            },
        ).flow.map {
            it.map { entry ->
                entry.toUI()
            }
        }

    override suspend fun getSnipCount(lessonId: Long): Int =
        try {
            try {
                snipService.count(lessonId)?.let {
                    lessonDao.updateUserSnipCount(
                        lessonId,
                        it.data.count,
                    )
                }
            } catch (e: Exception) {
            }

            lessonDao.getUserSnipCount(lessonId).toInt()
        } catch (e: Exception) {
            0
        }
}
