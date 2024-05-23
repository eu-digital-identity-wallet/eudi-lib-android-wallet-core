/*
 *  Copyright (c) 2023-2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet

import android.content.Context
import eu.europa.ec.eudi.wallet.transfer.openid4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openid4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openid4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openid4vp.PreregisteredVerifier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.security.cert.X509Certificate

class EudiWalletConfigTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.noBackupFilesDir).thenReturn(mock(File::class.java))
    }

    @Test
    fun testInvoke() {
        val storageDir = File("/tmp")
        val readerCertificate1 = mock(X509Certificate::class.java)
        val readerCertificate2 = mock(X509Certificate::class.java)
        val config = EudiWalletConfig(context) {
            documentsStorageDir(storageDir)
            userAuthenticationRequired(true)
            userAuthenticationTimeOut(10_000L)
            trustedReaderCertificates(readerCertificate1, readerCertificate2)
            openId4VpConfig {
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
                withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
                withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
            }
            openId4VciConfig {
                issuerUrl("https://example.com")
                clientId("client-id")
                authFlowRedirectionURI("eudi-openid4ci://authorize")
            }
        }
        assertEquals(storageDir, config.documentsStorageDir)
        assertTrue(config.userAuthenticationRequired)
        assertEquals(10_000L, config.userAuthenticationTimeOut)
        assertEquals(2, config.trustedReaderCertificates?.size)
        assertEquals(readerCertificate1, config.trustedReaderCertificates?.get(0))
        assertEquals(readerCertificate2, config.trustedReaderCertificates?.get(1))
        assertEquals(
            "https://example.com",
            (config.openId4VPConfig?.clientIdSchemes?.get(0) as ClientIdScheme.Preregistered).preregisteredVerifiers[0].verifierApi
        )
        assertEquals(
            "VerifierClientId",
            (config.openId4VPConfig?.clientIdSchemes?.get(0) as ClientIdScheme.Preregistered).preregisteredVerifiers[0].clientId
        )
        assertEquals(
            "VerifierLegalName",
            (config.openId4VPConfig?.clientIdSchemes?.get(0) as ClientIdScheme.Preregistered).preregisteredVerifiers[0].legalName
        )
        assertEquals(ClientIdScheme.X509SanDns, config.openId4VPConfig?.clientIdSchemes?.get(1))
        assertEquals(EncryptionAlgorithm.ECDH_ES, config.openId4VPConfig?.encryptionAlgorithms?.get(0))
        assertEquals(EncryptionMethod.A128CBC_HS256, config.openId4VPConfig?.encryptionMethods?.get(0))
        assertEquals("https://example.com", config.openId4VciConfig?.issuerUrl)
        assertEquals("client-id", config.openId4VciConfig?.clientId)
        assertEquals("eudi-openid4ci://authorize", config.openId4VciConfig?.authFlowRedirectionURI)
    }

    @Test
    fun testBuilder() {
        val builder = EudiWalletConfig.Builder(context)

        builder.encryptDocumentsInStorage(false)
        builder.useHardwareToStoreKeys(false)
        builder.bleTransferMode(
            EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE,
            EudiWalletConfig.BLE_CLIENT_CENTRAL_MODE
        )
        builder.bleClearCacheEnabled(true)
        builder.userAuthenticationRequired(true)
        builder.userAuthenticationTimeOut(30_000L)
        builder.trustedReaderCertificates(listOf(mock(X509Certificate::class.java)))

        val config = builder.build()

        assertFalse(config.encryptDocumentsInStorage)
        assertFalse(config.useHardwareToStoreKeys)
        assertEquals(
            EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE or EudiWalletConfig.BLE_CLIENT_CENTRAL_MODE,
            config.bleTransferMode
        )
        assertTrue(config.bleCentralClientModeEnabled)
        assertTrue(config.blePeripheralServerModeEnabled)
        assertTrue(config.bleClearCacheEnabled)
        assertTrue(config.userAuthenticationRequired)
        assertEquals(30_000L, config.userAuthenticationTimeOut)
        assertEquals(1, config.trustedReaderCertificates?.size)

    }

    @Test
    fun testDefaultValues() {
        val config = EudiWalletConfig(context) {}
        // assert default values here
        assertEquals(config.documentsStorageDir, context.noBackupFilesDir)
        assertTrue(config.encryptDocumentsInStorage)
        assertTrue(config.useHardwareToStoreKeys)
        assertEquals(config.bleTransferMode, EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE)
        assertFalse(config.bleClearCacheEnabled)
        assertFalse(config.userAuthenticationRequired)
        assertEquals(30_000L, config.userAuthenticationTimeOut)
        assertNull(config.trustedReaderCertificates)
        assertNull(config.openId4VPConfig)
        assertNull(config.openId4VciConfig)
    }
}