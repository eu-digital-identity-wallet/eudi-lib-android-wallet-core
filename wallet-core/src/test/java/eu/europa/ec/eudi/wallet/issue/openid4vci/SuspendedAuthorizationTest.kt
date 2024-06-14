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

import android.content.Intent
import android.net.Uri
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds


class SuspendedAuthorizationTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            mockkStatic(Log::class)
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
        }
    }

    @Test
    fun `resumeFromIntent resumes with success when authorization code and server state are present`() {

        var suspendedAuthorization: SuspendedAuthorization? = null
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns "testCode"
            every { data?.getQueryParameter("state") } returns "testState"
        }
        var result: Result<SuspendedAuthorization.Response>? = null
        runTest {
            launch {
                result = suspendCancellableCoroutine { continuation ->
                    suspendedAuthorization = spyk(SuspendedAuthorization(continuation))
                }
            }

            launch {
                delay(500.milliseconds)
                suspendedAuthorization!!.use { it.resumeFromIntent(intent) }
            }
        }
        assertTrue(result!!.isSuccess, "Result succeed")
        assertEquals("testCode", result!!.getOrNull()?.authorizationCode)
        assertEquals("testState", result!!.getOrNull()?.serverState)
        verify(exactly = 1) {
            suspendedAuthorization!!.resumeFromIntent(intent)
        }
    }

    @Test
    fun `resumeFromIntent resumes with failure when authorization code is missing`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns null
            every { data?.getQueryParameter("state") } returns "testState"
        }
        var result: Result<SuspendedAuthorization.Response>? = null
        runTest {
            launch {
                result = suspendCancellableCoroutine { continuation ->
                    suspendedAuthorization = spyk(SuspendedAuthorization(continuation))
                }
            }

            launch {
                delay(500.milliseconds)
                suspendedAuthorization!!.use {
                    it.resumeFromIntent(intent)
                }
            }
        }
        assertTrue(result!!.isFailure, "Result failed")
        verify(exactly = 1) {
            suspendedAuthorization!!.resumeFromIntent(intent)
        }
    }

    @Test
    fun `resumeFromIntent resumes with failure when server state is missing`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns "testCode"
            every { data?.getQueryParameter("state") } returns null
        }
        var result: Result<SuspendedAuthorization.Response>? = null
        runTest {
            launch(Dispatchers.Default) {
                result = suspendCancellableCoroutine { continuation ->
                    suspendedAuthorization = spyk(SuspendedAuthorization(continuation))
                }
            }


            launch(Dispatchers.Default) {
                delay(500.milliseconds)
                suspendedAuthorization!!.use { it.resumeFromIntent(intent) }
            }
        }
        assertTrue(result!!.isFailure, "Result failed")
        verify(exactly = 1) {
            suspendedAuthorization!!.resumeFromIntent(intent)
        }
    }

    @Test
    fun `close cancels the continuation`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        var result: Result<SuspendedAuthorization.Response>? = null
        runTest {
            launch {
                result = suspendCancellableCoroutine { continuation ->
                    suspendedAuthorization = spyk(SuspendedAuthorization(continuation))
                }
            }

            launch {
                delay(500.milliseconds)
                suspendedAuthorization!!.close()

            }
        }
        assertNull(result, "Result is null")
        verify(exactly = 1) { suspendedAuthorization!!.close() }
        assertTrue(suspendedAuthorization!!.continuation.isCancelled, "Continuation is cancelled")
    }

    @Test
    fun `verify that method resumeFromIntent calls resumeFromUri`() {
        val continuation = mockk<CancellableContinuation<Result<SuspendedAuthorization.Response>>>(relaxed = true)
        val suspendedAuthorization = spyk(SuspendedAuthorization(continuation))
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns "testCode"
            every { data?.getQueryParameter("state") } returns "testState"
        }
        suspendedAuthorization.resumeFromIntent(intent)
        verify(exactly = 1) {
            suspendedAuthorization.resumeFromUri(intent.data!!)
        }
    }

    @Test
    fun `verify that method resumeFromUri with String calls resumeFromUri with Uri`() {
        mockkStatic(Uri::class)

        val continuation = mockk<CancellableContinuation<Result<SuspendedAuthorization.Response>>>(relaxed = true)
        val suspendedAuthorization = spyk(SuspendedAuthorization(continuation))
        val uriStr = "https://test.com?code=testCode&state=testState"
        val uri = mockk<Uri>(relaxed = true) {
            every { getQueryParameter("code") } returns "testCode"
            every { getQueryParameter("state") } returns "testState"
        }
        every { Uri.parse(uriStr) } answers { uri }

        suspendedAuthorization.resumeFromUri(uri)
        verify(exactly = 1) {
            suspendedAuthorization.resumeFromUri(uri)
        }
    }
}