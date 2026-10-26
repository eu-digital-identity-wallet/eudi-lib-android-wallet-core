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

import android.util.Log
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.sample.generateData
import eu.europa.ec.eudi.wallet.document.sample.generateMso
import eu.europa.ec.eudi.wallet.document.sample.issuerCertificate
import eu.europa.ec.eudi.wallet.document.sample.issuerPrivateKey
import eu.europa.ec.eudi.wallet.document.sample.signMso
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.SignatureVerificationException
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
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

    @Test
    fun `should return failure result when public keys of document and mso don't match`() {
        val unsignedDocument = createPidDocument()
        val otherDeviceKey = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256).publicKey }
        val issuerProvidedCredentials = issuerProvidedCredentials(
            unsignedDocument = unsignedDocument,
            msoDocType = PID_DOC_TYPE,
            msoNameSpaces = pidNameSpaces("Doe"),
            issuedNameSpaces = pidNameSpaces("Doe"),
            msoDeviceKey = otherDeviceKey,
        )

        val storeDocumentResult =
            documentManager.storeIssuedDocument(unsignedDocument, issuerProvidedCredentials)

        assertTrue(storeDocumentResult.isFailure)
        assertContains(storeDocumentResult.exceptionOrNull()?.message.orEmpty(), "Public key in MSO")
    }

    @Test
    fun `should return failure result when the mso signature does not verify with the x5chain certificate`() {
        val unsignedDocument = createPidDocument()
        val issuerProvidedCredentials = issuerProvidedCredentials(
            unsignedDocument = unsignedDocument,
            msoDocType = PID_DOC_TYPE,
            msoNameSpaces = pidNameSpaces("Doe"),
            issuedNameSpaces = pidNameSpaces("Doe"),
            msoSigningKey = otherIssuerKey(),
        )

        val storeDocumentResult =
            documentManager.storeIssuedDocument(unsignedDocument, issuerProvidedCredentials)

        assertTrue(storeDocumentResult.isFailure)
        assertIs<SignatureVerificationException>(storeDocumentResult.exceptionOrNull())
    }

    @Test
    fun `should store an sd-jwt vc whose signature verifies with the x5c certificate`() {
        val unsignedDocument = createSdJwtVcDocument()

        val storeDocumentResult = documentManager.storeIssuedDocument(
            unsignedDocument,
            sdJwtVcCredentials(unsignedDocument, signingKey = issuerPrivateKey),
        )

        assertTrue(storeDocumentResult.isSuccess, "${storeDocumentResult.exceptionOrNull()}")
    }

    @Test
    fun `should return failure result when the sd-jwt vc signature does not verify with the x5c certificate`() {
        val unsignedDocument = createSdJwtVcDocument()

        val storeDocumentResult = documentManager.storeIssuedDocument(
            unsignedDocument,
            sdJwtVcCredentials(unsignedDocument, signingKey = otherIssuerKey()),
        )

        assertTrue(storeDocumentResult.isFailure)
        assertIs<SignatureVerificationException>(storeDocumentResult.exceptionOrNull())
    }

    @Test
    fun `should return failure result when a data element does not match its digest in the mso`() {
        val unsignedDocument = createPidDocument()
        val issuerProvidedCredentials = issuerProvidedCredentials(
            unsignedDocument = unsignedDocument,
            msoDocType = PID_DOC_TYPE,
            msoNameSpaces = pidNameSpaces("Doe"),
            issuedNameSpaces = pidNameSpaces("Roe"),
        )

        val storeDocumentResult =
            documentManager.storeIssuedDocument(unsignedDocument, issuerProvidedCredentials)

        assertTrue(storeDocumentResult.isFailure)
        assertContains(storeDocumentResult.exceptionOrNull()?.message.orEmpty(), "Digest mismatch")
    }

    @Test
    fun `should return failure result when the docType in the mso does not match the document`() {
        val unsignedDocument = createPidDocument()
        val issuerProvidedCredentials = issuerProvidedCredentials(
            unsignedDocument = unsignedDocument,
            msoDocType = "org.iso.18013.5.1.mDL",
            msoNameSpaces = pidNameSpaces("Doe"),
            issuedNameSpaces = pidNameSpaces("Doe"),
        )

        val storeDocumentResult =
            documentManager.storeIssuedDocument(unsignedDocument, issuerProvidedCredentials)

        assertTrue(storeDocumentResult.isFailure)
        assertContains(storeDocumentResult.exceptionOrNull()?.message.orEmpty(), "DocType in MSO")
    }

    /**
     * Builds issuer-provided credentials whose MSO is signed over [msoNameSpaces] and [msoDocType],
     * while the credential carries [issuedNameSpaces]. The MSO binds [msoDeviceKey], or the key of
     * the credential when null, and is signed with [msoSigningKey].
     */
    private fun issuerProvidedCredentials(
        unsignedDocument: UnsignedDocument,
        msoDocType: String,
        msoNameSpaces: CBORObject,
        issuedNameSpaces: CBORObject,
        msoDeviceKey: EcPublicKey? = null,
        msoSigningKey: PrivateKey = issuerPrivateKey,
    ): List<IssuerProvidedCredential> {
        // The MSO generator logs through android.util.Log, which is not available in unit tests
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        try {
            return runBlocking {
                unsignedDocument.getPoPSigners().map { signer ->
                    val keyInfo = signer.getKeyInfo()
                    val deviceKey = msoDeviceKey ?: keyInfo.publicKey
                    val mso = generateMso("sha-256", msoDocType, deviceKey, msoNameSpaces)
                    IssuerProvidedCredential(
                        publicKeyAlias = keyInfo.alias,
                        data = generateData(issuedNameSpaces, signMso(mso, msoSigningKey)),
                    )
                }
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    /**
     * Builds issuer-provided SD-JWT VCs bound to the keys of [unsignedDocument], signed with
     * [signingKey] and carrying the sample issuer certificate in their x5c header.
     */
    private fun sdJwtVcCredentials(
        unsignedDocument: UnsignedDocument,
        signingKey: PrivateKey,
    ): List<IssuerProvidedCredential> = runBlocking {
        unsignedDocument.getPoPSigners().map { signer ->
            val keyInfo = signer.getKeyInfo()
            val header = JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType("dc+sd-jwt"))
                .x509CertChain(listOf(Base64.encode(issuerCertificate.encoded)))
                .build()
            val claims = JWTClaimsSet.Builder()
                .issuer("https://issuer.example")
                .claim("vct", SD_JWT_VCT)
                .claim("cnf", mapOf("jwk" to JWK.parse(keyInfo.publicKey.toJwk().toString()).toJSONObject()))
                .build()
            val jwt = SignedJWT(header, claims).apply { sign(ECDSASigner(signingKey as ECPrivateKey)) }
            IssuerProvidedCredential(
                publicKeyAlias = keyInfo.alias,
                data = "${jwt.serialize()}~".encodeToByteArray(),
            )
        }
    }

    /** Returns a key of the curve of the sample issuer key, which its certificate does not certify. */
    private fun otherIssuerKey(): PrivateKey =
        KeyPairGenerator.getInstance("EC")
            .apply { initialize((issuerPrivateKey as ECPrivateKey).params) }
            .generateKeyPair()
            .private

    private fun createSdJwtVcDocument(): UnsignedDocument =
        documentManager.createDocument(
            format = SdJwtVcFormat(vct = SD_JWT_VCT),
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = secureArea.identifier,
                createKeySettings = SoftwareCreateKeySettings.Builder().build(),
            ),
        ).getOrThrow()

    private fun createPidDocument(manager: DocumentManagerImpl = documentManager): UnsignedDocument =
        manager.createDocument(
            format = MsoMdocFormat(docType = PID_DOC_TYPE),
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = secureArea.identifier,
                createKeySettings = SoftwareCreateKeySettings.Builder().build(),
            ),
        ).getOrThrow()

    private fun pidNameSpaces(familyName: String): CBORObject {
        val issuerSignedItem = CBORObject.NewOrderedMap()
            .Add("digestID", 0)
            .Add("random", ByteArray(16) { 1 })
            .Add("elementIdentifier", "family_name")
            .Add("elementValue", familyName)
        return CBORObject.NewMap().Add(
            PID_DOC_TYPE,
            CBORObject.NewArray().Add(CBORObject.FromObjectAndTag(issuerSignedItem.EncodeToBytes(), 24)),
        )
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

    @Test
    fun `should not delete a document managed by another document manager`() {
        val sharedStorage = EphemeralStorage()
        val document = createPidDocument(managerOver("owner", sharedStorage))

        val result = managerOver("other", sharedStorage).deleteDocumentById(document.id)

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertNotNull(managerOver("owner", sharedStorage).getDocumentById(document.id))
    }

    @Test
    fun `should report a document whose metadata cannot be read as not found when deleting`() {
        val sharedStorage = EphemeralStorage()
        val document = runBlocking { managerOver("owner", sharedStorage).documentStore.createDocument() }

        val result = managerOver("other", sharedStorage).deleteDocumentById(document.identifier)

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `should not store as issued a document managed by another document manager`() {
        val sharedStorage = EphemeralStorage()
        val document = createPidDocument(managerOver("owner", sharedStorage))
        val credentials = issuerProvidedCredentials(
            unsignedDocument = document,
            msoDocType = PID_DOC_TYPE,
            msoNameSpaces = pidNameSpaces("Doe"),
            issuedNameSpaces = pidNameSpaces("Doe"),
        )

        val result = managerOver("other", sharedStorage).storeIssuedDocument(document, credentials)

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        val stored = assertNotNull(managerOver("owner", sharedStorage).getDocumentById(document.id))
        assertFalse(stored is IssuedDocument)
    }

    @Test
    fun `should not store for deferred issuance a document managed by another document manager`() {
        val sharedStorage = EphemeralStorage()
        val document = createPidDocument(managerOver("owner", sharedStorage))

        val result = managerOver("other", sharedStorage)
            .storeDeferredDocument(document, "related data".encodeToByteArray())

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        val stored = assertNotNull(managerOver("owner", sharedStorage).getDocumentById(document.id))
        assertFalse(stored is DeferredDocument)
    }

    private fun managerOver(identifier: String, storage: Storage) = DocumentManagerImpl(
        identifier = identifier,
        storage = storage,
        secureAreaRepository = secureAreaRepository,
    )

    private companion object {
        private const val PID_DOC_TYPE = "eu.europa.ec.eudi.pid.1"
        private const val SD_JWT_VCT = "urn:eudi:pid:1"
    }
}
