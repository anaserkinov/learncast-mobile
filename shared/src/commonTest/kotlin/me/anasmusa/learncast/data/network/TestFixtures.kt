package me.anasmusa.learncast.data.network

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headers
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.auth.model.Credentials
import me.anasmusa.learncast.data.network.auth.model.LoginRequest
import me.anasmusa.learncast.data.network.auth.model.LoginResponse
import me.anasmusa.learncast.data.network.auth.model.UserResponse
import me.anasmusa.learncast.data.network.author.model.AuthorResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.DeletedResponse
import me.anasmusa.learncast.data.network.common.model.FileResponse
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.lesson.model.LessonProgressResponse
import me.anasmusa.learncast.data.network.lesson.model.LessonResponse
import me.anasmusa.learncast.data.network.lesson.model.ListenSessionCreateRequest
import me.anasmusa.learncast.data.network.lesson.model.ListenSessionResponse
import me.anasmusa.learncast.data.network.lesson.model.UpdateProgressRequest
import me.anasmusa.learncast.data.network.snip.model.SnipCURequest
import me.anasmusa.learncast.data.network.snip.model.SnipCountResponse
import me.anasmusa.learncast.data.network.snip.model.SnipResponse
import me.anasmusa.learncast.data.network.topic.model.TopicResponse
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Centralized test fixtures and data builders for all network service tests.
 * Contains constants, builders, and helper functions to create test data.
 */
@OptIn(ExperimentalTime::class)
object TestFixtures {

    // ========== PAGINATION ==========
    object Pagination {
        const val MAX_PAGE_LIMIT = 100
        const val MIN_PAGE_LIMIT = 0
        const val DEFAULT_LIMIT = 10
        const val DEFAULT_CURSOR = "next_page_cursor"
    }

    // ========== AUTHENTICATION ==========
    object Auth {
        // User Data
        const val TEST_USER_ID = 1L
        const val TEST_USER_FIRST_NAME = "FirstName"
        const val TEST_USER_LAST_NAME = "LastName"
        const val TEST_USER_EMAIL = "user@example.com"
        const val TEST_USER_TELEGRAM_USERNAME = "telegram_user"
        const val TEST_USER_AVATAR_PATH = "/avatars/user1.jpg"

        // Authentication Data
        const val VALID_TELEGRAM_DATA = "valid_telegram"
        const val VALID_GOOGLE_DATA = "valid_google"
        const val VALID_ACCESS_TOKEN = "AccessToken123"
        const val VALID_REFRESH_TOKEN = "RefreshToken123"
        const val INVALID_DATA = "invalid"
        const val INVALID_REFRESH_TOKEN = "invalid_refresh_token"
        const val EXPIRED_REFRESH_TOKEN = "expired_refresh_token"

        fun createLoginRequest(
            telegramData: String? = null,
            googleData: String? = null,
        ) = LoginRequest(
            telegramData = telegramData,
            googleData = googleData,
        )

        fun createUserResponse(
            id: Long = TEST_USER_ID,
            firstName: String = TEST_USER_FIRST_NAME,
            lastName: String = TEST_USER_LAST_NAME,
            email: String = TEST_USER_EMAIL,
            telegramUsername: String = TEST_USER_TELEGRAM_USERNAME,
            avatarPath: String = TEST_USER_AVATAR_PATH,
        ) = UserResponse(
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            telegramUsername = telegramUsername,
            avatarPath = avatarPath,
        )

        fun createCredentials(
            accessToken: String = VALID_ACCESS_TOKEN,
            refreshToken: String = VALID_REFRESH_TOKEN,
        ) = Credentials(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )

        fun createLoginResponse(
            user: UserResponse = createUserResponse(),
            credentials: Credentials = createCredentials(),
        ) = LoginResponse(
            user = user,
            credentials = credentials,
        )
    }

