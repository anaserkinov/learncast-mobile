package me.anasmusa.learncast.core.paging

import androidx.paging.PagingConfig

fun getDefaultPagingConfig() =
    PagingConfig(
        pageSize = 25,
        initialLoadSize = 50,
    )
