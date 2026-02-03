package me.anasmusa.learncast.data.local.db

import androidx.paging.PagingSource
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.core.toUTCInstant
import me.anasmusa.learncast.data.local.db.author.AuthorEntity
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonStateInput
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.local.db.pagingstate.PagingStateEntity
import me.anasmusa.learncast.data.local.db.queue.QueueItemEntity
import me.anasmusa.learncast.data.local.db.snip.SnipEntity
import me.anasmusa.learncast.data.local.db.topic.TopicEntity
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.OutboxStatus
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object TestFixtures {

    object Author {
        fun createAuthor(
            id: Long,
            name: String,
            lessonCount: Long,
            avatarPath: String? = null,
            createdAt: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
        ): AuthorEntity {
            return AuthorEntity(
                id = id,
                name = name,
                avatarPath = avatarPath,
                createdAt = createdAt,
                lessonCount = lessonCount,
            )
        }

        /**
         * Creates a list of authors with sequential IDs starting from 1
         *
         * @param count Number of authors to create
         * @param lessonCountRange Range of lesson counts (default: 1..10)
         */
        fun createAuthors(
            count: Int,
            lessonCountRange: IntRange = 1..10
        ): List<AuthorEntity> {
            return (1..count).map { index ->
                createAuthor(
                    id = index.toLong(),
                    name = "Author $index",
                    lessonCount = lessonCountRange.random().toLong(),
                    avatarPath = if (index%2 == 1) "/avatars/author_$index.jpg" else null,
                )
            }
        }

        /**
         * Creates authors with specific names for testing search functionality
         */
        fun createAuthorsWithNames(
            vararg names: String,
            lessonCountRange: IntRange = 1..10
        ): List<AuthorEntity> {
            return names.mapIndexed { index, name ->
                createAuthor(
                    id = (index + 1).toLong(),
                    name = name,
                    lessonCount = lessonCountRange.random().toLong(),
                )
            }
        }
    }

    object Topic {
        fun createTopic(
            id: Long,
            title: String = "Topic $id",
            description: String? = "Description for topic $id",
            coverImagePath: String? = "/images/topic_$id.jpg",
            authorId: Long = 1,
            authorName: String = "Author $authorId",
            lessonCount: Long = 10,
            totalDuration: kotlin.time.Duration = 60.minutes,
            completedLessonCount: Long = 0,
            createdAt: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
        ): TopicEntity {
            return TopicEntity(
                id = id,
                title = title,
                description = description,
                coverImagePath = coverImagePath,
                authorId = authorId,
                authorName = authorName,
                createdAt = createdAt,
                lessonCount = lessonCount,
                totalDuration = totalDuration,
                completedLessonCount = completedLessonCount,
            )
        }

        fun createTopics(count: Int): List<TopicEntity> {
            return (1..count).map { index ->
                createTopic(
                    id = index.toLong(),
                    title = "Topic $index",
                    authorId = (index % 3) + 1L,
                    authorName = "Author ${(index % 3) + 1}",
                    lessonCount = (index * 2).toLong(),
                    totalDuration = (index * 30).minutes,
                    completedLessonCount = index.toLong()
                )
            }
        }

        fun createTopicsWithTitles(vararg titles: String): List<TopicEntity> {
            return titles.mapIndexed { index, title ->
                createTopic(
                    id = (index + 1).toLong(),
                    title = title,
                    authorId = (index % 3) + 1L,
                    authorName = "Author ${(index % 3) + 1}"
                )
            }
        }
    }

    object Download {
        fun createDownloadState(
            id: Long,
            referenceId: Long = 1,
            referenceUuid: String = "default-uuid",
            referenceType: ReferenceType = ReferenceType.LESSON,
            audioPath: String = "/default/audio.mp3",
            startMs: Long? = 0L,
            endMs: Long? = 10000L,
            state: DownloadState = DownloadState.DOWNLOADING,
            percentDownloaded: Float = 0f,
        ): DownloadStateEntity {
            return DownloadStateEntity(
                id = id,
                referenceId = referenceId,
                referenceUuid = referenceUuid,
                referenceType = referenceType,
                audioPath = audioPath,
                startMs = startMs,
                endMs = endMs,
                state = state,
                percentDownloaded = percentDownloaded,
            )
        }
    }

    object Lesson {
        fun createLesson(
            id: Long,
            title: String = "Lesson $id",
            description: String? = "Description for lesson $id",
            coverImagePath: String? = "/images/lesson_$id.jpg",
            authorId: Long = 1,
            authorName: String = "Author $authorId",
            topicId: Long? = 1,
            topicTitle: String? = "Topic $topicId",
            audioPath: String = "/audio/lesson_$id.mp3",
            audioSize: Long = 1024000,
            audioDuration: kotlin.time.Duration = 60.minutes,
            createdAt: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
        ): LessonEntity {
            return LessonEntity(
                id = id,
                title = title,
                description = description,
                coverImagePath = coverImagePath,
                authorId = authorId,
                authorName = authorName,
                topicId = topicId,
                topicTitle = topicTitle,
                audioPath = audioPath,
                audioSize = audioSize,
                audioDuration = audioDuration,
                createdAt = createdAt,
            )
        }

        @OptIn(ExperimentalTime::class)
        fun createLessons(
            count: Int,
            topicId: Long = 1,
            authorId: Long = 1
        ): List<LessonEntity> {
            val baseDate = LocalDateTime(2024, 1, 1, 0, 0, 0).toUTCInstant()

            return (1..count).map { index ->
                createLesson(
                    id = index.toLong(),
                    title = "Lesson $index",
                    topicId = topicId,
                    authorId = authorId,
                    createdAt = baseDate.plus(index, DateTimeUnit.DAY, TimeZone.UTC).toDateTime()
                )
            }
        }

        fun createLessonStateInput(
            lessonId: Long,
            listenCount: Long = 0,
            snipCount: Long = 0,
            isFavourite: Boolean = false,
            startedAt: LocalDateTime? = null,
            lastPositionMs: kotlin.time.Duration? = null,
            status: UserProgressStatus = UserProgressStatus.NOT_STARTED,
            completedAt: LocalDateTime? = null,
        ): LessonStateInput {
            return LessonStateInput(
                lessonId = lessonId,
                listenCount = listenCount,
                snipCount = snipCount,
                isFavourite = isFavourite,
                startedAt = startedAt,
                lastPositionMs = lastPositionMs,
                status = status,
                completedAt = completedAt,
            )
        }
    }

    object Outbox {
        fun createOutboxEntity(
            id: Long,
            referenceId: Long,
            referenceUuid: String,
            referenceType: ReferenceType,
            actionType: ActionType,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime = createdAt,
            lastTriedAt: LocalDateTime? = null,
            status: OutboxStatus = OutboxStatus.PENDING
        ): OutboxEntity {
            return OutboxEntity(
                id = id,
                referenceId = referenceId,
                referenceUuid = referenceUuid,
                referenceType = referenceType,
                actionType = actionType,
                createdAt = createdAt,
                updatedAt = updatedAt,
                lastTriedAt = lastTriedAt,
                status = status
            )
        }

    }

    internal object Snip {
        fun createSnip(
            id: Long,
            clientSnipId: String,
            startMs: Long = 1000,
            endMs: Long = 5000,
            note: String? = "Test snip note",
            createdAt: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
            lessonId: Long = 1,
            title: String = "Lesson $lessonId",
            description: String? = "Description",
            coverImagePath: String? = "/image.jpg",
            authorId: Long = 1,
            authorName: String = "Author 1",
            topicId: Long? = 1,
            topicTitle: String? = "Topic 1",
            audioPath: String = "/audio.mp3",
            audioSize: Long = 1024000,
            audioDuration: kotlin.time.Duration = 60.minutes
        ): SnipEntity {
            return SnipEntity(
                clientSnipId = clientSnipId,
                id = id,
                startMs = startMs,
                endMs = endMs,
                note = note,
                createdAt = createdAt,
                lessonId = lessonId,
                title = title,
                description = description,
                coverImagePath = coverImagePath,
                authorId = authorId,
                authorName = authorName,
                topicId = topicId,
                topicTitle = topicTitle,
                audioPath = audioPath,
                audioSize = audioSize,
                audioDuration = audioDuration
            )
        }


        fun createSnips(count: Int): List<SnipEntity> {
            return (1..count).map { index ->
                createSnip(
                    clientSnipId = "snip-$index",
                    id = index.toLong(),
                    note = "Note $index"
                )
            }
        }

    }

    object Queue {
        fun createQueueItem(
            id: Long,
            order: Int,
            referenceId: Long = 1,
            referenceUuid: String = "ref-uuid-$order",
            referenceType: ReferenceType = ReferenceType.LESSON,
            startMs: Long? = null,
            endMs: Long? = null,
            lessonId: Long = 1,
            title: String = "Queue Item $order",
            description: String? = "Description",
            coverImagePath: String? = "/image.jpg",
            authorId: Long = 1,
            authorName: String = "Author 1",
            topicId: Long? = 1,
            topicTitle: String? = "Topic 1",
            audioPath: String = "/audio.mp3",
            audioSize: Long = 1024000,
            audioDuration: kotlin.time.Duration = 60.minutes
        ): QueueItemEntity {
            return QueueItemEntity(
                id = id,
                order = order,
                referenceId = referenceId,
                referenceUuid = referenceUuid,
                referenceType = referenceType,
                startMs = startMs,
                endMs = endMs,
                lessonId = lessonId,
                title = title,
                description = description,
                coverImagePath = coverImagePath,
                authorId = authorId,
                authorName = authorName,
                topicId = topicId,
                topicTitle = topicTitle,
                audioPath = audioPath,
                audioSize = audioSize,
                audioDuration = audioDuration
            )
        }

        fun createQueueItems(count: Int): List<QueueItemEntity> {
            return (0 until count).map { index ->
                createQueueItem(
                    id = 0,
                    order = index,
                    title = "Queue Item $index"
                )
            }
        }
    }

    object PagingState {
        fun createPagingState(
            resourceType: String,
            queryKey: String,
            lastDeletionSync: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0)
        ): PagingStateEntity {
            return PagingStateEntity(
                resourceType = resourceType,
                queryKey = queryKey,
                lastDeletionSync = lastDeletionSync
            )
        }
    }

    suspend fun <T : Any> PagingSource<Int, T>.loadList(): List<T> {
        val loadResult = load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 100,
                placeholdersEnabled = false,
            )
        )
        return when (loadResult) {
            is PagingSource.LoadResult.Page -> loadResult.data
            else -> emptyList()
        }
    }

}
