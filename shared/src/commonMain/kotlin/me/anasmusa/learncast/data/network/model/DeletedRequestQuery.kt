package me.anasmusa.learncast.data.network.model

import io.ktor.http.URLBuilder
import kotlin.time.Instant

class DeletedRequestQuery(
    val since: Instant,
) {
    fun load(url: URLBuilder) {
        url.apply {
            parameters.append("since", since.toString())
        }
    }
}
