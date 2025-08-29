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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation

import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifiablePresentations
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Unit tests for custom serializers in the transaction logging presentation layer.
 *
 * These tests verify the correct serialization and deserialization behavior of:
 * - VPTokenConsensusSerializer
 * - VerifiablePresentationsSerializer
 * - VerifiablePresentationSerializer
 */
class SerializersTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Test data setup
    private val queryId1 = QueryId("query1")
    private val queryId2 = QueryId("query2")

    private val genericPresentation = VerifiablePresentation.Generic("test-presentation-string")
    private val jsonObjPresentation = VerifiablePresentation.JsonObj(
        JsonObject(
            mapOf(
                "type" to JsonPrimitive("VerifiableCredential"),
                "id" to JsonPrimitive("test-id"),
                "issuer" to JsonPrimitive("test-issuer")
            )
        )
    )

    private val verifiablePresentations = VerifiablePresentations(
        mapOf(
            queryId1 to listOf(genericPresentation, jsonObjPresentation),
            queryId2 to listOf(genericPresentation)
        )
    )

    private val vpTokenConsensus = Consensus.PositiveConsensus.VPTokenConsensus(
        verifiablePresentations = verifiablePresentations
    )

    @Test
    fun `VPTokenConsensusSerializer - serialize and deserialize successfully`() {
        // Serialize
        val serialized = json.encodeToString(VPTokenConsensusSerializer, vpTokenConsensus)

        // Verify it's valid JSON
        assertTrue("Serialized result should be valid JSON", serialized.isNotEmpty())

        // Deserialize
        val deserialized = json.decodeFromString(VPTokenConsensusSerializer, serialized)

        // Verify the deserialized object matches the original
        assertEquals(
            vpTokenConsensus.verifiablePresentations.value.size,
            deserialized.verifiablePresentations.value.size
        )
        assertEquals(
            vpTokenConsensus.verifiablePresentations.value[queryId1]?.size,
            deserialized.verifiablePresentations.value[queryId1]?.size
        )
    }

    @Test
    fun `VPTokenConsensusSerializer - deserialize fails with missing verifiablePresentations`() {
        val invalidJson = "{}"

        assertFailsWith<SerializationException> {
            json.decodeFromString(VPTokenConsensusSerializer, invalidJson)
        }
    }

    @Test
    fun `VerifiablePresentationsSerializer - serialize and deserialize successfully`() {
        // Serialize
        val serialized =
            json.encodeToString(VerifiablePresentationsSerializer, verifiablePresentations)

        // Verify it's valid JSON and contains expected structure
        assertTrue("Serialized result should be valid JSON", serialized.isNotEmpty())
        assertTrue("Should contain query1", serialized.contains("query1"))
        assertTrue("Should contain query2", serialized.contains("query2"))

        // Deserialize
        val deserialized = json.decodeFromString(VerifiablePresentationsSerializer, serialized)

        // Verify the structure is maintained
        assertEquals(
            "Should have same number of query IDs",
            verifiablePresentations.value.size, deserialized.value.size
        )

        // Verify query1 presentations
        val originalQuery1 = verifiablePresentations.value[queryId1]
        val deserializedQuery1 = deserialized.value[queryId1]
        assertEquals(
            "Query1 should have same number of presentations",
            originalQuery1?.size, deserializedQuery1?.size
        )

        // Verify query2 presentations
        val originalQuery2 = verifiablePresentations.value[queryId2]
        val deserializedQuery2 = deserialized.value[queryId2]
        assertEquals(
            "Query2 should have same number of presentations",
            originalQuery2?.size, deserializedQuery2?.size
        )
    }

    @Test
    fun `VerifiablePresentationSerializer - serialize and deserialize Generic presentation`() {
        // Serialize
        val serialized = json.encodeToString(VerifiablePresentationSerializer, genericPresentation)

        // Verify JSON structure
        assertTrue("Should contain type field", serialized.contains("\"type\":\"Generic\""))
        assertTrue(
            "Should contain value field",
            serialized.contains("\"value\":\"test-presentation-string\"")
        )

        // Deserialize
        val deserialized = json.decodeFromString(VerifiablePresentationSerializer, serialized)

        // Verify it's the correct type and value
        assertTrue("Should be Generic type", deserialized is VerifiablePresentation.Generic)
        assertEquals(
            "Value should match",
            (genericPresentation as VerifiablePresentation.Generic).value,
            (deserialized as VerifiablePresentation.Generic).value
        )
    }

    @Test
    fun `VerifiablePresentationSerializer - serialize and deserialize JsonObj presentation`() {
        // Serialize
        val serialized = json.encodeToString(VerifiablePresentationSerializer, jsonObjPresentation)

        // Verify JSON structure
        assertTrue("Should contain type field", serialized.contains("\"type\":\"JsonObj\""))
        assertTrue("Should contain value field with JSON", serialized.contains("\"value\":"))

        // Deserialize
        val deserialized = json.decodeFromString(VerifiablePresentationSerializer, serialized)

        // Verify it's the correct type and structure
        assertTrue("Should be JsonObj type", deserialized is VerifiablePresentation.JsonObj)
        val deserializedJsonObj = deserialized as VerifiablePresentation.JsonObj
        val originalJsonObj = jsonObjPresentation as VerifiablePresentation.JsonObj

        assertEquals(
            "JSON object should have same keys",
            originalJsonObj.value.keys, deserializedJsonObj.value.keys
        )
        assertEquals(
            "Type field should match",
            originalJsonObj.value["type"], deserializedJsonObj.value["type"]
        )
        assertEquals(
            "ID field should match",
            originalJsonObj.value["id"], deserializedJsonObj.value["id"]
        )
        assertEquals(
            "Issuer field should match",
            originalJsonObj.value["issuer"], deserializedJsonObj.value["issuer"]
        )
    }

    @Test
    fun `VerifiablePresentationSerializer - deserialize fails with missing type`() {
        val invalidJson = """{"value":"test"}"""

        assertFailsWith<SerializationException> {
            json.decodeFromString(VerifiablePresentationSerializer, invalidJson)
        }
    }

    @Test
    fun `VerifiablePresentationSerializer - deserialize fails with missing value`() {
        val invalidJson = """{"type":"Generic"}"""

        assertFailsWith<SerializationException> {
            json.decodeFromString(VerifiablePresentationSerializer, invalidJson)
        }
    }

    @Test
    fun `VerifiablePresentationSerializer - deserialize fails with unknown type`() {
        val invalidJson = """{"type":"Unknown","value":"test"}"""

        assertFailsWith<SerializationException> {
            json.decodeFromString(VerifiablePresentationSerializer, invalidJson)
        }
    }

    @Test
    fun `VerifiablePresentationSerializer - JsonObj with invalid JSON value fails`() {
        val invalidJson = """{"type":"JsonObj","value":"invalid-json"}"""

        assertFailsWith<Exception> {
            json.decodeFromString(VerifiablePresentationSerializer, invalidJson)
        }
    }

    @Test
    fun `VerifiablePresentationSerializer - round trip with complex JsonObj`() {
        val complexJsonObj = VerifiablePresentation.JsonObj(
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("VerifiableCredential"),
                    "credentialSubject" to JsonObject(
                        mapOf(
                            "id" to JsonPrimitive("did:example:123"),
                            "name" to JsonPrimitive("John Doe"),
                            "age" to JsonPrimitive(30)
                        )
                    ),
                    "issuer" to JsonPrimitive("did:example:issuer"),
                    "issuanceDate" to JsonPrimitive("2023-01-01T00:00:00Z")
                )
            )
        )

        // Round trip
        val serialized = json.encodeToString(VerifiablePresentationSerializer, complexJsonObj)
        val deserialized = json.decodeFromString(
            VerifiablePresentationSerializer,
            serialized
        ) as VerifiablePresentation.JsonObj

        // Verify complex structure is preserved
        assertEquals(
            "Should preserve complex JSON structure",
            complexJsonObj.value.toString(), deserialized.value.toString()
        )
    }

    @Test
    fun `Integration test - full VPTokenConsensus round trip with multiple presentation types`() {
        val mixedPresentations = VerifiablePresentations(
            mapOf(
                QueryId("mixed") to listOf(
                    VerifiablePresentation.Generic("simple-string"),
                    VerifiablePresentation.JsonObj(
                        JsonObject(
                            mapOf(
                                "complex" to JsonPrimitive("object")
                            )
                        )
                    ),
                    VerifiablePresentation.Generic("another-string")
                )
            )
        )

        val consensus = Consensus.PositiveConsensus.VPTokenConsensus(
            verifiablePresentations = mixedPresentations
        )

        // Full round trip
        val serialized = json.encodeToString(VPTokenConsensusSerializer, consensus)
        val deserialized = json.decodeFromString(VPTokenConsensusSerializer, serialized)

        // Verify all presentations are preserved
        val originalPresentations = consensus.verifiablePresentations.value[QueryId("mixed")]!!
        val deserializedPresentations =
            deserialized.verifiablePresentations.value[QueryId("mixed")]!!

        assertEquals(
            "Should preserve number of presentations",
            originalPresentations.size, deserializedPresentations.size
        )

        // Verify each presentation type
        assertEquals(
            "First should be Generic",
            VerifiablePresentation.Generic::class, originalPresentations[0]::class
        )
        assertEquals(
            "Second should be JsonObj",
            VerifiablePresentation.JsonObj::class, originalPresentations[1]::class
        )
        assertEquals(
            "Third should be Generic",
            VerifiablePresentation.Generic::class, originalPresentations[2]::class
        )
    }

    @Test
    fun `VPTokenConsensusJson - contextual serializers work correctly`() {
        // Test that the VPTokenConsensusJson configuration with contextual serializers works
        val serialized = VPTokenConsensusJson.encodeToString(vpTokenConsensus)

        // Verify it's valid JSON
        assertTrue("Serialized result should be valid JSON", serialized.isNotEmpty())

        // Deserialize using the contextual serializers
        val deserialized =
            VPTokenConsensusJson.decodeFromString<Consensus.PositiveConsensus.VPTokenConsensus>(
                serialized
            )

        // Verify the deserialized object matches the original
        assertEquals(
            "Should have same number of query IDs",
            vpTokenConsensus.verifiablePresentations.value.size,
            deserialized.verifiablePresentations.value.size
        )

        // Verify specific presentations are preserved
        assertEquals(
            "Query1 should have same number of presentations",
            vpTokenConsensus.verifiablePresentations.value[queryId1]?.size,
            deserialized.verifiablePresentations.value[queryId1]?.size
        )

        assertEquals(
            "Query2 should have same number of presentations",
            vpTokenConsensus.verifiablePresentations.value[queryId2]?.size,
            deserialized.verifiablePresentations.value[queryId2]?.size
        )
    }
}
