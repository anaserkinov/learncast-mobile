package me.anasmusa.learncast.data.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.bodyIfNotCache
import me.anasmusa.learncast.data.network.model.BaseResponse
import me.anasmusa.learncast.data.network.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.model.DeletedResponse
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.model.PageResponse
import me.anasmusa.learncast.data.network.model.snip.SnipCURequest
import me.anasmusa.learncast.data.network.model.snip.SnipCountResponse
import me.anasmusa.learncast.data.network.model.snip.SnipResponse

internal class SnipService(
    private val client: HttpClient,
) {
    suspend fun page(requestQuery: PageRequestQuery) =
        client
            .get("v1/user/lesson/snip") {
                headers.append(HttpHeaders.CacheControl, "no-cache")
                requestQuery.load(url)
            }.body<BaseResponse<PageResponse<SnipResponse>>>()

    suspend fun count(lessonId: Long) =
        client
            .get("v1/user/lesson/$lessonId/snip/count")
            .bodyIfNotCache<BaseResponse<SnipCountResponse>>()

    suspend fun create(
        lessonId: Long,
        request: SnipCURequest,
    ) = client
        .post("v1/user/lesson/$lessonId/snip") {
            setBody(request)
        }.body<BaseResponse<SnipResponse>>()

    suspend fun update(
        clientSnipId: String,
        request: SnipCURequest,
    ) = client
        .put("v1/user/lesson/snip/$clientSnipId") {
            setBody(request)
        }.body<BaseResponse<SnipResponse>>()

    suspend fun delete(clientSnipId: String) =
        client
            .delete("v1/user/lesson/snip/$clientSnipId")
            .body<BaseResponse<SnipCountResponse?>>()

    suspend fun deleted(requestQuery: DeletedRequestQuery) =
        client
            .get("v1/user/lesson/snip/deleted") {
                requestQuery.load(url)
            }.body<BaseResponse<List<DeletedResponse>>>()
}
