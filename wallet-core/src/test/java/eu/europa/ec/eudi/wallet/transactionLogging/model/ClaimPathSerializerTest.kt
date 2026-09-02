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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests [ClaimPath] and its serializer against the OpenID4VP §7 wire shape (JSON arrays of strings,
 * non-negative integers, or null), including round-trips and all-string paths.
 */
class ClaimPathSerializerTest {

    private val json = Json

    @Test
    fun `keys-only path encodes as JSON array of strings`() {
        val path = ClaimPath.ofKeys("address", "locality")
        val encoded = json.encodeToString(ClaimPath.serializer(), path)
        assertEquals("""["address","locality"]""", encoded)
        // Round-trip
        assertEquals(path, json.decodeFromString(ClaimPath.serializer(), encoded))
    }

    @Test
    fun `array index segment encodes as JSON integer`() {
        val path = ClaimPath.ofKeys("documents").appendIndex(0).appendKey("type")
        val encoded = json.encodeToString(ClaimPath.serializer(), path)
        assertEquals("""["documents",0,"type"]""", encoded)
        assertEquals(path, json.decodeFromString(ClaimPath.serializer(), encoded))
    }

    @Test
    fun `wildcard segment encodes as JSON null`() {
        val path = ClaimPath.ofKeys("nationalities").appendWildcard()
        val encoded = json.encodeToString(ClaimPath.serializer(), path)
        assertEquals("""["nationalities",null]""", encoded)
        assertEquals(path, json.decodeFromString(ClaimPath.serializer(), encoded))
    }

    @Test
    fun `an all-string array decodes to all-Key segments`() {
        // An array of only strings becomes a path of only Key segments.
        val allStringPath = """["org.iso.18013.5.1","family_name"]"""
        val decoded = json.decodeFromString(ClaimPath.serializer(), allStringPath)
        assertEquals(ClaimPath.ofKeys("org.iso.18013.5.1", "family_name"), decoded)
    }

    @Test
    fun `OpenID4VP §7 example with mixed segment kinds round-trips`() {
        // The §7.3 example mixes string, integer, and wildcard segments; check all survive a round-trip.
        val mixed = listOf(
            ClaimPath.ofKeys("name"),
            ClaimPath.ofKeys("address", "locality"),
            ClaimPath.ofKeys("documents").appendIndex(0).appendKey("type"),
            ClaimPath.ofKeys("documents").appendWildcard().appendKey("number"),
        )
        val encoded = json.encodeToString(ListSerializer(ClaimPath.serializer()), mixed)
        assertEquals(
            """[["name"],["address","locality"],["documents",0,"type"],["documents",null,"number"]]""",
            encoded,
        )
        assertEquals(mixed, json.decodeFromString(ListSerializer(ClaimPath.serializer()), encoded))
    }

    @Test
    fun `non-empty path is enforced by the constructor`() {
        assertFailsWith<IllegalArgumentException> { ClaimPath(emptyList()) }
    }

    @Test
    fun `negative array index is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { ClaimPath.Segment.Index(-1) }
    }

    @Test
    fun `decoding a negative index fails`() {
        assertFailsWith<IllegalArgumentException> {
            json.decodeFromString(ClaimPath.serializer(), """["documents",-1]""")
        }
    }

    @Test
    fun `decoding a boolean segment fails`() {
        assertFailsWith<IllegalStateException> {
            json.decodeFromString(ClaimPath.serializer(), """["documents",true]""")
        }
    }

    @Test
    fun `decoding an empty array fails`() {
        assertFailsWith<IllegalArgumentException> {
            json.decodeFromString(ClaimPath.serializer(), """[]""")
        }
    }
}
