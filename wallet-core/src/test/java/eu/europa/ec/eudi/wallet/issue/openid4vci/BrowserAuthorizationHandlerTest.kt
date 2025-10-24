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
import androidx.core.net.toUri
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserAuthorizationHandlerTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            mockkStatic(Uri::class)
            mockkStatic("androidx.core.net.UriKt")
            every { any<String>().toUri() } answers {
                mockk<Uri>(relaxed = true)
            }

            mockkConstructor(Intent::class)
            every { anyConstructed<Intent>().addFlags(any()) } returns mockk(relaxed = true)
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            unmockkAll()
        }
    }

    lateinit var context: Context
    lateinit var logger: Logger
    lateinit var handler: BrowserAuthorizationHandler

    @BeforeTest
    fun setupTest() {
        context = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        handler = BrowserAuthorizationHandler(context, logger)
    }

    @Test
    fun `authorize opens browser and suspends until resumeWithUri is called`() = runTest {
        val authorizationUrl = "https://issuer.example.com/authorize?client_id=test&state=xyz"
        val callbackUri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "auth_code_123"
            every { getQueryParameter("state") } returns "xyz"
        }

        var result: Result<AuthorizationResponse>? = null

        val job = launch {
            result = handler.authorize(authorizationUrl)
        }

        // Ensure the coroutine has suspended
        testScheduler.runCurrent()

        handler.resumeWithUri(callbackUri)
        job.join()

        // Verify context.startActivity was called
        verify(exactly = 1) { context.startActivity(any()) }

        assertNotNull(result)
        assertTrue(result.isSuccess)
        assertEquals("auth_code_123", result.getOrNull()!!.authorizationCode)
        assertEquals("xyz", result.getOrNull()!!.serverState)
    }

    @Test
    fun `resumeWithUri fails when authorization code is missing`() = runTest {
        val authorizationUrl = "https://issuer.example.com/authorize"
        val callbackUri = mockk<Uri> {
            every { getQueryParameter("code") } returns null
            every { getQueryParameter("state") } returns "xyz"
        }

        var result: Result<AuthorizationResponse>? = null

        val job = launch {
            result = handler.authorize(authorizationUrl)
        }

        // Ensure the coroutine has suspended
        testScheduler.runCurrent()

        handler.resumeWithUri(callbackUri)
        job.join()

        assertNotNull(result)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("No authorization code found"))
    }

    @Test
    fun `resumeWithUri fails when server state is missing`() = runTest {
        val authorizationUrl = "https://issuer.example.com/authorize"
        val callbackUri = mockk<Uri> {
            every { getQueryParameter("code") } returns "auth_code_123"
            every { getQueryParameter("state") } returns null
        }

        var result: Result<AuthorizationResponse>? = null

        val job = launch {
            result = handler.authorize(authorizationUrl)
        }

        // Ensure the coroutine has suspended
        testScheduler.runCurrent()

        handler.resumeWithUri(callbackUri)
        job.join()

        assertNotNull(result)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("No server state found"))
    }

    @Test
    fun `resumeWithUri throws exception when no authorization in progress`() {
        val callbackUri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "auth_code_123"
            every { getQueryParameter("state") } returns "xyz"
        }

        assertFailsWith<IllegalStateException> {
            handler.resumeWithUri(callbackUri)
        }
    }

    @Test
    fun `cancel cancels ongoing authorization`() = runTest {
        val authorizationUrl = "https://issuer.example.com/authorize"

        var result: Result<AuthorizationResponse>? = null
        var exceptionCaught = false

        val job = launch {
            try {
                result = handler.authorize(authorizationUrl)
            } catch (e: Exception) {
                exceptionCaught = true
            }
        }

        // Ensure the coroutine has suspended
        testScheduler.runCurrent()

        handler.cancel()
        job.join()

        // The coroutine should be cancelled
        assertTrue(result == null || exceptionCaught)
    }

    @Test
    fun `multiple authorize calls cancel previous authorization`() = runTest {
        val authorizationUrl1 = "https://issuer.example.com/authorize1"
        val authorizationUrl2 = "https://issuer.example.com/authorize2"
        val callbackUri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "auth_code_123"
            every { getQueryParameter("state") } returns "xyz"
        }

        var result1: Result<AuthorizationResponse>? = null
        var result2: Result<AuthorizationResponse>? = null

        val job1 = launch {
            try {
                result1 = handler.authorize(authorizationUrl1)
            } catch (e: Exception) {
                // Expected to be cancelled
            }
        }

        // Ensure the first coroutine has suspended
        testScheduler.runCurrent()

        val job2 = launch {
            result2 = handler.authorize(authorizationUrl2)
        }

        // Ensure the second coroutine has suspended
        testScheduler.runCurrent()

        handler.resumeWithUri(callbackUri)
        job1.join()
        job2.join()

        // First authorization should be cancelled, second should succeed
        assertTrue(result1 == null)
        assertNotNull(result2)
        assertTrue(result2.isSuccess)
    }
}

