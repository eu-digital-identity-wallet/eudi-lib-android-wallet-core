/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.document.credential

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.document.Document
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea


class CredentialFactoryTest {

    private val testDomain = "test-domain.example"
    private val testDocType = "org.iso.18013.5.1.mDL"
    private val testVct = "VerifiableCredentialType"

    @Test
    fun `factory creates correct implementation based on format`() {
        // Given different document formats
        val mdocFormat = MsoMdocFormat(testDocType)
        val sdJwtFormat = SdJwtVcFormat(testVct)

        // When creating factories
        val mdocFactory = CredentialFactory(testDomain, mdocFormat)
        val sdJwtFactory = CredentialFactory(testDomain, sdJwtFormat)

        // Then correct implementations are returned
        assertTrue(mdocFactory is MdocCredentialFactory)
        assertEquals(testDomain, (mdocFactory as MdocCredentialFactory).domain)

        assertTrue(sdJwtFactory is SdJwtVcCredentialFactory)
        assertEquals(testDomain, (sdJwtFactory as SdJwtVcCredentialFactory).domain)
    }

    @Test
    fun `MdocCredentialFactory creates credentials with correct parameters`() = runTest {
        // Given
        val format = MsoMdocFormat(testDocType)
        val factoryToTest = spyk(MdocCredentialFactory(testDomain))

        // Mock document and CreateDocumentSettings
        val document = mockk<Document>(relaxed = true)
        val createKeySettings = mockk<CreateKeySettings>()
        val createDocumentSettings = mockk<CreateDocumentSettings> {
            every { this@mockk.createKeySettings } returns createKeySettings
            every { this@mockk.credentialPolicy } returns CreateDocumentSettings.CredentialPolicy.RotatingBatch(numberOfCredentials = 2)
        }

        // Mock SecureArea to return key info
        val keyInfo1 = mockk<org.multipaz.securearea.KeyInfo> {
            every { alias } returns "key1"
        }
        val keyInfo2 = mockk<org.multipaz.securearea.KeyInfo> {
            every { alias } returns "key2"
        }
        val batchResult = mockk<org.multipaz.securearea.BatchCreateKeyResult> {
            every { keyInfos } returns listOf(keyInfo1, keyInfo2)
            every { openid4vciKeyAttestationJws } returns null
        }
        val secureArea = mockk<SecureArea> {
            coEvery {
                batchCreateKey(
                    numKeys = 2,
                    createKeySettings = createKeySettings
                )
            } returns batchResult

            // Mock getKeyInfo for each key alias
            coEvery { getKeyInfo("key1") } returns keyInfo1
            coEvery { getKeyInfo("key2") } returns keyInfo2

            // Mock getIdentifier
            every { identifier } returns "mock-secure-area-id"
        }

        // When creating credentials
        val (credentials, keyAttestation) = factoryToTest.createCredentials(
            format = format,
            document = document,
            createDocumentSettings = createDocumentSettings,
            secureArea = secureArea
        )

        // Then correct number of credentials is created
        assertEquals(2, credentials.size)
        assertTrue(credentials.all { it.docType == testDocType })
        assertTrue(credentials.all { it.domain == testDomain })
        assertTrue(credentials.all { it.secureArea == secureArea })
    }

    @Test
    fun `SdJwtVcCredentialFactory creates credentials with correct parameters`() = runTest {
        // Given
        val format = SdJwtVcFormat(testVct)
        val factoryToTest = spyk(SdJwtVcCredentialFactory(testDomain))

        // Mock document and CreateDocumentSettings
        val document = mockk<Document>(relaxed = true)
        val createKeySettings = mockk<CreateKeySettings>()
        val createDocumentSettings = mockk<CreateDocumentSettings> {
            every { this@mockk.createKeySettings } returns createKeySettings
            every { this@mockk.credentialPolicy } returns CreateDocumentSettings.CredentialPolicy.RotatingBatch(numberOfCredentials = 2)
        }

        // Mock SecureArea to return key info
        val keyInfo1 = mockk<org.multipaz.securearea.KeyInfo> {
            every { alias } returns "key1"
        }
        val keyInfo2 = mockk<org.multipaz.securearea.KeyInfo> {
            every { alias } returns "key2"
        }
        val batchResult = mockk<org.multipaz.securearea.BatchCreateKeyResult> {
            every { keyInfos } returns listOf(keyInfo1, keyInfo2)
            every { openid4vciKeyAttestationJws } returns null
        }
        val secureArea = mockk<SecureArea> {
            coEvery {
                batchCreateKey(
                    numKeys = 2,
                    createKeySettings = createKeySettings
                )
            } returns batchResult

            // Mock getKeyInfo for each key alias
            coEvery { getKeyInfo("key1") } returns keyInfo1
            coEvery { getKeyInfo("key2") } returns keyInfo2

            // Mock getIdentifier
            every { identifier } returns "mock-secure-area-id"
        }

        // When creating credentials
        val (credentials, keyAttestation) = factoryToTest.createCredentials(
            format = format,
            document = document,
            createDocumentSettings = createDocumentSettings,
            secureArea = secureArea
        )

        // Then correct number of credentials is created and properties are set correctly
        assertEquals(2, credentials.size)
        assertTrue(credentials.all { it.vct == testVct })
        assertTrue(credentials.all { it.domain == testDomain })
        assertTrue(credentials.all { it.secureArea == secureArea })
    }

    @Test
    fun `MdocCredentialFactory throws when wrong format provided`() = runTest {
        // Given
        val format = SdJwtVcFormat(testVct)
        val factory = MdocCredentialFactory(testDomain)

        // Mock dependencies
        val document = mockk<Document>()
        val secureArea = mockk<SecureArea>()
        val createDocumentSettings = mockk<CreateDocumentSettings>()

        // When/Then - expect exception when wrong format is provided
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                factory.createCredentials(
                    format = format,
                    document = document,
                    createDocumentSettings = createDocumentSettings,
                    secureArea = secureArea
                )
            }
        }

        assertTrue(exception.message!!.contains("Expected class eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat"))
    }

    @Test
    fun `SdJwtVcCredentialFactory throws when wrong format provided`() = runTest {
        // Given
        val format = MsoMdocFormat(testDocType)
        val factory = SdJwtVcCredentialFactory(testDomain)

        // Mock dependencies
        val document = mockk<Document>()
        val secureArea = mockk<SecureArea>()
        val createDocumentSettings = mockk<CreateDocumentSettings>()

        // When/Then - expect exception when wrong format is provided
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                factory.createCredentials(
                    format = format,
                    document = document,
                    createDocumentSettings = createDocumentSettings,
                    secureArea = secureArea
                )
            }
        }

        assertTrue(exception.message!!.contains("Expected class eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat"))
    }
}

