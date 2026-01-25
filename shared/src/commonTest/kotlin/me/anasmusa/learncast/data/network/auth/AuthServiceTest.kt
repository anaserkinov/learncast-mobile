package me.anasmusa.learncast.data.network.auth

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import me.anasmusa.learncast.data.network.TestFixtures.Auth.EXPIRED_REFRESH_TOKEN
import me.anasmusa.learncast.data.network.TestFixtures.Auth.INVALID_DATA
import me.anasmusa.learncast.data.network.TestFixtures.Auth.INVALID_REFRESH_TOKEN
import me.anasmusa.learncast.data.network.TestFixtures.Auth.TEST_USER_AVATAR_PATH
import me.anasmusa.learncast.data.network.TestFixtures.Auth.TEST_USER_EMAIL
import me.anasmusa.learncast.data.network.TestFixtures.Auth.TEST_USER_FIRST_NAME
import me.anasmusa.learncast.data.network.TestFixtures.Auth.TEST_USER_ID
import me.anasmusa.learncast.data.network.TestFixtures.Auth.TEST_USER_LAST_NAME
import me.anasmusa.learncast.data.network.TestFixtures.Auth.TEST_USER_TELEGRAM_USERNAME
import me.anasmusa.learncast.data.network.TestFixtures.Auth.VALID_ACCESS_TOKEN
import me.anasmusa.learncast.data.network.TestFixtures.Auth.VALID_GOOGLE_DATA
import me.anasmusa.learncast.data.network.TestFixtures.Auth.VALID_REFRESH_TOKEN
import me.anasmusa.learncast.data.network.TestFixtures.Auth.VALID_TELEGRAM_DATA
import me.anasmusa.learncast.data.network.TestFixtures.Auth.createCredentials
import me.anasmusa.learncast.data.network.TestFixtures.Auth.createLoginRequest
import me.anasmusa.learncast.data.network.TestFixtures.Auth.createLoginResponse
import me.anasmusa.learncast.data.network.TestFixtures.baseResponse
import me.anasmusa.learncast.data.network.TestFixtures.parse
import me.anasmusa.learncast.data.network.TestFixtures.respondJson
import me.anasmusa.learncast.data.network.TestFixtures.wrapInBaseResponse
import me.anasmusa.learncast.data.network.auth.model.LoginRequest
import me.anasmusa.learncast.data.network.auth.model.RefreshTokenRequest
import me.anasmusa.learncast.data.network.createHttpClient
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AuthServiceTest : BehaviorSpec({

    lateinit var client: HttpClient
    lateinit var service: AuthService

    Given("AuthService") {

        When("user signs in with valid credentials") {

            And("using telegram authentication") {
                Then("returns user data with valid access and refresh tokens") {
                    val response = service.login(
                        createLoginRequest(telegramData = VALID_TELEGRAM_DATA)
                    )

                    response.data shouldNotBe null
                    response.data.user.id shouldBe TEST_USER_ID
                    response.data.user.firstName shouldBe TEST_USER_FIRST_NAME
                    response.data.user.lastName shouldBe TEST_USER_LAST_NAME
                    response.data.user.email shouldBe TEST_USER_EMAIL
                    response.data.user.telegramUsername shouldBe TEST_USER_TELEGRAM_USERNAME
                    response.data.user.avatarPath shouldBe TEST_USER_AVATAR_PATH
                    response.data.credentials.accessToken shouldBe VALID_ACCESS_TOKEN
                    response.data.credentials.refreshToken shouldBe VALID_REFRESH_TOKEN
                }
            }

            And("using google authentication") {
                Then("returns user data with valid access and refresh tokens") {
                    val response = service.login(
                        createLoginRequest(googleData = VALID_GOOGLE_DATA)
                    )

                    response.data shouldNotBe null
                    response.data.user.id shouldBe TEST_USER_ID
                    response.data.credentials.accessToken shouldBe VALID_ACCESS_TOKEN
                    response.data.credentials.refreshToken shouldBe VALID_REFRESH_TOKEN
                }
            }
        }

        When("user signs in with invalid credentials") {

            withData(
                nameFn = { "returns BadRequest for ${it.first}" },
                listOf(
                    "empty telegram data" to createLoginRequest(telegramData = ""),
                    "null telegram data" to createLoginRequest(telegramData = null),
                    "invalid telegram data" to createLoginRequest(telegramData = INVALID_DATA),
                    "empty google data" to createLoginRequest(googleData = ""),
                    "null google data" to createLoginRequest(googleData = null),
                    "invalid google data" to createLoginRequest(googleData = INVALID_DATA),
                )
            ) { (_, request) ->
                shouldThrow<ClientRequestException> {
                    service.login(request)
                }
            }
        }

        When("user refreshes authentication token") {

            And("refresh token is valid") {
                Then("returns new access token and refresh token") {
                    val response = service.refreshToken(
                        RefreshTokenRequest(refreshToken = VALID_REFRESH_TOKEN)
                    )

                    response.data shouldNotBe null
                    response.data.accessToken shouldBe VALID_ACCESS_TOKEN
                    response.data.refreshToken shouldBe VALID_REFRESH_TOKEN
                }
            }

            And("refresh token is invalid") {
                Then("throws ClientRequestException with Unauthorized status") {
                    shouldThrow<ClientRequestException> {
                        service.refreshToken(
                            RefreshTokenRequest(refreshToken = INVALID_REFRESH_TOKEN)
                        )
                    }
                }
            }

            And("refresh token is expired") {
                Then("throws ClientRequestException with Unauthorized status") {
                    shouldThrow<ClientRequestException> {
                        service.refreshToken(
                            RefreshTokenRequest(refreshToken = EXPIRED_REFRESH_TOKEN)
                        )
                    }
                }
            }
        }

        When("user logs out") {

            Then("completes successfully without throwing exceptions") {
                shouldNotThrow<Throwable> {
                    service.logout()
                }
            }
        }
    }

    beforeTest {
        client = createTestHttpClient()
        service = AuthService(client)
    }
}) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun createTestHttpClient() = createHttpClient {
            addHandler {
                when (it.url.encodedPath.removePrefix("/")) {
                    AuthService.SIGN_IN -> handleSignIn(it)
                    AuthService.REFRESH_TOKEN -> handleRefreshToken(it)
                    AuthService.LOGOUT -> handleLogout(it)
                    else -> throw NotImplementedError("Endpoint not mocked: ${it.url.encodedPath}")
                }
            }
        }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleSignIn(request: io.ktor.client.request.HttpRequestData) =
            when (request.method) {
                HttpMethod.Post -> {
                    val body = request.body.parse<LoginRequest>()
                    when {
                        body.telegramData == VALID_TELEGRAM_DATA ||
                            body.googleData == VALID_GOOGLE_DATA -> {
                            val response = createLoginResponse()
                            respondJson(
                                content = response.wrapInBaseResponse()
                            )
                        }
                        else -> respondJson(
                            content = baseResponse(message = "Invalid credentials"),
                            status = HttpStatusCode.BadRequest
                        )
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleRefreshToken(request: io.ktor.client.request.HttpRequestData) =
            when (request.method) {
                HttpMethod.Post -> {
                    if (!request.attributes.contains(AuthCircuitBreaker)) {
                        error("AuthCircuitBreaker attribute must be present in refresh token request")
                    }

                    val body = request.body.parse<RefreshTokenRequest>()
                    when (body.refreshToken) {
                        VALID_REFRESH_TOKEN -> {
                            val response = createCredentials()
                            respondJson(
                                content = response.wrapInBaseResponse()
                            )
                        }
                        else -> respondJson(
                            content = baseResponse(message = "Invalid or expired refresh token"),
                            status = HttpStatusCode.Unauthorized
                        )
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        @OptIn(ExperimentalTime::class)
        private fun MockRequestHandleScope.handleLogout(request: io.ktor.client.request.HttpRequestData) =
            when (request.method) {
                HttpMethod.Post -> {
                    if (!request.attributes.contains(AuthCircuitBreaker)) {
                        error("AuthCircuitBreaker attribute must be present in logout request")
                    }
                    respondJson(
                        content = baseResponse(message = "Logged out successfully")
                    )
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }
    }
}