    // ========== AUTHORS ==========
    object Authors {
        // Author 1 Data
        const val AUTHOR_1_ID = 1L
        const val AUTHOR_1_NAME = "John Doe"
        const val AUTHOR_1_AVATAR_PATH = "/avatars/john.jpg"
        const val AUTHOR_1_CREATED_AT = "2025-01-18T14:20:00Z"
        const val AUTHOR_1_LESSON_COUNT = 4L

        // Author 2 Data
        const val AUTHOR_2_ID = 2L
        const val AUTHOR_2_NAME = "Jane Smith"
        const val AUTHOR_2_CREATED_AT = "2025-01-18T14:20:00Z"
        const val AUTHOR_2_LESSON_COUNT = 4L

        // Deleted Authors
        const val DELETED_AUTHOR_1_ID = 1L
        const val DELETED_AUTHOR_1_DATE = "2026-01-01T15:30:00Z"
        const val DELETED_AUTHOR_2_ID = 2L
        const val DELETED_AUTHOR_2_DATE = "2026-01-02T15:30:00Z"

        fun author1() = AuthorResponse(
            id = AUTHOR_1_ID,
            name = AUTHOR_1_NAME,
            avatarPath = AUTHOR_1_AVATAR_PATH,
            createdAt = Instant.parse(AUTHOR_1_CREATED_AT),
            lessonCount = AUTHOR_1_LESSON_COUNT,
        )

        fun author2() = AuthorResponse(
            id = AUTHOR_2_ID,
            name = AUTHOR_2_NAME,
            avatarPath = null,
            createdAt = Instant.parse(AUTHOR_2_CREATED_AT),
            lessonCount = AUTHOR_2_LESSON_COUNT,
        )

        fun createAuthor(
            id: Long = AUTHOR_1_ID,
            name: String = AUTHOR_1_NAME,
            avatarPath: String? = AUTHOR_1_AVATAR_PATH,
            createdAt: String = AUTHOR_1_CREATED_AT,
            lessonCount: Long = AUTHOR_1_LESSON_COUNT,
        ) = AuthorResponse(
            id = id,
            name = name,
            avatarPath = avatarPath,
            createdAt = Instant.parse(createdAt),
            lessonCount = lessonCount,
        )

        fun authorsResponseList() = listOf(author1(), author2())

        fun deletedAuthorsList() = listOf(
            DeletedResponse(DELETED_AUTHOR_1_ID, Instant.parse(DELETED_AUTHOR_1_DATE)),
            DeletedResponse(DELETED_AUTHOR_2_ID, Instant.parse(DELETED_AUTHOR_2_DATE)),
        )
    }

    // ========== TOPICS ==========
    object Topics {
        // Topic Response IDs
        const val TOPIC_RESPONSE_1_ID = 1L
        const val TOPIC_1_COMPLETED_LESSONS = 3L
        const val TOPIC_RESPONSE_2_ID = 2L
        const val TOPIC_2_COMPLETED_LESSONS = 5L

        // Topic 1 Details
        const val TOPIC_1_ID = 1L
        const val TOPIC_1_TITLE = "Introduction to Kotlin"
        const val TOPIC_1_DESCRIPTION = "Learn Kotlin programming from scratch"
        const val TOPIC_1_COVER_IMAGE = "/images/kotlin-intro.jpg"
        const val TOPIC_1_CREATED_AT = "2026-01-24T15:40:00Z"
        const val TOPIC_1_LESSON_COUNT = 5L
        const val TOPIC_1_TOTAL_DURATION = 3000000L

        // Topic 2 Details
        const val TOPIC_2_ID = 2L
        const val TOPIC_2_TITLE = "Advanced Kotlin Concepts"
        const val TOPIC_2_CREATED_AT = "2026-01-04T15:40:00Z"
        const val TOPIC_2_LESSON_COUNT = 7L
        const val TOPIC_2_TOTAL_DURATION = 67000000L

        // Deleted Topics
        const val DELETED_TOPIC_1_ID = 1L
        const val DELETED_TOPIC_1_DATE = "2026-01-01T15:30:00Z"
        const val DELETED_TOPIC_2_ID = 2L
        const val DELETED_TOPIC_2_DATE = "2026-01-02T15:30:00Z"

