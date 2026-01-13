package me.anasmusa.learncast.data.network.service

import me.anasmusa.learncast.data.network.model.BaseResponse
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.model.PageResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.model.DeletedResponse
import me.anasmusa.learncast.data.network.model.author.AuthorResponse

internal class AuthorService(private val client: HttpClient) {

    suspend fun page(requestQuery: PageRequestQuery) =  client.get("v1/user/author"){
        headers.append(HttpHeaders.CacheControl, "no-cache")
        requestQuery.load(url)
    }.body<BaseResponse<PageResponse<AuthorResponse>>>()

    suspend fun deleted(requestQuery: DeletedRequestQuery) =  client.get("v1/user/author/deleted"){
        requestQuery.load(url)
    }.body<BaseResponse<List<DeletedResponse>>>()

}