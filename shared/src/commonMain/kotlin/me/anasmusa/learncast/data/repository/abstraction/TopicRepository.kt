package me.anasmusa.learncast.data.repository.abstraction

import androidx.paging.PagingData
import me.anasmusa.learncast.data.model.Topic
import kotlinx.coroutines.flow.Flow

interface TopicRepository {

    fun page(
        search: String? = null,
        authorId: Long? = null
    ): Flow<PagingData<Topic>>

}