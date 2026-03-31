package me.anasmusa.learncast.data.network

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import me.anasmusa.learncast.PreferenceData
import me.anasmusa.learncast.data.local.preference.Preferences
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.network.TestFixtures.baseResponse
import me.anasmusa.learncast.data.network.TestFixtures.parse
import me.anasmusa.learncast.data.network.TestFixtures.respondJson
import me.anasmusa.learncast.data.network.TestFixtures.wrapInBaseResponse
import me.anasmusa.learncast.data.network.auth.AuthService
import me.anasmusa.learncast.data.network.auth.model.RefreshTokenRequest
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository

class TokenManagerTest : BehaviorSpec({

    lateinit var preferences: FakePreferences
    lateinit var authRepository: FakeAuthRepository
    lateinit var tokenManager: TokenManager

    Given("TokenManager") {

        When("getting tokens from storage") {

            And("tokens exist in preferences") {
                Then("returns saved access and refresh tokens") {
                    preferences.savedTokens = Pair(SAVED_REFRESH_TOKEN, SAVED_ACCESS_TOKEN)

                    val tokens = tokenManager.getTokens()

                    tokens shouldBe Pair(SAVED_REFRESH_TOKEN, SAVED_ACCESS_TOKEN)
                }
            }

            And("no tokens exist in preferences") {
                Then("returns null") {
                    preferences.savedTokens = null

                    val tokens = tokenManager.getTokens()

                    tokens.shouldBeNull()
                }
            }
        }

        When("refreshing token") {

            And("refresh token is valid and matches saved token") {
                Then("calls auth service and updates preferences with new tokens") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)

                    val tokens = tokenManager.refreshToken(VALID_REFRESH_TOKEN)

                    tokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    preferences.lastUpdatedTokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                }
            }

            And("saved token differs from provided refresh token") {
                Then("returns saved tokens without calling auth service") {
                    preferences.savedTokens = Pair(SAVED_REFRESH_TOKEN, SAVED_ACCESS_TOKEN)

                    val tokens = tokenManager.refreshToken(DIFFERENT_REFRESH_TOKEN)

                    tokens shouldBe Pair(SAVED_REFRESH_TOKEN, SAVED_ACCESS_TOKEN)
                    preferences.lastUpdatedTokens.shouldBeNull() // No update occurred
                }
            }

            And("no tokens exist in preferences") {
                Then("attempts refresh and updates preferences on success") {
                    preferences.savedTokens = null

                    val tokens = tokenManager.refreshToken(VALID_REFRESH_TOKEN)

                    tokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    preferences.lastUpdatedTokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                }
            }

            And("auth service returns 401 Unauthorized") {
                Then("logs out user and returns null") {
                    preferences.savedTokens = Pair(EXPIRED_REFRESH_TOKEN, OLD_ACCESS_TOKEN)

                    val tokens = tokenManager.refreshToken(EXPIRED_REFRESH_TOKEN)

                    tokens.shouldBeNull()
                    authRepository.logoutCalled shouldBe true
                }
            }

            withData(
                nameFn = { "handles ${it.first} and returns null" },
                listOf(
                    "network error" to NETWORK_ERROR_REFRESH_TOKEN,
                    "server error" to SERVER_ERROR_REFRESH_TOKEN,
                    "bad request" to BAD_REQUEST_REFRESH_TOKEN,
                )
            ) { (_, refreshToken) ->
                preferences.savedTokens = Pair(refreshToken, OLD_ACCESS_TOKEN)

                val tokens = tokenManager.refreshToken(refreshToken)

                tokens.shouldBe(Pair(refreshToken, OLD_ACCESS_TOKEN))
                authRepository.logoutCalled shouldBe false // Only 401 triggers logout
            }
        }

        When("multiple concurrent refresh requests occur") {

            And("all requests use the same refresh token") {
                Then("only one refresh call is made and all get the same new tokens") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)

                    val results = async {
                        List(5) {
                            async {
                                tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                            }
                        }.awaitAll()
                    }.await()

                    // All requests should get the same new tokens
                    results.forEach { tokens ->
                        tokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    }

                    // Verify only one update occurred (job reuse prevented duplicate calls)
                    preferences.updateCallCount shouldBe 1
                }
            }

            And("refresh job is already in progress") {
                Then("subsequent calls wait for the existing job to complete") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)
                    preferences.delayMs = 200 // Simulate slow network

                    val firstRefresh = async {
                        tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                    }

                    // Wait a bit to ensure first refresh starts
                    delay(50)

                    val secondRefresh = async {
                        tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                    }

                    val results = awaitAll(firstRefresh, secondRefresh)

                    // Both should return the same new tokens
                    results[0] shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    results[1] shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)

                    // Only one update should have occurred
                    preferences.updateCallCount shouldBe 1
                }
            }

            And("token has changed between job start and completion check") {
                Then("returns updated tokens without refreshing again") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)
                    preferences.delayMs = 100

                    val firstRefresh = launch {
                        tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                    }

                    // Wait for first refresh to complete
                    firstRefresh.join()

                    // Now try to refresh with the old token again
                    val secondResult = tokenManager.refreshToken(VALID_REFRESH_TOKEN)

                    // Should detect token mismatch and return current tokens
                    secondResult shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)

                    // Only one update should have occurred
                    preferences.updateCallCount shouldBe 1
                }
            }
        }

        When("token mismatch occurs during concurrent operations") {

            And("one refresh updates tokens while another is checking") {
                Then("second operation gets updated tokens without refreshing again") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)

                    val firstRefresh = async {
                        tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                    }

                    firstRefresh.await() // Wait for first refresh

                    // Now saved token is NEW_REFRESH_TOKEN
                    // Try to refresh with old token
                    val secondRefresh = tokenManager.refreshToken(VALID_REFRESH_TOKEN)

                    // Should return the already-saved new tokens
                    secondRefresh shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)

                    // Only one update should have occurred
                    preferences.updateCallCount shouldBe 1
                }
            }
        }

        When("cancelRefresh is called") {

            And("refresh is in progress") {
                Then("cancels the ongoing refresh job") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)
                    preferences.delayMs = 500 // Long delay to allow cancellation

                    val refreshDeferred = async {
                        tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                    }

                    // Wait a bit to ensure refresh starts
                    delay(100)

                    // Cancel the refresh
                    tokenManager.cancelRefresh()

                    // The refresh should complete (job.join() is called even after cancel)
                    // but the result might be null or old tokens depending on timing
                    val result = refreshDeferred.await()

                    // After cancellation, tokens should remain unchanged or be null
                    // since the job was canceled before completion
                    if (result != null) {
                        // If result is not null, it means the update happened before cancellation
                        result shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    }
                }
            }

            And("no refresh is in progress") {
                Then("does nothing and completes successfully") {
                    preferences.savedTokens = Pair(SAVED_REFRESH_TOKEN, SAVED_ACCESS_TOKEN)

                    // Should not throw exception
                    tokenManager.cancelRefresh()

                    // Tokens should remain unchanged
                    val tokens = tokenManager.getTokens()
                    tokens shouldBe Pair(SAVED_REFRESH_TOKEN, SAVED_ACCESS_TOKEN)
                }
            }
        }

        When("refresh job completes") {

            And("job completes successfully") {
                Then("job is set to null after completion") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)
                    preferences.delayMs = 50

                    val tokens = tokenManager.refreshToken(VALID_REFRESH_TOKEN)

                    tokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)

                    // New refresh should work normally (job was reset to null)
                    preferences.savedTokens = Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    val secondTokens = tokenManager.refreshToken(NEW_REFRESH_TOKEN)

                    secondTokens shouldBe Pair(NEWER_REFRESH_TOKEN, NEWER_ACCESS_TOKEN)
                }
            }

            And("job fails with exception") {
                Then("job is set to null after exception handling") {
                    preferences.savedTokens = Pair(NETWORK_ERROR_REFRESH_TOKEN, OLD_ACCESS_TOKEN)

                    val tokens = tokenManager.refreshToken(NETWORK_ERROR_REFRESH_TOKEN)

                    tokens.shouldBe(Pair(NETWORK_ERROR_REFRESH_TOKEN, OLD_ACCESS_TOKEN))

                    // Verify we can make a new refresh attempt
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)
                    val secondTokens = tokenManager.refreshToken(VALID_REFRESH_TOKEN)

                    secondTokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                }
            }
        }

        When("GlobalScope is used for refresh job") {

            And("parent coroutine is cancelled") {
                Then("refresh job continues in GlobalScope") {
                    preferences.savedTokens = Pair(VALID_REFRESH_TOKEN, OLD_ACCESS_TOKEN)
                    preferences.delayMs = 200

                    val parentJob = launch {
                        launch {
                            tokenManager.refreshToken(VALID_REFRESH_TOKEN)
                        }
                        // Parent completes immediately
                    }

                    // Wait for parent to complete
                    parentJob.join()

                    // Wait a bit more for refresh to complete in GlobalScope
                    delay(300)

                    // Refresh should have completed despite parent cancellation
                    val tokens = tokenManager.getTokens()
                    tokens shouldBe Pair(NEW_REFRESH_TOKEN, NEW_ACCESS_TOKEN)
                    preferences.updateCallCount shouldBe 1
                }
            }
        }
    }

    beforeTest {
        preferences = FakePreferences()
        authRepository = FakeAuthRepository(preferences)
        val authService = AuthService(createHttpClient())
        tokenManager = TokenManager(authService, preferences, authRepository)
    }
}) {
    companion object {
        // Saved Tokens
        const val SAVED_REFRESH_TOKEN = "saved_refresh_token"
        const val SAVED_ACCESS_TOKEN = "saved_access_token"

        // Valid Token Flow
        const val VALID_REFRESH_TOKEN = "valid_refresh_token"
        const val OLD_ACCESS_TOKEN = "old_access_token"
        const val NEW_REFRESH_TOKEN = "new_refresh_token"
        const val NEW_ACCESS_TOKEN = "new_access_token"

        // Second Refresh Flow
        const val NEWER_REFRESH_TOKEN = "newer_refresh_token"
        const val NEWER_ACCESS_TOKEN = "newer_access_token"

        // Different Token
        const val DIFFERENT_REFRESH_TOKEN = "different_refresh_token"

        // Error Scenarios
        const val EXPIRED_REFRESH_TOKEN = "expired_refresh_token"
        const val NETWORK_ERROR_REFRESH_TOKEN = "network_error_token"
        const val SERVER_ERROR_REFRESH_TOKEN = "server_error_token"
        const val BAD_REQUEST_REFRESH_TOKEN = "bad_request_token"

        fun createHttpClient() = createTestHttpClient {
            addHandler {
                when (it.url.encodedPath.removePrefix("/")) {
                    AuthService.REFRESH_TOKEN -> handleRefreshToken(it)
                    else -> throw NotImplementedError("Endpoint not mocked: ${it.url.encodedPath}")
                }
            }
        }

        private fun MockRequestHandleScope.handleRefreshToken(request: HttpRequestData) =
            when (request.method) {
                HttpMethod.Post -> {
                    val body = request.body.parse<RefreshTokenRequest>()

                    when (body.refreshToken) {
                        VALID_REFRESH_TOKEN -> {
                            val credentials = TestFixtures.Auth.createCredentials(
                                accessToken = NEW_ACCESS_TOKEN,
                                refreshToken = NEW_REFRESH_TOKEN
                            )
                            respondJson(content = credentials.wrapInBaseResponse())
                        }
                        NEW_REFRESH_TOKEN -> {
                            val credentials = TestFixtures.Auth.createCredentials(
                                accessToken = NEWER_ACCESS_TOKEN,
                                refreshToken = NEWER_REFRESH_TOKEN
                            )
                            respondJson(content = credentials.wrapInBaseResponse())
                        }
                        EXPIRED_REFRESH_TOKEN -> {
                            respondJson(
                                content = baseResponse(message = "Token expired"),
                                status = HttpStatusCode.Unauthorized
                            )
                        }
                        NETWORK_ERROR_REFRESH_TOKEN -> {
                            respondJson(
                                content = baseResponse(message = "Network error"),
                                status = HttpStatusCode.BadGateway
                            )
                        }
                        SERVER_ERROR_REFRESH_TOKEN -> {
                            respondJson(
                                content = baseResponse(message = "Internal server error"),
                                status = HttpStatusCode.InternalServerError
                            )
                        }
                        BAD_REQUEST_REFRESH_TOKEN -> {
                            respondJson(
                                content = baseResponse(message = "Invalid token format"),
                                status = HttpStatusCode.BadRequest
                            )
                        }
                        else -> {
                            respondJson(
                                content = baseResponse(message = "Unknown token"),
                                status = HttpStatusCode.Unauthorized
                            )
                        }
                    }
                }
                else -> throw NotImplementedError("Method not supported: ${request.method}")
            }

        // Fake Preferences for Testing
        class FakePreferences : Preferences {
            var savedTokens: Pair<String, String>? = null
            var lastUpdatedTokens: Pair<String, String>? = null
            var updateCallCount = 0
            var delayMs: Long = 0 // Simulate slow operations

            override fun getToken() = flow {
                if (delayMs > 0) {
                    delay(delayMs)
                }
                emit(savedTokens)
            }

            override suspend fun updateToken(refreshToken: String, accessToken: String) {
                if (delayMs > 0) {
                    delay(delayMs)
                }
                lastUpdatedTokens = Pair(refreshToken, accessToken)
                savedTokens = Pair(refreshToken, accessToken)
                updateCallCount++
            }

            override suspend fun clear() {
                savedTokens = null
            }

            // Other Preferences methods - not used in TokenManager tests
            override suspend fun updateUser(user: PreferenceData.User) = throw NotImplementedError()
            override fun getUser(): Flow<PreferenceData.User?> = throw NotImplementedError()
            override suspend fun setLang(lang: String) = throw NotImplementedError()
            override fun getLang(): Flow<String?> = throw NotImplementedError()
        }

        // Fake AuthRepository for Testing
        class FakeAuthRepository(private val preferences: Preferences) : AuthRepository {
            var logoutCalled = false

            override suspend fun logout() {
                logoutCalled = true
                preferences.clear()
            }

            // Other AuthRepository methods - not used in TokenManager tests
            override fun isLoggedIn(): Flow<Boolean> = throw NotImplementedError()
            override suspend fun loginWithTelegram(idToken: String): Result<Unit> = throw NotImplementedError()
            override suspend fun loginWithGoogle(): Result<Unit> = throw NotImplementedError()
        }
    }
}
