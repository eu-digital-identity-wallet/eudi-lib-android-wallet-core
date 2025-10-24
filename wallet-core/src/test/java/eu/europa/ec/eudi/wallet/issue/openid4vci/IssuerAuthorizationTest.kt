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
import android.content.Intent
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IssuerAuthorizationTest {

    companion object {
        lateinit var context: Context
        lateinit var logger: Logger
        lateinit var issuer: Issuer

        @BeforeClass
        @JvmStatic
        fun setup() {

            mockkStatic(Uri::class)
            every { Uri.parse(any()) } returns mockk(relaxed = true)

            mockkConstructor(Intent::class)
            every { anyConstructed<Intent>().addFlags(any()) } returns mockk(relaxed = true)

            context = mockk(relaxed = true)
            logger = mockk(relaxed = true)
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            unmockkAll()
        }
    }

    lateinit var preparedAuthorizationRequest: AuthorizationRequestPrepared
    lateinit var authorizedRequest: AuthorizedRequest

    @BeforeTest
    fun setupTest() {
        preparedAuthorizationRequest = mockk(relaxed = true)
        every {
            preparedAuthorizationRequest.authorizationCodeURL
        } returns HttpsUrl("https://test.com").getOrThrow()

        issuer = mockk(relaxed = true)
        authorizedRequest = mockk(relaxed = true)
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
    fun `authorize method when no preAuthorizedCode in offer and txCode is null calls authorization handler`() {
        every { issuer.credentialOffer } returns mockk(relaxed = true) {
            every { grants } returns mockk(relaxed = true) {
                every { preAuthorizedCode() } returns null
            }
        }
        val mockHandler = mockk<BrowserAuthorizationHandler>(relaxed = true)
        coEvery { mockHandler.authorize(any()) } returns Result.success(
            AuthorizationResponse("test_code", "test_state")
        )
        val issuerAuthorization = IssuerAuthorization(mockHandler, logger)
        runTest {
            issuerAuthorization.authorize(issuer, null)
        }
        coVerify(exactly = 1) {
            issuer.prepareAuthorizationRequest()
            mockHandler.authorize(any())
        }
    }

    @Test
    fun `authorize method when preAuthorizedCode in offer and passing txCode does not call authorization handler but calls authorizeWithPreAuthorizationCode`() {
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
        runTest {
            issuerAuthorization.authorize(issuer, "1234")
        }
        coVerify(exactly = 0) {
            issuer.prepareAuthorizationRequest()
            mockHandler.authorize(any())
        }
        coVerify(exactly = 1) {
            issuer.authorizeWithPreAuthorizationCode("1234")
        }
    }

    @Test
    fun `resumeFromUri resumes with success when authorization code and server state are present`() {
        val browserHandler = BrowserAuthorizationHandler(context, logger)
        val issuerAuthorization = IssuerAuthorization(browserHandler, logger)
        val uri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "testCode"
            every { getQueryParameter("state") } returns "testState"
        }
        var result: Result<AuthorizationResponse>? = null
        runTest {
            launch {
                result = browserHandler.authorize("https://test.com")
            }

            advanceUntilIdle()
            issuerAuthorization.resumeFromUri(uri)
            advanceUntilIdle()
        }
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals("testCode", result.getOrNull()!!.authorizationCode)
        assertEquals("testState", result.getOrNull()!!.serverState)
        verify(exactly = 1) {
            uri.getQueryParameter("code")
            uri.getQueryParameter("state")
        }
    }

    @Test
    fun `resumeFromUri resumes with failure when authorization code is missing`() {
        val browserHandler = BrowserAuthorizationHandler(context, logger)
        val issuerAuthorization = IssuerAuthorization(browserHandler, logger)
        val uri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns null
            every { getQueryParameter("state") } returns "testState"
        }
        var result: Result<AuthorizationResponse>? = null
        runTest {
            launch {
                result = browserHandler.authorize("https://test.com")
            }

            advanceUntilIdle()
            issuerAuthorization.resumeFromUri(uri)
            advanceUntilIdle()
        }
        assertNotNull(result)
        assertTrue(result!!.isFailure)
        verify(exactly = 1) {
            uri.getQueryParameter("code")
        }
    }

    @Test
    fun `resumeFromUri resumes with failure when server state is missing`() {
        val browserHandler = BrowserAuthorizationHandler(context, logger)
        val issuerAuthorization = IssuerAuthorization(browserHandler, logger)
        val uri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "testCode"
            every { getQueryParameter("state") } returns null
        }
        var result: Result<AuthorizationResponse>? = null
        runTest {
            launch {
                result = browserHandler.authorize("https://test.com")
            }

            advanceUntilIdle()
            issuerAuthorization.resumeFromUri(uri)
            advanceUntilIdle()
        }
        assertNotNull(result)
        assertTrue(result!!.isFailure)
        verify(exactly = 1) {
            uri.getQueryParameter("state")
        }
    }
}