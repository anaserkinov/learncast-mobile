package me.anasmusa.learncast.data.local.db

import androidx.paging.PagingSource
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.author.AuthorEntity
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonStateInput
import me.anasmusa.learncast.data.local.db.topic.TopicEntity
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.time.Duration.Companion.minutes

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
            topicId: Long = id,
            createdAt: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0),
        ): TopicEntity {
            return TopicEntity(
                id = id,
                topicId = topicId,
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

        fun createLessons(
            count: Int,
            topicId: Long = 1,
            authorId: Long = 1
        ): List<LessonEntity> {
            return (1..count).map { index ->
                createLesson(
                    id = index.toLong(),
                    title = "Lesson $index",
                    topicId = topicId,
                    authorId = authorId,
                    createdAt = LocalDateTime(2024, 1, index, 0, 0, 0)
                )
            }
        }

        fun createLessonStateInput(
            lessonId: Long,
            listenCount: Long = 0,
            snipCount: Long = 0,
            userSnipCount: Long = 0,
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
