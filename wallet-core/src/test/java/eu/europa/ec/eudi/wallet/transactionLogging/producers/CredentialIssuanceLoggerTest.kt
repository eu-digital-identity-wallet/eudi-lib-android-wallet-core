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

import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialIssuanceLoggerTest {

    private class RecordingLogManager : TransactionLogManager {
        /** Every log() call, in the order it happened. */
        val emissions = mutableListOf<TransactionEntry>()

        /** Stored state: a new entry replaces any earlier one with the same id, like the real storage. */
        private val store = linkedMapOf<String, TransactionEntry>()
        val entries: List<TransactionEntry> get() = store.values.toList()

        override fun log(entry: TransactionEntry) {
            emissions.add(entry)
            store[entry.transactionIdentifier] = entry
        }
    }

    private val issuerMetadata = IssuerMetadata(
        documentConfigurationIdentifier = "cfg",
        display = emptyList(),
        claims = null,
        credentialIssuerIdentifier = "https://issuer.example",
        issuerDisplay = listOf(IssuerMetadata.IssuerDisplay(name = "Test Issuer")),
    )

    private fun issuedDocument(id: String, docType: String) = mockk<IssuedDocument> {
        every { this@mockk.id } returns id
        every { name } returns docType
        every { format } returns MsoMdocFormat(docType)
        every { issuerMetadata } returns this@CredentialIssuanceLoggerTest.issuerMetadata
    }

    private fun deferredDocument(id: String, docType: String) = mockk<DeferredDocument> {
        every { this@mockk.id } returns id
        every { name } returns docType
        every { format } returns MsoMdocFormat(docType)
        every { issuerMetadata } returns this@CredentialIssuanceLoggerTest.issuerMetadata
    }

    @Test
    fun `batch issuance aggregates events into one completed CredentialIssuance`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA", "cfgB"), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 2))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.DocumentIssued(issuedDocument("d2", "eu.europa.ec.eudi.pid.1")))
            this(IssueEvent.Finished(listOf("d1", "d2")))
        }

        val entry = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        assertEquals(2, entry.details.credentialNumberRequested)
        assertEquals(2, entry.details.credentialNumberIssued)
        assertEquals(
            listOf("org.iso.18013.5.1.mDL", "eu.europa.ec.eudi.pid.1"),
            entry.details.credentialIdentifier
        )
        assertEquals("Test Issuer", entry.details.interactingPartyName?.content)
        // Legal-entity identifier isn't available without TS02, so it stays null.
        assertNull(entry.details.interactingPartyIdentifier)
        assertEquals(TransactionResult.Completed, entry.transactionResult)
        // User picked the credential, so this is user-triggered (TS10 §3.5).
        assertEquals(true, entry.details.isUserTriggered)
    }

    @Test
    fun `issuance by offer is issuer-initiated (not user-triggered)`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByOffer(any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByOffer(mockk(relaxed = true), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.Finished(listOf("d1")))
        }

        // A credential offer comes from the issuer, so it's issuer-initiated, not user-triggered
        // (TS10 §3.5), even though the user scans/accepts it.
        val entry = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        assertEquals(false, entry.details.isUserTriggered)
    }

    @Test
    fun `reissuance produces a CredentialReissuance entry`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.reissueDocument(any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.reissueDocument("d1", true, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.Finished(listOf("d1")))
        }

        val entry = assertIs<TransactionEntry.CredentialReissuance>(recorder.entries.single())
        assertEquals(1, entry.details.credentialNumberIssued)
        // allowAuthorizationFallback = true is the user-initiated re-issue path, so user-triggered.
        assertEquals(true, entry.details.isUserTriggered)
    }

    @Test
    fun `background re-issuance is not user-triggered`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.reissueDocument(any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        // allowAuthorizationFallback = false is the background ReIssuanceWorkManager path (no user).
        logger.reissueDocument("d1", false, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.Finished(listOf("d1")))
        }

        // Wallet-automatic, not user-initiated, so false (TS10 §3.5 / ARF DASH_05(f)).
        val entry = assertIs<TransactionEntry.CredentialReissuance>(recorder.entries.single())
        assertEquals(false, entry.details.isUserTriggered)
    }

    @Test
    fun `failed re-issuance carries the issuer name resolved from the existing document`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.reissueDocument(any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(
            delegate = delegate,
            transactionLogManager = recorder,
            // Looks up the existing document being re-issued; it carries the issuer metadata.
            documentResolver = { id ->
                if (id == "d1") issuedDocument("d1", "org.iso.18013.5.1.mDL") else null
            },
        )

        logger.reissueDocument("d1", true, null) {}

        // A re-issuance that starts then fails issues no document, so the issuer name can only
        // come from the resolved existing document; the failure event has none.
        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.Failure(IllegalStateException("CredentialIssuanceError")))
        }

        val entry = assertIs<TransactionEntry.CredentialReissuance>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("CredentialIssuanceError", result.reason)
        assertEquals(0, entry.details.credentialNumberIssued)
        // interactingPartyName is required (TS10 §3.5), so it's set even on failure via the resolver.
        assertEquals("Test Issuer", entry.details.interactingPartyName?.content)
    }

    @Test
    fun `overall failure produces a NotCompleted issuance with zero issued`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA"), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.Failure(IllegalStateException("authorization failed")))
        }

        val entry = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        assertEquals(0, entry.details.credentialNumberIssued)
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("authorization failed", result.reason)
    }

    @Test
    fun `an overall failure with a null message records the exception type as the reason`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA"), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.Failure(IllegalStateException())) // null message
        }

        val entry = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("IllegalStateException", result.reason)
    }

    @Test
    fun `an all-deferred issuance is NotCompleted with the deferred reason, not a fake completed`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA"), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.DocumentDeferred(deferredDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.Finished(listOf("d1")))
        }

        // Delivery was deferred and nothing was issued yet: not a failure, but not "Completed"
        // either. The credential gets its own entry when issueDeferredDocument() later resolves.
        val entry = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        assertEquals(0, entry.details.credentialNumberIssued)
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("Credential issuance deferred — awaiting the credential", result.reason)
        // The issuer name still comes through (from DocumentDeferred), so the row isn't nameless.
        assertEquals("Test Issuer", entry.details.interactingPartyName?.content)
    }

    @Test
    fun `a mixed batch yields a completed batch counting only issued, plus an awaiting per-deferred row`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA", "cfgB"), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 2))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.DocumentDeferred(deferredDocument("d2", "eu.europa.ec.eudi.pid.1")))
            this(IssueEvent.Finished(listOf("d1", "d2")))
        }

        // The batch row counts only the credential issued now ("1 of 1 Completed", not "1 of 2");
        // the deferred one gets its own awaiting row, keyed by its id.
        val entries = recorder.entries
        assertEquals(2, entries.size)

        val batch = assertIs<TransactionEntry.CredentialIssuance>(
            entries.first { it.transactionIdentifier != "deferred:d2" }
        )
        assertEquals(1, batch.details.credentialNumberRequested)
        assertEquals(1, batch.details.credentialNumberIssued)
        assertEquals(TransactionResult.Completed, batch.transactionResult)

        val deferred = assertIs<TransactionEntry.CredentialIssuance>(
            entries.first { it.transactionIdentifier == "deferred:d2" }
        )
        assertEquals(1, deferred.details.credentialNumberRequested)
        assertEquals(0, deferred.details.credentialNumberIssued)
        val pending = assertIs<TransactionResult.NotCompleted>(deferred.transactionResult)
        assertEquals("Credential issuance deferred — awaiting the credential", pending.reason)
        // The trigger is known here, still inside the original wallet-initiated flow.
        assertEquals(true, deferred.details.isUserTriggered)
    }

    @Test
    fun `a deferred credential from a mixed batch updates its awaiting row on resolution`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val issueCallback = slot<OpenId4VciManager.OnIssueEvent>()
        val deferredCallback = slot<OpenId4VciManager.OnDeferredIssueResult>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(issueCallback))
        } just Runs
        every {
            delegate.issueDeferredDocument(any(), any(), capture(deferredCallback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA", "cfgB"), null, null) {}
        issueCallback.captured.apply {
            this(IssueEvent.Started(total = 2))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.DocumentDeferred(deferredDocument("d2", "eu.europa.ec.eudi.pid.1")))
            this(IssueEvent.Finished(listOf("d1", "d2")))
        }
        // Completed batch row plus the awaiting "deferred:d2" row.
        assertEquals(2, recorder.entries.size)

        // The deferred credential later resolves via issueDeferredDocument().
        logger.issueDeferredDocument(mockk(relaxed = true), null) {}
        deferredCallback.captured(
            DeferredIssueResult.DocumentIssued(issuedDocument("d2", "eu.europa.ec.eudi.pid.1"))
        )

        // No new row: the awaiting row was updated in place (keyed by its DocumentId).
        assertEquals(2, recorder.entries.size)
        val resolved = assertIs<TransactionEntry.CredentialIssuance>(
            recorder.entries.first { it.transactionIdentifier == "deferred:d2" }
        )
        assertEquals(1, resolved.details.credentialNumberIssued)
        assertEquals(listOf("eu.europa.ec.eudi.pid.1"), resolved.details.credentialIdentifier)
        assertEquals(TransactionResult.Completed, resolved.transactionResult)
    }

    @Test
    fun `deferred completion is logged as a single CredentialIssuance`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnDeferredIssueResult>()
        every {
            delegate.issueDeferredDocument(any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDeferredDocument(mockk(relaxed = true), null) {}

        callback.captured(
            DeferredIssueResult.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL"))
        )

        val entry = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        assertEquals(1, entry.details.credentialNumberRequested)
        assertEquals(1, entry.details.credentialNumberIssued)
        assertEquals(listOf("org.iso.18013.5.1.mDL"), entry.details.credentialIdentifier)
        assertEquals(TransactionResult.Completed, entry.transactionResult)
        // The original trigger is unknown at deferred-resolution time, so null (TS10 §3.5).
        assertNull(entry.details.isUserTriggered)
    }

    @Test
    fun `host callback still receives every event`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        val received = mutableListOf<IssueEvent>()
        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA"), null, null) {
            received.add(it)
        }

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.Finished(listOf("d1")))
        }

        assertTrue(received.any { it is IssueEvent.Started })
        assertTrue(received.any { it is IssueEvent.DocumentIssued })
        assertTrue(received.any { it is IssueEvent.Finished })
    }

    @Test
    fun `an issuance abandoned before Started logs nothing`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), any())
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        // The user started issuance, went to the authorization browser, then backed out, so no
        // IssueEvent fires. The entry is written at IssueEvent.Started (after authorization), not at
        // initiation, so backing out before authorization logs nothing.
        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA", "cfgB"), null, null) {}

        assertTrue(recorder.emissions.isEmpty())
        assertTrue(recorder.entries.isEmpty())
    }

    @Test
    fun `Started supersedes the seeded requested count`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        // One configuration id (seed count = 1) but Started reports a batch of 3.
        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA"), null, null) {}
        callback.captured(IssueEvent.Started(total = 3))

        val pending = assertIs<TransactionEntry.CredentialIssuance>(recorder.entries.single())
        assertIs<TransactionResult.NotCompleted>(pending.transactionResult)
        assertEquals(3, pending.details.credentialNumberRequested)
    }

    @Test
    fun `the early entry and the final entry share the identifier and collapse to one row`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.issueDocumentByConfigurationIdentifiers(any(), any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.issueDocumentByConfigurationIdentifiers("https://issuer.example", listOf("cfgA"), null, null) {}

        callback.captured.apply {
            this(IssueEvent.Started(total = 1))
            this(IssueEvent.DocumentIssued(issuedDocument("d1", "org.iso.18013.5.1.mDL")))
            this(IssueEvent.Finished(listOf("d1")))
        }

        // Several log calls (early entry at Started, then the terminal one): not-completed until the
        // final Completed, all sharing one identifier ...
        assertTrue(recorder.emissions.size >= 2)
        assertIs<TransactionResult.NotCompleted>(recorder.emissions.first().transactionResult)
        assertEquals(TransactionResult.Completed, recorder.emissions.last().transactionResult)
        assertEquals(1, recorder.emissions.map { it.transactionIdentifier }.distinct().size)
        // ... so storage collapses them to one completed row.
        assertEquals(TransactionResult.Completed, recorder.entries.single().transactionResult)
    }

    @Test
    fun `failure before issuance starts logs nothing (background re-issuance noise suppressed)`() {
        val delegate = mockk<OpenId4VciManager>(relaxed = true)
        val callback = slot<OpenId4VciManager.OnIssueEvent>()
        every {
            delegate.reissueDocument(any(), any(), any(), capture(callback))
        } just Runs
        val recorder = RecordingLogManager()
        val logger = CredentialIssuanceLogger(delegate, recorder)

        logger.reissueDocument("d1", false, null) {}

        // A background re-issuance with an expired refresh token fails before IssueEvent.Started.
        callback.captured(IssueEvent.Failure(IllegalStateException("refresh token expired")))

        assertTrue(recorder.emissions.isEmpty())
        assertTrue(recorder.entries.isEmpty())
    }
}
