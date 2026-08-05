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

import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.multipaz.cbor.Cbor
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentManagerImplTest {

    lateinit var documentManager: DocumentManagerImpl
    lateinit var storage: Storage
    lateinit var secureArea: SecureArea
    lateinit var secureAreaRepository: SecureAreaRepository

    @BeforeTest
    fun setUp() {
        storage = EphemeralStorage()
        secureArea = runBlocking { SoftwareSecureArea.create(storage) }
        secureAreaRepository = runBlocking {
            SecureAreaRepository.Builder().apply {
                add(SoftwareSecureArea.create(storage))
            }.build()
        }
        documentManager = DocumentManagerImpl(
            identifier = "document_manager",
            storage = EphemeralStorage(),
            secureAreaRepository = secureAreaRepository,
        )
    }

    @AfterTest
    fun tearDown() {
        documentManager.getDocuments().forEach { documentManager.deleteDocumentById(it.id) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun nameSpacedData() {
        val issuerData = getResourceAsText("eu_pid.hex").hexToByteArray(HexFormat.Default)

        Cbor.decode(issuerData)
    }

    @Test
    fun `should return failure result when document is not found when storing issued document`() {
        val mockDocument = mockk<UnsignedDocument>(relaxed = true) { // Added relaxed = true
            every { id } returns "non_existent_document_id_123"
        }
        // issuerProvidedData needs to be List<IssuerProvidedCredential>
        val dummyIssuerProvidedData = listOf(
            IssuerProvidedCredential("dummyAlias", byteArrayOf(0x01, 0x02))
        )
        val storeDocumentResult = documentManager.storeIssuedDocument(
            unsignedDocument = mockDocument,
            issuerProvidedData = dummyIssuerProvidedData
        )
        assertTrue(storeDocumentResult.isFailure)
        // The original test expected IllegalArgumentException. This might vary based on implementation.
        // If the document is not found in an internal map/list by its ID,
        // NoSuchElementException or a custom domain exception might also be plausible.
        // Sticking to IllegalArgumentException as per the original commented test's intent.
        assertIs<IllegalArgumentException>(storeDocumentResult.exceptionOrNull())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `should return failure result when public keys of document and mso don't match`() {
        val createKeySettings = SoftwareCreateKeySettings.Builder().build()
        val createDocumentSettings = CreateDocumentSettings(
            secureAreaIdentifier = secureArea.identifier,
            createKeySettings = createKeySettings,

        )
        val createDocumentResult = documentManager.createDocument(
            format = MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1"),
            createSettings = createDocumentSettings
        )
        assertTrue(createDocumentResult.isSuccess)
        val unsignedDocument = createDocumentResult.getOrThrow()

        // Use issuer data that is known to not match the document's generated key.
        // "eu_pid.hex" is used here, assuming it contains a key different from the dynamically generated one.
        val mismatchedIssuerDataBytes =
            getResourceAsText("eu_pid.hex").hexToByteArray(HexFormat.Default)

        // Construct IssuerProvidedCredential list using the alias from the unsignedDocument
        // but with the mismatched MSO data.
        val issuerProvidedCredentials = runBlocking {
            unsignedDocument.baseDocument.getPendingCredentials()
                .filterIsInstance<SecureAreaBoundCredential>()
                .map {
                    IssuerProvidedCredential(
                        publicKeyAlias = it.alias,
                        data = mismatchedIssuerDataBytes
                    )
                }
        }

        // Ensure there's at least one credential to provide, otherwise the test might not hit the intended logic.
        assertTrue(
            issuerProvidedCredentials.isNotEmpty(),
            "Document should have pending credentials to test MSO mismatch."
        )

        val storeDocumentResult =
            documentManager.storeIssuedDocument(unsignedDocument, issuerProvidedCredentials)
        assertTrue(storeDocumentResult.isFailure)
        // The specific exception for a key mismatch might be more specific,
        // but IllegalArgumentException is a common fallback.
        assertIs<IllegalArgumentException>(storeDocumentResult.exceptionOrNull())
    }

    @Test
    fun `should create document with null issuerMetaData if not provided`() {
        val createKeySettings = SoftwareCreateKeySettings.Builder().build()
        val createSettings = CreateDocumentSettings(
            secureAreaIdentifier = secureArea.identifier,
            createKeySettings = createKeySettings,

        )
        val result = documentManager.createDocument(
            format = MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1"),
            createSettings = createSettings
            // Assuming issuerMetadata is not passed here and UnsignedDocument.issuerMetadata defaults to null
        )

        assertTrue(result.isSuccess)
        val document = result.getOrThrow()
        // This assertion assumes UnsignedDocument has an 'issuerMetadata' property that is nullable
        // and defaults to null or is not set by the createDocument method without explicit input.
        assertNull(document.issuerMetadata)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `should store deferred document successfully`() {
        // Prepare an unsigned document first
        val createKeySettings = SoftwareCreateKeySettings.Builder().build()
        val createDocumentSettings = CreateDocumentSettings(
            secureAreaIdentifier = secureArea.identifier,
            createKeySettings = createKeySettings
        )
        val createDocumentResult = documentManager.createDocument(
            format = MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1"),
            createSettings = createDocumentSettings
        )
        assertTrue(createDocumentResult.isSuccess)
        val unsignedDocument = createDocumentResult.getOrThrow()
        unsignedDocument.name = "Deferred Document Test"

        // Sample related data for deferred issuance
        val deferredRelatedData = "Sample deferred issuance data".encodeToByteArray()

        // Store the document for deferred issuance
        val storeDeferredResult = documentManager.storeDeferredDocument(
            unsignedDocument = unsignedDocument,
            relatedData = deferredRelatedData
        )

        // Validate result
        assertTrue(storeDeferredResult.isSuccess)
        val deferredDocument = storeDeferredResult.getOrThrow()

        // Verify properties of the deferred document
        assertEquals(unsignedDocument.id, deferredDocument.id)
        assertEquals(unsignedDocument.name, deferredDocument.name)
        assertEquals(documentManager.identifier, deferredDocument.documentManagerId)

        // Verify document is in deferred state
        assertFalse(deferredDocument.isCertified)

        // Verify we can retrieve the document from the manager
        val retrievedDocument = documentManager.getDocumentById(deferredDocument.id)
        assertNotNull(retrievedDocument)
        assertIs<DeferredDocument>(retrievedDocument)
    }

    @Test
    fun `should return failure when document not found for deferred issuance`() {
        val mockDocument = mockk<UnsignedDocument>(relaxed = true) {
            every { id } returns "non_existent_document_id_456"
        }

        // Sample related data
        val dummyRelatedData = "Test related data".toByteArray()

        // Attempt to store a deferred document with non-existent ID
        val storeDeferredResult = documentManager.storeDeferredDocument(
            unsignedDocument = mockDocument,
            relatedData = dummyRelatedData
        )

        // Validate failure
        assertTrue(storeDeferredResult.isFailure)
        assertIs<IllegalArgumentException>(storeDeferredResult.exceptionOrNull())
    }
}
