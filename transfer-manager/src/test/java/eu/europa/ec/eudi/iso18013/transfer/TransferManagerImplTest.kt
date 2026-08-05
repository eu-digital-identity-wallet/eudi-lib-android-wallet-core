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
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.iso18013.transfer.internal.QrEngagement
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
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
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.mockito.MockedStatic
import org.multipaz.util.Constants.SESSION_DATA_STATUS_SESSION_TERMINATION
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        assertIs<(ByteArray) -> Unit>(service.onNewDeviceRequest)
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

        service.onNewDeviceRequest(ByteArray(0))
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
    fun `sendResponse triggers onResponseSent event and class deviceRetrievalHelper send with SessionTermination`() {
        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = mockk(),
        )
        val listener = mockk<TransferEvent.Listener>(relaxed = true)
        manager.addTransferEventListener(listener)

        val mockDeviceRetrievalHelper: DeviceRetrievalHelper = mockk(relaxed = true)
        manager.deviceRetrievalHelper = mockDeviceRetrievalHelper

        manager.sendResponse(
            DeviceResponse(
                deviceResponseBytes = ByteArray(0),
                sessionTranscriptBytes = byteArrayOf(),
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

        manager.qrEngagement!!.onNewDeviceRequest(DeviceRequest.deviceRequestBytes)
        verify { listener.onTransferEvent(any<TransferEvent.RequestReceived>()) }

        manager.qrEngagement!!.onDisconnected(true)
        verify { listener.onTransferEvent(any<TransferEvent.Disconnected>()) }

        manager.qrEngagement!!.onCommunicationError(Throwable())
        verify { listener.onTransferEvent(any<TransferEvent.Error>()) }
    }

    @Test
    fun `setting and getting readerTrustStore delegates to requestProcessor if the latter is ReaderTrustStoreAware`() {

        val processor: RequestProcessor = object : RequestProcessor, ReaderTrustStoreAware {
            override suspend fun process(request: Request): RequestProcessor.ProcessedRequest {
                return mockk()
            }

            override var readerTrustStore: ReaderTrustStore? = null
        }

        val manager = TransferManagerImpl(
            context = Context,
            requestProcessor = processor
        )

        val readerTrustStore: ReaderTrustStore = mockk()

        manager.readerTrustStore = readerTrustStore

        assertSame(readerTrustStore, manager.readerTrustStore)
    }
}