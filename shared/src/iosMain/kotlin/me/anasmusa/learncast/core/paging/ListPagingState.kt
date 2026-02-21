package me.anasmusa.learncast.core.paging

import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

actual class ListPagingState<T : Any> actual constructor(
    private val flow: Flow<PagingData<T>>,
) {
    /**
     * If the [flow] is a SharedFlow, it is expected to be the flow returned by from
     * pager.flow.cachedIn(scope) which could contain a cached PagingData. We pass the cached
     * PagingData to the presenter so that if the PagingData contains cached data, the presenter can
     * be initialized with the data prior to collection on pager.
     */
    private val pagingDataPresenter =
        object :
            PagingDataPresenter<T>(
                mainContext = Dispatchers.Main,
                cachedPagingData =
                    if (flow is SharedFlow<PagingData<T>>) flow.replayCache.firstOrNull() else null,
            ) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
                updateItemSnapshotList()
            }
        }

    /**
     * Contains the immutable [ItemSnapshotList] of currently presented items, including any
     * placeholders if they are enabled. Note that similarly to [peek] accessing the items in a list
     * will not trigger any loads. Use [get] to achieve such behavior.
     */
    val itemSnapshotList: StateFlow<ItemSnapshotList<T>>
        field = MutableStateFlow(pagingDataPresenter.snapshot())

    /** The number of items which can be accessed. */
    val itemCount: Int
        get() = itemSnapshotList.value.size

    private fun updateItemSnapshotList() {
        itemSnapshotList.value = pagingDataPresenter.snapshot()
    }

    /**
     * Returns the presented item at the specified position, notifying Paging of the item access to
     * trigger any loads necessary to fulfill prefetchDistance.
     *
     * @see peek
     */
    fun notify(index: Int) {
        try {
            pagingDataPresenter[index] // this registers the value load
        } catch (e: IndexOutOfBoundsException) {
        }
    }

    fun getCurrentSnapshot(): ItemSnapshotList<T> = itemSnapshotList.value

    /**
     * Returns the presented item at the specified position, without notifying Paging of the item
     * access that would normally trigger page loads.
     *
     * @param index Index of the presented item to return, including placeholders.
     * @return The presented item at position [index], `null` if it is a placeholder
     */
    fun peek(index: Int): T? {
        val list = itemSnapshotList.value
        return if (index in list.placeholdersBefore until (list.placeholdersBefore + list.items.size)) {
            list.items[index - list.placeholdersBefore]
        } else {
            null
        }
    }

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     * [LazyPagingItems].
     *
     * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
     * within the same generation of [PagingData].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     * * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     * * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    fun retry() {
        pagingDataPresenter.retry()
    }

    /**
     * Refresh the data presented by this [LazyPagingItems].
     *
     * [refresh] triggers the creation of a new [PagingData] with a new instance of [PagingSource]
     * to represent an updated snapshot of the backing dataset. If a [RemoteMediator] is set,
     * calling [refresh] will also trigger a call to [RemoteMediator.load] with [LoadType] [REFRESH]
     * to allow [RemoteMediator] to check for updates to the dataset backing [PagingSource].
     *
     * Note: This API is intended for UI-driven refresh signals, such as swipe-to-refresh.
     * Invalidation due repository-layer signals, such as DB-updates, should instead use
     * [PagingSource.invalidate].
     *
     * @see PagingSource.invalidate
     */
    fun refresh() {
        pagingDataPresenter.refresh()
    }

    /** A [CombinedLoadStates] object which represents the current loading state. */
    val loadState: StateFlow<CombinedLoadStates>
        field =
        MutableStateFlow(
            pagingDataPresenter.loadStateFlow.value
                ?: CombinedLoadStates(
                    refresh = InitialLoadStates.refresh,
                    prepend = InitialLoadStates.prepend,
                    append = InitialLoadStates.append,
                    source = InitialLoadStates,
                ),
        )

    actual suspend fun collectLoadState() {
        pagingDataPresenter.loadStateFlow.filterNotNull().collect { loadState.value = it }
    }

    actual suspend fun collectPagingData() {
        flow.collectLatest { pagingDataPresenter.collectFrom(it) }
    }
}

val IncompleteLoadState = LoadState.NotLoading(false)
val InitialLoadStates =
    LoadStates(LoadState.Loading, IncompleteLoadState, IncompleteLoadState)
