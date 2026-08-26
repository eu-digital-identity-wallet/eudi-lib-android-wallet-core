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
    fun `falls back to the simple class name when no message is available`() {
        // IllegalStateException() has a null message and no cause.
        assertEquals("IllegalStateException", IllegalStateException().toNoncompletionReason("fallback"))
    }

    @Test
    fun `uses the default only when neither message nor class name are available`() {
        // An anonymous throwable has no simple class name.
        val anonymous = object : Throwable() {}
        assertEquals("fallback", anonymous.toNoncompletionReason("fallback"))
    }
}
