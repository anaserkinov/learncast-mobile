package me.anasmusa.learncast.core.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

actual class ListPagingState<T : Any> actual constructor(
    flow: Flow<PagingData<T>>,
) {
    actual suspend fun collectLoadState() {
    }

    actual suspend fun collectPagingData() {
    }
}
