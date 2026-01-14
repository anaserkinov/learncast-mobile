package me.anasmusa.learncast.data.repository.implementation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.topic.TopicDao
import me.anasmusa.learncast.data.mapper.toUI
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.service.TopicService
import me.anasmusa.learncast.data.paging.CommonPager
import me.anasmusa.learncast.data.paging.TopicMediator
import me.anasmusa.learncast.data.repository.abstraction.TopicRepository

internal class TopicRepositoryImpl(
    private val topicService: TopicService,
    private val topicDao: TopicDao,
    private val pagingStateDao: PagingStateDao,
) : TopicRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun page(
        search: String?,
        authorId: Long?,
    ): Flow<PagingData<Topic>> =
        CommonPager(
            config =
                PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                ),
            commonMediator =
                TopicMediator(
                    service = topicService,
                    topicDao = topicDao,
                    pagingStateDao = pagingStateDao,
                    request =
                        PageRequestQuery(
                            search = search,
                            authorId = authorId,
                        ),
                ),
            pagingSourceFactory = {
                topicDao.getTopics(
                    search = search,
                    authorId = authorId,
                )
            },
        ).flow.map {
            it.map { entry ->
                entry.toUI()
            }
        }
}
