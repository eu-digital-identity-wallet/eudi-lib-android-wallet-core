/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds


class IssuerAuthorizationTest {

    companion object {
        lateinit var context: Context
        lateinit var logger: Logger
        lateinit var issuer: Issuer

        @BeforeAll
        @JvmStatic
        fun setup() {

            mockkStatic(Uri::class)
            every { Uri.parse(any()) } returns mockk(relaxed = true)

            mockkConstructor(Intent::class)
            every { anyConstructed<Intent>().addFlags(any()) } returns mockk(relaxed = true)

            context = mockk(relaxed = true)
            logger = mockk(relaxed = true)
        }
    }

    lateinit var preparedAuthorizationRequest: AuthorizationRequestPrepared
    lateinit var authorizedRequest: AuthorizedRequest

    @BeforeEach
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
    fun `authorize method when no preAuthorizedCode in offer and txCode is null calls openBrowserForAuthorization`() {
        every { issuer.credentialOffer } returns mockk(relaxed = true) {
            every { grants } returns mockk(relaxed = true) {
                every { preAuthorizedCode() } returns null
            }
        }
        val issuerAuthorization = spyk(IssuerAuthorization(context, logger))
        runTest {
            launch {
                issuerAuthorization.authorize(issuer, null)
            }
            launch {
                delay(500.milliseconds)
                issuerAuthorization.close()
            }
        }
        coVerify(exactly = 1) {
            issuer.prepareAuthorizationRequest()
            issuerAuthorization.openBrowserForAuthorization(preparedAuthorizationRequest)
        }
    }

    @Test
    fun `authorize method when preAuthorizedCode in offer and passing txCode does not call openBrowserForAuthorization but calls authorizeWithPreAuthorizationCode`() {
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
        val issuerAuthorization = spyk(IssuerAuthorization(context, logger))
        runTest {
            launch {
                issuerAuthorization.authorize(issuer, "1234")
            }
            launch {
                delay(500.milliseconds)
                issuerAuthorization.close()
            }
        }
        coVerify(exactly = 0) {
            issuer.prepareAuthorizationRequest()
            issuerAuthorization.openBrowserForAuthorization(preparedAuthorizationRequest)
        }
        coVerify(exactly = 1) {
            issuer.authorizeWithPreAuthorizationCode("1234")
        }
    }

    @Test
    fun `resumeFromUri resumes with success when authorization code and server state are present`() {

        val issuerAuthorization = spyk(IssuerAuthorization(context, logger))
        val uri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "testCode"
            every { getQueryParameter("state") } returns "testState"
        }
        var result: Result<IssuerAuthorization.Response>? = null
        runTest {
            launch {
                result = issuerAuthorization.openBrowserForAuthorization(preparedAuthorizationRequest)
            }

            launch {
                delay(500.milliseconds)
                issuerAuthorization.use { it.resumeFromUri(uri) }
            }
        }
        assertNotNull(result, "Result is not null")
        assertTrue(result!!.isSuccess)
        assertEquals("testCode", result!!.getOrNull()!!.authorizationCode)
        assertEquals("testState", result!!.getOrNull()!!.serverState)
        verify(exactly = 1) {
            issuerAuthorization.resumeFromUri(uri)
        }
    }

    @Test
    fun `resumeFromUri resumes with failure when authorization code is missing`() {
        val issuerAuthorization: IssuerAuthorization = spyk(IssuerAuthorization(context, logger))
        val uri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns null
            every { getQueryParameter("state") } returns "testState"
        }
        var result: Result<IssuerAuthorization.Response>? = null
        runTest {
            launch {
                result = issuerAuthorization.openBrowserForAuthorization(preparedAuthorizationRequest)
            }

            launch {
                delay(500.milliseconds)
                issuerAuthorization.use { it.resumeFromUri(uri) }
            }
        }
        assertNotNull(result, "Result is not null")
        assertTrue(result!!.isFailure)
        verify(exactly = 1) {
            issuerAuthorization.resumeFromUri(uri)
        }
    }

    @Test
    fun `resumeFromUri resumes with failure when server state is missing`() {
        val issuerAuthorization: IssuerAuthorization = spyk(IssuerAuthorization(context, logger))
        val uri = mockk<Uri> {
            every { getQueryParameter("code") } returns "testCode"
            every { getQueryParameter("state") } returns null
        }
        var result: Result<IssuerAuthorization.Response>? = null
        runTest {
            launch(Dispatchers.Default) {
                result = issuerAuthorization.openBrowserForAuthorization(preparedAuthorizationRequest)
            }


            launch(Dispatchers.Default) {
                delay(500.milliseconds)
                issuerAuthorization.use { it.resumeFromUri(uri) }
            }
        }
        assertNotNull(result, "Result is not null")
        assertTrue(result!!.isFailure, "Result failed")
        verify(exactly = 1) {
            issuerAuthorization.resumeFromUri(uri)
        }
    }

    @Test
    fun `close cancels the continuation`() {
        val issuerAuthorization: IssuerAuthorization = spyk(IssuerAuthorization(context, logger))
        var result: Result<IssuerAuthorization.Response>? = null
        runTest {
            launch {
                result = issuerAuthorization.openBrowserForAuthorization(preparedAuthorizationRequest)
            }

            launch {
                delay(500.milliseconds)
                issuerAuthorization.close()

            }
        }
        assertNull(result, "Result is null")
        verify(exactly = 1) { issuerAuthorization.close() }
        assertNull(issuerAuthorization.continuation, "Continuation is removed")
    }
}