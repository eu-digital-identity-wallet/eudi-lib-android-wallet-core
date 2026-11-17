/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci.tokens

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class BoundValuesTest {

    @Test
    fun `the fields present in a record are bound by name, in the order of their names`() {
        val record = JsonObject(
            mapOf(
                "string" to JsonPrimitive("x"),
                "list" to JsonArray(listOf(JsonPrimitive("k1"), JsonPrimitive("k2"))),
                "absentValue" to JsonNull,
                "number" to JsonPrimitive(1700000000L),
                "token" to JsonPrimitive("secret"),
            )
        )

        assertEquals(
            listOf("@absentValue", "null", "@list", "[2", "=k1", "=k2", "@number", "#1700000000", "@string", "=x"),
            boundValues(record, unbound = setOf("token"))
        )
    }
}
