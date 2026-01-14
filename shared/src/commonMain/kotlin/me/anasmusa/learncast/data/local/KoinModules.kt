package me.anasmusa.learncast.data.local

import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.DBConnection
import me.anasmusa.learncast.data.local.db.author.AuthorDao
import me.anasmusa.learncast.data.local.db.createDBConnection
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.getAppDatabase
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.queue.QueueItemDao
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.local.db.topic.TopicDao
import me.anasmusa.learncast.data.local.storage.StorageManager
import me.anasmusa.learncast.data.local.storage.createStorageManager
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import me.anasmusa.learncast.data.repository.implementation.OutboxRepositoryImpl
import org.koin.core.module.Module

internal fun Module.localModule() {
    single {
        Preferences()
    }
    single {
        getAppDatabase()
    }

    factory<StorageManager> {
        createStorageManager()
    }

    factory<DBConnection> {
        createDBConnection(get())
    }

    single<OutboxRepository> {
        OutboxRepositoryImpl(
            get(),
            get(),
        )
    }

    factory<AuthorDao> {
        get<AppDatabase>().getAuthorDao()
    }
    factory<TopicDao> {
        get<AppDatabase>().getTopicDao()
    }
    factory<LessonDao> {
        get<AppDatabase>().getLessonDao()
    }
    factory<SnipDao> {
        get<AppDatabase>().getSnipDao()
    }
    factory<QueueItemDao> {
        get<AppDatabase>().getQueueItemDao()
    }
    factory<OutboxDao> {
        get<AppDatabase>().getOutboxDao()
    }
    factory<PagingStateDao> {
        get<AppDatabase>().getPagingStateDao()
    }
    factory<DownloadDao> {
        get<AppDatabase>().getDownloadDao()
    }
}
