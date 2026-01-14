package me.anasmusa.learncast.data.repository.abstraction

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.Topic

interface TopicRepository {
    fun page(
        search: String? = null,
        authorId: Long? = null,
    ): Flow<PagingData<Topic>>
}
