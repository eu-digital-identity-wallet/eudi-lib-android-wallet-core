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

package eu.europa.ec.eudi.iso18013.transfer.engagement

import android.nfc.cardemulation.HostApduService
import android.os.Looper
import com.android.identity.android.mdoc.deviceretrieval.DeviceRetrievalHelper
import com.android.identity.android.mdoc.transport.DataTransport
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NfcEngagementServiceTest {

    private val selectApdu = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)

    private lateinit var manager: TransferManagerImpl
    private lateinit var service: NfcEngagementService
    private val deviceRetrievalHelpers = mutableListOf<DeviceRetrievalHelper>()
    private val connectionListeners = mutableListOf<DeviceRetrievalHelper.Listener>()

    @BeforeTest
    fun setUp() {
        manager = TransferManagerImpl(
            context = RuntimeEnvironment.getApplication(),
            requestProcessor = mockk(),
        )
        service = object : NfcEngagementService() {
            override val transferManager: TransferManager
                get() = manager
        }
        manager.setupNfcEngagement(service)
        service.buildNfcEngagementHelper = { _, _ -> mockk(relaxed = true) }
        service.buildDeviceRetrievalHelper = { _, _, listener ->
            connectionListeners += listener
            mockk<DeviceRetrievalHelper>(relaxed = true).also { deviceRetrievalHelpers += it }
        }
    }

    private fun idleFor(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    @Test
    fun `each NFC tap creates a new engagement with a new ephemeral device key`() {
        service.processCommandApdu(selectApdu, null)
        val firstTap = assertNotNull(service.engagement)
        service.processCommandApdu(selectApdu, null)
        assertSame(firstTap, service.engagement)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)

        service.processCommandApdu(selectApdu, null)
        val secondTap = assertNotNull(service.engagement)

        assertNotSame(firstTap, secondTap)
        assertNotEquals(firstTap.eDevicePrivateKey, secondTap.eDevicePrivateKey)
        verify { firstTap.helper.close() }
    }

    @Test
    fun `the delayed close of a tap does not close the engagement of a newer tap`() {
        service.processCommandApdu(selectApdu, null)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)
        idleFor(1)
        service.processCommandApdu(selectApdu, null)
        val secondTap = assertNotNull(service.engagement)

        idleFor(15)

        assertSame(secondTap, service.engagement)
        verify(exactly = 0) { secondTap.helper.close() }

        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)
        idleFor(15)

        assertNull(service.engagement)
        verify { secondTap.helper.close() }
    }

    @Test
    fun `a connection through the engagement of an earlier tap is ignored`() {
        service.processCommandApdu(selectApdu, null)
        val firstTap = assertNotNull(service.engagement)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)
        service.processCommandApdu(selectApdu, null)

        val transport = mockk<DataTransport>(relaxed = true)
        firstTap.listener.onDeviceConnected(transport)

        verify { transport.close() }
        assertTrue(deviceRetrievalHelpers.isEmpty())
        assertNull(manager.deviceRetrievalHelper)
    }

    @Test
    fun `a new NFC presentation can start after stopPresentation`() {
        service.processCommandApdu(selectApdu, null)
        assertNotNull(service.engagement).listener.onDeviceConnected(mockk(relaxed = true))
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)
        assertSame(deviceRetrievalHelpers[0], manager.deviceRetrievalHelper)

        manager.stopPresentation(
            sendSessionTerminationMessage = true,
            useTransportSpecificSessionTermination = false,
        )
        assertNull(manager.deviceRetrievalHelper)

        service.processCommandApdu(selectApdu, null)
        assertNotNull(service.engagement).listener.onDeviceConnected(mockk(relaxed = true))

        assertSame(deviceRetrievalHelpers[1], manager.deviceRetrievalHelper)
    }

    @Test
    fun `a new NFC presentation can start on the same service after the verifier disconnects`() {
        service.processCommandApdu(selectApdu, null)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)
        assertNotNull(service.engagement).listener.onDeviceConnected(mockk(relaxed = true))
        assertSame(deviceRetrievalHelpers[0], manager.deviceRetrievalHelper)

        connectionListeners[0].onDeviceDisconnected(false)
        assertNull(manager.deviceRetrievalHelper)
        idleFor(15)

        service.processCommandApdu(selectApdu, null)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)
        assertNotNull(service.engagement).listener.onDeviceConnected(mockk(relaxed = true))

        assertSame(deviceRetrievalHelpers[1], manager.deviceRetrievalHelper)
    }

    @Test
    fun `a tap refused during an active session leaves the pending engagement of an earlier tap`() {
        service.processCommandApdu(selectApdu, null)
        val pending = assertNotNull(service.engagement)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)

        manager.deviceRetrievalHelper = mockk()
        val response = service.processCommandApdu(selectApdu, null)
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)

        assertContentEquals(byteArrayOf(0x6A, 0x82.toByte()), response)
        assertSame(pending, service.engagement)
        verify(exactly = 0) { pending.helper.close() }
    }

    @Test
    fun `the rest of a tap is refused after a verifier connected during it`() {
        service.processCommandApdu(selectApdu, null)
        assertNotNull(service.engagement).listener.onDeviceConnected(mockk(relaxed = true))
        connectionListeners[0].onDeviceDisconnected(false)

        val response = service.processCommandApdu(selectApdu, null)

        assertContentEquals(byteArrayOf(0x6A, 0x82.toByte()), response)
        assertNull(service.engagement)
    }
}
