package me.anasmusa.learncast.data.network.author

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_AVATAR_PATH
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_CREATED_AT
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_LESSON_COUNT
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_1_NAME
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_2_CREATED_AT
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_2_ID
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_2_LESSON_COUNT
import me.anasmusa.learncast.data.network.TestFixtures.Authors.AUTHOR_2_NAME
import me.anasmusa.learncast.data.network.TestFixtures.Authors.DELETED_AUTHOR_1_DATE
import me.anasmusa.learncast.data.network.TestFixtures.Authors.DELETED_AUTHOR_1_ID
import me.anasmusa.learncast.data.network.TestFixtures.Authors.DELETED_AUTHOR_2_DATE
import me.anasmusa.learncast.data.network.TestFixtures.Authors.DELETED_AUTHOR_2_ID
import me.anasmusa.learncast.data.network.TestFixtures.Authors.authorsResponseList
import me.anasmusa.learncast.data.network.TestFixtures.Authors.deletedAuthorsList
import me.anasmusa.learncast.data.network.TestFixtures.Pagination.MAX_PAGE_LIMIT
import me.anasmusa.learncast.data.network.TestFixtures.Pagination.MIN_PAGE_LIMIT
import me.anasmusa.learncast.data.network.TestFixtures.baseResponse
import me.anasmusa.learncast.data.network.TestFixtures.respondJson
import me.anasmusa.learncast.data.network.TestFixtures.wrapInBaseResponse
import me.anasmusa.learncast.data.network.TestFixtures.wrapInPageResponse
import me.anasmusa.learncast.data.network.common.model.DeletedRequestQuery
import me.anasmusa.learncast.data.network.common.model.PageRequestQuery
import me.anasmusa.learncast.data.network.createTestHttpClient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class AuthorServiceTest : BehaviorSpec({

    val service = AuthorService(createHttpClient())

    Given("AuthorService") {

        When("requesting author page with default pagination") {

            And("server returns success with authors list") {
                Then("returns paginated authors with complete data") {
                    val response = service.page(PageRequestQuery())

                    response.data shouldNotBe null
                    response.data.items shouldHaveSize 2

                    // Verify first author
                    response.data.items[0].apply {
                        id shouldBe AUTHOR_1_ID
                        name shouldBe AUTHOR_1_NAME
                        avatarPath shouldBe AUTHOR_1_AVATAR_PATH
                        createdAt shouldBe Instant.parse(AUTHOR_1_CREATED_AT)
                        lessonCount shouldBe AUTHOR_1_LESSON_COUNT
                    }

                    // Verify second author
                    response.data.items[1].apply {
                        id shouldBe AUTHOR_2_ID
                        name shouldBe AUTHOR_2_NAME
                        avatarPath shouldBe null
                        createdAt shouldBe Instant.parse(AUTHOR_2_CREATED_AT)
                        lessonCount shouldBe AUTHOR_2_LESSON_COUNT
                    }
                }
            }
        }

        When("requesting author page with custom pagination parameters") {

            withData(
                nameFn = { "returns success for limit=${it.first}, cursor=${it.second}" },
                listOf(
                    10 to null,
                    50 to null,
                    100 to null,
                    25 to "cursor_abc123",
                    75 to "cursor_xyz789",
                )
            ) { (limit, cursor) ->
                val response = service.page(
                    PageRequestQuery(limit = limit, cursor = cursor)
                )
                response.data.items shouldHaveSize 2
            }
        }

        When("requesting author page with invalid parameters") {

            And("limit exceeds maximum allowed") {
                Then("throws ClientRequestException with BadRequest status") {
                    shouldThrow<ClientRequestException> {
                        service.page(PageRequestQuery(limit = 101))
                    }
                }
            }

            And("limit is negative") {
                Then("throws ClientRequestException with BadRequest status") {
                    shouldThrow<ClientRequestException> {
                        service.page(PageRequestQuery(limit = -1))
                    }
                }
            }
        }

        When("requesting deleted authors since specific date") {

            And("date is in the past") {
                Then("returns list of deleted authors with deletion timestamps") {
                    val response = service.deleted(
                        DeletedRequestQuery(since = Clock.System.now() - 1.days)
                    )

                    response.data shouldNotBe null
                    response.data shouldHaveSize 2

                    response.data[0].apply {
                        id shouldBe DELETED_AUTHOR_1_ID
                        deletedAt shouldBe Instant.parse(DELETED_AUTHOR_1_DATE)
                    }

                    response.data[1].apply {
                        id shouldBe DELETED_AUTHOR_2_ID
                        deletedAt shouldBe Instant.parse(DELETED_AUTHOR_2_DATE)
                    }
                }
            }

            withData(
                nameFn = { "returns deleted authors for ${it.first} days ago" },
                listOf(
                    "1" to 1.days,
                    "7" to 7.days,
                    "30" to 30.days,
                    "90" to 90.days,
                )
            ) { (_, duration) ->
                val response = service.deleted(
                    DeletedRequestQuery(since = Clock.System.now() - duration)
                )
                response.data shouldHaveSize 2
            }
        }

        When("requesting deleted authors with invalid date") {

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
}) {
    companion object {
        fun createHttpClient() = createTestHttpClient {
            addHandler {
                when (it.url.encodedPath.removePrefix("/")) {
                    AuthorService.PAGE -> handlePageRequest(it)
                    AuthorService.DELETED -> handleDeletedRequest(it)
                    else -> throw NotImplementedError("Endpoint not mocked: ${it.url.encodedPath}")
                }
            }
        }

        private fun MockRequestHandleScope.handlePageRequest(request: HttpRequestData) =
            when (request.method) {
                HttpMethod.Get -> {
                    val limit = request.url.parameters["limit"]?.toInt() ?: 0

                    when {
                        limit < MIN_PAGE_LIMIT -> respondJson(
                            content = baseResponse(message = "Limit must be non-negative"),
                            status = HttpStatusCode.BadRequest
                        )
                        limit > MAX_PAGE_LIMIT -> respondJson(
                            content = baseResponse(message = "Limit exceeds maximum allowed"),
                            status = HttpStatusCode.BadRequest
                        )
                        else -> {
                            val authors = authorsResponseList()
                            respondJson(
                                content = authors.wrapInPageResponse()
                            )
                        }
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        private fun MockRequestHandleScope.handleDeletedRequest(request: HttpRequestData) =
            when (request.method) {
                HttpMethod.Get -> {
                    val since = Instant.parse(request.url.parameters["since"].toString())

                    when {
                        since >= Clock.System.now() -> respondJson(
                            content = baseResponse(message = "Date cannot be in the future"),
                            status = HttpStatusCode.BadRequest
                        )
                        else -> {
                            val deletedAuthors = deletedAuthorsList()
                            respondJson(
                                content = deletedAuthors.wrapInBaseResponse()
                            )
                        }
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }
    }
}
