package me.anasmusa.learncast.data.repository.implementation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.mapper.toUI
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.service.LessonService
import me.anasmusa.learncast.data.paging.CommonPager
import me.anasmusa.learncast.data.paging.LessonMediator
import me.anasmusa.learncast.data.repository.abstraction.LessonRepository

internal class LessonRepositoryImpl(
    private val lessonService: LessonService,
    private val lessonDao: LessonDao,
    private val pagingStateDao: PagingStateDao,
) : LessonRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun page(
        search: String?,
        authorId: Long?,
        topicId: Long?,
        isFavourite: Boolean?,
        status: UserProgressStatus?,
        isDownloaded: Boolean?,
        sort: QuerySort?,
        order: QueryOrder?,
    ): Flow<PagingData<Lesson>> =
        CommonPager(
            config =
                PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                ),
            commonMediator =
                LessonMediator(
                    service = lessonService,
                    lessonDao = lessonDao,
                    pagingStateDao = pagingStateDao,
                    request =
                        PageRequestQuery(
                            search = search,
                            authorId = authorId,
                            topicId = topicId,
                            favourite = isFavourite,
                            status = status,
                            sort = sort,
                            order = order,
                        ),
                    isDownloaded = isDownloaded,
                ),
            pagingSourceFactory = {
                if (isDownloaded != null) {
                    lessonDao.getDownloadedLessons(
                        search = search,
                        isDownloaded = isDownloaded,
                    )
                } else {
                    lessonDao.getLessons(
                        search = search,
                        authorId = authorId,
                        topicId = topicId,
                        isFavourite = isFavourite,
                        isDownloaded = isDownloaded,
                        status = status,
                        sort = sort,
                        order = order,
                    )
                }
            },
        ).flow.map {
            it.map { entry ->
                entry.toUI()
            }
        }
}
