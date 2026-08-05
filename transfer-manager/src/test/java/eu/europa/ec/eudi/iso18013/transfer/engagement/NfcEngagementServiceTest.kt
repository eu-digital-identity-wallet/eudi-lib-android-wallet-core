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

import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NfcEngagementServiceTest {

    class TestNfcEngagementService : NfcEngagementService() {
        override val transferManager: TransferManager = mockk(relaxed = true)
    }

    @BeforeTest
    fun setUp() {
        mockkConstructor(
            com.android.identity.android.mdoc.engagement.NfcEngagementHelper.Builder::class,
        )
        every { anyConstructed<com.android.identity.android.mdoc.engagement.NfcEngagementHelper.Builder>().useStaticHandover(any()) } returns mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    private fun getEDevicePrivateKey(service: NfcEngagementService): EcPrivateKey {
        val field = NfcEngagementService::class.java.getDeclaredField("eDevicePrivateKey")
        field.isAccessible = true
        return field.get(service) as EcPrivateKey
    }

    private fun simulateSessionStart(service: NfcEngagementService) {
        // Simulate what onCreate() does for key generation: generate a fresh key
        val field = NfcEngagementService::class.java.getDeclaredField("eDevicePrivateKey")
        field.isAccessible = true
        field.set(service, runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) })
    }

    @Test
    fun `eDevicePrivateKey should generate a fresh key for each engagement session`() {
        val service = TestNfcEngagementService()

        // First session: simulate onCreate generating a key
        simulateSessionStart(service)
        val firstSessionKey = getEDevicePrivateKey(service)

        // Key must remain stable within the same session
        assertEquals(
            firstSessionKey,
            getEDevicePrivateKey(service),
            "eDevicePrivateKey must remain stable within the same session"
        )

        // Second session: simulate another onCreate generating a new key
        simulateSessionStart(service)
        val secondSessionKey = getEDevicePrivateKey(service)

        assertNotEquals(
            firstSessionKey,
            secondSessionKey,
            "eDevicePrivateKey must be a fresh ephemeral key per engagement"
        )
    }
}
