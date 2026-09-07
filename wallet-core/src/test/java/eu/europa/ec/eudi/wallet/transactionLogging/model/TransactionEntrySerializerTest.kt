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

import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Round-trip tests for the [TransactionEntry] serializer (TS10 wire shape). Also checks that
 * presentation JSON never contains claim values.
 */
class TransactionEntrySerializerTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val time: Instant = Instant.parse("2026-06-08T10:15:30Z")

    private fun assertRoundTrip(entry: TransactionEntry) {
        val encoded = json.encodeToString(TransactionEntry.serializer(), entry)
        val decoded = json.decodeFromString(TransactionEntry.serializer(), encoded)
        assertEquals(entry, decoded, "round-trip mismatch for ${entry.transactionType}")
    }

    @Test
    fun `presentation completed round-trips`() {
        assertRoundTrip(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-1",
                time = time,
                transactionResult = TransactionResult.Completed,
                listOfClaimsRequested = listOf(
                    ClaimInfo("eu.europa.ec.eudi.pid.1", listOf(ClaimPath.ofKeys("family_name"), ClaimPath.ofKeys("given_name")))
                ),
                listOfClaimsPresented = listOf(
                    ClaimInfo("eu.europa.ec.eudi.pid.1", listOf(ClaimPath.ofKeys("family_name")))
                ),
                interactingPartyName = MultiLangString("en", "Acme Verifier"),
            )
        )
    }

    @Test
    fun `presentation not-completed preserves reason`() {
        assertRoundTrip(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-2",
                time = time,
                transactionResult = TransactionResult.NotCompleted("User cancelled"),
                listOfClaimsRequested = listOf(ClaimInfo("vct", listOf(ClaimPath.ofKeys("age_over_18")))),
                listOfClaimsPresented = emptyList(),
            )
        )
    }

    @Test
    fun `credential issuance round-trips`() {
        assertRoundTrip(
            TransactionEntry.CredentialIssuance(
                transactionIdentifier = "tx-3",
                time = time,
                transactionResult = TransactionResult.Completed,
                details = TransactionEntry.CredentialIssuanceDetails(
                    credentialNumberRequested = 5,
                    credentialNumberIssued = 5,
                    credentialIdentifier = listOf("eu.europa.ec.eudi.pid.1"),
                    isUserTriggered = true,
                    interactingPartyName = MultiLangString("en", "PID Provider"),
                ),
            )
        )
    }

    @Test
    fun `credential reissuance is distinct from issuance (different discriminator)`() {
        val entry = TransactionEntry.CredentialReissuance(
            transactionIdentifier = "tx-4",
            time = time,
            transactionResult = TransactionResult.Completed,
            details = TransactionEntry.CredentialIssuanceDetails(
                credentialNumberRequested = 1,
                credentialNumberIssued = 1,
                credentialIdentifier = listOf("org.iso.18013.5.1.mDL"),
                isUserTriggered = false,
            ),
        )
        val encoded = json.encodeToString(TransactionEntry.serializer(), entry)
        assertTrue(encoded.contains(""""transactionType":"CredentialReissuance""""))
        assertRoundTrip(entry)
    }

    @Test
    fun `credential deletion round-trips`() {
        assertRoundTrip(
            TransactionEntry.CredentialDeletion(
                transactionIdentifier = "tx-5",
                time = time,
                transactionResult = TransactionResult.Completed,
                credentialIdentifier = "eu.europa.ec.eudi.pid.1",
                credentialIssuerName = MultiLangString("en", "ABC Issuer"),
            )
        )
    }

    @Test
    fun `time is encoded as ISO 8601 string`() {
        val entry = TransactionEntry.Presentation(
            transactionIdentifier = "tx-time",
            time = Instant.parse("2026-06-08T10:15:30Z"),
            transactionResult = TransactionResult.Completed,
            listOfClaimsRequested = emptyList(),
            listOfClaimsPresented = emptyList(),
        )
        val encoded = json.encodeToString(TransactionEntry.serializer(), entry)
        assertTrue(encoded.contains(""""time":"2026-06-08T10:15:30Z""""))
    }

    @Test
    fun `presentation JSON stores claim identifiers and paths only - never values`() {
        val entry = TransactionEntry.Presentation(
            transactionIdentifier = "tx-dash03b",
            time = time,
            transactionResult = TransactionResult.Completed,
            listOfClaimsRequested = listOf(
                ClaimInfo("eu.europa.ec.eudi.pid.1", listOf(ClaimPath.ofKeys("family_name")))
            ),
            listOfClaimsPresented = listOf(
                ClaimInfo("eu.europa.ec.eudi.pid.1", listOf(ClaimPath.ofKeys("family_name")))
            ),
        )
        val encoded = json.encodeToString(TransactionEntry.serializer(), entry)
        // Identifiers and paths are present...
        assertTrue(encoded.contains("family_name"))
        assertTrue(encoded.contains("eu.europa.ec.eudi.pid.1"))
        // ...but no value-like fields.
        assertFalse(encoded.contains("\"value\""))
        assertFalse(encoded.contains("\"rawValue\""))
    }
}
