package me.anasmusa.learncast.core.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

expect class ListPagingState<T : Any>(
    flow: Flow<PagingData<T>>,
) {
    suspend fun collectLoadState()

    suspend fun collectPagingData()
}
