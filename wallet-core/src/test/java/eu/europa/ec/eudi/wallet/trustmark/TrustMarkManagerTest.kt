/*
 * Copyright (c) 2025-2026 European Commission
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

package eu.europa.ec.eudi.wallet.trustmark

import io.ktor.client.HttpClient
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrustMarkManagerTest {

    private lateinit var mockProvider: TrustMarkProvider
    private lateinit var mockHttpClientFactory: () -> HttpClient

    private val sampleInfo = TrustMarkInformation(
        trustMarkResourceURL = "https://eidas.ec.europa.eu/efda/wallet/trust-mark/resources",
        listOfCertifiedWalletsURL = "https://eidas.ec.europa.eu/efda/wallet/certified",
        walletSolutionInfoPageURL = "https://eidas.ec.europa.eu/efda/wallet/certified?id=WALLET_123",
        listOfCertifiedWalletsQRCode = "base64encodedQR==",
        walletSolutionInfoPageQRCode = null,
        walletVerifierToolURL = null,
    )

    @Before
    fun setUp() {
        mockProvider = mockk<TrustMarkProvider>()
        mockHttpClientFactory = { mockk<HttpClient>(relaxed = true) }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getTrustMark fails when provider fails`() = runTest {
        val error = RuntimeException("Provider unavailable")
        coEvery { mockProvider.getTrustMarkInformation() } returns Result.failure(error)

        val manager = TrustMarkManager(
            trustMarkProvider = mockProvider,
            ktorHttpClientFactory = mockHttpClientFactory,
        )

        val result = manager.getTrustMark()

        assertTrue(result.isFailure)
        assertEquals("Provider unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `TrustMarkInformation deserializes from JSON matching EC schema`() {
        val json = """
        {
            "TrustMarkResourceURL": "https://example.com/resources",
            "ListOfCertifiedWalletsURL": "https://example.com/wallets",
            "WalletSolutionInfoPageURL": "https://example.com/wallets?id=123"
        }
        """.trimIndent()

        val parsed = Json.decodeFromString<TrustMarkInformation>(json)

        assertEquals("https://example.com/resources", parsed.trustMarkResourceURL)
        assertEquals("https://example.com/wallets", parsed.listOfCertifiedWalletsURL)
        assertEquals("https://example.com/wallets?id=123", parsed.walletSolutionInfoPageURL)
        assertEquals(null, parsed.listOfCertifiedWalletsQRCode)
        assertEquals(null, parsed.walletSolutionInfoPageQRCode)
        assertEquals(null, parsed.walletVerifierToolURL)
    }

    @Test
    fun `TrustMarkInformation deserializes all fields from JSON`() {
        val json = """
        {
            "TrustMarkResourceURL": "https://example.com/resources",
            "ListOfCertifiedWalletsURL": "https://example.com/wallets",
            "WalletSolutionInfoPageURL": "https://example.com/wallets?id=123",
            "ListOfCertifiedWalletsQRCode": "base64QR==",
            "WalletSolutionInfoPageQRCode": "base64QR2==",
            "WalletVerifierToolURL": "https://example.com/.well-known/openid-credential-issuer"
        }
        """.trimIndent()

        val parsed = Json.decodeFromString<TrustMarkInformation>(json)

        assertEquals("base64QR==", parsed.listOfCertifiedWalletsQRCode)
        assertEquals("base64QR2==", parsed.walletSolutionInfoPageQRCode)
        assertEquals(
            "https://example.com/.well-known/openid-credential-issuer",
            parsed.walletVerifierToolURL,
        )
    }

    @Test
    fun `TrustMarkInformation serializes with correct SerialName values`() {
        val info = TrustMarkInformation(
            trustMarkResourceURL = "https://example.com/resources",
            listOfCertifiedWalletsURL = "https://example.com/wallets",
            walletSolutionInfoPageURL = "https://example.com/wallets?id=123",
        )

        val jsonString = Json.encodeToString(info)

        assertTrue(jsonString.contains("\"TrustMarkResourceURL\""))
        assertTrue(jsonString.contains("\"ListOfCertifiedWalletsURL\""))
        assertTrue(jsonString.contains("\"WalletSolutionInfoPageURL\""))
    }

    @Test
    fun `TrustMarkResource deserializes from JSON matching EC schema`() {
        val json = """
        {
            "image": {
                "name": "eudi-wallet-trustmark-logo.png",
                "url": "https://example.com/logo.png"
            },
            "text": {
                "name": "Trust Mark user information",
                "localisations": {
                    "en": "This wallet is certified",
                    "fr": "Ce portefeuille est certifié",
                    "de": "Diese Wallet ist zertifiziert"
                }
            }
        }
        """.trimIndent()

        val parsed = Json.decodeFromString<TrustMarkResource>(json)

        assertEquals("eudi-wallet-trustmark-logo.png", parsed.image.name)
        assertEquals("https://example.com/logo.png", parsed.image.url)
        assertEquals("Trust Mark user information", parsed.text.name)
        assertEquals(3, parsed.text.localisations.size)
        assertEquals("This wallet is certified", parsed.text.localisations["en"])
        assertEquals("Ce portefeuille est certifié", parsed.text.localisations["fr"])
        assertEquals("Diese Wallet ist zertifiziert", parsed.text.localisations["de"])
    }

    @Test
    fun `TrustMarkManager companion factory creates instance`() {
        val info = sampleInfo
        val manager = TrustMarkManager(
            trustMarkProvider = TrustMarkProvider { Result.success(info) },
        )
        assertTrue(manager is TrustMarkManager)
    }
}
