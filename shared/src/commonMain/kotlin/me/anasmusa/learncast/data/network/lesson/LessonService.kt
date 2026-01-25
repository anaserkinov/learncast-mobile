package me.anasmusa.learncast.data.network.lesson

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.common.model.BaseResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.DeletedResponse
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.common.model.PageResponse
import me.anasmusa.learncast.data.network.lesson.model.LessonProgressResponse
import me.anasmusa.learncast.data.network.lesson.model.LessonResponse
import me.anasmusa.learncast.data.network.lesson.model.ListenSessionCreateRequest
import me.anasmusa.learncast.data.network.lesson.model.ListenSessionResponse
import me.anasmusa.learncast.data.network.lesson.model.UpdateProgressRequest

internal class LessonService(
    private val client: HttpClient,
) {
    companion object {
        const val PAGE_PATH = "v1/user/lesson"
        const val DELETED_PATH = "v1/user/lesson/deleted"

        fun progressPath(lessonId: Long) = "v1/user/lesson/$lessonId/progress"

        fun listenPath(lessonId: Long) = "v1/user/lesson/$lessonId/listen"

        fun setFavouritePath(lessonId: Long) = "v1/user/lesson/$lessonId/favourite"

        fun removeFavouritePath(lessonId: Long) = "v1/user/lesson/$lessonId/favourite"
    }

    suspend fun page(requestQuery: PageRequestQuery) =
        client
            .get(PAGE_PATH) {
                headers.append(HttpHeaders.CacheControl, "no-cache")
                requestQuery.load(url)
            }.body<BaseResponse<PageResponse<LessonResponse>>>()

    suspend fun deleted(requestQuery: DeletedRequestQuery) =
        client
            .get(DELETED_PATH) {
                requestQuery.load(url)
            }.body<BaseResponse<List<DeletedResponse>>>()

    suspend fun updateProgress(
        lessonId: Long,
        request: UpdateProgressRequest,
    ) = client
        .patch(progressPath(lessonId)) {
            setBody(request)
        }.body<BaseResponse<LessonProgressResponse>>()

    suspend fun listen(
        lessonId: Long,
        request: ListenSessionCreateRequest,
    ) = client
        .post(listenPath(lessonId)) {
            setBody(request)
        }.body<BaseResponse<ListenSessionResponse>>()

    suspend fun setFavourite(lessonId: Long) = client.post(setFavouritePath(lessonId)).body<BaseResponse<Unit?>>()

    suspend fun removeFavourite(lessonId: Long) = client.delete(removeFavouritePath(lessonId)).body<BaseResponse<Unit?>>()
}