        fun topic1() = TopicResponse.Topic(
            id = TOPIC_1_ID,
            title = TOPIC_1_TITLE,
            description = TOPIC_1_DESCRIPTION,
            coverImagePath = TOPIC_1_COVER_IMAGE,
            createdAt = Instant.parse(TOPIC_1_CREATED_AT),
            lessonCount = TOPIC_1_LESSON_COUNT,
            totalDuration = TOPIC_1_TOTAL_DURATION,
        )

        fun topic2() = TopicResponse.Topic(
            id = TOPIC_2_ID,
            title = TOPIC_2_TITLE,
            description = null,
            coverImagePath = null,
            createdAt = Instant.parse(TOPIC_2_CREATED_AT),
            lessonCount = TOPIC_2_LESSON_COUNT,
            totalDuration = TOPIC_2_TOTAL_DURATION,
        )

        fun createTopic(
            id: Long = TOPIC_1_ID,
            title: String = TOPIC_1_TITLE,
            description: String? = TOPIC_1_DESCRIPTION,
            coverImagePath: String? = TOPIC_1_COVER_IMAGE,
            createdAt: String = TOPIC_1_CREATED_AT,
            lessonCount: Long = TOPIC_1_LESSON_COUNT,
            totalDuration: Long = TOPIC_1_TOTAL_DURATION,
        ) = TopicResponse.Topic(
            id = id,
            title = title,
            description = description,
            coverImagePath = coverImagePath,
            createdAt = Instant.parse(createdAt),
            lessonCount = lessonCount,
            totalDuration = totalDuration,
        )

        fun topicResponse1() = TopicResponse(
            id = TOPIC_RESPONSE_1_ID,
            topic = topic1(),
            author = Authors.author1(),
            completedLessonCount = TOPIC_1_COMPLETED_LESSONS,
        )

        fun topicResponse2() = TopicResponse(
            id = TOPIC_RESPONSE_2_ID,
            topic = topic2(),
            author = Authors.author2(),
            completedLessonCount = TOPIC_2_COMPLETED_LESSONS,
        )

        fun topicResponsesList() = listOf(topicResponse1(), topicResponse2())

        fun deletedTopicsList() = listOf(
            DeletedResponse(DELETED_TOPIC_1_ID, Instant.parse(DELETED_TOPIC_1_DATE)),
            DeletedResponse(DELETED_TOPIC_2_ID, Instant.parse(DELETED_TOPIC_2_DATE)),
        )
    }

    // ========== FILES (AUDIO) ==========
    object Files {
        const val AUDIO_PATH = "/audio/lesson-2.mp3"
        const val AUDIO_SIZE = 7340032L
        const val AUDIO_DURATION = 450000L

        fun audio() = FileResponse(
            path = AUDIO_PATH,
            size = AUDIO_SIZE,
            duration = AUDIO_DURATION,
        )

        fun createAudio(
            path: String = AUDIO_PATH,
            size: Long = AUDIO_SIZE,
            duration: Long = AUDIO_DURATION,
        ) = FileResponse(
            path = path,
            size = size,
            duration = duration,
        )
    }

    // ========== PROGRESS ==========
    object Progress {
        const val PROGRESS_STARTED_AT = "2025-01-20T08:00:00Z"
        const val PROGRESS_LAST_POSITION = 45000L
        const val PROGRESS_COMPLETED_AT = "2025-01-25T10:30:00Z"

        fun inProgress(
            lessonId: Long = 1L,
            lastPositionMs: Long = PROGRESS_LAST_POSITION,
        ) = LessonProgressResponse(
            lessonId = lessonId,
            startedAt = Instant.parse(PROGRESS_STARTED_AT),
            lastPositionMs = lastPositionMs,
            status = UserProgressStatus.IN_PROGRESS,
            completedAt = Instant.parse(PROGRESS_COMPLETED_AT),
        )

