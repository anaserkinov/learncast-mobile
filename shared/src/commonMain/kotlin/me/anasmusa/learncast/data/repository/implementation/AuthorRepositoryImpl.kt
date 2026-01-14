package me.anasmusa.learncast.data.repository.implementation

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.anasmusa.learncast.data.local.db.author.AuthorDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.mapper.toUI
import me.anasmusa.learncast.data.model.Author
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.service.AuthorService
import me.anasmusa.learncast.data.paging.AuthorMediator
import me.anasmusa.learncast.data.paging.CommonPager
import me.anasmusa.learncast.data.repository.abstraction.AuthorRepository

internal class AuthorRepositoryImpl(
    private val authorService: AuthorService,
    private val authorDao: AuthorDao,
    private val pagingStateDao: PagingStateDao,
) : AuthorRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun page(search: String?): Flow<PagingData<Author>> =
        CommonPager(
            config =
                PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                ),
            commonMediator =
                AuthorMediator(
                    service = authorService,
                    authorDao = authorDao,
                    pagingStateDao = pagingStateDao,
                    request =
                        PageRequestQuery(
                            search = search,
                        ),
                ),
            pagingSourceFactory = {
                authorDao.getAuthors(search = search)
            },
        ).flow.map {
            it.map { entry ->
                entry.toUI()
            }
        }
}
