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

import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UnsignedDocumentTest {
    private lateinit var documentManager: DocumentManagerImpl
    private lateinit var secureArea: SecureArea
    private lateinit var secureAreaRepository: SecureAreaRepository
    private lateinit var unsignedDocument: UnsignedDocument

    @BeforeTest
    fun setUp() {
        val storage = EphemeralStorage()
        secureArea = runBlocking { SoftwareSecureArea.create(storage) }
        secureAreaRepository = SecureAreaRepository.Builder()
            .add(secureArea)
            .build()
        documentManager = DocumentManagerImpl(
            identifier = "test_document_manager",
            storage = storage,
            secureAreaRepository = secureAreaRepository,
        )

        // Set up with real document creation or mock based on testing needs
        setupWithRealDocument()
        // Alternatively, we could use: setupWithMockDocument()
    }

    private fun setupWithRealDocument() {
        val createKeySettings = SoftwareCreateKeySettings.Builder().build()

        val createDocumentResult = documentManager.createDocument(
            format = MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1"),
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = secureArea.identifier,
                createKeySettings = createKeySettings,
            ),
            issuerMetadata = null
        )

        assertTrue(createDocumentResult.isSuccess)
        unsignedDocument = createDocumentResult.getOrThrow()
        unsignedDocument.name = "Test Document"
    }

    @AfterTest
    fun tearDown() {
        // Clean up any documents created during tests
        runBlocking {
            documentManager.getDocuments().forEach {
                documentManager.deleteDocumentById(it.id)
            }
        }
    }

    @Test
    fun `test document properties`() {
        // Basic document properties should be accessible
        assertNotNull(unsignedDocument.id)
        assertEquals("Test Document", unsignedDocument.name)
        assertIs<MsoMdocFormat>(unsignedDocument.format)
        assertEquals("test_document_manager", unsignedDocument.documentManagerId)
        assertNotNull(unsignedDocument.createdAt)
        assertFalse(unsignedDocument.isCertified)
    }

    @Test
    fun `test key information access`() {
        // Should be able to access key information
        runBlocking {
            assertNotNull(unsignedDocument.keyInfo)
            assertNotNull(unsignedDocument.keyAlias)
            assertNotNull(unsignedDocument.publicKeyCoseBytes)
            assertFalse(unsignedDocument.isKeyInvalidated)
        }
    }

    @Test
    fun `test credentials count`() {
        // Should return the correct number of credentials
        runBlocking {
            assertEquals(1, unsignedDocument.credentialsCount())
        }
    }

    @Test
    fun `test get proof of possession signers`() {
        // Should return valid POP signers
        runBlocking {
            val popSigners = unsignedDocument.getPoPSigners()
            assertNotNull(popSigners)
            assertTrue(popSigners.isNotEmpty())

            val firstSigner = popSigners.first()
            assertNotNull(firstSigner)
            assertEquals(unsignedDocument.keyAlias, firstSigner.keyAlias)
            assertEquals(unsignedDocument.secureArea, firstSigner.secureArea)

            val keyInfo = firstSigner.getKeyInfo()
            assertNotNull(keyInfo)
        }
    }

    @Test
    fun `test proof of possession signing`() {
        // Should be able to sign data with the POP signer
        runBlocking {
            val popSigners = unsignedDocument.getPoPSigners()
            val signer = popSigners.first()

            val dataToSign = "test data".toByteArray()
            val signature = signer.signPoP(dataToSign, null)

            assertNotNull(signature)
            // In a real test, we could verify the signature against the public key
        }
    }
}
