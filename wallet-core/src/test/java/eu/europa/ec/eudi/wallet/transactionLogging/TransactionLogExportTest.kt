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

package eu.europa.ec.eudi.wallet.transactionLogging

import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the TS10 §4.1 export shape ([TransactionEntryExportSerializer] / [TransactionLogExport]).
 */
class TransactionLogExportTest {

    private val json = Json
    private val time: Instant = Instant.parse("2026-06-08T10:15:30Z")

    private fun export(entry: TransactionEntry): JsonObject =
        json.parseToJsonElement(TransactionLogExport().encodeEntry(entry)).jsonObject

    @Test
    fun `transactionResult is a flat string and Completed has no reasonOfNoncompletion`() {
        val obj = export(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-1",
                time = time,
                transactionResult = TransactionResult.Completed,
                listOfClaimsRequested = emptyList(),
                listOfClaimsPresented = emptyList(),
            )
        )
        assertEquals("Completed", obj["transactionResult"]?.jsonPrimitive?.contentOrNull)
        assertTrue(obj["transactionResult"] is JsonPrimitive, "transactionResult must be a flat string")
        assertNull(obj["reasonOfNoncompletion"], "Completed must not carry reasonOfNoncompletion")
    }

    @Test
    fun `NotCompleted hoists reason to a sibling reasonOfNoncompletion field`() {
        val obj = export(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-2",
                time = time,
                transactionResult = TransactionResult.NotCompleted("session interrupted"),
                listOfClaimsRequested = emptyList(),
                listOfClaimsPresented = emptyList(),
            )
        )
        assertEquals("NotCompleted", obj["transactionResult"]?.jsonPrimitive?.contentOrNull)
        assertEquals(
            "session interrupted",
            obj["reasonOfNoncompletion"]?.jsonPrimitive?.contentOrNull,
            "reason must be hoisted to a top-level sibling",
        )
    }

    @Test
    fun `NotCompleted with null reason omits reasonOfNoncompletion`() {
        val obj = export(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-2b",
                time = time,
                transactionResult = TransactionResult.NotCompleted(null),
                listOfClaimsRequested = emptyList(),
                listOfClaimsPresented = emptyList(),
            )
        )
        assertEquals("NotCompleted", obj["transactionResult"]?.jsonPrimitive?.contentOrNull)
        assertNull(obj["reasonOfNoncompletion"])
    }

    @Test
    fun `credential issuance details are flattened to top-level attributes`() {
        val obj = export(
            TransactionEntry.CredentialIssuance(
                transactionIdentifier = "tx-3",
                time = time,
                transactionResult = TransactionResult.Completed,
                details = TransactionEntry.CredentialIssuanceDetails(
                    credentialNumberRequested = 5,
                    credentialNumberIssued = 4,
                    credentialIdentifier = listOf("eu.europa.ec.eudi.pid.1"),
                    isUserTriggered = true,
                    interactingPartyName = MultiLangString("en", "PID Provider"),
                ),
            )
        )
        assertNull(obj["details"], "the details wrapper must be gone")
        assertEquals(5, obj["credentialNumberRequested"]?.jsonPrimitive?.int)
        assertEquals(4, obj["credentialNumberIssued"]?.jsonPrimitive?.int)
        assertEquals(true, obj["isUserTriggered"]?.jsonPrimitive?.contentOrNull?.toBoolean())
        assertEquals(
            "eu.europa.ec.eudi.pid.1",
            obj["credentialIdentifier"]?.jsonArray?.first()?.jsonPrimitive?.contentOrNull,
        )
        // MultiLangString stays an object, not a bare string (TS10 §3.x).
        assertEquals(
            "PID Provider",
            obj["interactingPartyName"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull,
        )
        assertEquals("CredentialIssuance", obj["transactionType"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `time is a UTC instant truncated to seconds, keeping the Z designator`() {
        val obj = export(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-time",
                time = Instant.parse("2025-07-29T09:11:20.987654321Z"),
                transactionResult = TransactionResult.Completed,
                listOfClaimsRequested = emptyList(),
                listOfClaimsPresented = emptyList(),
            )
        )
        val rendered = obj["time"]?.jsonPrimitive?.contentOrNull
        // UTC, seconds only (fractional dropped), with the Z suffix.
        assertEquals("2025-07-29T09:11:20Z", rendered)
        assertTrue(rendered!!.endsWith("Z"), "export time keeps the UTC Z designator (ISO 8601 + RFC 3339)")
    }

    @Test
    fun `claims are preserved as OpenID4VP path-pointer arrays, not collapsed to strings`() {
        val obj = export(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-claims",
                time = time,
                transactionResult = TransactionResult.Completed,
                listOfClaimsRequested = emptyList(),
                listOfClaimsPresented = listOf(
                    ClaimInfo(
                        credentialIdentifier = "org.iso.18013.5.1.mDL",
                        claims = listOf(
                            // mdoc path: [namespace, element]
                            ClaimPath.ofKeys("org.iso.18013.5.1", "family_name"),
                            // indexed and wildcard paths
                            ClaimPath(
                                listOf(
                                    ClaimPath.Segment.Key("nationalities"),
                                    ClaimPath.Segment.Index(0),
                                )
                            ),
                            ClaimPath(
                                listOf(
                                    ClaimPath.Segment.Key("nationalities"),
                                    ClaimPath.Segment.Wildcard,
                                )
                            ),
                        ),
                    )
                ),
            )
        )
        val claims = obj["listOfClaimsPresented"]!!.jsonArray.first().jsonObject["claims"]!!.jsonArray

        // Each claim is an array (path pointer), not a bare string.
        val mdocPath = claims[0]
        assertTrue(mdocPath is JsonArray, "claim path must stay an OpenID4VP §7 array")
        assertEquals(
            listOf("org.iso.18013.5.1", "family_name"),
            mdocPath.map { it.jsonPrimitive.content },
        )

        // Index segment stays a JSON number.
        val indexedPath = claims[1].jsonArray
        assertEquals("nationalities", indexedPath[0].jsonPrimitive.content)
        assertEquals(0, indexedPath[1].jsonPrimitive.int)

        // Wildcard segment stays JSON null.
        val wildcardPath = claims[2].jsonArray
        assertEquals("nationalities", wildcardPath[0].jsonPrimitive.content)
        assertTrue(wildcardPath[1] is kotlinx.serialization.json.JsonNull)
    }

    @Test
    fun `null optional fields are omitted from the export`() {
        val obj = export(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-nulls",
                time = time,
                transactionResult = TransactionResult.Completed,
                listOfClaimsRequested = emptyList(),
                listOfClaimsPresented = emptyList(),
            )
        )
        // Null by default, so they must not appear at all.
        assertNull(obj["interactingPartyIdentifier"])
        assertNull(obj["interactingPartyName"])
        assertNull(obj["registrarURL"])
        // A non-null default is present.
        assertEquals(
            TransactionEntry.Presentation.INTERACTING_PARTY_TYPE,
            obj["interactingPartyType"]?.jsonPrimitive?.contentOrNull,
        )
    }

    @Test
    fun `encode wraps entries in a TransactionLog array`() {
        val out = TransactionLogExport().encode(
            listOf(
                TransactionEntry.Presentation(
                    transactionIdentifier = "tx-a",
                    time = time,
                    transactionResult = TransactionResult.Completed,
                    listOfClaimsRequested = emptyList(),
                    listOfClaimsPresented = emptyList(),
                ),
                TransactionEntry.CredentialDeletion(
                    transactionIdentifier = "tx-b",
                    time = time,
                    transactionResult = TransactionResult.Completed,
                    credentialIdentifier = "eu.europa.ec.eudi.pid.1",
                ),
            )
        )
        val root = json.parseToJsonElement(out).jsonObject
        val log = root[TransactionLogExport.TRANSACTION_LOG_KEY]?.jsonArray
        assertEquals(2, log?.size)
        assertEquals("tx-a", log!![0].jsonObject["transactionIdentifier"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Presentation", log[0].jsonObject["transactionType"]?.jsonPrimitive?.contentOrNull)
        assertEquals("CredentialDeletion", log[1].jsonObject["transactionType"]?.jsonPrimitive?.contentOrNull)
    }
}
