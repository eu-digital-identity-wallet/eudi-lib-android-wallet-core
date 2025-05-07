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

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionsDecoratorTest {

    // Mocks
    private lateinit var presentationManager: PresentationManager
    private lateinit var documentManager: DocumentManager
    private lateinit var transactionLogger: TransactionLogger
    private lateinit var logger: Logger

    // System under test
    private lateinit var transactionsDecorator: TransactionsDecorator

    // Helper slots to capture arguments
    private val listenerSlot = slot<TransferEvent.Listener>()
    private val transactionLogSlot = slot<TransactionLog>()
    private val loggerRecordSlot = slot<Logger.Record>()

    @Before
    fun setup() {
        // Mock PresentationManager
        presentationManager = mockk {
            every { addTransferEventListener(capture(listenerSlot)) } returns this@mockk
            every { removeTransferEventListener(any()) } returns this@mockk
            every { removeAllTransferEventListeners() } returns this@mockk
            every { sendResponse(any()) } just Runs
        }

        // Mock DocumentManager
        documentManager = mockk {
            every { getDocumentById(any()) } returns createMockDocument()
        }

        // Mock TransactionLogger
        transactionLogger = mockk {
            every { log(capture(transactionLogSlot)) } just Runs
        }

        // Mock Logger
        logger = mockk {
            every { log(capture(loggerRecordSlot)) } just Runs
        }

        // Create system under test
        transactionsDecorator = TransactionsDecorator(
            presentationManager,
            documentManager,
            transactionLogger,
            logger
        )
    }

    @Test
    fun `constructor adds transaction listener to delegate`() {
        verify(exactly = 1) { presentationManager.addTransferEventListener(any()) }
        assertTrue(listenerSlot.isCaptured)
        assertEquals(transactionsDecorator.transactionListener, listenerSlot.captured)
    }

    @Test
    fun `sendResponse delegates to presentation manager and logs response`() {
        // Arrange
        val response = createMockResponse()

        // Act
        transactionsDecorator.sendResponse(response)

        // Assert
        verify(exactly = 1) { presentationManager.sendResponse(response) }
        verify(exactly = 1) { transactionLogger.log(any()) }

        // Verify the transaction log was updated with the response
        assertTrue(transactionLogSlot.isCaptured)
        val loggedTransaction = transactionLogSlot.captured
        assertNotNull(loggedTransaction.rawResponse)
    }

    @Test
    fun `sendResponse logs error when delegate throws exception`() {
        // Arrange
        val response = createMockResponse()
        val exception = RuntimeException("Test exception")

        every { presentationManager.sendResponse(any()) } throws exception

        // Act & Assert
        var caughtException: Throwable? = null
        try {
            transactionsDecorator.sendResponse(response)
        } catch (e: Throwable) {
            caughtException = e
        }

        // Verify exception was rethrown
        assertNotNull(caughtException)
        assertEquals(exception, caughtException)

        // Verify logging occurred
        verify(exactly = 1) { transactionLogger.log(any()) }

        // Verify the transaction log was updated with error status
        assertTrue(transactionLogSlot.isCaptured)
        val loggedTransaction = transactionLogSlot.captured
        assertEquals(TransactionLog.Status.Error, loggedTransaction.status)
    }

    @Test
    fun `removeAllTransferEventListeners clears listeners and re-adds transaction listener`() {
        // Act
        transactionsDecorator.removeAllTransferEventListeners()

        // Assert
        verifyOrder {
            presentationManager.removeAllTransferEventListeners()
            presentationManager.addTransferEventListener(transactionsDecorator.transactionListener)
        }
    }

    @Test
    fun `delegation methods forward calls to delegate`() {
        // Test other delegated methods to ensure they're properly forwarded

        val mockListener = mockk<TransferEvent.Listener>()

        // Act
        transactionsDecorator.removeTransferEventListener(mockListener)

        // Assert
        verify(exactly = 1) { presentationManager.removeTransferEventListener(mockListener) }
    }

    // Helper methods
    private fun createMockDocument(): IssuedDocument {
        val metadata = mockk<DocumentMetaData> {
            every { toJson() } returns """{"docType": "TestDocument", "name": "Test Doc"}"""
        }

        return mockk {
            every { this@mockk.id } returns "test-document-id"
            every { this@mockk.metadata } returns metadata
        }
    }

    private fun createMockResponse(): Response {
        return DeviceResponse(
            deviceResponseBytes = ByteArray(0),
            sessionTranscriptBytes = ByteArray(0),
            documentIds = listOf("test-document-id")
        )
    }
}
