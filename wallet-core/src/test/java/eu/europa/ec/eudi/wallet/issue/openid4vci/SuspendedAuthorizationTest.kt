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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendedAuthorizationTest {

    @Test
    fun `resumeFromIntent resumes with success when authorization code and server state are present`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        CoroutineScope(Dispatchers.Default).launch {
            val result = suspendCancellableCoroutine { continuation ->
                suspendedAuthorization = SuspendedAuthorization(continuation)
            }
            assertTrue("Result succeed", result.isSuccess)
            assertEquals("testCode", result.getOrNull()?.authorizationCode)
            assertEquals("testState", result.getOrNull()?.serverState)
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(100L)
            val intent = mockk<Intent> {
                every { data?.getQueryParameter("code") } returns "testCode"
                every { data?.getQueryParameter("state") } returns "testState"
            }
            suspendedAuthorization!!.resumeFromIntent(intent)
        }
    }

    @Test
    fun `resumeFromIntent resumes with failure when authorization code is missing`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        CoroutineScope(Dispatchers.Default).launch {
            val result = suspendCancellableCoroutine { continuation ->
                suspendedAuthorization = SuspendedAuthorization(continuation)


            }

            assertTrue("Result failed", result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(100L)
            val intent = mockk<Intent> {
                every { data?.getQueryParameter("code") } returns null
                every { data?.getQueryParameter("state") } returns "testState"
            }
            suspendedAuthorization!!.resumeFromIntent(intent)
        }
    }

    @Test
    fun `resumeFromIntent resumes with failure when server state is missing`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        CoroutineScope(Dispatchers.Default).launch {
            val result = suspendCancellableCoroutine { continuation ->
                suspendedAuthorization = SuspendedAuthorization(continuation)
            }
            assertTrue("Result failed", result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }


        CoroutineScope(Dispatchers.Default).launch {
            delay(100L)
            val intent = mockk<Intent> {
                every { data?.getQueryParameter("code") } returns "testCode"
                every { data?.getQueryParameter("state") } returns null
            }
            suspendedAuthorization!!.resumeFromIntent(intent)
        }
    }

    @Test
    fun `close cancels the continuation`() {
        var suspendedAuthorization: SuspendedAuthorization? = null
        var cont: CancellableContinuation<Result<SuspendedAuthorization.Response>>? = null
        CoroutineScope(Dispatchers.Default).launch {
            val result = suspendCancellableCoroutine { continuation ->
                cont = continuation
                suspendedAuthorization = SuspendedAuthorization(continuation)
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(100L)
            suspendedAuthorization!!.close()
            assertTrue("Continuation is cancelled", cont!!.isCancelled)
        }
    }
}