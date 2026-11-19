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

package eu.europa.ec.eudi.wallet.provider

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SecureAreaWalletKeyManagerTest {

    @Test
    fun `the key alias is derived from the whole digest of the authorization server`() = runTest {
        val secureArea = SoftwareSecureArea.create(EphemeralStorage())
        // A key created by an earlier version, under the first 16 hexadecimal digits of the digest
        val earlierKey = secureArea.createKey(EARLIER_ALIAS, settings())
        val manager = SecureAreaWalletKeyManager(secureArea, createKeySettingsProvider = { settings() })

        val key = manager.getOrCreateWalletAttestationKey(AUTHORIZATION_SERVER, listOf(Algorithm.ESP256)).getOrThrow()

        assertEquals(ALIAS, key.keyInfo.alias)
        assertEquals(earlierKey.publicKey, manager.getWalletAttestationKey(EARLIER_ALIAS)?.keyInfo?.publicKey)
    }

    @Test
    fun `the earlier alias is the first 16 hexadecimal digits of the digest`() {
        assertEquals(EARLIER_ALIAS, earlierClientAttestationKeyAlias(AUTHORIZATION_SERVER))
    }

    private fun settings() = SoftwareCreateKeySettings.Builder().setAlgorithm(Algorithm.ESP256).build()

    private companion object {
        const val AUTHORIZATION_SERVER = "https://auth.example.com"

        // "client-attestation-" followed by the SHA-256 digest of AUTHORIZATION_SERVER
        const val ALIAS = "client-attestation-1ce18487e0b9bfefc8259d779bfe98742d3cb9bcb19af96d0cd63e1274b168a2"
        const val EARLIER_ALIAS = "client-attestation-1ce18487e0b9bfef"
    }
}
