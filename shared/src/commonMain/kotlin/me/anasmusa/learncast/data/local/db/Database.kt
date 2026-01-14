package me.anasmusa.learncast.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import me.anasmusa.learncast.data.local.db.author.AuthorDao
import me.anasmusa.learncast.data.local.db.author.AuthorEntity
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.lesson.LessonEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonStateEntity
import me.anasmusa.learncast.data.local.db.outbox.LessonOutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.ListenOutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.SnipOutboxEntity
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateDao
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateEntity
import me.anasmusa.learncast.data.local.db.queue.QueueItemDao
import me.anasmusa.learncast.data.local.db.queue.QueueItemEntity
import me.anasmusa.learncast.data.local.db.queue.QueueItemWithState
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.local.db.snip.SnipEntity
import me.anasmusa.learncast.data.local.db.topic.TopicDao
import me.anasmusa.learncast.data.local.db.topic.TopicEntity

@Database(
    entities = [
        AuthorEntity::class,
        TopicEntity::class,
        LessonEntity::class,
        LessonStateEntity::class,
        QueueItemEntity::class,
        SnipEntity::class,
        OutboxEntity::class,
        LessonOutboxEntity::class,
        SnipOutboxEntity::class,
        ListenOutboxEntity::class,
        PagingStateEntity::class,
        DownloadStateEntity::class,
    ],
    views = [
        QueueItemWithState::class,
    ],
    version = 1,
)
@TypeConverters(
    LocalDateTimeConverter::class,
    DurationConverter::class,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun getAuthorDao(): AuthorDao

    internal abstract fun getTopicDao(): TopicDao

    internal abstract fun getLessonDao(): LessonDao

    internal abstract fun getSnipDao(): SnipDao

    internal abstract fun getQueueItemDao(): QueueItemDao

    internal abstract fun getOutboxDao(): OutboxDao

    internal abstract fun getPagingStateDao(): PagingStateDao

    internal abstract fun getDownloadDao(): DownloadDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun getAppDatabase(): AppDatabase =
    getDatabaseBuilder()
        .fallbackToDestructiveMigration(true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
