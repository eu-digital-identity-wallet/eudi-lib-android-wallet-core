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
import eu.europa.ec.eudi.openid4vci.TransactionId
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class ProcessDeferredOutcomePendingTest {

    private val documentManager = mockk<DocumentManager>(relaxed = true)
    private val deferredDocument = mockk<DeferredDocument> {
        every { id } returns "deferred-doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("test.vct")
    }
    private val storedDeferredDocument = mockk<DeferredDocument> {
        every { id } returns "stored-deferred-doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("test.vct")
    }
    private val pending = DeferredCredentialQueryOutcome.IssuancePending(
        transactionId = TransactionId("transaction-1"),
        interval = 48.seconds,
    )

    @After
    fun tearDown() {
        unmockkStatic(DeferredContext::toBytes)
    }

    private fun createProcessDeferredOutcome(
        callback: OpenId4VciManager.OnResult<DeferredIssueResult>,
        deferredContext: DeferredContext?,
    ) = ProcessDeferredOutcome(
        documentManager = documentManager,
        callback = callback,
        deferredContext = deferredContext,
    )

    @Test
    fun pendingIssuanceWithoutContextCarriesTheIssuersInterval() = runTest {
        var capturedResult: DeferredIssueResult? = null
        val callback = OpenId4VciManager.OnResult<DeferredIssueResult> { capturedResult = it }

        createProcessDeferredOutcome(callback, deferredContext = null)
            .process(deferredDocument, emptyList(), pending)

        assertTrue("Expected DocumentNotReady but got $capturedResult",
            capturedResult is DeferredIssueResult.DocumentNotReady)
        val notReady = capturedResult as DeferredIssueResult.DocumentNotReady
        assertEquals(deferredDocument, notReady.document)
        assertEquals(48.seconds, notReady.interval)
    }

    @Test
    fun pendingIssuanceWithStoredContextCarriesTheIssuersInterval() = runTest {
        val deferredContext = mockk<DeferredContext>()
        mockkStatic(DeferredContext::toBytes)
        every { deferredContext.toBytes() } returns byteArrayOf(1)
        every {
            documentManager.storeDeferredDocument(any(), any())
        } returns Outcome.success(storedDeferredDocument)

        var capturedResult: DeferredIssueResult? = null
        val callback = OpenId4VciManager.OnResult<DeferredIssueResult> { capturedResult = it }

        createProcessDeferredOutcome(callback, deferredContext)
            .process(deferredDocument, emptyList(), pending)

        assertTrue("Expected DocumentNotReady but got $capturedResult",
            capturedResult is DeferredIssueResult.DocumentNotReady)
        val notReady = capturedResult as DeferredIssueResult.DocumentNotReady
        assertEquals(storedDeferredDocument, notReady.document)
        assertEquals(48.seconds, notReady.interval)
    }
}
