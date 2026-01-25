package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import me.anasmusa.learncast.data.network.auth.AuthService
import me.anasmusa.learncast.data.network.author.AuthorService
import me.anasmusa.learncast.data.network.lesson.LessonService
import me.anasmusa.learncast.data.network.snip.SnipService
import me.anasmusa.learncast.data.network.topic.TopicService
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun Module.networkModule() {
    single<HttpClient> {
        createHttpClient {
            configure(
                getTokenManager = { get<TokenManager>() },
            )
        }
    }

    single {
        TokenManager(get(), get(), get())
    }
    services()
}

private fun Module.services() {
    factoryOf(::AuthService)
    factoryOf(::TopicService)
    factoryOf(::AuthorService)
    factoryOf(::LessonService)
    factoryOf(::SnipService)
}
