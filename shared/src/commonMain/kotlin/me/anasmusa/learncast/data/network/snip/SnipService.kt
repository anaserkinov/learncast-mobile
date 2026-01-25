package me.anasmusa.learncast.data.network.snip

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.bodyIfNotCache
import me.anasmusa.learncast.data.network.common.model.BaseResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.DeletedResponse
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.common.model.PageResponse
import me.anasmusa.learncast.data.network.snip.model.SnipCURequest
import me.anasmusa.learncast.data.network.snip.model.SnipCountResponse
import me.anasmusa.learncast.data.network.snip.model.SnipResponse

internal class SnipService(
    private val client: HttpClient,
) {
    companion object {
        const val PAGE_PATH = "v1/user/lesson/snip"

        fun countPath(lessonId: Long) = "v1/user/lesson/$lessonId/snip/count"

        fun createPath(lessonId: Long) = "v1/user/lesson/$lessonId/snip"

        fun updatePath(clientSnipId: String) = "v1/user/lesson/snip/$clientSnipId"

        fun deletePath(clientSnipId: String) = "v1/user/lesson/snip/$clientSnipId"

        const val DELETED_PATH = "v1/user/lesson/snip/deleted"
    }

    suspend fun page(requestQuery: PageRequestQuery) =
        client
            .get(PAGE_PATH) {
                headers.append(HttpHeaders.CacheControl, "no-cache")
                requestQuery.load(url)
            }.body<BaseResponse<PageResponse<SnipResponse>>>()

    suspend fun count(lessonId: Long) =
        client
            .get(countPath(lessonId))
            .bodyIfNotCache<BaseResponse<SnipCountResponse>>()

    suspend fun create(
        lessonId: Long,
        request: SnipCURequest,
    ) = client
        .post(createPath(lessonId)) {
            setBody(request)
        }.body<BaseResponse<SnipResponse>>()

    suspend fun update(
        clientSnipId: String,
        request: SnipCURequest,
    ) = client
        .put(updatePath(clientSnipId)) {
            setBody(request)
        }.body<BaseResponse<SnipResponse>>()

    suspend fun delete(clientSnipId: String) =
        client
            .delete(deletePath(clientSnipId))
            .body<BaseResponse<SnipCountResponse?>>()

    suspend fun deleted(requestQuery: DeletedRequestQuery) =
        client
            .get(DELETED_PATH) {
                requestQuery.load(url)
            }.body<BaseResponse<List<DeletedResponse>>>()
}