        fun createProgress(
            lessonId: Long = 1L,
            startedAt: String = PROGRESS_STARTED_AT,
            lastPositionMs: Long = PROGRESS_LAST_POSITION,
            status: UserProgressStatus = UserProgressStatus.IN_PROGRESS,
            completedAt: String? = PROGRESS_COMPLETED_AT,
        ) = LessonProgressResponse(
            lessonId = lessonId,
            startedAt = Instant.parse(startedAt),
            lastPositionMs = lastPositionMs,
            status = status,
            completedAt = completedAt?.let { Instant.parse(it) },
        )

        fun createUpdateProgressRequest(
            status: UserProgressStatus = UserProgressStatus.IN_PROGRESS,
            startedAt: Instant = Clock.System.now(),
            completedAt: Instant? = null,
            lastPositionMs: Long = 5000L,
        ) = UpdateProgressRequest(
            status = status,
            startedAt = startedAt,
            completedAt = completedAt,
            lastPositionMs = lastPositionMs,
        )
    }

    // ========== LESSONS ==========
    object Lessons {
        // Lesson IDs
        const val VALID_LESSON_ID = 1L
        const val INVALID_LESSON_ID = 2L

        // Lesson 1 Data
        const val LESSON_1_ID = 1L
        const val LESSON_1_TITLE = "Introduction to Kotlin Multiplatform"
        const val LESSON_1_DESCRIPTION = "Learn the basics of KMP development"
        const val LESSON_1_COVER_IMAGE = "/images/kotlin-kmp.jpg"
        const val LESSON_1_LISTEN_COUNT = 150L
        const val LESSON_1_SNIP_COUNT = 12L
        const val LESSON_1_CREATED_AT = "2025-01-15T10:30:00Z"

        // Lesson 2 Data
        const val LESSON_2_ID = 2L
        const val LESSON_2_TITLE = "Advanced Coroutines in Kotlin"
        const val LESSON_2_LISTEN_COUNT = 230L
        const val LESSON_2_SNIP_COUNT = 18L
        const val LESSON_2_CREATED_AT = "2025-01-18T14:20:00Z"

        // Listen Session
        const val VALID_SESSION_ID = "session-123"

        // Deleted Lessons
        const val DELETED_LESSON_1_ID = 5L
        const val DELETED_LESSON_1_DATE = "2025-01-22T12:00:00Z"
        const val DELETED_LESSON_2_ID = 8L
        const val DELETED_LESSON_2_DATE = "2025-01-23T09:30:00Z"

        fun lesson1() = LessonResponse(
            id = LESSON_1_ID,
            title = LESSON_1_TITLE,
            description = LESSON_1_DESCRIPTION,
            coverImagePath = LESSON_1_COVER_IMAGE,
            author = Authors.author1(),
            topic = Topics.topic1(),
            audio = Files.audio(),
            listenCount = LESSON_1_LISTEN_COUNT,
            snipCount = LESSON_1_SNIP_COUNT,
            createdAt = Instant.parse(LESSON_1_CREATED_AT),
            isFavourite = true,
            progress = Progress.inProgress(LESSON_1_ID),
        )

        fun lesson2() = LessonResponse(
            id = LESSON_2_ID,
            title = LESSON_2_TITLE,
            description = null,
            coverImagePath = null,
            author = Authors.author2(),
            topic = Topics.topic2(),
            audio = Files.audio(),
            listenCount = LESSON_2_LISTEN_COUNT,
            snipCount = LESSON_2_SNIP_COUNT,
            createdAt = Instant.parse(LESSON_2_CREATED_AT),
            isFavourite = null,
            progress = null,
        )

