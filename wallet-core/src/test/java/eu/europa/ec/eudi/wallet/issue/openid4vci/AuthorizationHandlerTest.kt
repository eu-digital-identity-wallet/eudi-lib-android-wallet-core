/*
 * Copyright (c) 2024-2025 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorizationHandlerTest {

    lateinit var context: Context
    lateinit var logger: Logger
    lateinit var issuer: Issuer
    lateinit var authorizedRequest: AuthorizedRequest

    @BeforeTest
    fun setup() {
        context = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        issuer = mockk(relaxed = true)
        authorizedRequest = mockk(relaxed = true)

        val preparedAuthorizationRequest = mockk<eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared>(relaxed = true) {
            every { authorizationCodeURL } returns HttpsUrl("https://issuer.example.com/authorize?client_id=test&state=xyz").getOrThrow()
        }

        coEvery {
            issuer.prepareAuthorizationRequest()
        } returns Result.success(preparedAuthorizationRequest)

        coEvery {
            with(issuer) {
                preparedAuthorizationRequest.authorizeWithAuthorizationCode(any(), any())
            }
        } returns Result.success(authorizedRequest)

        coEvery {
            issuer.authorizeWithPreAuthorizationCode(any())
        } returns Result.success(authorizedRequest)
    }

    @Test
    fun `IssuerAuthorization with BrowserAuthorizationHandler calls authorize on handler`() = runTest {
        every { issuer.credentialOffer } returns mockk(relaxed = true) {
            every { grants } returns mockk(relaxed = true) {
                every { preAuthorizedCode() } returns null
            }
        }

        val mockHandler = mockk<BrowserAuthorizationHandler>(relaxed = true)
        coEvery { mockHandler.authorize(any()) } returns Result.success(
            AuthorizationResponse("auth_code_123", "xyz")
        )

        val issuerAuthorization = IssuerAuthorization(mockHandler, logger)

        launch {
            val result = issuerAuthorization.authorize(issuer, null)
            assertNotNull(result)
        }

        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockHandler.authorize(match { it.contains("https://issuer.example.com/authorize") })
        }
    }

    @Test
    fun `IssuerAuthorization with custom handler calls authorize on custom handler`() = runTest {
        every { issuer.credentialOffer } returns mockk(relaxed = true) {
            every { grants } returns mockk(relaxed = true) {
                every { preAuthorizedCode() } returns null
            }
        }

        val customHandler = CustomTestAuthorizationHandler()
        val issuerAuthorization = IssuerAuthorization(customHandler, logger)

        val result = issuerAuthorization.authorize(issuer, null)

        assertNotNull(result)
        assertTrue(customHandler.authorizeCalled)
        assertEquals("https://issuer.example.com/authorize?client_id=test&state=xyz", customHandler.lastAuthorizationUrl)
    }

    @Test
    fun `IssuerAuthorization with pre-authorized code does not call handler`() = runTest {
        every { issuer.credentialOffer } returns mockk(relaxed = true) {
            every { grants } returns mockk(relaxed = true) {
                every { preAuthorizedCode() } returns mockk(relaxed = true) {
                    every { txCode } returns mockk(relaxed = true) {
                        every { length } returns 4
                        every { inputMode } returns TxCodeInputMode.NUMERIC
                    }
                }
            }
        }

        val mockHandler = mockk<AuthorizationHandler>(relaxed = true)
        val issuerAuthorization = IssuerAuthorization(mockHandler, logger)

        issuerAuthorization.authorize(issuer, "1234")

        coVerify(exactly = 0) {
            mockHandler.authorize(any())
        }
        coVerify(exactly = 1) {
            issuer.authorizeWithPreAuthorizationCode("1234")
        }
    }

    @Test
    fun `resumeFromUri delegates to BrowserAuthorizationHandler`() {
        val browserHandler = mockk<BrowserAuthorizationHandler>(relaxed = true)
        val issuerAuthorization = IssuerAuthorization(browserHandler, logger)
        val uri = mockk<Uri>(relaxed = true)

        issuerAuthorization.resumeFromUri(uri)

        verify(exactly = 1) {
            browserHandler.resumeWithUri(uri)
        }
    }

    @Test
    fun `resumeFromUri throws exception with custom handler`() {
        val customHandler = CustomTestAuthorizationHandler()
        val issuerAuthorization = IssuerAuthorization(customHandler, logger)
        val uri = mockk<Uri>(relaxed = true)

        assertFailsWith<IllegalStateException> {
            issuerAuthorization.resumeFromUri(uri)
        }
    }

    @Test
    fun `Config builder with custom authorization handler`() {
        val customHandler = CustomTestAuthorizationHandler()

        val config = OpenId4VciManager.Config {
            withIssuerUrl("https://issuer.example.com")
            withClientId("client-id")
            withAuthFlowRedirectionURI("eudi-wallet://callback")
            withAuthorizationHandler(customHandler)
        }

        assertNotNull(config)
        assertEquals(customHandler, config.authorizationHandler)
    }

    @Test
    fun `Config builder without authorization handler defaults to null`() {
        val config = OpenId4VciManager.Config {
            withIssuerUrl("https://issuer.example.com")
            withClientId("client-id")
            withAuthFlowRedirectionURI("eudi-wallet://callback")
        }

        assertNotNull(config)
        assertEquals(null, config.authorizationHandler)
    }

    /**
     * Custom test implementation of AuthorizationHandler for testing
     */
    private class CustomTestAuthorizationHandler : AuthorizationHandler {
        var authorizeCalled = false
        var lastAuthorizationUrl: String? = null

        override suspend fun authorize(authorizationUrl: String): Result<AuthorizationResponse> {
            authorizeCalled = true
            lastAuthorizationUrl = authorizationUrl
            // Simulate custom authorization logic
            return Result.success(
                AuthorizationResponse(
                    authorizationCode = "custom_auth_code",
                    serverState = "custom_state"
                )
            )
        }
    }
}

