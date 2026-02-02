package me.anasmusa.learncast

import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.Dispatchers

class SwiftPagingDataPresenter<T : Any>(
    cachedPagingData: PagingData<T>? = null,
) : PagingDataPresenter<T>(Dispatchers.Main, cachedPagingData) {
    lateinit var onEvent: (PagingDataEvent<T>) -> Unit

    override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
        onEvent(event)
    }
}
