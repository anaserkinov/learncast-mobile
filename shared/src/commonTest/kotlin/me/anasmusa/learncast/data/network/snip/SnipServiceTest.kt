package me.anasmusa.learncast.data.network.snip

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
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_NAME
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.INVALID_LESSON_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_DESCRIPTION
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.LESSON_1_TITLE
import me.anasmusa.learncast.data.network.TestFixtures.Lessons.VALID_LESSON_ID
import me.anasmusa.learncast.data.network.TestFixtures.Pagination.MAX_PAGE_LIMIT
import me.anasmusa.learncast.data.network.TestFixtures.Requests.pageRequest
import me.anasmusa.learncast.data.network.TestFixtures.Snips.DELETED_SNIP_1_DATE
import me.anasmusa.learncast.data.network.TestFixtures.Snips.DELETED_SNIP_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.DELETED_SNIP_2_DATE
import me.anasmusa.learncast.data.network.TestFixtures.Snips.DELETED_SNIP_2_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.INVALID_SNIP_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.LESSON_1_SNIP_COUNT
import me.anasmusa.learncast.data.network.TestFixtures.Snips.LESSON_1_SNIP_COUNT_AFTER_DELETE
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_CLIENT_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_CREATED_AT
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_END_MS
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_NOTE
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_START_MS
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_1_USER_COUNT
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_2_CLIENT_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.SNIP_2_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.VALID_SNIP_ID
import me.anasmusa.learncast.data.network.TestFixtures.Snips.createSnip
import me.anasmusa.learncast.data.network.TestFixtures.Snips.createSnipCountResponse
import me.anasmusa.learncast.data.network.TestFixtures.Snips.createSnipRequest
import me.anasmusa.learncast.data.network.TestFixtures.Snips.deletedSnipsList
import me.anasmusa.learncast.data.network.TestFixtures.Snips.snipResponsesList
import me.anasmusa.learncast.data.network.TestFixtures.baseResponse
import me.anasmusa.learncast.data.network.TestFixtures.parse
import me.anasmusa.learncast.data.network.TestFixtures.respondJson
import me.anasmusa.learncast.data.network.TestFixtures.wrapInBaseResponse
import me.anasmusa.learncast.data.network.TestFixtures.wrapInPageResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.createHttpClient
import me.anasmusa.learncast.data.network.snip.model.SnipCURequest
import me.anasmusa.learncast.data.network.snip.model.SnipCountResponse
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class SnipServiceTest : BehaviorSpec({

    lateinit var service: SnipService

    Given("SnipService") {

        When("requesting snip page with default pagination") {

            And("server returns success with snips list") {
                Then("returns paginated snips with complete lesson data and progress") {
                    val response = service.page(PageRequestQuery())

                    response.data shouldNotBe null
                    response.data.items shouldHaveSize 2

                    // Verify first snip with note and lesson progress
                    response.data.items[0].apply {
                        id shouldBe SNIP_1_ID
                        clientSnipId shouldBe SNIP_1_CLIENT_ID
                        startMs shouldBe SNIP_1_START_MS
                        endMs shouldBe SNIP_1_END_MS
                        note shouldBe SNIP_1_NOTE
                        createdAt shouldBe Instant.parse(SNIP_1_CREATED_AT)
                        userSnipCount shouldBe SNIP_1_USER_COUNT

                        lesson.apply {
                            id shouldBe LESSON_1_ID
                            title shouldBe LESSON_1_TITLE
                            description shouldBe LESSON_1_DESCRIPTION

                            author.apply {
                                id shouldBe AUTHOR_1_ID
                                name shouldBe AUTHOR_1_NAME
                            }

                            progress?.apply {
                                lessonId shouldBe LESSON_1_ID
                                status shouldBe UserProgressStatus.IN_PROGRESS
                            }
                        }
                    }

                    // Verify second snip without note
                    response.data.items[1].apply {
                        id shouldBe SNIP_2_ID
                        clientSnipId shouldBe SNIP_2_CLIENT_ID
                        note.shouldBeNull()
                        userSnipCount.shouldBeNull()
                        lesson.progress.shouldBeNull()
                    }
                }
            }
        }

        When("requesting snip page with custom pagination parameters") {

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
                    pageRequest(limit = limit, cursor = cursor)
                )
                response.data.items shouldHaveSize 2
            }
        }

        When("requesting snip page with invalid parameters") {

            And("limit exceeds maximum allowed") {
                Then("throws ClientRequestException with BadRequest status") {
                    shouldThrow<ClientRequestException> {
                        service.page(PageRequestQuery(limit = 101))
                    }
                }
            }
        }

        When("requesting snip count for a lesson") {

            And("lesson exists") {
                Then("returns snip count for the lesson") {
                    val response = service.count(lessonId = VALID_LESSON_ID)!!

                    response.data shouldNotBe null
                    response.data.lessonId shouldBe VALID_LESSON_ID
                    response.data.count shouldBe LESSON_1_SNIP_COUNT
                }
            }

            And("lesson does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.count(lessonId = INVALID_LESSON_ID)
                    }
                }
            }
        }

        When("creating a snip") {

            And("lesson exists and snip data is valid") {
                Then("returns created snip with complete data") {
                    val response = service.create(
                        lessonId = VALID_LESSON_ID,
                        request = createSnipRequest(
                            clientSnipId = "snip-new-123",
                            startMs = 10000L,
                            endMs = 15000L,
                            note = "Important concept"
                        )
                    )

                    response.data shouldNotBe null
                    response.data.clientSnipId shouldBe "snip-new-123"
                    response.data.startMs shouldBe 10000L
                    response.data.endMs shouldBe 15000L
                    response.data.note shouldBe "Important concept"
                }
            }

            withData(
                nameFn = { "creates snip successfully with ${it.first}" },
                listOf(
                    "note text" to "Important section",
                    "null note" to null,
                    "empty note" to "",
                )
            ) { (_, note) ->
                val response = service.create(
                    lessonId = VALID_LESSON_ID,
                    request = createSnipRequest(note = note)
                )
                response.data.note shouldBe note
            }

            And("lesson does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.create(
                            lessonId = INVALID_LESSON_ID,
                            request = createSnipRequest()
                        )
                    }
                }
            }
        }

        When("updating a snip") {

            And("snip exists and update data is valid") {
                Then("returns updated snip with modified fields") {
                    val response = service.update(
                        clientSnipId = VALID_SNIP_ID,
                        request = createSnipRequest(
                            clientSnipId = VALID_SNIP_ID,
                            startMs = 12000L,
                            endMs = 18000L,
                            note = "Updated note"
                        )
                    )

                    response.data shouldNotBe null
                    response.data.clientSnipId shouldBe VALID_SNIP_ID
                    response.data.startMs shouldBe 12000L
                    response.data.endMs shouldBe 18000L
                    response.data.note shouldBe "Updated note"
                }
            }

            withData(
                nameFn = { "updates snip time range from ${it.first}ms to ${it.second}ms" },
                listOf(
                    5000L to 10000L,
                    10000L to 20000L,
                    30000L to 45000L,
                )
            ) { (start, end) ->
                val response = service.update(
                    clientSnipId = VALID_SNIP_ID,
                    request = createSnipRequest(
                        clientSnipId = VALID_SNIP_ID,
                        startMs = start,
                        endMs = end
                    )
                )
                response.data.startMs shouldBe start
                response.data.endMs shouldBe end
            }

            And("snip does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.update(
                            clientSnipId = INVALID_SNIP_ID,
                            request = createSnipRequest(clientSnipId = INVALID_SNIP_ID)
                        )
                    }
                }
            }
        }

        When("deleting a snip") {

            And("snip exists") {
                Then("returns updated snip count after deletion") {
                    val response = service.delete(clientSnipId = VALID_SNIP_ID)

                    response.data shouldNotBe null
                    response.data?.lessonId shouldBe VALID_LESSON_ID
                    response.data?.count shouldBe LESSON_1_SNIP_COUNT_AFTER_DELETE
                }
            }

            And("snip does not exist") {
                Then("throws ClientRequestException with NotFound status") {
                    shouldThrow<ClientRequestException> {
                        service.delete(clientSnipId = INVALID_SNIP_ID)
                    }
                }
            }
        }

        When("requesting deleted snips since specific date") {

            And("date is in the past") {
                Then("returns list of deleted snips with deletion timestamps") {
                    val response = service.deleted(
                        DeletedRequestQuery(since = Clock.System.now() - 1.days)
                    )

                    response.data shouldNotBe null
                    response.data shouldHaveSize 2

                    response.data[0].apply {
                        id shouldBe DELETED_SNIP_1_ID
                        deletedAt shouldBe Instant.parse(DELETED_SNIP_1_DATE)
                    }

                    response.data[1].apply {
                        id shouldBe DELETED_SNIP_2_ID
                        deletedAt shouldBe Instant.parse(DELETED_SNIP_2_DATE)
                    }
                }
            }

            withData(
                nameFn = { "returns deleted snips for ${it.first} days ago" },
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

        When("requesting deleted snips with invalid date") {

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
    }

    beforeTest {
        service = SnipService(createTestHttpClient())
    }
}) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun createTestHttpClient() = createHttpClient {
            addHandler {
                when (it.url.encodedPath.removePrefix("/")) {
                    SnipService.PAGE_PATH -> handlePageRequest(it)
                    SnipService.countPath(VALID_LESSON_ID) -> handleCountRequest(it, true)
                    SnipService.countPath(INVALID_LESSON_ID) -> handleCountRequest(it, false)
                    SnipService.createPath(VALID_LESSON_ID) -> handleCreateRequest(it, true)
                    SnipService.createPath(INVALID_LESSON_ID) -> handleCreateRequest(it, false)
                    SnipService.updatePath(VALID_SNIP_ID) -> handleUpdateRequest(it, true)
                    SnipService.updatePath(INVALID_SNIP_ID) -> handleUpdateRequest(it, false)
                    SnipService.DELETED_PATH -> handleDeletedRequest(it)
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
                        else -> respondJson(content = snipResponsesList().wrapInPageResponse())
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleCountRequest(
            request: HttpRequestData,
            exists: Boolean
        ) = when (request.method) {
            HttpMethod.Get -> {
                if (!exists) {
                    respondJson(
                        content = baseResponse(message = "Lesson not found"),
                        status = HttpStatusCode.NotFound
                    )
                } else {
                    val count = createSnipCountResponse()
                    respondJson(content = count.wrapInBaseResponse())
                }
            }
            else -> throw NotImplementedError("Method not supported: ${request.method}")
        }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleCreateRequest(
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
                    val body = request.body.parse<SnipCURequest>()
                    val snip = createSnip(
                        clientSnipId = body.clientSnipId,
                        startMs = body.startMs,
                        endMs = body.endMs,
                        note = body.note
                    )
                    respondJson(content = snip.wrapInBaseResponse())
                }
            }
            else -> throw NotImplementedError("Method not supported: ${request.method}")
        }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleUpdateRequest(
            request: HttpRequestData,
            exists: Boolean
        ) = when (request.method) {
            HttpMethod.Put -> {
                if (!exists) {
                    respondJson(
                        content = baseResponse(message = "Snip not found"),
                        status = HttpStatusCode.NotFound
                    )
                } else {
                    val body = request.body.parse<SnipCURequest>()
                    val snip = createSnip(
                        clientSnipId = body.clientSnipId,
                        startMs = body.startMs,
                        endMs = body.endMs,
                        note = body.note
                    )
                    respondJson(content = snip.wrapInBaseResponse())
                }
            }
            HttpMethod.Delete -> {
                if (!exists) {
                    respondJson(
                        content = baseResponse(message = "Snip not found"),
                        status = HttpStatusCode.NotFound
                    )
                } else {
                    val count = SnipCountResponse(
                        lessonId = VALID_LESSON_ID,
                        count = LESSON_1_SNIP_COUNT_AFTER_DELETE
                    )
                    respondJson(content = count.wrapInBaseResponse())
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
                        else -> respondJson(content = deletedSnipsList().wrapInBaseResponse())
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }
    }
}
