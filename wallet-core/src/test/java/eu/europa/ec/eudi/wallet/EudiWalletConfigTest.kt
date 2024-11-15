/*
 * Copyright (c) 2023-2024 European Commission
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

package eu.europa.ec.eudi.wallet

import android.content.Context
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.PreregisteredVerifier
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.X509Certificate

class EudiWalletConfigTest {

    private val context: Context = mockk(relaxed = true) {
        every { applicationContext } returns this@mockk
        every { noBackupFilesDir } returns mockk()
    }

    @Test
    fun testInvoke() {
        val readerCertificate1 = mockk<X509Certificate>()
        val readerCertificate2 = mockk<X509Certificate>()
        val config = EudiWalletConfig() {
            configureReaderTrustStore(readerCertificate1, readerCertificate2)
            configureLogging(Logger.LEVEL_ERROR)
            configureDocumentManager(context.noBackupFilesDir)
            configureOpenId4Vci {
                withIssuerUrl("https://example.com")
                withClientId("client-id")
                withAuthFlowRedirectionURI("eudi-openid4ci://authorize")
            }
            configureOpenId4Vp {

                withClientIdSchemes(
                    listOf(
                        ClientIdScheme.Preregistered(
                            listOf(
                                PreregisteredVerifier(
                                    "VerifierClientId",
                                    "VerifierLegalName",
                                    "https://example.com"
                                )
                            )
                        ),
                        ClientIdScheme.X509SanDns
                    )
                )
                withSchemes(
                    listOf(
                        "eudi-openid4vp",
                        "mdoc-openid4vp"
                    )
                )
                withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
                withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
            }
        }
        assertEquals(2, config.readerTrustedCertificates?.size)
        assertEquals(readerCertificate1, config.readerTrustedCertificates?.get(0))
        assertEquals(readerCertificate2, config.readerTrustedCertificates?.get(1))
        assertEquals(
            "https://example.com",
            (config.openId4VpConfig?.clientIdSchemes?.get(0) as ClientIdScheme.Preregistered).preregisteredVerifiers[0].verifierApi
        )
        assertEquals(
            "VerifierClientId",
            (config.openId4VpConfig?.clientIdSchemes?.get(0) as ClientIdScheme.Preregistered).preregisteredVerifiers[0].clientId
        )
        assertEquals(
            "VerifierLegalName",
            (config.openId4VpConfig?.clientIdSchemes?.get(0) as ClientIdScheme.Preregistered).preregisteredVerifiers[0].legalName
        )
        assertEquals(ClientIdScheme.X509SanDns, config.openId4VpConfig?.clientIdSchemes?.get(1))
        assertEquals("eudi-openid4vp", config.openId4VpConfig?.schemes?.get(0))
        assertEquals("mdoc-openid4vp", config.openId4VpConfig?.schemes?.get(1))
        assertEquals(
            EncryptionAlgorithm.ECDH_ES,
            config.openId4VpConfig?.encryptionAlgorithms?.get(0)
        )
        assertEquals(
            EncryptionMethod.A128CBC_HS256,
            config.openId4VpConfig?.encryptionMethods?.get(0)
        )
        assertEquals("https://example.com", config.openId4VciConfig?.issuerUrl)
        assertEquals("client-id", config.openId4VciConfig?.clientId)
        assertEquals("eudi-openid4ci://authorize", config.openId4VciConfig?.authFlowRedirectionURI)
    }

    @Test
    fun testDefaultValues() {
        val config = EudiWalletConfig()
        // assert default values here
        assertTrue(config.enableBlePeripheralMode)
        assertFalse(config.enableBleCentralMode)
        assertTrue(config.clearBleCache)
        assertNull(config.readerTrustedCertificates)
        assertNull(config.openId4VpConfig)
        assertNull(config.openId4VciConfig)
    }

    @Test
    fun testConfigureLogging() {
        val config = EudiWalletConfig {
            configureLogging(Logger.OFF, 10)
        }

        assertEquals(Logger.OFF, config.logLevel)
        assertEquals(10, config.logSizeLimit)
    }
}