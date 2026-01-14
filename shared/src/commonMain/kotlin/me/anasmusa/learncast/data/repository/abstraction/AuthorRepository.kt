package me.anasmusa.learncast.data.repository.abstraction

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.Author

interface AuthorRepository {
    fun page(search: String?): Flow<PagingData<Author>>
}
