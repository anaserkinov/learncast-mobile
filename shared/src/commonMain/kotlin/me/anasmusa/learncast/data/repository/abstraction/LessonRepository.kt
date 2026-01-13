package me.anasmusa.learncast.data.repository.abstraction

import androidx.paging.PagingData
import me.anasmusa.learncast.data.model.Topic
import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.UserProgressStatus

interface LessonRepository {

    fun page(
        search: String? = null,
        authorId: Long? = null,
        topicId: Long? = null,
        isFavourite: Boolean? = null,
        status: UserProgressStatus? = null,
        isDownloaded: Boolean? = null,
        sort: QuerySort? = null,
        order: QueryOrder? = null
    ): Flow<PagingData<Lesson>>

}