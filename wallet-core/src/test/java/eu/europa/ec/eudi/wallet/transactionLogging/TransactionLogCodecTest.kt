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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trip tests for the storage codec ([toJson] / [toTransactionEntryOrNull]): what is saved
 * must decode back to an equal [TransactionEntry].
 */
class TransactionLogCodecTest {

    private val time: Instant = Instant.parse("2026-06-09T07:36:02.409738Z")

    private fun assertRoundTrip(entry: TransactionEntry) {
        val decoded = entry.toJson().toTransactionEntryOrNull()
        assertEquals(entry, decoded, "storage round-trip mismatch for ${entry.transactionType}")
    }

    @Test
    fun `presentation round-trips through storage codec`() {
        assertRoundTrip(
            TransactionEntry.Presentation(
                transactionIdentifier = "tx-1",
                time = time,
                transactionResult = TransactionResult.Completed,
                listOfClaimsRequested = listOf(
                    ClaimInfo(
                        credentialIdentifier = "org.iso.18013.5.1.mDL",
                        claims = listOf(ClaimPath.ofKeys("org.iso.18013.5.1", "family_name")),
                    )
                ),
                listOfClaimsPresented = listOf(
                    ClaimInfo(
                        credentialIdentifier = "org.iso.18013.5.1.mDL",
                        claims = listOf(ClaimPath.ofKeys("org.iso.18013.5.1", "family_name")),
                    )
                ),
                interactingPartyName = MultiLangString("en", "mdoc reader authentication"),
            )
        )
    }

    @Test
    fun `credential issuance with not-completed reason round-trips`() {
        assertRoundTrip(
            TransactionEntry.CredentialIssuance(
                transactionIdentifier = "tx-2",
                time = time,
                transactionResult = TransactionResult.NotCompleted("User cancelled"),
                details = TransactionEntry.CredentialIssuanceDetails(
                    credentialNumberRequested = 3,
                    credentialNumberIssued = 2,
                    credentialIdentifier = listOf("eu.europa.ec.eudi.pid.1", "urn:eudi:pid:1"),
                    interactingPartyName = MultiLangString("en", "Digital Credentials Issuer"),
                ),
            )
        )
    }

    @Test
    fun `decoding malformed JSON returns null instead of throwing`() {
        assertNull("not json at all".toTransactionEntryOrNull())
        assertNull("""{"foo":"bar"}""".toTransactionEntryOrNull())
    }

    @Test
    fun `unknown keys are ignored on decode (forward compatibility)`() {
        val entry = TransactionEntry.CredentialDeletion(
            transactionIdentifier = "tx-3",
            time = time,
            transactionResult = TransactionResult.Completed,
            credentialIdentifier = "eu.europa.ec.eudi.pid.1",
        )
        // Add an unknown field to the saved JSON; it must still decode.
        val withExtraKey = entry.toJson().replaceFirst("{", """{"futureField":"x",""")
        val decoded = withExtraKey.toTransactionEntryOrNull()
        assertTrue(decoded is TransactionEntry.CredentialDeletion)
        assertEquals(entry, decoded)
    }
}
