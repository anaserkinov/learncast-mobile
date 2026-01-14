package me.anasmusa.learncast

import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class SwiftPagingDataPresenter<T : Any>(
    mainContext: CoroutineContext = EmptyCoroutineContext,
    cachedPagingData: PagingData<T>? = null,
) : PagingDataPresenter<T>(mainContext, cachedPagingData) {
    lateinit var onEvent: (PagingDataEvent<T>) -> Unit

    override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
        onEvent(event)
    }

    suspend fun collectFromPagingData(pagingData: PagingData<T>) {
        collectFrom(pagingData)
    }
}

fun <T : Any> createSwiftPagingPresenter(
    cached: PagingData<T>?,
): SwiftPagingDataPresenter<T> = SwiftPagingDataPresenter(cachedPagingData = cached)
