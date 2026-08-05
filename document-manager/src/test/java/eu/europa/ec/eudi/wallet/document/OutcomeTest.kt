/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.document

import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutcomeTest {

    @Test
    fun `success with Valid Value returns Successful Outcome`() {
        val outcome = Outcome.success("Success")

        assertTrue(outcome.isSuccess)
        assertFalse(outcome.isFailure)
        assertEquals("Success", outcome.getOrNull())
    }

    @Test
    fun `failure with Exception returns Failed Outcome`() {
        val exception = RuntimeException("Failure")
        val outcome = Outcome.failure<String>(exception)

        assertTrue(outcome.isFailure)
        assertFalse(outcome.isSuccess)
        assertEquals(exception, outcome.exceptionOrNull())
    }

    @Test
    fun `get Or Null with Success returns Value`() {
        val outcome = Outcome.success(42)

        assertEquals(42, outcome.getOrNull())
    }

    @Test
    fun `get Or Null with Failure returns Null`() {
        val outcome = Outcome.failure<Int>(RuntimeException("Error"))

        assertNull(outcome.getOrNull())
    }

    @Test
    fun `exception Or Null with Success returns Null`() {
        val outcome = Outcome.success("No Error")

        assertNull(outcome.exceptionOrNull())
    }

    @Test
    fun `exception Or Null with Failure returns Exception`() {
        val exception = IllegalArgumentException("Invalid argument")
        val outcome = Outcome.failure<String>(exception)

        assertEquals(exception, outcome.exceptionOrNull())
    }

    @Test
    fun `get Or Throw with Success returns Value`() {
        val outcome = Outcome.success("Value")

        assertEquals("Value", outcome.getOrThrow())
    }

    @Test
    fun `get Or Throw with Failure throws Exception`() {
        val exception = IllegalStateException("State error")
        val outcome = Outcome.failure<String>(exception)

        val thrown = assertThrows(IllegalStateException::class.java) {
            outcome.getOrThrow()
        }
        assertEquals(exception, thrown)
    }

    @Test
    fun `to String with Success returns Correct String`() {
        val outcome = Outcome.success("Success")

        assertEquals("Outcome(value = Success)", outcome.toString())
    }

    @Test
    fun `to String with Failure returns Correct String`() {
        val exception = RuntimeException("Failure")
        val outcome = Outcome.failure<String>(exception)

        assertEquals("Outcome(error = Failure)", outcome.toString())
    }
}