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
package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.DeferredCredentialQueryOutcome
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceFormatMismatchException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A deferred re-issuance started before reissueDocument checked the format of the document is
 * checked when its credential is issued.
 */
@RunWith(RobolectricTestRunner::class)
class ProcessDeferredOutcomeReissueTest {

    private val documentManager = mockk<DocumentManager>(relaxed = true)
    private val deferredDocument = mockk<DeferredDocument> {
        every { id } returns "deferred-doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("urn:eudi:pid:1")
    }
    private val issuedDocument = mockk<IssuedDocument> {
        every { id } returns "new-doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("urn:eudi:pid:1")
    }
    private val reissuance = mockk<DeferredContext>(relaxed = true) {
        every { replacesDocumentId } returns "old-doc-id"
    }
    private val issued = mockk<DeferredCredentialQueryOutcome.Issued> {
        every { credentials } returns listOf(IssuedCredential.string("credential"))
    }

    private fun oldDocumentWithFormat(vct: String) {
        every { documentManager.getDocumentById("old-doc-id") } returns mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat(vct)
        }
    }

    private suspend fun process(): DeferredIssueResult? {
        var result: DeferredIssueResult? = null
        ProcessDeferredOutcome(
            documentManager = documentManager,
            callback = { result = it },
            deferredContext = reissuance,
        ).process(deferredDocument, listOf("key-alias"), issued)
        return result
    }

    @Test
    fun `a deferred credential of another type fails and keeps the document`() = runTest {
        oldDocumentWithFormat("urn:eu.europa.ec.eudi:pid:1")

        val result = process()

        assertTrue("Expected DocumentFailed but got $result", result is DeferredIssueResult.DocumentFailed)
        val cause = (result as DeferredIssueResult.DocumentFailed).cause
        assertTrue("Expected ReissuanceFormatMismatchException but got $cause",
            cause is ReissuanceFormatMismatchException)
        assertEquals(SdJwtVcFormat("urn:eu.europa.ec.eudi:pid:1"),
            (cause as ReissuanceFormatMismatchException).documentFormat)
        assertEquals(SdJwtVcFormat("urn:eudi:pid:1"), cause.configurationFormat)
        verify(exactly = 0) { documentManager.storeIssuedDocument(any(), any()) }
        verify(exactly = 0) { documentManager.deleteDocumentById("old-doc-id") }
    }

    @Test
    fun `a deferred credential of the same type replaces the document`() = runTest {
        oldDocumentWithFormat("urn:eudi:pid:1")
        every {
            documentManager.storeIssuedDocument(any(), any())
        } returns Outcome.success(issuedDocument)

        val result = process()

        assertTrue("Expected DocumentIssued but got $result", result is DeferredIssueResult.DocumentIssued)
        verify { documentManager.deleteDocumentById("old-doc-id") }
    }
}
