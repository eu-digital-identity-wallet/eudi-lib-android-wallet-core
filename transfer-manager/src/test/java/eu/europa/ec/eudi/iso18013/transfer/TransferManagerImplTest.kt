/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer

import android.util.Log
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.transport.DataTransport
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.internal.QrEngagement
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.mockito.MockedStatic
import org.multipaz.util.Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransferManagerImplTest {

    lateinit var mockLog: MockedStatic<Log>

    @BeforeTest
    fun setUp() {
        mockLog = mockAndroidLog()
    }

    @AfterTest
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun `retrievalMethods should set the when passed as constructor argument`() {

        val retrievalMethods: List<DeviceRetrievalMethod> = listOf(
            mockk(), mockk()
        )

        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
            retrievalMethods = retrievalMethods
        )
        assertEquals(retrievalMethods, manager.retrievalMethods)
    }

    @Test
    fun `setRetrievalMethods should set the retrieval methods`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val retrievalMethods: List<DeviceRetrievalMethod> = listOf(
            mockk(), mockk()
        )

        assertTrue(manager.retrievalMethods.isEmpty())
        manager.setRetrievalMethods(retrievalMethods)

        assertEquals(retrievalMethods, manager.retrievalMethods)
    }

    @Test
    fun `addTransferEventListener should add the listener`() {
        // Given
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = TransferEvent.Listener { }

        // When
        manager.addTransferEventListener(listener)

        // Then
        assert(manager.transferEventListeners.contains(listener))
    }

    @Test
    fun `removeTransferEventListener should remove the listener`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = TransferEvent.Listener { }
        manager.addTransferEventListener(listener)
        assert(manager.transferEventListeners.contains(listener))

        manager.removeTransferEventListener(listener)
        assert(!manager.transferEventListeners.contains(listener))
    }

    @Test
    fun `removeAllTransferEventListeners should remove all listeners`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener1 = TransferEvent.Listener { }
        val listener2 = TransferEvent.Listener {}
        manager.addTransferEventListener(listener1)
        manager.addTransferEventListener(listener2)

        assertEquals(2, manager.transferEventListeners.size)
        assert(manager.transferEventListeners.contains(listener1))
        assert(manager.transferEventListeners.contains(listener2))

        manager.removeAllTransferEventListeners()
        assert(manager.transferEventListeners.isEmpty())
    }

    @Test
    fun `setupNfcEngagement should setup NfcEngagementService`() {
        val retrievalMethods: List<DeviceRetrievalMethod> = listOf(
            mockk(), mockk()
        )
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk() {
                coEvery { process(any<DeviceRequest>()) } returns mockk(relaxed = true)
            },
            retrievalMethods = retrievalMethods
        )

        class TestListener : TransferEvent.Listener {
            override fun onTransferEvent(event: TransferEvent) {
                println(event)
            }
        }

        val listener = spyk(TestListener())

        manager.addTransferEventListener(listener)

        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }

        manager.setupNfcEngagement(service)

        assertEquals(retrievalMethods, service.retrievalMethods)
        assertIs<() -> Unit>(service.onConnecting)
        assertIs<(DeviceRetrievalHelper) -> Unit>(service.onDeviceRetrievalHelperReady)
        assertIs<(DeviceRetrievalHelper, ByteArray) -> Unit>(service.onNewDeviceRequest)
        assertIs<(Throwable) -> Unit>(service.onCommunicationError)
        assertIs<(Boolean) -> Unit>(service.onDisconnected)

        service.onConnecting()
        verify { listener.onTransferEvent(TransferEvent.Connecting) }

        val deviceRetrievalHelper = mockk<DeviceRetrievalHelper> {
            every { sessionTranscript } returns ByteArray(0)
        }
        service.onDeviceRetrievalHelperReady(deviceRetrievalHelper)
        assertSame(deviceRetrievalHelper, manager.deviceRetrievalHelper)
        verify { listener.onTransferEvent(TransferEvent.Connected) }

        service.onNewDeviceRequest(deviceRetrievalHelper, ByteArray(0))
        verify {
            listener.onTransferEvent(any<TransferEvent.RequestReceived>())
        }

        val throwable = Throwable()
        service.onCommunicationError(throwable)
        verify { listener.onTransferEvent(TransferEvent.Error(throwable)) }

        service.onDisconnected(true)
        verify { listener.onTransferEvent(TransferEvent.Disconnected) }
    }

    @Test
    fun `stopPresentation should call DeviceRetrievalHelper sendDeviceResponse with SESSION_TERMINATION and disconnect`() {
        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true)
        val manager = spyk(
            TransferManagerImpl(
                context = Context,
                requestProcessor = mockk(),
            )
        )

        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.stopPresentation()

        verify(exactly = 1) {
            mockDeviceRetrievalHelper.sendDeviceResponse(
                null,
                status = SESSION_DATA_STATUS_SESSION_TERMINATION
            )
        }
        verify(exactly = 0) { mockDeviceRetrievalHelper.sendTransportSpecificTermination() }
        verify(exactly = 1) { mockDeviceRetrievalHelper.disconnect() }

    }

    @Test
    fun `stopPresentation should call DeviceRetrievalHelper sendTransportSpecificTermination and disconnect`() {
        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true)
        val manager = spyk(
            TransferManagerImpl(
                context = Context,
                requestProcessor = mockk(),
            )
        )

        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.stopPresentation(
            sendSessionTerminationMessage = true,
            useTransportSpecificSessionTermination = true
        )

        verify(exactly = 0) { mockDeviceRetrievalHelper.sendDeviceResponse(null, any()) }
        verify(exactly = 1) { mockDeviceRetrievalHelper.sendTransportSpecificTermination() }
        verify(exactly = 1) { mockDeviceRetrievalHelper.disconnect() }

    }

    @Test
    fun `stopPresentation disconnects even when the session termination message cannot be sent`() {
        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true)
        every {
            mockDeviceRetrievalHelper.sendDeviceResponse(any(), any())
        } throws IllegalStateException("send failed")
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.stopPresentation()

        verify(exactly = 1) { mockDeviceRetrievalHelper.disconnect() }
        assertNull(manager.deviceRetrievalHelper)
    }

    @Test
    fun `stopPresentation should call DeviceRetrievalHelper disconnect`() {
        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true)
        val manager = spyk(
            TransferManagerImpl(
                context = Context,
                requestProcessor = mockk(),
            )
        )

        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.stopPresentation(
            sendSessionTerminationMessage = false,
            useTransportSpecificSessionTermination = true
        )

        verify(exactly = 0) { mockDeviceRetrievalHelper.sendTransportSpecificTermination() }
        verify(exactly = 0) { mockDeviceRetrievalHelper.sendDeviceResponse(null, any()) }
        verify(exactly = 1) { mockDeviceRetrievalHelper.disconnect() }
    }

    @Test
    fun `startQrEngagement fails when already started`() {
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)
        manager.startQrEngagement()
        manager.startQrEngagement()

        verify(exactly = 1) { listener.onTransferEvent(any<TransferEvent.Error>()) }
    }

    @Test
    fun `startQrEngagement fails while a presentation session is active`() {
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)
        val nfcDeviceRetrievalHelper = mockk<DeviceRetrievalHelper>(relaxed = true)
        manager.deviceRetrievalHelper = nfcDeviceRetrievalHelper

        manager.startQrEngagement()

        verify(exactly = 0) { nfcDeviceRetrievalHelper.sendDeviceResponse(any(), any()) }
        verify(exactly = 0) { nfcDeviceRetrievalHelper.disconnect() }
        assertSame(nfcDeviceRetrievalHelper, manager.deviceRetrievalHelper)
        assertFalse(manager.hasStarted)
        assertNull(manager.qrEngagement)
        verify(exactly = 1) { listener.onTransferEvent(ofType<TransferEvent.Error>()) }
    }

    @Test
    fun `sendResponse triggers onResponseSent event and class deviceRetrievalHelper send with SessionTermination`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)

        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true) {
            every { sessionTranscript } returns byteArrayOf(1)
        }
        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.sendResponse(
            DeviceResponse(
                deviceResponseBytes = ByteArray(0),
                sessionTranscriptBytes = byteArrayOf(1),
                documentIds = listOf()
            )
        )

        verify(exactly = 1) { listener.onTransferEvent(any<TransferEvent.ResponseSent>()) }
        verify(exactly = 1) {
            mockDeviceRetrievalHelper.sendDeviceResponse(
                any(),
                SESSION_DATA_STATUS_SESSION_TERMINATION
            )
        }
    }

    @Test
    fun `sendResponse does not send a response generated for another session`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)

        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true) {
            every { sessionTranscript } returns byteArrayOf(1)
        }
        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.sendResponse(
            DeviceResponse(
                deviceResponseBytes = ByteArray(0),
                sessionTranscriptBytes = byteArrayOf(2),
                documentIds = listOf()
            )
        )

        verify(exactly = 0) { mockDeviceRetrievalHelper.sendDeviceResponse(any(), any()) }
        verify(exactly = 0) { listener.onTransferEvent(TransferEvent.ResponseSent) }
        verify(exactly = 1) {
            listener.onTransferEvent(
                match { it is TransferEvent.Error && it.error is ResponseNotSentException }
            )
        }
    }

    @Test
    fun `an NFC request is stamped with the session transcript of the connection that received it`() {
        val requestProcessor = mockk<RequestProcessor>()
        val request = slot<Request>()
        coEvery { requestProcessor.process(capture(request)) } returns mockk(relaxed = true)
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = requestProcessor,
        )
        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)
        manager.deviceRetrievalHelper = mockk<DeviceRetrievalHelper> {
            every { sessionTranscript } returns byteArrayOf(1)
        }
        val receivingDeviceRetrievalHelper = mockk<DeviceRetrievalHelper> {
            every { sessionTranscript } returns byteArrayOf(2)
        }

        service.onNewDeviceRequest(receivingDeviceRetrievalHelper, ByteArray(0))

        assertContentEquals(byteArrayOf(2), assertIs<DeviceRequest>(request.captured).sessionTranscriptBytes)
    }

    @Test
    fun `a QR request is stamped with the session transcript of the connection that received it`() {
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val requestProcessor = mockk<RequestProcessor>()
        val request = slot<Request>()
        coEvery { requestProcessor.process(capture(request)) } returns mockk(relaxed = true)
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = requestProcessor,
        )
        manager.startQrEngagement()
        manager.deviceRetrievalHelper = mockk<DeviceRetrievalHelper> {
            every { sessionTranscript } returns byteArrayOf(1)
        }
        val receivingDeviceRetrievalHelper = mockk<DeviceRetrievalHelper> {
            every { sessionTranscript } returns byteArrayOf(2)
        }

        manager.qrEngagement!!.onNewDeviceRequest(receivingDeviceRetrievalHelper, ByteArray(0))

        assertContentEquals(byteArrayOf(2), assertIs<DeviceRequest>(request.captured).sessionTranscriptBytes)
    }

    @Test
    fun `sendResponse before the first message of the session triggers an error`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)

        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true) {
            every { sessionTranscript } throws IllegalStateException("No message received from reader")
        }
        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.sendResponse(
            DeviceResponse(
                deviceResponseBytes = ByteArray(0),
                sessionTranscriptBytes = byteArrayOf(1),
                documentIds = listOf()
            )
        )

        verify(exactly = 0) { mockDeviceRetrievalHelper.sendDeviceResponse(any(), any()) }
        verify(exactly = 1) {
            listener.onTransferEvent(
                match { it is TransferEvent.Error && it.error is ResponseNotSentException }
            )
        }
    }

    @Test
    fun `sendResponse without an active session triggers an error`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)

        manager.sendResponse(
            DeviceResponse(
                deviceResponseBytes = ByteArray(0),
                sessionTranscriptBytes = byteArrayOf(1),
                documentIds = listOf()
            )
        )

        verify(exactly = 0) { listener.onTransferEvent(TransferEvent.ResponseSent) }
        verify(exactly = 1) {
            listener.onTransferEvent(
                match { it is TransferEvent.Error && it.error is ResponseNotSentException }
            )
        }
    }

    @Test
    fun `a presentation is active only while a session is active`() {
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)
        manager.startQrEngagement()
        val qrEngagement = manager.qrEngagement!!

        assertFalse(qrEngagement.isPresentationActive())
        assertFalse(service.isPresentationActive())

        val connectionListener = service.connectNfc(mockk<DeviceRetrievalHelper>())
        assertTrue(qrEngagement.isPresentationActive())
        assertTrue(service.isPresentationActive())

        connectionListener.onDeviceDisconnected(false)
        assertFalse(qrEngagement.isPresentationActive())
        assertFalse(service.isPresentationActive())
    }

    @Test
    fun `an NFC connection is ignored while a QR presentation is active`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)
        val qrDeviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        manager.deviceRetrievalHelper = qrDeviceRetrievalHelper

        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)

        service.buildNfcEngagementHelper = { _, _ -> mockk(relaxed = true) }
        service.engagement = service.Engagement()
        val transport = mockk<DataTransport>(relaxed = true)
        service.engagement!!.listener.onDeviceConnected(transport)

        assertSame(qrDeviceRetrievalHelper, manager.deviceRetrievalHelper)
        verify(exactly = 0) { listener.onTransferEvent(TransferEvent.Connected) }
        verify(exactly = 1) { transport.close() }
    }

    @Test
    fun `an NFC engagement is refused while a presentation is active`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        manager.deviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)

        val response = service.processCommandApdu(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00), null)

        assertContentEquals(byteArrayOf(0x6A, 0x82.toByte()), response)
    }

    @Test
    fun `a QR connection is ignored while an NFC presentation is active`() {
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)
        manager.startQrEngagement()
        val nfcDeviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        manager.deviceRetrievalHelper = nfcDeviceRetrievalHelper

        val transport = mockk<DataTransport>(relaxed = true)
        manager.qrEngagement!!.qrEngagementListener.onDeviceConnected(transport)

        assertSame(nfcDeviceRetrievalHelper, manager.deviceRetrievalHelper)
        verify(exactly = 0) { listener.onTransferEvent(TransferEvent.Connected) }
        verify(exactly = 1) { transport.close() }
    }

    @Test
    fun `a QR session that disconnects or fails releases the presentation`() {
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        manager.startQrEngagement()
        val qrEngagement = manager.qrEngagement!!
        val deviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        qrEngagement.deviceRetrievalHelper = deviceRetrievalHelper

        manager.deviceRetrievalHelper = deviceRetrievalHelper
        qrEngagement.deviceRetrievalHelperListener.onDeviceDisconnected(false)
        assertNull(manager.deviceRetrievalHelper)

        manager.deviceRetrievalHelper = deviceRetrievalHelper
        qrEngagement.deviceRetrievalHelperListener.onError(Throwable())
        assertNull(manager.deviceRetrievalHelper)
    }

    @Test
    fun `an NFC session that disconnects or fails releases the presentation for a new connection`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)

        val firstDeviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        val firstConnection = service.connectNfc(firstDeviceRetrievalHelper)
        assertSame(firstDeviceRetrievalHelper, manager.deviceRetrievalHelper)
        firstConnection.onDeviceDisconnected(false)
        assertNull(manager.deviceRetrievalHelper)

        val secondDeviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        val secondConnection = service.connectNfc(secondDeviceRetrievalHelper)
        assertSame(secondDeviceRetrievalHelper, manager.deviceRetrievalHelper)
        secondConnection.onError(Throwable())
        assertNull(manager.deviceRetrievalHelper)
    }

    @Test
    fun `a session that ended earlier does not release a newer presentation`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)
        val endedConnection = service.connectNfc(mockk<DeviceRetrievalHelper>())
        val newerDeviceRetrievalHelper = mockk<DeviceRetrievalHelper>()
        manager.deviceRetrievalHelper = newerDeviceRetrievalHelper

        endedConnection.onDeviceDisconnected(false)

        assertSame(newerDeviceRetrievalHelper, manager.deviceRetrievalHelper)
    }

    @Test
    fun `sendResponse throws when response is not a DeviceResponse instance`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            manager.sendResponse(object : Response {})
        }
    }

    @Test
    fun `events are triggered from qrEngagement`() {
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        mockkConstructor(QrEngagement::class)
        every { anyConstructed<QrEngagement>().configure() } just Runs
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(relaxed = true),
        )

        manager.addTransferEventListener(listener)
        manager.startQrEngagement()

        manager.qrEngagement!!.onConnecting()
        verify { listener.onTransferEvent(any<TransferEvent.Connecting>()) }

        val deviceRetrievalHelper = mockk<DeviceRetrievalHelper>(relaxed = true)
        manager.qrEngagement!!.onDeviceRetrievalHelperReady(deviceRetrievalHelper)
        assertSame(deviceRetrievalHelper, manager.deviceRetrievalHelper)
        verify { listener.onTransferEvent(any<TransferEvent.Connected>()) }

        manager.qrEngagement!!.onNewDeviceRequest(deviceRetrievalHelper, DeviceRequest.deviceRequestBytes)
        verify { listener.onTransferEvent(any<TransferEvent.RequestReceived>()) }

        manager.qrEngagement!!.onDisconnected(true)
        verify { listener.onTransferEvent(any<TransferEvent.Disconnected>()) }

        manager.qrEngagement!!.onCommunicationError(Throwable())
        verify { listener.onTransferEvent(any<TransferEvent.Error>()) }
    }

    /**
     * Connects an mdoc verifier through a new NFC engagement of this service, with
     * [deviceRetrievalHelper] as its connection, and returns the listener of that connection.
     */
    private fun NfcEngagementService.connectNfc(
        deviceRetrievalHelper: DeviceRetrievalHelper,
    ): DeviceRetrievalHelper.Listener {
        lateinit var connectionListener: DeviceRetrievalHelper.Listener
        buildNfcEngagementHelper = { _, _ -> mockk(relaxed = true) }
        buildDeviceRetrievalHelper = { _, _, listener ->
            connectionListener = listener
            deviceRetrievalHelper
        }
        engagement = Engagement()
        engagement!!.listener.onDeviceConnected(mockk(relaxed = true))
        return connectionListener
    }
}
