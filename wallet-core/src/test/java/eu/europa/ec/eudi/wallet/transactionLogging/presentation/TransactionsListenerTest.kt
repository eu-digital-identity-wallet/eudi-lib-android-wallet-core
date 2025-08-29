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
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class TransactionsListenerTest {

    // Mocks
    private lateinit var transactionLogger: TransactionLogger
    private lateinit var documentManager: DocumentManager
    private lateinit var logger: Logger

    // Test subject
    private lateinit var listener: TransactionsListener

    // Test data
    private lateinit var emptyLog: TransactionLog
    private lateinit var documentId: DocumentId
    private lateinit var document: Document
    private lateinit var mockMetadata: IssuerMetadata

    @Before
    fun setup() {
        // Initialize mocks
        transactionLogger = mockk(relaxed = true)
        documentManager = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        // Create test data
        emptyLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Presentation,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        documentId = "test-document-id"
        mockMetadata = mockk {
            every { toJson() } returns "{\"docType\":\"TestDoc\"}"
        }
        document = mockk {
            every { this@mockk.issuerMetadata } returns mockMetadata
        }

        // Setup document manager mock
        every { documentManager.getDocumentById(any()) } returns document

        // Create test subject
        listener = TransactionsListener(transactionLogger, documentManager, logger)
    }

//    @Test
//    fun `metadataResolver returns correct metadata for document IDs`() {
//        // Setup additional document
//        val documentId2 = "test-document-id-2"
//        val metadata2 = mockk<IssuerMetadata> {
//            every { toJson() } returns "{\"docType\":\"TestDoc2\"}"
//        }
//        val document2 = mockk<Document> {
//            every { issuerMetadata } returns metadata2
//        }
//
//        every { documentManager.getDocumentById(documentId) } returns document
//        every { documentManager.getDocumentById(documentId2) } returns document2
//
//        // Test
//        val respondedDocuments = listOf(
//            OpenId4VpResponse.RespondedDocument.IndexBased(
//                index = 0,
//                format = FORMAT_MSO_MDOC,
//                documentId = documentId
//            ),
//            OpenId4VpResponse.RespondedDocument.IndexBased(
//                index = 1,
//                format = FORMAT_MSO_MDOC,
//                documentId = documentId2
//            )
//        )
//        val result = listener.metadataResolver(respondedDocuments)
//
//        // Verify
//        assertEquals(2, result.size)
//        val expectedMetadata = listOf(
//            TransactionLog.Metadata.IndexBased(
//                index = 0,
//                format = FORMAT_MSO_MDOC,
//                issuerMetadata = "{\"docType\":\"TestDoc\"}",
//            ),
//            TransactionLog.Metadata.IndexBased(
//                index = 1,
//                format = FORMAT_MSO_MDOC,
//                issuerMetadata = "{\"docType\":\"TestDoc2\"}",
//            )
//        )
//        val metadata = result.map { TransactionLog.Metadata.fromJson(it) }
//        assertContentEquals(expectedMetadata, metadata)
//        assertEquals("{\"docType\":\"TestDoc\"}", metadata[0].issuerMetadata)
//        assertEquals("{\"docType\":\"TestDoc2\"}", metadata[1].issuerMetadata)
//    }
//
//    @Test
//    fun `metadataResolver handles null metadata`() {
//        // Setup document with null metadata
//        every { documentManager.getDocumentById(any()) } returns null
//
//        // Test
//        val respondedDocuments = listOf(
//            OpenId4VpResponse.RespondedDocument.IndexBased(
//                index = 0,
//                format = FORMAT_MSO_MDOC,
//                documentId = documentId
//            )
//        )
//        val result = listener.metadataResolver(respondedDocuments)
//
//        // Verify
//        assertEquals(1, result.size)
//        val transactionLogMetadata = TransactionLog.Metadata.fromJson(result[0])
//        assertIs<TransactionLog.Metadata.IndexBased>(transactionLogMetadata)
//        assertNull(transactionLogMetadata.issuerMetadata)
//    }

    @Test
    fun `onTransferEvent with Connected event resets log`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        every { mockedLogBuilder.createEmptyPresentationLog() } returns emptyLog

        // Set the mocked logBuilder
        listener.logBuilder = mockedLogBuilder

        // Test
        listener.onTransferEvent(TransferEvent.Connected)

        // Verify
        verify(exactly = 1) { mockedLogBuilder.createEmptyPresentationLog() }
        assertEquals(emptyLog, listener.log)
    }

    @Test
    fun `onTransferEvent with RequestReceived event updates log with request and relying party`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val request = mockk<DeviceRequest>()
        val processedRequest = mockk<RequestProcessor.ProcessedRequest>()

        val logWithRequest = mockk<TransactionLog>()
        val logWithRelyingParty = mockk<TransactionLog>()

        every { mockedLogBuilder.createEmptyPresentationLog() } returns emptyLog
        every { mockedLogBuilder.withRequest(any(), request) } returns logWithRequest
        every {
            mockedLogBuilder.withRelyingParty(
                logWithRequest,
                processedRequest
            )
        } returns logWithRelyingParty

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = emptyLog

        // Test
        listener.onTransferEvent(
            TransferEvent.RequestReceived(
                request = request,
                processedRequest = processedRequest
            )
        )

        // Verify
        verify(exactly = 1) { mockedLogBuilder.withRequest(emptyLog, request) }
        verify(exactly = 1) { mockedLogBuilder.withRelyingParty(logWithRequest, processedRequest) }
        assertEquals(logWithRelyingParty, listener.log)
    }

    @Test
    fun `onTransferEvent with RequestReceived handles exceptions`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val request = mockk<DeviceRequest>()
        val processedRequest = mockk<RequestProcessor.ProcessedRequest>()
        val errorLog = mockk<TransactionLog>()

        every {
            mockedLogBuilder.withRequest(
                any(),
                request
            )
        } throws RuntimeException("Test exception")
        every { mockedLogBuilder.withError(any()) } returns errorLog

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = emptyLog

        // Test
        listener.onTransferEvent(
            TransferEvent.RequestReceived(
                request = request,
                processedRequest = processedRequest
            )
        )

        // Verify logger called with error
        verify(exactly = 1) {
            logger.log(match {
                it.level == Logger.LEVEL_ERROR &&
                        it.message == "Failed to log transaction" &&
                        it.sourceClassName == TransactionsListener::class.java.name
            })
        }

        // Verify log marked with error
        verify(exactly = 1) { mockedLogBuilder.withError(emptyLog) }
        assertEquals(errorLog, listener.log)
    }

    @Test
    fun `onTransferEvent with Error event marks log with error and logs it`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val errorLog = mockk<TransactionLog>()

        every { mockedLogBuilder.withError(any()) } returns errorLog

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = emptyLog

        // Test
        listener.onTransferEvent(TransferEvent.Error(Exception("Test error")))

        // Verify log marked with error and logged
        verify(exactly = 1) { mockedLogBuilder.withError(emptyLog) }
        verify(exactly = 1) { transactionLogger.log(errorLog) }
        assertEquals(errorLog, listener.log)
    }

    @Test
    fun `onTransferEvent with Error handles exceptions during error handling`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()

        every { mockedLogBuilder.withError(any()) } throws RuntimeException("Secondary error")

        // Set the mocked logBuilder
        listener.logBuilder = mockedLogBuilder

        // Test
        listener.onTransferEvent(TransferEvent.Error(Exception("Primary error")))

        // Verify logger called with the secondary error
        verify(exactly = 1) {
            logger.log(match {
                it.level == Logger.LEVEL_ERROR &&
                        it.message == "Failed to log transaction" &&
                        it.thrown?.message == "Secondary error"
            })
        }
    }

    @Test
    fun `onTransferEvent with other event type does nothing`() {
        // Setup
        val otherEvent = TransferEvent.Connecting
        val initialLog = emptyLog

        // Set initial log
        listener.log = initialLog

        // Test
        listener.onTransferEvent(otherEvent)

        // Verify no interactions with logBuilder or transactionLogger
        verify(exactly = 0) { transactionLogger.log(any()) }
        assertEquals(initialLog, listener.log)
    }

    @Test
    fun `logResponse updates log with response and logs it`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val response = mockk<DeviceResponse>()
        val updatedLog = mockk<TransactionLog>()

        every { mockedLogBuilder.withResponse(any(), response, null) } returns updatedLog

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = emptyLog

        // Test
        listener.logResponse(response)

        // Verify
        verify(exactly = 1) { mockedLogBuilder.withResponse(emptyLog, response, null) }
        verify(exactly = 1) { transactionLogger.log(updatedLog) }
        assertEquals(updatedLog, listener.log)
    }

    @Test
    fun `logResponse with error updates log with response and error`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val response = mockk<DeviceResponse>()
        val error = RuntimeException("Test error")
        val updatedLog = mockk<TransactionLog>()

        every { mockedLogBuilder.withResponse(any(), response, error) } returns updatedLog

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = emptyLog

        // Test
        listener.logResponse(response, error)

        // Verify
        verify(exactly = 1) { mockedLogBuilder.withResponse(emptyLog, response, error) }
        verify(exactly = 1) { transactionLogger.log(updatedLog) }
        assertEquals(updatedLog, listener.log)
    }

    @Test
    fun `logResponse handles exceptions during response logging`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val response = mockk<Response>()
        val errorLog = mockk<TransactionLog>()

        every {
            mockedLogBuilder.withResponse(
                any(),
                response,
                null
            )
        } throws RuntimeException("Test exception")
        every { mockedLogBuilder.withError(any()) } returns errorLog

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = emptyLog

        // Test
        listener.logResponse(response)

        // Verify logger called with error
        verify(exactly = 1) {
            logger.log(match {
                it.level == Logger.LEVEL_ERROR &&
                        it.message == "Failed to log transaction"
            })
        }

        // Verify log marked with error and logged
        verify(exactly = 1) { mockedLogBuilder.withError(emptyLog) }
        verify(exactly = 1) { transactionLogger.log(errorLog) }
        assertEquals(errorLog, listener.log)
    }

    @Test
    fun `logStopped marks incomplete log with error and logs it`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()
        val errorLog = mockk<TransactionLog>()

        // Set status to Incomplete
        val incompleteLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Presentation,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        every { mockedLogBuilder.withError(any()) } returns errorLog

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = incompleteLog

        // Test
        listener.logStopped()

        // Verify log marked with error and logged
        verify(exactly = 1) { mockedLogBuilder.withError(incompleteLog) }
        verify(exactly = 1) { transactionLogger.log(errorLog) }
        assertEquals(errorLog, listener.log)
    }

    @Test
    fun `logStopped does not log completed transactions`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()

        // Set status to Completed
        val completedLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Completed,
            type = TransactionLog.Type.Presentation,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = completedLog

        // Test
        listener.logStopped()

        // Verify no interactions with logBuilder or transactionLogger
        verify(exactly = 0) { mockedLogBuilder.withError(any()) }
        verify(exactly = 0) { transactionLogger.log(any()) }
        assertEquals(completedLog, listener.log)
    }

    @Test
    fun `logStopped handles exceptions during error handling`() {
        // Setup
        val mockedLogBuilder = mockk<TransactionLogBuilder>()

        // Set status to Incomplete
        val incompleteLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Presentation,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        every { mockedLogBuilder.withError(any()) } throws RuntimeException("Error during stop")

        // Set the mocked logBuilder and initial log
        listener.logBuilder = mockedLogBuilder
        listener.log = incompleteLog

        // Test
        listener.logStopped()

        // Verify logger called with error
        verify(exactly = 1) {
            logger.log(match {
                it.level == Logger.LEVEL_ERROR &&
                        it.message == "Failed to log transaction" &&
                        it.sourceMethod == "onTransferEvent: Disconnected"
            })
        }
    }

    @Test
    fun `onTransferEvent with Disconnected event calls logStopped method`() {
        // Create a spy to verify the stop method is called
        val spyListener = spyk(listener)

        // Test
        spyListener.onTransferEvent(TransferEvent.Disconnected)

        // Verify stop is called
        verify(exactly = 1) { spyListener.logStopped() }
    }
}