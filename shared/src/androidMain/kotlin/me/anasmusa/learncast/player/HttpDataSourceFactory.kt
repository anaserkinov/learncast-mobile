package me.anasmusa.learncast.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.runBlocking
import me.anasmusa.learncast.data.network.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Response

@UnstableApi
internal class HttpDataSourceFactory(
    private val tokenManager: TokenManager,
) : HttpDataSource.Factory {
    private var lastRequestUrl: String? = null
    private var lastRedirectUrl: String? = null

    private val defaultDataSourceFactory =
        OkHttpDataSource.Factory(
            OkHttpClient
                .Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor {
                    val request = it.request()
                    if (lastRequestUrl != request.url.toString()) {
                        lastRedirectUrl = null
                    }
                    lastRequestUrl = request.url.toString()
                    var response: Response? = null
                    var requestCount = 0
                    while (true) {
                        response?.close()
                        if (lastRedirectUrl != null) {
                            response =
                                it.proceed(
                                    request
                                        .newBuilder()
                                        .url(lastRedirectUrl!!)
                                        .build(),
                                )
                            if (response.code == 403) {
                                if (requestCount >= 1) break
                                lastRedirectUrl = null
                            } else {
                                break
                            }
                        }

                        val tokens = runBlocking { tokenManager.getTokens() }
                        response =
                            it.proceed(
                                request
                                    .newBuilder()
                                    .addHeader(
                                        "Authorization",
                                        "Bearer ${tokens?.second}",
                                    ).build(),
                            )
                        if (response.isRedirect) {
                            if (requestCount >= 2) break
                            lastRedirectUrl = response.header("Location") ?: break
                        } else if (response.code == 401) {
                            runBlocking {
                                if (tokenManager.refreshToken(tokens?.second ?: "") == null) return@runBlocking response
                            }
                        } else {
                            break
                        }

                        requestCount++
                    }
                    response
                }.build(),
        )

    override fun createDataSource(): HttpDataSource = defaultDataSourceFactory.createDataSource()

    override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): HttpDataSource.Factory = this
}
