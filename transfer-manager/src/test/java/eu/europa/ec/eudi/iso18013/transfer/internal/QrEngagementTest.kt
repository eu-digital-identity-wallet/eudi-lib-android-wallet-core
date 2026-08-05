/*
 * Copyright (c) 2024-2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.internal

import android.content.Context
import com.android.identity.android.mdoc.engagement.QrEngagementHelper
import com.android.identity.android.mdoc.transport.DataTransportBleCentralClientMode
import com.android.identity.android.mdoc.transport.DataTransportBlePeripheralServerMode
import eu.europa.ec.eudi.iso18013.transfer.engagement.BleRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.engagement.QrCode
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.runner.RunWith
import org.multipaz.mdoc.connectionmethod.MdocConnectionMethodBle
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QrEngagementTest {

    lateinit var context: Context
    lateinit var retrievalMethods: List<DeviceRetrievalMethod>

    @BeforeTest
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockkConstructor(MdocConnectionMethodBle::class)
        mockkConstructor(DataTransportBlePeripheralServerMode::class)
        mockkConstructor(DataTransportBleCentralClientMode::class)
        every { anyConstructed<DataTransportBlePeripheralServerMode>().connect() } returns mockk()
        retrievalMethods = listOf(BleRetrievalMethod(true, false, false))
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `configure set the internal qrEngagmentHelper and sets the deviceEngagement available`() {
        val onQrEngementReady = spyk(OnQrEngementReady())
        val qrEngagement = QrEngagement(
            context = context,
            retrievalMethods = retrievalMethods,
            onQrEngagementReady = onQrEngementReady,
            onDisconnected = {},
            onConnecting = {},
            onNewDeviceRequest = {},
            onCommunicationError = {},
            onDeviceRetrievalHelperReady = {},
        )
        assertThrows(UninitializedPropertyAccessException::class.java) {
            qrEngagement.deviceEngagementUriEncoded
        }
        qrEngagement.configure()
        assertNotNull(qrEngagement.helper)
        assertNotNull(qrEngagement.deviceEngagementUriEncoded)
        val expected = QrCode(qrEngagement.deviceEngagementUriEncoded)
        assertEquals(expected, onQrEngementReady.qrCode)
    }

    @Test
    fun `close calls qrEngagementHelper close method`() {
        val qrEngagement = QrEngagement(
            context = context,
            retrievalMethods = retrievalMethods,
            onQrEngagementReady = { },
            onDisconnected = {},
            onConnecting = {},
            onNewDeviceRequest = {},
            onCommunicationError = {},
            onDeviceRetrievalHelperReady = {},
        )
        qrEngagement.helper = mockk<QrEngagementHelper> {
            every { close() } just Runs
        }
        qrEngagement.close()
        verify(exactly = 1) { qrEngagement.helper.close() }
    }

    @Test
    fun `eDevicePrivateKey should generate a fresh key for each engagement session`() {
        val qrEngagement = QrEngagement(
            context = context,
            retrievalMethods = retrievalMethods,
            onQrEngagementReady = { },
            onDisconnected = {},
            onConnecting = {},
            onNewDeviceRequest = {},
            onCommunicationError = {},
            onDeviceRetrievalHelperReady = {},
        )

        // First session: configure() generates a fresh key
        qrEngagement.configure()
        val firstSessionKey = qrEngagement.eDevicePrivateKey

        // Key must remain stable within the same session
        assertEquals(
            firstSessionKey,
            qrEngagement.eDevicePrivateKey,
            "eDevicePrivateKey must remain stable within the same session"
        )

        // Second session: configure() must generate a new ephemeral key
        qrEngagement.configure()
        val secondSessionKey = qrEngagement.eDevicePrivateKey

        assertNotEquals(
            firstSessionKey,
            secondSessionKey,
            "eDevicePrivateKey must be a fresh ephemeral key per engagement"
        )
    }

    class OnQrEngementReady : Function1<QrCode, Unit> {
        lateinit var qrCode: QrCode
        override fun invoke(p1: QrCode) {
            qrCode = p1
        }
    }
}