        fun createLesson(
            id: Long = LESSON_1_ID,
            title: String = LESSON_1_TITLE,
            description: String? = LESSON_1_DESCRIPTION,
            coverImagePath: String? = LESSON_1_COVER_IMAGE,
            author: AuthorResponse = Authors.author1(),
            topic: TopicResponse.Topic? = Topics.topic1(),
            audio: FileResponse = Files.audio(),
            listenCount: Long = LESSON_1_LISTEN_COUNT,
            snipCount: Long = LESSON_1_SNIP_COUNT,
            createdAt: String = LESSON_1_CREATED_AT,
            isFavourite: Boolean? = true,
            progress: LessonProgressResponse? = Progress.inProgress(),
        ) = LessonResponse(
            id = id,
            title = title,
            description = description,
            coverImagePath = coverImagePath,
            author = author,
            topic = topic,
            audio = audio,
            listenCount = listenCount,
            snipCount = snipCount,
            createdAt = Instant.parse(createdAt),
            isFavourite = isFavourite,
            progress = progress,
        )

        fun lessonResponsesList() = listOf(lesson1(), lesson2())

        fun deletedLessonsList() = listOf(
            DeletedResponse(DELETED_LESSON_1_ID, Instant.parse(DELETED_LESSON_1_DATE)),
            DeletedResponse(DELETED_LESSON_2_ID, Instant.parse(DELETED_LESSON_2_DATE)),
        )

        fun createListenSessionRequest(
            sessionId: String = VALID_SESSION_ID,
            createdAt: Instant = Clock.System.now(),
        ) = ListenSessionCreateRequest(
            sessionId = sessionId,
            createdAt = createdAt,
        )

        fun createListenSessionResponse(
            listenCount: Long = 1L,
        ) = ListenSessionResponse(
            listenCount = listenCount,
        )
    }

    // ========== SNIPS ==========
    object Snips {
        // Snip IDs
        const val VALID_SNIP_ID = "snip-123"
        const val INVALID_SNIP_ID = "snip-999"

        // Snip 1 Data
        const val SNIP_1_ID = 1L
        const val SNIP_1_CLIENT_ID = "snip-123"
        const val SNIP_1_START_MS = 10000L
        const val SNIP_1_END_MS = 15000L
        const val SNIP_1_NOTE = "Important concept about coroutines"
        const val SNIP_1_CREATED_AT = "2025-01-20T14:30:00Z"
        const val SNIP_1_USER_COUNT = 5L

        // Snip 2 Data
        const val SNIP_2_ID = 2L
        const val SNIP_2_CLIENT_ID = "snip-456"
        const val SNIP_2_START_MS = 45000L
        const val SNIP_2_END_MS = 52000L
        const val SNIP_2_CREATED_AT = "2025-01-21T09:15:00Z"

        // Snip Counts
        const val LESSON_1_SNIP_COUNT = 5L
        const val LESSON_1_SNIP_COUNT_AFTER_DELETE = 4L

        // Deleted Snips
        const val DELETED_SNIP_1_ID = 10L
        const val DELETED_SNIP_1_DATE = "2025-01-22T15:30:00Z"
        const val DELETED_SNIP_2_ID = 15L
        const val DELETED_SNIP_2_DATE = "2025-01-23T11:45:00Z"

        fun snip1() = SnipResponse(
            id = SNIP_1_ID,
            clientSnipId = SNIP_1_CLIENT_ID,
            startMs = SNIP_1_START_MS,
            endMs = SNIP_1_END_MS,
            note = SNIP_1_NOTE,
            lesson = Lessons.lesson1(),
            createdAt = Instant.parse(SNIP_1_CREATED_AT),
            userSnipCount = SNIP_1_USER_COUNT,
        )

        fun snip2() = SnipResponse(
            id = SNIP_2_ID,
            clientSnipId = SNIP_2_CLIENT_ID,
            startMs = SNIP_2_START_MS,
            endMs = SNIP_2_END_MS,
            note = null,
            lesson = Lessons.lesson2(),
            createdAt = Instant.parse(SNIP_2_CREATED_AT),
            userSnipCount = null,
        )

