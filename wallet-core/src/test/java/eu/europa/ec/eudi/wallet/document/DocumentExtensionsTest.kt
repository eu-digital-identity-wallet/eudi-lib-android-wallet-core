/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.document

import com.android.identity.android.securearea.AndroidKeystoreCreateKeySettings
import com.android.identity.android.securearea.AndroidKeystoreKeyInfo
import com.android.identity.android.securearea.AndroidKeystoreKeyUnlockData
import com.android.identity.android.securearea.AndroidKeystoreSecureArea
import com.android.identity.android.securearea.UserAuthenticationType
import com.android.identity.crypto.EcCurve
import com.android.identity.securearea.KeyInfo
import com.android.identity.securearea.SecureArea
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.DefaultKeyUnlockData
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentExtensionsTest {

    @Test
    fun `Document DefaultKeyUnlockData should return null if document requires no user authentication`() {
        val mockKeyInfo = mockk<AndroidKeystoreKeyInfo> {
            every { isUserAuthenticationRequired } returns false
        }
        val document: Document = mockk {
            every { keyInfo } returns mockKeyInfo
        }

        val result = document.DefaultKeyUnlockData
        assertNull(result)
    }

    @Test
    fun `Document DefaultKeyUnlockData should throw IllegalStateException if document is not managed by an AndroidKeystoreSecureArea`() {
        val mockKeyInfo = mockk<KeyInfo>()
        val document: Document = mockk {
            every { keyInfo } returns mockKeyInfo
        }

        val exception = runCatching { document.DefaultKeyUnlockData }
        assertTrue(exception.isFailure)
        assertIs<IllegalStateException>(exception.exceptionOrNull())
    }

    @Test
    fun `Document DefaultKeyUnlockData should return an AndroidKeystoreKeyUnlockData for the document alias`() {
        val mockKeyInfo = mockk<AndroidKeystoreKeyInfo> {
            every { isUserAuthenticationRequired } returns true
        }
        val document: Document = mockk {
            every { keyInfo } returns mockKeyInfo
            every { keyAlias } returns "keyAlias"
        }

        val result = document.DefaultKeyUnlockData
        assertIs<AndroidKeystoreKeyUnlockData>(result)
        assertEquals(document.keyAlias, result.alias)
    }

    @Test
    fun `EudiWallet getDefaultKeyUnlockData for documentId should throw NoSuchElementFound if document is not found`() {
        val documentId = "nonExistentDocument"
        val wallet: EudiWallet = mockk {
            every { getDocumentById(documentId) } returns null
        }

        val exception = runCatching { wallet.getDefaultKeyUnlockData(documentId) }
        assertTrue(exception.isFailure)
        assertIs<NoSuchElementException>(exception.exceptionOrNull())
    }

    @Test
    fun `EudiWallet getDefaultKeyUnlockData for documentId should call Document DefaultKeyUnlockData extension`() {
        val documentId = "existingDocument"
        val mockDocument: Document = mockk {
            every { id } returns documentId
        }

        val wallet: EudiWallet = mockk {
            every { getDocumentById(documentId) } returns mockDocument
        }

        val keyUnlockData: AndroidKeystoreKeyUnlockData = mockk()

        mockkStatic("eu.europa.ec.eudi.wallet.document.DocumentExtensions")
        every { mockDocument.DefaultKeyUnlockData } returns keyUnlockData
        every { wallet.getDefaultKeyUnlockData(documentId) } answers { callOriginal() }

        val result = wallet.getDefaultKeyUnlockData(documentId)
        assertEquals(keyUnlockData, result)
        verify(exactly = 1) { mockDocument.DefaultKeyUnlockData }
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should throw if no AndroidKeystoreSecureArea is found in the wallet`() {
        val wallet: EudiWallet = mockk {
            every { secureAreaRepository } returns mockk {
                every { implementations } returns listOf(
                    mockk(), mockk()
                )
            }
        }

        val exception = runCatching { wallet.getDefaultCreateDocumentSettings() }
        assertTrue(exception.isFailure)
        assertIs<NoSuchElementException>(exception.exceptionOrNull())
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should return the CreateDocumentSettings with first secureArea identifier and AndroidKeystoreCreateKeySettings based on EudiWalletConfig`() {

        val secureArea1: SecureArea = mockk()
        val secureArea2: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns "secureArea1"
        }
        val eudiWalletConfig = EudiWalletConfig()
            .configureDocumentKeyCreation(
                userAuthenticationRequired = true,
                userAuthenticationTimeout = 1000,
                useStrongBoxForKeys = true,
            )
        val wallet: EudiWallet = mockk {
            every { config } returns eudiWalletConfig
            every { secureAreaRepository } returns mockk {
                every { implementations } returns listOf(
                    secureArea1, secureArea2
                )
            }
        }

        val result = wallet.getDefaultCreateDocumentSettings()
        assertEquals(secureArea2.identifier, result.secureAreaIdentifier)
        val createKeySettings = result.createKeySettings
        assertIs<AndroidKeystoreCreateKeySettings>(createKeySettings)
        assertEquals(
            eudiWalletConfig.userAuthenticationRequired,
            createKeySettings.userAuthenticationRequired
        )
        assertEquals(eudiWalletConfig.useStrongBoxForKeys, createKeySettings.useStrongBox)
        assertEquals(
            eudiWalletConfig.userAuthenticationTimeout,
            createKeySettings.userAuthenticationTimeoutMillis
        )
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should return the CreateDocumentSettings with first secureArea identifier and AndroidKeystoreCreateKeySettings based on configure argument`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns "secureArea1"
        }
        val wallet: EudiWallet = mockk {
            every { secureAreaRepository } returns mockk {
                every { implementations } returns listOf(
                    secureArea
                )
            }
        }

        val result = wallet.getDefaultCreateDocumentSettings {
            setUserAuthenticationRequired(
                required = true,
                timeoutMillis = 1000,
                userAuthenticationTypes = setOf(
                    UserAuthenticationType.LSKF,
                    UserAuthenticationType.BIOMETRIC
                )
            )
            setUseStrongBox(true)
            setEcCurve(EcCurve.P384)
        }

        assertEquals(secureArea.identifier, result.secureAreaIdentifier)
        val createKeySettings = result.createKeySettings
        assertIs<AndroidKeystoreCreateKeySettings>(createKeySettings)
        assertTrue(createKeySettings.userAuthenticationRequired)
        assertEquals(1000, createKeySettings.userAuthenticationTimeoutMillis)
        assertEquals(
            setOf(UserAuthenticationType.LSKF, UserAuthenticationType.BIOMETRIC),
            createKeySettings.userAuthenticationTypes
        )
        assertTrue(createKeySettings.useStrongBox)
        assertEquals(EcCurve.P384, createKeySettings.ecCurve)
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should return the CreateDocumentSettings with first secureArea identifier and AndroidKeystoreCreateKeySettings with attestationChallenge from arguments`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns "secureArea1"
        }
        val wallet: EudiWallet = mockk {
            every { secureAreaRepository } returns mockk {
                every { implementations } returns listOf(
                    secureArea
                )
            }
        }
        val attestationChallenge = byteArrayOf(1, 2, 3)
        val result = wallet.getDefaultCreateDocumentSettings(
            attestationChallenge = attestationChallenge
        ) {}

        assertEquals(secureArea.identifier, result.secureAreaIdentifier)
        val createKeySettings = result.createKeySettings
        assertIs<AndroidKeystoreCreateKeySettings>(createKeySettings)
        assertEquals(attestationChallenge, createKeySettings.attestationChallenge)
    }
}