/*
 * Copyright (c) 2024-2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer

import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExtensionsTest {

    @Test
    fun `ProcessedRequest toKotlinResult returns Result success carrying the same Success instance`() {
        val success = mockk<RequestProcessor.ProcessedRequest.Success>(relaxed = true)

        val result = success.toKotlinResult()

        assertTrue(result.isSuccess, "Result must be in the success state")
        assertSame(success, result.getOrNull(), "The original Success instance must round-trip unchanged")
    }

    @Test
    fun `ProcessedRequest toKotlinResult returns Result failure carrying the wrapped error`() {
        val error = IllegalStateException("malformed request")
        val failure = RequestProcessor.ProcessedRequest.Failure(error)

        val result = failure.toKotlinResult()

        assertTrue(result.isFailure, "Result must be in the failure state")
        assertSame(error, result.exceptionOrNull(), "The original throwable must round-trip unchanged")
    }

    @Test
    fun `ResponseResult toKotlinResult returns Result success carrying the same Success instance`() {
        val success = ResponseResult.Success(mockk<Response>())

        val result = success.toKotlinResult()

        assertTrue(result.isSuccess, "Result must be in the success state")
        assertSame(success, result.getOrNull(), "The original Success instance must round-trip unchanged")
    }

    @Test
    fun `ResponseResult toKotlinResult returns Result failure carrying the wrapped throwable`() {
        val throwable = RuntimeException("signing failed")
        val failure = ResponseResult.Failure(throwable)

        val result = failure.toKotlinResult()

        assertTrue(result.isFailure, "Result must be in the failure state")
        assertSame(throwable, result.exceptionOrNull(), "The original throwable must round-trip unchanged")
    }
}