        fun createSnip(
            id: Long = SNIP_1_ID,
            clientSnipId: String = SNIP_1_CLIENT_ID,
            startMs: Long = SNIP_1_START_MS,
            endMs: Long = SNIP_1_END_MS,
            note: String? = SNIP_1_NOTE,
            lesson: LessonResponse = Lessons.lesson1(),
            createdAt: String = SNIP_1_CREATED_AT,
            userSnipCount: Long? = SNIP_1_USER_COUNT,
        ) = SnipResponse(
            id = id,
            clientSnipId = clientSnipId,
            startMs = startMs,
            endMs = endMs,
            note = note,
            lesson = lesson,
            createdAt = Instant.parse(createdAt),
            userSnipCount = userSnipCount,
        )

        fun snipResponsesList() = listOf(snip1(), snip2())

        fun deletedSnipsList() = listOf(
            DeletedResponse(DELETED_SNIP_1_ID, Instant.parse(DELETED_SNIP_1_DATE)),
            DeletedResponse(DELETED_SNIP_2_ID, Instant.parse(DELETED_SNIP_2_DATE)),
        )

        fun createSnipRequest(
            clientSnipId: String = "snip-test-123",
            startMs: Long = 10000L,
            endMs: Long = 15000L,
            note: String? = null,
            createdAt: Instant = Clock.System.now(),
        ) = SnipCURequest(
            clientSnipId = clientSnipId,
            startMs = startMs,
            endMs = endMs,
            note = note,
            createdAt = createdAt,
        )

        fun createSnipCountResponse(
            lessonId: Long = Lessons.VALID_LESSON_ID,
            count: Long = LESSON_1_SNIP_COUNT,
        ) = SnipCountResponse(
            lessonId = lessonId,
            count = count,
        )
    }

    // ========== COMMON REQUEST BUILDERS ==========
    object Requests {
        fun pageRequest(
            limit: Int = Pagination.DEFAULT_LIMIT,
            cursor: String? = null,
        ) = PageRequestQuery(
            limit = limit,
            cursor = cursor,
        )

        fun deletedRequest(
            since: Instant = Clock.System.now() - 1.days,
        ) = DeletedRequestQuery(
            since = since,
        )
    }

    // ========== RESPONSE WRAPPERS ==========
    @OptIn(ExperimentalTime::class)
    inline fun <reified T> T.wrapInBaseResponse(message: String? = null): String {
        return buildJsonObject {
            put("data", Json.encodeToJsonElement(this@wrapInBaseResponse))
            put("message", message?.let { JsonPrimitive(it) } ?: JsonNull)
            put("time", JsonPrimitive(Clock.System.now().toString()))
        }.let { Json.encodeToString(it) }
    }

    @OptIn(ExperimentalTime::class)
    inline fun <reified T> List<T>.wrapInPageResponse(nextCursor: String? = "next_page_cursor", message: String? = null): String {
        return buildJsonObject {
            put(
                "data",
                buildJsonObject {
                    put("items", Json.encodeToJsonElement(this@wrapInPageResponse))
                    put("next_cursor", nextCursor)
                }
            )
            put("message", message?.let { JsonPrimitive(it) } ?: JsonNull)
            put("time", JsonPrimitive(Clock.System.now().toString()))
        }.let { Json.encodeToString(it) }
    }

    @OptIn(ExperimentalTime::class)
    fun baseResponse(message: String? = null): String {
        val baseResponse = buildJsonObject {
            put("data", JsonNull)
            put("message", message?.let { JsonPrimitive(it) } ?: JsonNull)
            put("time", JsonPrimitive(Clock.System.now().toString()))
        }

        return Json.encodeToString(baseResponse)
    }

    inline fun <reified T> OutgoingContent.parse(): T {
        return Json.decodeFromString<T>(
            (this as OutgoingContent.ByteArrayContent)
                .bytes()
                .decodeToString()
        )
    }

    fun MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: Headers = headersOf()
    ): HttpResponseData {
        return respond(
            content = content,
            status = status,
            headers = headers {
                appendAll(headers)
                append(HttpHeaders.ContentType, "application/json")
            }
        )
    }

}
