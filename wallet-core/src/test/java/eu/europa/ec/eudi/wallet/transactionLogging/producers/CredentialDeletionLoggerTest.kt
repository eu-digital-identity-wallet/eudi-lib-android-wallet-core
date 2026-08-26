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

import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialDeletionLoggerTest {

    private class RecordingLogManager : TransactionLogManager {
        val entries = mutableListOf<TransactionEntry>()
        override fun log(entry: TransactionEntry) {
            entries.add(entry)
        }
    }

    private fun issuerMetadata(
        issuerId: String = "https://issuer.example",
        issuerName: String = "Test Issuer",
    ) = IssuerMetadata(
        documentConfigurationIdentifier = "cfg",
        display = emptyList(),
        claims = null,
        credentialIssuerIdentifier = issuerId,
        issuerDisplay = listOf(IssuerMetadata.IssuerDisplay(name = issuerName)),
    )

    @Test
    fun `successful deletion logs a completed CredentialDeletion with issuer info`() {
        val document = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat("org.iso.18013.5.1.mDL")
            every { issuerMetadata } returns issuerMetadata()
        }
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { getDocumentById("doc-1") } returns document
            every { deleteDocumentById("doc-1") } returns Outcome.success<ByteArray?>(null)
        }
        val recorder = RecordingLogManager()

        val outcome = CredentialDeletionLogger(delegate, recorder).deleteDocumentById("doc-1")

        assertTrue(outcome.isSuccess)
        assertEquals(1, recorder.entries.size)
        val entry = assertIs<TransactionEntry.CredentialDeletion>(recorder.entries.first())
        assertEquals("org.iso.18013.5.1.mDL", entry.credentialIdentifier)
        assertEquals("Test Issuer", entry.credentialIssuerName?.content)
        // Legal-entity identifier isn't available without TS02, so it stays null.
        assertNull(entry.credentialIssuerIdentifier)
        assertEquals(TransactionResult.Completed, entry.transactionResult)
    }

    @Test
    fun `sd-jwt vc deletion uses the vct as credential identifier`() {
        val document = mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat("eu.europa.ec.eudi.pid.1")
            every { issuerMetadata } returns issuerMetadata()
        }
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { getDocumentById("doc-2") } returns document
            every { deleteDocumentById("doc-2") } returns Outcome.success<ByteArray?>(null)
        }
        val recorder = RecordingLogManager()

        CredentialDeletionLogger(delegate, recorder).deleteDocumentById("doc-2")

        val entry = assertIs<TransactionEntry.CredentialDeletion>(recorder.entries.single())
        assertEquals("eu.europa.ec.eudi.pid.1", entry.credentialIdentifier)
    }

    @Test
    fun `failed deletion logs a NotCompleted CredentialDeletion with the error reason`() {
        val document = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat("org.iso.18013.5.1.mDL")
            every { issuerMetadata } returns issuerMetadata()
        }
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { getDocumentById("doc-1") } returns document
            every { deleteDocumentById("doc-1") } returns
                    Outcome.failure<ByteArray?>(IllegalStateException("boom"))
        }
        val recorder = RecordingLogManager()

        val outcome = CredentialDeletionLogger(delegate, recorder).deleteDocumentById("doc-1")

        assertTrue(outcome.isFailure)
        val entry = assertIs<TransactionEntry.CredentialDeletion>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("boom", result.reason)
    }

    @Test
    fun `failed deletion with a null message records the exception type as the reason`() {
        val document = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat("org.iso.18013.5.1.mDL")
            every { issuerMetadata } returns issuerMetadata()
        }
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { getDocumentById("doc-1") } returns document
            every { deleteDocumentById("doc-1") } returns
                    Outcome.failure(IllegalStateException()) // null message
        }
        val recorder = RecordingLogManager()

        CredentialDeletionLogger(delegate, recorder).deleteDocumentById("doc-1")

        val entry = assertIs<TransactionEntry.CredentialDeletion>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("IllegalStateException", result.reason)
    }

    @Test
    fun `deleting a missing document does not log anything`() {
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { getDocumentById("missing") } returns null
            every { deleteDocumentById("missing") } returns
                    Outcome.failure<ByteArray?>(IllegalArgumentException("not found"))
        }
        val recorder = RecordingLogManager()

        val outcome = CredentialDeletionLogger(delegate, recorder).deleteDocumentById("missing")

        assertTrue(outcome.isFailure)
        assertTrue(recorder.entries.isEmpty())
    }

    @Test
    fun `deleting a non-issued document does not log a CredentialDeletion`() {
        // e.g. cleanup of a failed (re)issuance, which deletes the in-progress document.
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { getDocumentById("unsigned-1") } returns mockk<UnsignedDocument>(relaxed = true)
            every { deleteDocumentById("unsigned-1") } returns Outcome.success<ByteArray?>(null)
        }
        val recorder = RecordingLogManager()

        val outcome = CredentialDeletionLogger(delegate, recorder).deleteDocumentById("unsigned-1")

        assertTrue(outcome.isSuccess)
        assertTrue(recorder.entries.isEmpty())
    }
}
