/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.producers

import kotlin.test.Test
import kotlin.test.assertEquals

class NoncompletionReasonTest {

    @Test
    fun `uses the throwable's own message when present`() {
        val reason = IllegalStateException("boom").toNoncompletionReason("fallback")
        assertEquals("boom", reason)
    }

    @Test
    fun `falls back to the cause's message when the message is blank`() {
        val throwable = IllegalStateException("   ", IllegalArgumentException("root cause"))
        assertEquals("root cause", throwable.toNoncompletionReason("fallback"))
    }

    @Test
    fun `uses the default when no message is available`() {
        // The exception type is not a reason, so it is never used.
        assertEquals("fallback", IllegalStateException().toNoncompletionReason("fallback"))
    }

    @Test
    fun `keeps only the first line`() {
        val throwable = IllegalStateException("Request failed\nRequest header `Accept: application/json`")
        assertEquals("Request failed", throwable.toNoncompletionReason("fallback"))
    }

    @Test
    fun `a message too long to read is replaced by the default`() {
        val throwable = IllegalStateException("x".repeat(121))
        assertEquals("fallback", throwable.toNoncompletionReason("fallback"))
    }

    @Test
    fun `falls back to the cause when the message is unusable`() {
        val throwable = IllegalStateException("x".repeat(121), IllegalArgumentException("root cause"))
        assertEquals("root cause", throwable.toNoncompletionReason("fallback"))
    }

    @Test
    fun `a library diagnostic never reaches the reason`() {
        val throwable = IllegalStateException(
            "Expected response body of the type 'class eu.europa.ec.eudi.openid4vci.internal.http." +
                    "GenericErrorResponseTO (Kotlin reflection is not available)' but was 'class " +
                    "io.ktor.utils.io.SourceByteReadChannel (Kotlin reflection is not available)'\n" +
                    "In response from `https://issuer.example/wallet/deferredEndpoint`\n" +
                    "Response status `401 Unauthorized`\n" +
                    "You can read how to resolve NoTransformationFoundException at FAQ: \n" +
                    "https://ktor.io/docs/faq.html#no-transformation-found-exception"
        )

        assertEquals("fallback", throwable.toNoncompletionReason("fallback"))
    }
}
