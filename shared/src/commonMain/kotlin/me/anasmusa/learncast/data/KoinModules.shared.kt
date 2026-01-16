package me.anasmusa.learncast.data

import me.anasmusa.learncast.core.getOrCreateScope
import me.anasmusa.learncast.data.local.localModule
import me.anasmusa.learncast.data.network.networkModule
import me.anasmusa.learncast.data.repository.abstraction.AppRepository
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository
import me.anasmusa.learncast.data.repository.abstraction.AuthorRepository
import me.anasmusa.learncast.data.repository.abstraction.LessonRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import me.anasmusa.learncast.data.repository.abstraction.SnipRepository
import me.anasmusa.learncast.data.repository.abstraction.StorageRepository
import me.anasmusa.learncast.data.repository.abstraction.SyncRepository
import me.anasmusa.learncast.data.repository.abstraction.TopicRepository
import me.anasmusa.learncast.data.repository.abstraction.UserRepository
import me.anasmusa.learncast.data.repository.implementation.AppRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.AuthRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.AuthorRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.LessonRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.PlayerRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.QueueRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.SnipRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.StorageRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.SyncRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.TopicRepositoryImpl
import me.anasmusa.learncast.data.repository.implementation.UserRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.module

object AppScope {
    const val ID = "app-ui-scope"
}

internal expect fun Module.platformModule()

internal fun dataModule() =
    module {
        platformModule()

        localModule()
        networkModule()

        factory<AppRepository> {
            AppRepositoryImpl(
                get(),
            )
        }

        factory<AuthRepository> {
            AuthRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        factory<TopicRepository> {
            TopicRepositoryImpl(
                get(),
                get(),
                get(),
            )
        }
        factory<AuthorRepository> {
            AuthorRepositoryImpl(
                get(),
                get(),
                get(),
            )
        }
        factory<LessonRepository> {
            LessonRepositoryImpl(
                get(),
                get(),
                get(),
            )
        }
        factory<SnipRepository> {
            SnipRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        factory<QueueRepository> {
            QueueRepositoryImpl(
                get(),
                get(),
                get(),
            )
        }
        factory<UserRepository> {
            UserRepositoryImpl(
                get(),
            )
        }
        factory<SyncRepository> {
            SyncRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }

        factory<StorageRepository> {
            StorageRepositoryImpl(
                get(),
                get(),
            )
        }

        scope<AppScope> {
            scoped<PlayerRepository> {
                PlayerRepositoryImpl(get())
            }
        }
        factory<PlayerRepository> {
            getOrCreateScope<AppScope>(AppScope.ID).get()
        }
    }
