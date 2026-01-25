package me.anasmusa.learncast.data.network.topic

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.common.model.BaseResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.DeletedResponse
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.common.model.PageResponse
import me.anasmusa.learncast.data.network.topic.model.TopicResponse

internal class TopicService(
    private val client: HttpClient,
) {
    companion object {
        const val PAGE = "v1/user/topic"
        const val DELETED = "v1/user/topic/deleted"
    }

    suspend fun page(requestQuery: PageRequestQuery) =
        client
            .get(PAGE) {
                headers.append(HttpHeaders.CacheControl, "no-cache")
                requestQuery.load(url)
            }.body<BaseResponse<PageResponse<TopicResponse>>>()

    suspend fun deleted(requestQuery: DeletedRequestQuery) =
        client
            .get(DELETED) {
                requestQuery.load(url)
            }.body<BaseResponse<List<DeletedResponse>>>()
}
