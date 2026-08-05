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

package eu.europa.ec.eudi.wallet.document.internal

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApplicationMetadataTest {

    private val testDocumentId = "test-document-id"
    private val testDocumentManagerId = "test-manager-id"
    private val testFormat = MsoMdocFormat("test-doc-type")
    private val testSdJwtVcFormat = SdJwtVcFormat("test-vct")
    private val testKeyAttestation = "test-key-attestation"

    @Test
    fun `create factory method returns ApplicationMetaData instance`() {
        val metadata = ApplicationMetadata.create(
            documentId = testDocumentId,
            serializedData = null,
        )

        assertNotNull(metadata)
    }

    @Test
    fun `format property throws exception when not set`() {
        val metadata = ApplicationMetadata.create(
            documentId = testDocumentId,
            serializedData = null,
        )

        assertFailsWith<IllegalStateException> {
            metadata.format
        }
    }

    @Test
    fun `documentManagerId property throws exception when not set`() {
        val metadata = ApplicationMetadata.create(
            documentId = testDocumentId,
            serializedData = null,
        )

        assertFailsWith<IllegalStateException> {
            metadata.documentManagerId
        }
    }

    @Test
    fun `initialCredentialsCount property throws exception when not set`() {
        val metadata = ApplicationMetadata.create(
            documentId = testDocumentId,
            serializedData = null,
        )

        assertFailsWith<IllegalStateException> {
            metadata.initialCredentialsCount
        }
    }

    @Test
    fun `create with full parameters sets all core properties`() {
        val issuerMetadataJson = """
        {
            "documentConfigurationIdentifier": "test-doc-config",
            "display": [
                {
                    "name": "Test Document",
                    "locale": "en-US"
                }
            ],
            "claims": null,
            "credentialIssuerIdentifier": "test-issuer",
            "issuerDisplay": [
                {
                    "name": "Test Issuer",
                    "locale": "en-US"
                }
            ]
        }
        """.trimIndent()
        val issuerMetadata = IssuerMetadata.fromJson(issuerMetadataJson).getOrThrow()

        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
            issuerMetadata = issuerMetadata,
            keyAttestation = testKeyAttestation,
        )

        assertEquals(testFormat, metadata.format)
        assertEquals(testDocumentManagerId, metadata.documentManagerId)
        assertEquals(issuerMetadata, metadata.issuerMetadata)
        assertEquals(CreateDocumentSettings.CredentialPolicy.RotatingBatch(), metadata.credentialPolicy)
        assertEquals(testKeyAttestation, metadata.keyAttestation)
    }

    @Test
    fun `create with SdJwtVcFormat sets correct format`() {
        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testSdJwtVcFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        )

        assertEquals(testSdJwtVcFormat, metadata.format)
        assertEquals(testDocumentManagerId, metadata.documentManagerId)
    }

    @Test
    fun `create with minimal parameters uses defaults`() {
        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        )

        assertEquals(testFormat, metadata.format)
        assertEquals(testDocumentManagerId, metadata.documentManagerId)
        assertEquals(1, metadata.initialCredentialsCount)
        assertNull(metadata.issuerMetadata)
        assertNull(metadata.keyAttestation)
        assertEquals(CreateDocumentSettings.CredentialPolicy.RotatingBatch(), metadata.credentialPolicy)
    }

    @Test
    fun `issue sets issuerProvidedData and issuedAt`() {
        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        )

        val testData = ByteString("test-data".toByteArray())
        metadata.issue(testData)

        assertNotNull(metadata.issuerProvidedData)
        assertEquals("test-data", metadata.issuerProvidedData?.let { String(it) })
        assertNotNull(metadata.issuedAt)
    }

    @Test
    fun `issue clears deferredRelatedData`() {
        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        )

        metadata.issueDeferred(ByteString("deferred".toByteArray()))
        assertNotNull(metadata.deferredRelatedData)

        metadata.issue(ByteString("issued".toByteArray()))
        assertNull(metadata.deferredRelatedData)
    }

    @Test
    fun `issueDeferred sets deferredRelatedData`() {
        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        )

        val testData = ByteString("deferred-data".toByteArray())
        metadata.issueDeferred(testData)

        assertNotNull(metadata.deferredRelatedData)
        assertEquals("deferred-data", metadata.deferredRelatedData?.let { String(it) })
    }

    @Test
    fun `setKeyAttestation updates keyAttestation`() {
        val keyAttestation = "test-attestation"

        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        )

        metadata.setKeyAttestation(keyAttestation)

        assertEquals(keyAttestation, metadata.keyAttestation)
    }

    @Test
    fun `serialization and deserialization preserves all metadata`() {
        val metadata = ApplicationMetadataImpl.create(
            documentManagerId = testDocumentManagerId,
            format = testFormat,
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
            keyAttestation = testKeyAttestation,
        )

        val testData = ByteString("test-data".toByteArray())
        metadata.issue(testData)

        // Serialize and deserialize
        val serialized = metadata.serialize()
        val restored = ApplicationMetadata.create(
            documentId = testDocumentId,
            serializedData = serialized,
        )

        assertEquals(testFormat, restored.format)
        assertEquals(testDocumentManagerId, restored.documentManagerId)
        assertEquals(CreateDocumentSettings.CredentialPolicy.RotatingBatch(), restored.credentialPolicy)
        assertEquals(testKeyAttestation, restored.keyAttestation)
        assertNotNull(restored.issuerProvidedData)
        assertEquals("test-data", restored.issuerProvidedData?.let { String(it) })
        assertNotNull(restored.issuedAt)
    }
}
