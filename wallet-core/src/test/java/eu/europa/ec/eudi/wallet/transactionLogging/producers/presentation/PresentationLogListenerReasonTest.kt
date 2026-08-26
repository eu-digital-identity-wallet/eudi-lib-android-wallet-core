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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import io.mockk.every
import io.mockk.mockk
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests how the presentation producer sets `reasonOfNoncompletion` (ARF Topic 19, DASH_02).
 */
class PresentationLogListenerReasonTest {

    private class RecordingLogManager : TransactionLogManager {
        val entries = mutableListOf<TransactionEntry>()
        override fun log(entry: TransactionEntry) {
            entries.add(entry)
        }
    }

    private fun listener(recorder: TransactionLogManager) =
        PresentationLogListener(recorder)

    @Test
    fun `stopping after a processable request logs NotCompleted with the stopped reason`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply { hasProcessableRequest = true }

        listener.logStopped()

        val entry = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals(PresentationLogListener.REASON_STOPPED, result.reason)
    }

    @Test
    fun `stopping with no processable request logs nothing`() {
        val recorder = RecordingLogManager()

        // No request was ever received (e.g. a wrong-type scan), so stopping must not log anything.
        listener(recorder).logStopped()

        assertTrue(recorder.entries.isEmpty())
    }

    @Test
    fun `a transfer error after a processable request logs NotCompleted with the error message`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply { hasProcessableRequest = true }

        listener.onTransferEvent(TransferEvent.Error(IllegalStateException("transport down")))

        val entry = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("transport down", result.reason)
    }

    @Test
    fun `a transfer error with a null message records the exception type as the reason`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply { hasProcessableRequest = true }

        // IllegalStateException() has a null message — the reason must still be non-blank.
        listener.onTransferEvent(TransferEvent.Error(IllegalStateException()))

        val entry = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        val result = assertIs<TransactionResult.NotCompleted>(entry.transactionResult)
        assertEquals("IllegalStateException", result.reason)
    }

    @Test
    fun `a transfer error with no processable request logs nothing`() {
        val recorder = RecordingLogManager()

        listener(recorder).onTransferEvent(TransferEvent.Error(IllegalStateException("boom")))

        assertTrue(recorder.entries.isEmpty())
    }

    @Test
    fun `ResponseSent after a processable request marks the transaction completed`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply { hasProcessableRequest = true }

        listener.onTransferEvent(TransferEvent.ResponseSent)

        val entry = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        assertEquals(TransactionResult.Completed, entry.transactionResult)
        assertTrue(listener.finalized)
    }

    @Test
    fun `a success redirect marks the transaction completed`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply { hasProcessableRequest = true }

        listener.onTransferEvent(TransferEvent.Redirect(URI.create("https://verifier.example/cb")))

        val entry = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        assertEquals(TransactionResult.Completed, entry.transactionResult)
    }

    @Test
    fun `IntentToSend (DCAPI hand-off) marks the transaction completed`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply { hasProcessableRequest = true }

        listener.onTransferEvent(TransferEvent.IntentToSend(mockk(relaxed = true)))

        val entry = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        assertEquals(TransactionResult.Completed, entry.transactionResult)
    }

    @Test
    fun `ResponseSent with no processable request logs nothing`() {
        val recorder = RecordingLogManager()

        listener(recorder).onTransferEvent(TransferEvent.ResponseSent)

        assertTrue(recorder.entries.isEmpty())
    }

    @Test
    fun `an already finalized transaction is not logged again when stopped`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder).apply {
            finalized = true
            hasProcessableRequest = true
        }

        listener.logStopped()

        assertTrue(recorder.entries.isEmpty())
    }

    @Test
    fun `receiving a processable request logs an early NotCompleted entry that is not finalized`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder)

        listener.onTransferEvent(TransferEvent.RequestReceived(processableRequest(), mockk<Request>()))

        // The request is logged right away (DASH_02), so an abandoned presentation is still recorded.
        // It stays not-finalized so a later terminal event can promote it.
        val pending = assertIs<TransactionEntry.Presentation>(recorder.entries.single())
        assertIs<TransactionResult.NotCompleted>(pending.transactionResult)
        assertFalse(listener.finalized)
        assertTrue(listener.hasProcessableRequest)
    }

    @Test
    fun `receiving an unprocessable request logs nothing (wrong-type or garbage scan)`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder)

        // A request the wallet can't process (e.g. a credential offer scanned during presentation).
        // Neither the receipt nor the later disconnect should log anything.
        listener.onTransferEvent(TransferEvent.RequestReceived(unprocessableRequest(), mockk<Request>()))
        listener.logStopped()

        assertTrue(recorder.entries.isEmpty())
        assertFalse(listener.hasProcessableRequest)
    }

    @Test
    fun `a terminal event reuses the same identifier so storage keeps one entry`() {
        val recorder = RecordingLogManager()
        val listener = listener(recorder)

        listener.onTransferEvent(TransferEvent.RequestReceived(processableRequest(), mockk<Request>()))
        listener.logStopped()

        // Two log calls (early + final) with the same identifier, so storage keeps one row.
        assertEquals(2, recorder.entries.size)
        assertEquals(
            recorder.entries[0].transactionIdentifier,
            recorder.entries[1].transactionIdentifier,
        )
    }

    /** A request the wallet could process (Success) — with no trust metadata for the relying party. */
    private fun processableRequest(): RequestProcessor.ProcessedRequest {
        val success = mockk<RequestProcessor.ProcessedRequest.Success>(relaxed = true) {
            every { trustMetadata } returns null
        }
        return mockk<RequestProcessor.ProcessedRequest> { every { getOrNull() } returns success }
    }

    /** A request the wallet could NOT process (no success payload) — a wrong-type/garbage scan. */
    private fun unprocessableRequest(): RequestProcessor.ProcessedRequest =
        mockk<RequestProcessor.ProcessedRequest> { every { getOrNull() } returns null }
}
