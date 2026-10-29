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

package eu.europa.ec.eudi.wallet.document.internal

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonElementExtensionsTest {

    @Test
    fun `parse with a value inside 64 nested arrays returns the value`() {
        var expected: Any = 1
        repeat(64) { expected = listOf(expected) }

        val element = Json.parseToJsonElement("[".repeat(64) + "1" + "]".repeat(64))

        assertEquals(expected, element.parse())
    }

    @Test
    fun `parse with 65 nested arrays or objects throws when the innermost is empty`() {
        val arrays = Json.parseToJsonElement("[".repeat(65) + "]".repeat(65))
        val objects = Json.parseToJsonElement("""{"key":""".repeat(64) + "{}" + "}".repeat(64))

        assertFailsWith<IllegalArgumentException> { arrays.parse() }
        assertFailsWith<IllegalArgumentException> { objects.parse() }
    }
}
