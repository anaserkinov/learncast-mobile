package me.anasmusa.learncast

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.getSampleLesson

fun getLessonSamplePagingData(count: Int): Flow<PagingData<Lesson>> =
    flowOf(
        PagingData.from(
            MutableList(count) {
                getSampleLesson(id = it + 1L)
            },
        ),
    )
