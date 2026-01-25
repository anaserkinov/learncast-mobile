package me.anasmusa.learncast.data.network.lesson

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_AVATAR_PATH
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_NAME
import me.anasmusa.learncast.data.network.TestFixtures.Files.AUDIO_DURATION
import me.anasmusa.learncast.data.network.TestFixtures.Files.AUDIO_PATH
import me.anasmusa.learncast.data.network.TestFixtures.Files.AUDIO_SIZE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.DELETED_LESSON_1_DATE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.DELETED_LESSON_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.DELETED_LESSON_2_DATE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.DELETED_LESSON_2_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.INVALID_LESSON_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_COVER_IMAGE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_CREATED_AT
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_DESCRIPTION
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_LISTEN_COUNT
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_SNIP_COUNT
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_TITLE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_2_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_2_TITLE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.VALID_LESSON_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.VALID_SESSION_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.createListenSessionRequest
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.createListenSessionResponse
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.deletedLessonsList
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.lessonResponsesList
import me.anasmusa.learncast.data.network.TestFixtures.Pagination.MAX_PAGE_LIMIT
import me.anasmusa.learncast.data.network.TestFixtures.Progress.PROGRESS_COMPLETED_AT
import me.anasmusa.learncast.data.network.TestFixtures.Progress.PROGRESS_LAST_POSITION
import me.anasmusa.learncast.data.network.TestFixtures.Progress.PROGRESS_STARTED_AT
import me.anasmusa.learncast.data.network.TestFixtures.Progress.createProgress
import me.anasmusa.learncast.data.network.TestFixtures.Progress.createUpdateProgressRequest
import me.anasmusa.learncast.data.network.TestFixtures.Topics.TOPIC_1_DESCRIPTION
import me.anasmusa.learncast.data.network.TestFixtures.Topics.TOPIC_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Topics.TOPIC_1_TITLE
import me.anasmusa.learncast.data.network.TestFixtures.baseResponse
import me.anasmusa.learncast.data.network.TestFixtures.parse
import me.anasmusa.learncast.data.network.TestFixtures.respondJson
import me.anasmusa.learncast.data.network.TestFixtures.wrapInBaseResponse
import me.anasmusa.learncast.data.network.TestFixtures.wrapInPageResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.createTestHttpClient
import me.anasmusa.learncast.data.network.lesson.model.UpdateProgressRequest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LessonServiceTest : BehaviorSpec({

    val service = LessonService(createHttpClient())

    Given("LessonService") {

        When("requesting lesson page with default pagination") {

            And("server returns success with lessons list") {
                Then("returns paginated lessons with complete data including progress and topic") {
                    val response = service.page(PageRequestQuery())

                    response.data shouldNotBe null
                    response.data.items shouldHaveSize 2

                    // Verify first lesson with progress
                    response.data.items[0].apply {
                        id shouldBe LESSON_1_ID
                        title shouldBe LESSON_1_TITLE
                        description shouldBe LESSON_1_DESCRIPTION
                        coverImagePath shouldBe LESSON_1_COVER_IMAGE
                        listenCount shouldBe LESSON_1_LISTEN_COUNT
                        snipCount shouldBe LESSON_1_SNIP_COUNT
                        createdAt shouldBe Instant.parse(LESSON_1_CREATED_AT)
                        isFavourite shouldBe true

                        author.apply {
                            id shouldBe AUTHOR_1_ID
                            name shouldBe AUTHOR_1_NAME
                            avatarPath shouldBe AUTHOR_1_AVATAR_PATH
                        }

                        topic?.apply {
                            id shouldBe TOPIC_1_ID
                            title shouldBe TOPIC_1_TITLE
                            description shouldBe TOPIC_1_DESCRIPTION
                        }

                        audio.apply {
                            path shouldBe AUDIO_PATH
                            size shouldBe AUDIO_SIZE
                            duration shouldBe AUDIO_DURATION
                        }

                        progress?.apply {
                            lessonId shouldBe LESSON_1_ID
                            startedAt shouldBe Instant.parse(PROGRESS_STARTED_AT)
                            lastPositionMs shouldBe PROGRESS_LAST_POSITION
                            status shouldBe UserProgressStatus.IN_PROGRESS
                            completedAt shouldBe Instant.parse(PROGRESS_COMPLETED_AT)
                        }
                    }

                    // Verify second lesson without progress
                    response.data.items[1].apply {
                        id shouldBe LESSON_2_ID
                        title shouldBe LESSON_2_TITLE
                        description.shouldBeNull()
                        coverImagePath.shouldBeNull()
                        isFavourite.shouldBeNull()
                        progress.shouldBeNull()
                    }
                }
            }
        }

        When("requesting lesson page with custom pagination parameters") {

            withData(
                nameFn = { "returns success for limit=${it.first}, cursor=${it.second}" },
                listOf(
                    10 to null,
                    50 to null,
                    100 to null,
                    25 to "cursor_abc",
                )
            ) { (limit, cursor) ->
                val response = service.page(
                    PageRequestQuery(limit = limit, cursor = cursor)
                )
                response.data.items shouldHaveSize 2
            }
        }

        When("requesting lesson page with invalid parameters") {

            And("limit exceeds maximum allowed") {
                Then("throws ClientRequestException with BadRequest status") {
                    shouldThrow<ClientRequestException> {
                        service.page(PageRequestQuery(limit = 101))
                    }
                }
            }
        }

        When("requesting deleted lessons since specific date") {

            And("date is in the past") {
                Then("returns list of deleted lessons with deletion timestamps") {
                    val response = service.deleted(
                        DeletedRequestQuery(since = Clock.System.now() - 1.days)
                    )

                    response.data shouldNotBe null
                    response.data shouldHaveSize 2

                    response.data[0].apply {
                        id shouldBe DELETED_LESSON_1_ID
                        deletedAt shouldBe Instant.parse(DELETED_LESSON_1_DATE)
                    }

                    response.data[1].apply {
                        id shouldBe DELETED_LESSON_2_ID
                        deletedAt shouldBe Instant.parse(DELETED_LESSON_2_DATE)
                    }
                }
            }

            withData(
                nameFn = { "returns deleted lessons for ${it.first} days ago" },
                listOf(
                    "1" to 1.days,
                    "7" to 7.days,
                    "30" to 30.days,
                )
            ) { (_, duration) ->
                val response = service.deleted(
                    DeletedRequestQuery(since = Clock.System.now() - duration)
                )
                response.data shouldHaveSize 2
            }
        }

        When("requesting deleted lessons with invalid date") {

            And("date is in the future") {
                Then("throws ClientRequestException with BadRequest status") {
                    shouldThrow<ClientRequestException> {
                        service.deleted(
                            DeletedRequestQuery(since = Clock.System.now() + 1.days)
                        )
                    }
                }
            }
        }

        When("updating lesson progress") {

            And("lesson exists and progress data is valid") {
                Then("returns updated progress with correct status and position") {
                    val response = service.updateProgress(
                        lessonId = VALID_LESSON_ID,
                        request = createUpdateProgressRequest(
                            status = UserProgressStatus.IN_PROGRESS,
                            lastPositionMs = 5000L
                        )
                    )

                    response.data shouldNotBe null
                    response.data.lessonId shouldBe VALID_LESSON_ID
                    response.data.status shouldBe UserProgressStatus.IN_PROGRESS
                    response.data.lastPositionMs shouldBe 5000L
                }
            }

            withData(
                nameFn = { "updates progress successfully with status ${it.first}" },
                listOf(
                    "IN_PROGRESS" to UserProgressStatus.IN_PROGRESS,
                    "COMPLETED" to UserProgressStatus.COMPLETED,
                    "NOT_STARTED" to UserProgressStatus.NOT_STARTED,
                )
            ) { (_, status) ->
                val response = service.updateProgress(
                    lessonId = VALID_LESSON_ID,
                    request = createUpdateProgressRequest(status = status)
                )
                response.data.status shouldBe status
            }

            And("lesson does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.updateProgress(
                            lessonId = INVALID_LESSON_ID,
                            request = createUpdateProgressRequest()
                        )
                    }
                }
            }
        }

        When("creating listen session") {

            And("lesson exists and session data is valid") {
                Then("returns listen session response with updated count") {
                    val response = service.listen(
                        lessonId = VALID_LESSON_ID,
                        request = createListenSessionRequest(sessionId = VALID_SESSION_ID)
                    )

                    response.data shouldNotBe null
                    response.data.listenCount shouldBe 1L
                }
            }

            And("lesson does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.listen(
                            lessonId = INVALID_LESSON_ID,
                            request = createListenSessionRequest()
                        )
                    }
                }
            }
        }

        When("setting lesson as favourite") {

            And("lesson exists") {
                Then("operation completes successfully with null data") {
                    shouldNotThrow<Throwable> {
                        service.setFavourite(lessonId = VALID_LESSON_ID)
                    }
                }
            }

            And("lesson does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.setFavourite(lessonId = INVALID_LESSON_ID)
                    }
                }
            }
        }

        When("removing lesson from favourites") {

            And("lesson exists") {
                Then("operation completes successfully with null data") {
                    val response = service.removeFavourite(lessonId = VALID_LESSON_ID)
                    response.data.shouldBeNull()
                }
            }

            And("lesson does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.removeFavourite(lessonId = INVALID_LESSON_ID)
                    }
                }
            }
        }
    }
}) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun createHttpClient() = createTestHttpClient {
            addHandler {
                when (it.url.encodedPath.removePrefix("/")) {
                    LessonService.PAGE_PATH -> handlePageRequest(it)
                    LessonService.DELETED_PATH -> handleDeletedRequest(it)
                    LessonService.progressPath(VALID_LESSON_ID) -> handleProgressRequest(it, true)
                    LessonService.progressPath(INVALID_LESSON_ID) -> handleProgressRequest(it, false)
                    LessonService.listenPath(VALID_LESSON_ID) -> handleListenRequest(it, true)
                    LessonService.listenPath(INVALID_LESSON_ID) -> handleListenRequest(it, false)
                    LessonService.setFavouritePath(VALID_LESSON_ID) -> handleFavouriteRequest(it, true)
                    LessonService.setFavouritePath(INVALID_LESSON_ID) -> handleFavouriteRequest(it, false)
                    else -> throw NotImplementedError("Endpoint not mocked: ${it.url.encodedPath}")
                }
            }
        }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handlePageRequest(request: HttpRequestData) =
            when (request.method) {
                HttpMethod.Get -> {
                    val limit = request.url.parameters["limit"]?.toInt() ?: 0
                    val cacheControl = request.headers["Cache-Control"]

                    when {
                        limit > MAX_PAGE_LIMIT || cacheControl != "no-cache" -> respondJson(
                            content = baseResponse(message = "Invalid request parameters"),
                            status = HttpStatusCode.BadRequest
                        )
                        else -> respondJson(content = lessonResponsesList().wrapInPageResponse())
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleDeletedRequest(request: HttpRequestData) =
            when (request.method) {
                HttpMethod.Get -> {
                    val since = Instant.parse(request.url.parameters["since"].toString())

                    when {
                        since >= Clock.System.now() -> respondJson(
                            content = baseResponse(message = "Date cannot be in the future"),
                            status = HttpStatusCode.BadRequest
                        )
                        else -> respondJson(content = deletedLessonsList().wrapInBaseResponse())
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleProgressRequest(
            request: HttpRequestData,
            exists: Boolean
        ) = when (request.method) {
            HttpMethod.Patch -> {
                if (!exists) {
                    respondJson(
                        content = baseResponse(message = "Lesson not found"),
                        status = HttpStatusCode.NotFound
                    )
                } else {
                    val body = request.body.parse<UpdateProgressRequest>()
                    val progress = createProgress(
                        lastPositionMs = body.lastPositionMs,
                        status = body.status ?: UserProgressStatus.IN_PROGRESS,
                        completedAt = body.completedAt?.toString()
                    )
                    respondJson(content = progress.wrapInBaseResponse())
                }
            }
            else -> throw NotImplementedError("Method not supported: ${request.method}")
        }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleListenRequest(
            request: HttpRequestData,
            exists: Boolean
        ) = when (request.method) {
            HttpMethod.Post -> {
                if (!exists) {
                    respondJson(
                        content = baseResponse(message = "Lesson not found"),
                        status = HttpStatusCode.NotFound
                    )
                } else {
                    val response = createListenSessionResponse(listenCount = 1L)
                    respondJson(content = response.wrapInBaseResponse())
                }
            }
            else -> throw NotImplementedError("Method not supported: ${request.method}")
        }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleFavouriteRequest(
            request: HttpRequestData,
            exists: Boolean
        ) = when (request.method) {
            HttpMethod.Post, HttpMethod.Delete -> {
                if (!exists) {
                    respondJson(
                        content = baseResponse(message = "Lesson not found"),
                        status = HttpStatusCode.NotFound
                    )
                } else {
                    respondJson(content = baseResponse(message = "Operation successful"))
                }
            }
            else -> throw NotImplementedError("Method not supported: ${request.method}")
        }
    }
}
