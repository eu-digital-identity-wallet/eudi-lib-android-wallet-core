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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that a proximity response is recorded only for the request it was generated for.
 */
class PresentationLogListenerResponseTest {

    private class RecordingLogManager : TransactionLogManager {
        val entries = mutableListOf<TransactionEntry>()
        override fun log(entry: TransactionEntry) {
            entries.add(entry)
        }
    }

    private val sessionTranscript = byteArrayOf(1, 2, 3)

    /** A listener whose current request is a proximity request of [sessionTranscript]. */
    private fun listenerWithRequest(recorder: TransactionLogManager): PresentationLogListener {
        val listener = PresentationLogListener(recorder)
        listener.logBuilder = spyk(PresentationLogBuilder()) {
            every { withPresentedClaims(any(), any()) } answers { firstArg() }
        }
        val success = mockk<RequestProcessor.ProcessedRequest.Success>(relaxed = true) {
            every { trustMetadata } returns null
        }
        val processedRequest = mockk<RequestProcessor.ProcessedRequest> {
            every { getOrNull() } returns success
        }
        listener.onTransferEvent(
            TransferEvent.RequestReceived(
                processedRequest,
                DeviceRequest(byteArrayOf(0), sessionTranscript),
            )
        )
        return listener
    }

    @Test
    fun `a proximity response generated for another session is not recorded`() {
        val recorder = RecordingLogManager()
        val listener = listenerWithRequest(recorder)
        val entries = recorder.entries.size

        listener.recordResponse(DeviceResponse(byteArrayOf(0), byteArrayOf(9, 9, 9), emptyList()))

        assertEquals(entries, recorder.entries.size)
    }

    @Test
    fun `a proximity response of an earlier session is not recorded after a new connection`() {
        val recorder = RecordingLogManager()
        val listener = listenerWithRequest(recorder)
        listener.onTransferEvent(TransferEvent.Connected)
        val entries = recorder.entries.size

        listener.recordResponse(DeviceResponse(byteArrayOf(0), sessionTranscript.copyOf(), emptyList()))

        assertEquals(entries, recorder.entries.size)
    }

    @Test
    fun `a proximity response generated for the current request is recorded`() {
        val recorder = RecordingLogManager()
        val listener = listenerWithRequest(recorder)
        val entries = recorder.entries.size

        listener.recordResponse(DeviceResponse(byteArrayOf(0), sessionTranscript.copyOf(), emptyList()))

        assertEquals(entries + 1, recorder.entries.size)
    }
}
