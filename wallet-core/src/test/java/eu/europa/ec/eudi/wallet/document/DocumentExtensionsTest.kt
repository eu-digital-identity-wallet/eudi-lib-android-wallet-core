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
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import kotlin.time.Duration.Companion.milliseconds

class DocumentExtensionsTest {

    @Test
    @Suppress("DEPRECATION")
    fun `Document DefaultKeyUnlockData should return null if document is not IssuedDocument`() {
        // Create a mock Document that is not an IssuedDocument
        val document = mockk<UnsignedDocument>()

        // Get the result
        val result = document.DefaultKeyUnlockData

        // Verify the result is null
        assertNull(result)
    }

    @Test
    @Suppress("DEPRECATION")
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
    @Suppress("DEPRECATION")
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
    fun `IssuedDocument getDefaultKeyUnlockData should return AndroidKeystoreKeyUnlockData when credential exists`() = runTest {
        // Set up mocks
        val androidKeystoreSecureArea = mockk<AndroidKeystoreSecureArea>()
        val credentialAlias = "testCredentialAlias"
        val credential = mockk<SecureAreaBoundCredential> {
            every { secureArea } returns androidKeystoreSecureArea
            every { alias } returns credentialAlias
        }

        // Create a mock IssuedDocument that will return our credential
        val issuedDocument = mockk<IssuedDocument>()
        coEvery { issuedDocument.findCredential() } returns credential

        // Get the result using the new extension function
        val result = issuedDocument.getDefaultKeyUnlockData()

        // Verify the result properties
        assertNotNull(result)
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
    fun `EudiWallet getDefaultCreateDocumentSettings should use offeredDocument batchCredentialIssuanceSize`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
        }

        val eudiWalletConfig = EudiWalletConfig()
            .configureDocumentKeyCreation(
                userAuthenticationRequired = true,
                userAuthenticationTimeout = 1000.milliseconds,
                useStrongBoxForKeys = true,
            )

        val wallet: EudiWallet = mockk {
            every { config } returns eudiWalletConfig
            every { secureAreaRepository } returns mockk {
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea
            }
        }

        // Mock Offer.OfferedDocument - specify the numberOfCredentials parameter to use batchCredentialIssuanceSize
        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { batchCredentialIssuanceSize } returns 3
        }

        // Test with default parameters and explicitly set numberOfCredentials=offeredDocument.batchCredentialIssuanceSize
        val result = wallet.getDefaultCreateDocumentSettings(
            offeredDocument = offeredDocument,
            numberOfCredentials = 3 // Explicitly set to match batchCredentialIssuanceSize
        )

        assertEquals(secureArea.identifier, result.secureAreaIdentifier)
        assertEquals(3, result.numberOfCredentials)  // Should match the batchCredentialIssuanceSize
        val createKeySettings = result.createKeySettings
        assertIs<AndroidKeystoreCreateKeySettings>(createKeySettings)
        assertEquals(
            eudiWalletConfig.userAuthenticationRequired,
            createKeySettings.userAuthenticationRequired
        )
        assertEquals(eudiWalletConfig.useStrongBoxForKeys, createKeySettings.useStrongBox)
        assertEquals(
            eudiWalletConfig.userAuthenticationTimeout,
            createKeySettings.userAuthenticationTimeout
        )
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should respect numberOfCredentials parameter if less than batchCredentialIssuanceSize`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
        }

        val wallet: EudiWallet = mockk {
            every { config } returns EudiWalletConfig()
            every { secureAreaRepository } returns mockk {
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea
            }
        }

        // Mock Offer.OfferedDocument with batchCredentialIssuanceSize of 5
        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { batchCredentialIssuanceSize } returns 5
        }

        // Test with numberOfCredentials = 2 (less than batchCredentialIssuanceSize)
        val result = wallet.getDefaultCreateDocumentSettings(
            offeredDocument = offeredDocument,
            numberOfCredentials = 2,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.OneTimeUse
        )

        assertEquals(2, result.numberOfCredentials)
        assertEquals(CreateDocumentSettings.CredentialPolicy.OneTimeUse, result.credentialPolicy)
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should limit numberOfCredentials to batchCredentialIssuanceSize if greater`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
        }

        val wallet: EudiWallet = mockk {
            every { config } returns EudiWalletConfig()
            every { secureAreaRepository } returns mockk {
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea
            }
        }

        // Mock Offer.OfferedDocument with batchCredentialIssuanceSize of 2
        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { batchCredentialIssuanceSize } returns 2
        }

        // Test with numberOfCredentials = 5 (greater than batchCredentialIssuanceSize)
        val result = wallet.getDefaultCreateDocumentSettings(
            offeredDocument = offeredDocument,
            numberOfCredentials = 5
        )

        // Should be limited to the batchCredentialIssuanceSize
        assertEquals(2, result.numberOfCredentials)
    }

    @Test
    fun `EudiWallet getDefaultCreateDocumentSettings should apply custom configuration with attestationChallenge`() {
        val secureArea: AndroidKeystoreSecureArea = mockk {
            every { identifier } returns AndroidKeystoreSecureArea.IDENTIFIER
        }

        val wallet: EudiWallet = mockk {
            every { config } returns EudiWalletConfig()
            every { secureAreaRepository } returns mockk {
                coEvery {
                    getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
                } returns secureArea
            }
        }

        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { batchCredentialIssuanceSize } returns 1
        }

        val attestationChallenge = byteArrayOf(1, 2, 3)
        val result = wallet.getDefaultCreateDocumentSettings(
            offeredDocument = offeredDocument,
            attestationChallenge = attestationChallenge
        ) {
            setUserAuthenticationRequired(
                required = true,
                timeout = 1000.milliseconds,
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
        assertEquals(ByteString(attestationChallenge), createKeySettings.attestationChallenge)
        assertEquals(Algorithm.ESP384, createKeySettings.algorithm)
    }

    @Test
    fun `getDefaultKeyUnlockData should return null when document has no credential`() = runTest {
        val issuedDocument: IssuedDocument = mockk {
            coEvery { findCredential() } returns null
        }

        val result = getDefaultKeyUnlockData(issuedDocument)

        assertNull(result)
    }

    @Test
    fun `getDefaultKeyUnlockData should throw IllegalArgumentException when credential's secureArea is not AndroidKeystoreSecureArea`() =
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
        assertEquals(androidKeystoreSecureArea, result.secureArea)
        assertEquals(credentialAlias, result.alias)
    }

    @Test
    fun `getDefaultKeyUnlockData with secureArea and keyAlias should return AndroidKeystoreKeyUnlockData when secureArea is AndroidKeystoreSecureArea`() {
        val androidKeystoreSecureArea = mockk<AndroidKeystoreSecureArea>()
        val keyAlias = "testKeyAlias"

        val result = DocumentExtensions.getDefaultKeyUnlockData(androidKeystoreSecureArea, keyAlias)

        assertNotNull(result)
        assertIs<AndroidKeystoreKeyUnlockData>(result)
        assertEquals(keyAlias, result.alias)
        assertEquals(androidKeystoreSecureArea, result.secureArea)
    }

    @Test
    fun `getDefaultKeyUnlockData with secureArea and keyAlias should return null when secureArea is not AndroidKeystoreSecureArea`() {
        val nonAndroidKeystoreSecureArea = mockk<SecureArea>()
        val keyAlias = "testKeyAlias"

        val result = DocumentExtensions.getDefaultKeyUnlockData(nonAndroidKeystoreSecureArea, keyAlias)

        assertNull(result)
    }
}
