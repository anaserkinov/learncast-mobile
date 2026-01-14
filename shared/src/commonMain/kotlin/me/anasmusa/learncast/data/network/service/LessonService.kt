package me.anasmusa.learncast.data.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.model.BaseResponse
import me.anasmusa.learncast.data.network.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.model.DeletedResponse
import me.anasmusa.learncast.data.network.model.PageRequestQuery
import me.anasmusa.learncast.data.network.model.PageResponse
import me.anasmusa.learncast.data.network.model.lesson.LessonProgressResponse
import me.anasmusa.learncast.data.network.model.lesson.LessonResponse
import me.anasmusa.learncast.data.network.model.lesson.ListenSessionCreateRequest
import me.anasmusa.learncast.data.network.model.lesson.ListenSessionResponse
import me.anasmusa.learncast.data.network.model.lesson.UpdateProgressRequest

internal class LessonService(
    private val client: HttpClient,
) {
    suspend fun page(requestQuery: PageRequestQuery) =
        client
            .get("v1/user/lesson") {
                headers.append(HttpHeaders.CacheControl, "no-cache")
                requestQuery.load(url)
            }.body<BaseResponse<PageResponse<LessonResponse>>>()

    suspend fun deleted(requestQuery: DeletedRequestQuery) =
        client
            .get("v1/user/lesson/deleted") {
                requestQuery.load(url)
            }.body<BaseResponse<List<DeletedResponse>>>()

    suspend fun updateProgress(
        lessonId: Long,
        request: UpdateProgressRequest,
    ) = client
        .patch(
            "v1/user/lesson/$lessonId/progress",
        ) {
            setBody(request)
        }.body<BaseResponse<LessonProgressResponse>>()

    suspend fun listen(
        lessonId: Long,
        request: ListenSessionCreateRequest,
    ) = client
        .post(
            "v1/user/lesson/$lessonId/listen",
        ) {
            setBody(request)
        }.body<BaseResponse<ListenSessionResponse>>()

    suspend fun setFavourite(lessonId: Long) =
        client
            .post(
                "v1/user/lesson/$lessonId/favourite",
            ).body<BaseResponse<Unit?>>()

    suspend fun removeFavourite(lessonId: Long) =
        client
            .delete(
                "v1/user/lesson/$lessonId/favourite",
            ).body<BaseResponse<Unit?>>()
}
