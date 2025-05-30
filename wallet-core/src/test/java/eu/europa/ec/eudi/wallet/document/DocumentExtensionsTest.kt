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

package eu.europa.ec.eudi.wallet.document

import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.DefaultKeyUnlockData
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreKeyUnlockData
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.UserAuthenticationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class DocumentExtensionsTest {

    @Test
    fun `Document DefaultKeyUnlockData should return null if document is not IssuedDocument`() {
        // Create a mock Document that is not an IssuedDocument
        val document = mockk<UnsignedDocument>()

        // Get the result
        val result = document.DefaultKeyUnlockData

        // Verify the result is null
        assertNull(result)
    }

    @Test
    fun `Document DefaultKeyUnlockData should throw IllegalArgumentException if document credential uses non-AndroidKeystoreSecureArea`() {
        // Set up mocks
        val nonAndroidKeystoreSecureArea = mockk<SecureArea>()
        val credential = mockk<SecureAreaBoundCredential> {
            every { secureArea } returns nonAndroidKeystoreSecureArea
            every { alias } returns "credentialAlias"
        }

        // Create a mock IssuedDocument that will return our credential
        val issuedDocument = mockk<IssuedDocument>()

        // IMPORTANT: coEvery must be used since findCredential is a suspend function
        // And we have to match the suspend context by using suspendCoroutine function
        coEvery {
            issuedDocument.findCredential()
        } returns credential

        try {
            // This should throw an IllegalArgumentException
            issuedDocument.DefaultKeyUnlockData
            fail("Expected IllegalArgumentException but no exception was thrown")
        } catch (e: IllegalArgumentException) {
            // This is the expected exception
            assertIs<IllegalArgumentException>(e)
        }
    }

    @Test
    fun `Document DefaultKeyUnlockData should return AndroidKeystoreKeyUnlockData with correct secureArea and alias when credential exists`() {
        // Set up mocks
        val androidKeystoreSecureArea = mockk<AndroidKeystoreSecureArea>()
        val credentialAlias = "testCredentialAlias"
        val credential = mockk<SecureAreaBoundCredential> {
            every { secureArea } returns androidKeystoreSecureArea
            every { alias } returns credentialAlias
        }

        // Create a mock IssuedDocument that will return our credential
        val issuedDocument = mockk<IssuedDocument>()

        // IMPORTANT: coEvery must be used since findCredential is a suspend function
        coEvery {
            issuedDocument.findCredential()
        } returns credential

        // Get the result - runBlocking is already used inside the property getter
        val result = issuedDocument.DefaultKeyUnlockData

        // Verify the result is not null
        assertNotNull(result, "Expected AndroidKeystoreKeyUnlockData but got null")

        // Verify the result properties
        assertIs<AndroidKeystoreKeyUnlockData>(result)
        assertEquals(credentialAlias, result.alias)
        assertEquals(androidKeystoreSecureArea, result.secureArea)
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
    fun `EudiWallet getDefaultCreateDocumentSettings should return the CreateDocumentSettings with first secureArea identifier and AndroidKeystoreCreateKeySettings based on EudiWalletConfig`() {

        val secureArea1: SecureArea = mockk {
            every { identifier } returns "secureArea1"
        }
        val secureArea2: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
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
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea2
                coEvery {
                    getImplementation("secureArea1")
                } returns secureArea1
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
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
        }
        val wallet: EudiWallet = mockk {
            every { secureAreaRepository } returns mockk {
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea
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
            setAlgorithm(Algorithm.ESP384)
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
        assertEquals(Algorithm.ESP384, createKeySettings.algorithm)
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should return the CreateDocumentSettings with first secureArea identifier and AndroidKeystoreCreateKeySettings with attestationChallenge from arguments`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
        }
        val wallet: EudiWallet = mockk {
            every { secureAreaRepository } returns mockk {
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea
            }
        }
        val attestationChallenge = byteArrayOf(1, 2, 3)
        val result = wallet.getDefaultCreateDocumentSettings(
            attestationChallenge = attestationChallenge
        ) {}

        assertEquals(secureArea.identifier, result.secureAreaIdentifier)
        val createKeySettings = result.createKeySettings
        assertIs<AndroidKeystoreCreateKeySettings>(createKeySettings)
        assertEquals(ByteString(attestationChallenge), createKeySettings.attestationChallenge)
    }

    @Test
    fun `getDefaultKeyUnlockData should return null when document has no credential`() = runBlocking {
        val issuedDocument: IssuedDocument = mockk {
            coEvery { findCredential() } returns null
        }

        val result = getDefaultKeyUnlockData(issuedDocument)

        assertNull(result)
    }

    @Test
    fun `getDefaultKeyUnlockData should throw IllegalStateException when credential's secureArea is not AndroidKeystoreSecureArea`() =
        runTest {
            val nonAndroidKeystoreSecureArea: SecureArea = mockk()
            val credential: SecureAreaBoundCredential = mockk {
                every { secureArea } returns nonAndroidKeystoreSecureArea
                every { alias } returns "credentialAlias"
            }
            val issuedDocument: IssuedDocument = mockk {
                coEvery { findCredential() } returns credential
            }

            val exception = runCatching { getDefaultKeyUnlockData(issuedDocument) }

            assertTrue(exception.isFailure)
            assertIs<IllegalArgumentException>(exception.exceptionOrNull())
        }

    @Test
    fun `getDefaultKeyUnlockData should return AndroidKeystoreKeyUnlockData with correct secureArea and alias`() = runTest {
        val androidKeystoreSecureArea: AndroidKeystoreSecureArea = mockk()
        val credentialAlias = "testCredentialAlias"
        val credential: SecureAreaBoundCredential = mockk {
            every { secureArea } returns androidKeystoreSecureArea
            every { alias } returns credentialAlias
        }
        val issuedDocument: IssuedDocument = mockk {
            coEvery { findCredential() } returns credential
        }

        val result = getDefaultKeyUnlockData(issuedDocument)

        assertIs<AndroidKeystoreKeyUnlockData>(result)
        assertEquals(androidKeystoreSecureArea, result?.secureArea)
        assertEquals(credentialAlias, result?.alias)
    }
}
