package me.anasmusa.learncast.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator

@OptIn(ExperimentalPagingApi::class)
internal abstract class CommonMediator<Key : Any, Value : Any>(): RemoteMediator<Key, Value>(){
    abstract val hasItemLoaded: Boolean
    abstract fun isLastLoadedItem(item: Value): Boolean
}

@OptIn(ExperimentalPagingApi::class)
internal class CommonPager<Key : Any, Value : Any>(
    config: PagingConfig,
    initialKey: Key? = null,
    commonMediator: CommonMediator<Key, Value>,
    pagingSourceFactory: () -> PagingSource<Key, Value>,
){
    val flow = Pager(
        config = config,
        initialKey = initialKey,
        remoteMediator = commonMediator,
        pagingSourceFactory = {
            object: PagingSource<Key, Value>() {

                private val source = pagingSourceFactory.invoke()

                override val jumpingSupported: Boolean
                    get() = source.jumpingSupported
                override val keyReuseSupported: Boolean
                    get() = source.keyReuseSupported

                init {
                    source.registerInvalidatedCallback {
                        invalidate()
                    }
                }

                override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
                    val result = source.load(params)
                    if (commonMediator.hasItemLoaded && result is LoadResult.Page){
                        val index = result.data.indexOfLast(commonMediator::isLastLoadedItem)
                        if (index != -1){
                            val mutableList = result.data as MutableList
                            repeat(mutableList.size - index - 1){
                                mutableList.removeAt(mutableList.size - 1)
                            }
                            return LoadResult.Page(
                                mutableList,
                                result.prevKey,
                                null,
                                result.itemsBefore,
                                0
                            )
                        }
                    }
                    return result
                }

                override fun getRefreshKey(state: PagingState<Key, Value>) =
                    source.getRefreshKey(state)
            }
        }
    ).flow
}