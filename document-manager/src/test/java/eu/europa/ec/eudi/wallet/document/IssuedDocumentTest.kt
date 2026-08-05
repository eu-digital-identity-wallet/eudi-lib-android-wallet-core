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

import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.internal.ApplicationMetadata
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.EcSignature
import org.multipaz.document.Document
import org.multipaz.prompt.Reason
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class IssuedDocumentTest {

    // Helper methods for creating common mocks

    /**
     * Creates a mock SecureArea that returns the specified invalidated state for keys
     */
    private fun createMockSecureArea(isKeyInvalidated: Boolean = false): SecureArea {
        return mockk<SecureArea> {
            coEvery { getKeyInvalidated(any()) } returns isKeyInvalidated
        }
    }

    /**
     * Creates a mock credential with the specified properties
     */
    private fun createMockCredential(
        alias: String,
        domain: String = "test-document-manager-id",
        isCertified: Boolean = true,
        usageCount: Int = 0,
        now: kotlinx.datetime.Instant = Clock.System.now(),
        isInvalidated: Boolean = false,
        validFrom: kotlinx.datetime.Instant = now.minus(1.days),
        validUntil: kotlinx.datetime.Instant = now.plus(1.days),
    ): SecureAreaBoundCredential {
        val mockSecureArea = createMockSecureArea(isInvalidated)

        return mockk<SecureAreaBoundCredential>(relaxed = true) {
            every { this@mockk.alias } returns alias
            every { this@mockk.domain } returns domain
            every { this@mockk.isCertified } returns isCertified
            every { this@mockk.usageCount } returns usageCount
            every { this@mockk.validFrom } returns validFrom
            every { this@mockk.validUntil } returns validUntil
            every { this@mockk.secureArea } returns mockSecureArea
        }
    }

    /**
     * Creates a mock Document with configurable metadata and credentials
     */
    private fun createMockBaseDocument(
        format: DocumentFormat = mockk(),
        id: String = "test-document-id",
        name: String = "test-document-name",
        documentManagerId: String = "test-document-manager-id",
        createdAt: Instant = Instant.now(),
        issuedAt: Instant = Instant.now(),
        credentialPolicy: CreateDocumentSettings.CredentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
        issuerMetaData: IssuerMetadata? = mockk(),
        initialCredentialsCount: Int = 1,
        certifiedCredentials: List<SecureAreaBoundCredential> = emptyList(),
        pendingCredentials: List<SecureAreaBoundCredential> = emptyList()
    ): Document {
        return mockk<Document> {
            every { identifier } returns id
            every { displayName } returns name
            every { created } returns createdAt.toKotlinInstant()
            coEvery { getCredentials() } returns certifiedCredentials
            coEvery { getCertifiedCredentials() } returns certifiedCredentials
            coEvery { getPendingCredentials() } returns pendingCredentials
            every { metadata } returns mockk<ApplicationMetadata> {
                every { this@mockk.format } returns format
                every { this@mockk.documentManagerId } returns documentManagerId
                every { this@mockk.issuedAt } returns issuedAt.toKotlinInstant()
                every { this@mockk.credentialPolicy } returns credentialPolicy
                every { this@mockk.issuerMetadata } returns issuerMetaData
                every { this@mockk.initialCredentialsCount } returns initialCredentialsCount
            }
        }
    }

    /**
     * Creates a mock IssuedDocument with the specified properties
     */
    private fun createMockIssuedDocument(
        baseDocument: Document = createMockBaseDocument()
    ): IssuedDocument {
        return IssuedDocument(baseDocument)
    }

    @Test
    fun `test document format property returns expected value`() {
        val expectedFormat = MsoMdocFormat(docType = "test-doc-type")
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(format = expectedFormat)
        )
        assert(issuedDocument.format == expectedFormat)
    }

    @Test
    fun `test document id property returns expected value`() {
        val expectedIdentifier = "test-document-id"
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(id = expectedIdentifier)
        )
        assert(issuedDocument.id == expectedIdentifier)
    }

    @Test
    fun `test name property returns expected value`() {
        val expectedName = "test-document-name"
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(name = expectedName)
        )
        assert(issuedDocument.name == expectedName)
    }

    @Test
    fun `test documentManagerId property returns expected value`() {
        val expectedDocumentManagerId = "test-document-manager-id"
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(documentManagerId = expectedDocumentManagerId)
        )
        assert(issuedDocument.documentManagerId == expectedDocumentManagerId)
    }

    @Test
    fun `test createdAt property returns expected value`() {
        val expectedCreatedAt = Instant.now()
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(createdAt = expectedCreatedAt)
        )
        assert(issuedDocument.createdAt == expectedCreatedAt)
    }

    @Test
    fun `test issuedAt property returns expected value`() {
        val expectedIssuedAt = Instant.now()
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(issuedAt = expectedIssuedAt)
        )
        assert(issuedDocument.issuedAt == expectedIssuedAt)
    }

    @Test
    fun `test issuerMetaData property returns expected value`() {
        val expectedIssuerMetadata = mockk<IssuerMetadata>()
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(issuerMetaData = expectedIssuerMetadata)
        )
        assert(issuedDocument.issuerMetadata == expectedIssuerMetadata)
    }

    @Test
    fun `test credentialPolicy returns expected policy`() {
        val expectedPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch()
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(credentialPolicy = expectedPolicy)
        )
        assert(issuedDocument.credentialPolicy == expectedPolicy)
    }

    // Credential management tests
    @Test
    fun `test getCredentials returns all secure area bound credentials`() = runTest {
        val mockCredential = createMockCredential("test-alias")
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = listOf(mockCredential)
            )
        )
        assertContentEquals(listOf(mockCredential), issuedDocument.getCredentials())
    }

    @Test
    fun `test findCredential returns valid credential`() = runTest {
        val now = Clock.System.now()
        val documentManagerId = "test-document-manager-id"

        val credential1 = createMockCredential(
            alias = "alias1",
            domain = documentManagerId,
            usageCount = 1,
            now = now
        )

        val credential2 = createMockCredential(
            alias = "alias2",
            domain = documentManagerId,
            usageCount = 0,
            now = now
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                documentManagerId = documentManagerId,
                certifiedCredentials = listOf(credential1, credential2)
            )
        )

        assert(issuedDocument.findCredential() == credential2)
    }

    @Test
    fun `test findCredential with time parameter returns credential valid at that time`() =
        runTest {
            val documentManagerId = "test-document-manager-id"
            val now = Clock.System.now()

            val credential1 = createMockCredential(
                alias = "alias1",
                domain = documentManagerId,
                validFrom = now.minus(2.days),
                validUntil = now.minus(1.days)
            )

            val credential2 = createMockCredential(
                alias = "alias2",
                domain = documentManagerId,
                validFrom = now.minus(1.days),
                validUntil = now.plus(1.days)
            )

            val issuedDocument = createMockIssuedDocument(
                baseDocument = createMockBaseDocument(
                    documentManagerId = documentManagerId,
                    certifiedCredentials = listOf(credential1, credential2)
                )
            )

            assert(issuedDocument.findCredential() == credential2)
        }

    @Test
    fun `test findCredential returns null when no valid credential exists`() = runTest {
        val documentManagerId = "test-document-manager-id"
        val now = Clock.System.now()

        val credential1 = createMockCredential(
            alias = "alias1",
            domain = documentManagerId,
            validFrom = now.minus(2.days),
            validUntil = now.minus(1.days)
        )

        val credential2 = createMockCredential(
            alias = "alias2",
            domain = documentManagerId,
            isCertified = false,
            validFrom = now.minus(1.days),
            validUntil = now.plus(1.days)
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                documentManagerId = documentManagerId,
                certifiedCredentials = listOf(credential1),
                pendingCredentials = listOf(credential2)
            )
        )

        assertNull(issuedDocument.findCredential())
    }

    @Test
    fun `test findCredential with OnceOnly policy only returns unused credentials`() = runTest {
        val documentManagerId = "test-document-manager-id"

        val credential1 = createMockCredential(
            alias = "alias1",
            domain = documentManagerId,
            usageCount = 1
        )

        val credential2 = createMockCredential(
            alias = "alias2",
            domain = documentManagerId,
            usageCount = 0
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                documentManagerId = documentManagerId,
                credentialPolicy = CreateDocumentSettings.CredentialPolicy.OnceOnly(),
                certifiedCredentials = listOf(credential1, credential2)
            )
        )

        assert(issuedDocument.findCredential() == credential2)
    }

    @Test
    fun `test findCredential with RotatingBatch policy returns credential with lowest usage count`() =
        runTest {
            val documentManagerId = "test-document-manager-id"

            val credential1 = createMockCredential(
                alias = "alias1",
                domain = documentManagerId,
                usageCount = 1
            )

            val credential2 = createMockCredential(
                alias = "alias2",
                domain = documentManagerId,
                usageCount = 0
            )

            val issuedDocument = createMockIssuedDocument(
                baseDocument = createMockBaseDocument(
                    documentManagerId = documentManagerId,
                    credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
                    certifiedCredentials = listOf(credential1, credential2)
                )
            )

            assert(issuedDocument.findCredential() == credential2)
        }

    // Document validity tests
    @Test
    fun `test isValidAt returns true when valid credential exists at given time`() = runTest {
        val now = Clock.System.now()

        val credential = createMockCredential(
            alias = "test-alias",
            validFrom = now.minus(1.days),
            validUntil = now.plus(1.days)
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = listOf(credential)
            )
        )

        assert(issuedDocument.isValidAt(now.toJavaInstant()))
    }

    @Test
    fun `test isValidAt returns false when no valid credential exists at given time`() = runTest {
        val now = Instant.now()
        val javaInstantNow = now
        val kotlinInstantNow = now.toKotlinInstant()

        val credential = createMockCredential(
            alias = "test-alias",
            validFrom = kotlinInstantNow.plus(1.days),
            validUntil = kotlinInstantNow.plus(2.days)
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = listOf(credential)
            )
        )

        assert(!issuedDocument.isValidAt(now))
    }

    @Test
    fun `test isCertified returns true when no pending credentials exist`() = runTest {
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                pendingCredentials = emptyList()
            )
        )

        assert(issuedDocument.isCertified())
    }

    @Test
    fun `test isCertified returns false when pending credentials exist`() = runTest {
        val credential = createMockCredential(
            alias = "test-alias"
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                pendingCredentials = listOf(credential)
            )
        )

        assert(!issuedDocument.isCertified())
    }

    // Data retrieval tests
    @Test
    fun `test credentialsCount returns correct number of certified credentials`() = runTest {
        val credentials = listOf(
            createMockCredential("alias1"),
            createMockCredential("alias2"),
            createMockCredential("alias3")
        )

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = credentials
            )
        )

        assert(issuedDocument.credentialsCount() == 3)
    }

    @Test
    fun `test initialCredentialsCount returns correct number of initial credentials`() = runTest {
        // Mock the base document with a specific initial credential count
        val baseDocument = createMockBaseDocument(
            initialCredentialsCount = 5
        )

        val issuedDocument = createMockIssuedDocument(baseDocument)

        // Assert that initialCredentialsCount returns the expected value
        assert(issuedDocument.initialCredentialsCount() == 5)
    }

    // consumingCredential method tests
    @Test
    fun `test consumingCredential executes provided block with valid credential`() = runTest {
        // Create a test credential
        val credential = createMockCredential(
            alias = "test-credential",
            usageCount = 0
        )

        // Create the document with RotatingBatch policy to keep the credential after use
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
                certifiedCredentials = listOf(credential)
            )
        )

        // Execute a simple block that returns the credential alias
        val result = issuedDocument.consumingCredential {
            // This block should be executed with the credential as receiver
            this.alias
        }

        // Assert the operation succeeded and returned the expected value
        assert(result.isSuccess)
        assert(result.getOrNull() == "test-credential")
    }

    @Test
    fun `test consumingCredential returns failure when no valid credential found`() = runTest {
        // Create a document with no valid credentials
        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = emptyList()
            )
        )

        // Execute a simple block that shouldn't run
        val result = issuedDocument.consumingCredential {
            "This should not be returned"
        }

        // Assert the operation failed with the expected error
        assert(result.isFailure)
        assert(result.exceptionOrNull() is IllegalStateException)
        assert(result.exceptionOrNull()?.message == "Credential not found")
    }

    @Test
    fun `test consumingCredential applies RotatingBatch policy correctly`() = runTest {
        // Setup - Create a mock credential and a helper to track interactions
        val credential = createMockCredential(
            alias = "rotate-credential",
            usageCount = 0
        )

        // Create a mock document that can track credential updates
        val baseDocument = createMockBaseDocument(
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
            certifiedCredentials = listOf(credential)
        )


        val issuedDocument = createMockIssuedDocument(
            baseDocument = baseDocument
        )

        // Act - Execute a block that returns a test value
        val result = issuedDocument.consumingCredential {
            "test-result"
        }

        // Assert - The operation succeeded and policy was applied
        assert(result.isSuccess)
        assert(result.getOrNull() == "test-result")
        coVerify(exactly = 1) { credential.increaseUsageCount() }
    }

    @Test
    fun `test consumingCredential applies OnceOnly policy correctly`() = runTest {
        // Setup - Create a mock credential and a helper to track interactions
        val credential = createMockCredential(
            alias = "onetime-credential",
            usageCount = 0
        )

        // Create a mock document that can track credential deletions
        val baseDocument = createMockBaseDocument(
            credentialPolicy = CreateDocumentSettings.CredentialPolicy.OnceOnly(),
            certifiedCredentials = listOf(credential)
        )

        // Setup a counter to track if the policy was applied
        var policyApplied = false

        // Override our mocks to track policy application
        coEvery {
            baseDocument.deleteCredential(any())
        } answers {
            policyApplied = true
        }

        val issuedDocument = createMockIssuedDocument(
            baseDocument = baseDocument
        )

        // Act - Execute a block that returns a test value
        val result = issuedDocument.consumingCredential {
            "test-result"
        }

        // Assert - The operation succeeded and policy was applied
        assert(result.isSuccess)
        assert(result.getOrNull() == "test-result")
        assert(policyApplied) { "OnceOnly policy wasn't applied" }
    }

    // Add additional tests for signConsumingCredential and keyAgreementConsumingCredential

    // signConsumingCredential tests
    @Test
    fun `test signConsumingCredential signs data with valid credential`() = runTest {
        // Setup - Create test data and credential
        val dataToSign = "test-data".toByteArray()
        val expectedSignature = mockk<EcSignature>()
        val alias = "sign-credential"

        val secureArea = mockk<SecureArea> {
            coEvery {
                sign(eq(alias), eq(dataToSign), any())
            } returns expectedSignature
            coEvery { getKeyInvalidated(any()) } returns false
        }

        val credential = createMockCredential(
            alias = alias,
            usageCount = 0
        )

        // Override the secureArea for our credential
        every { credential.secureArea } returns secureArea

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotatingBatch(),
                certifiedCredentials = listOf(credential)
            )
        )

        // Act - Sign the data
        val result = issuedDocument.signConsumingCredential(dataToSign)

        // Assert - The signature is as expected
        assert(result.isSuccess)
        assert(result.getOrNull() == expectedSignature)

        // Verify the signing was done with the right parameters
        coVerify {
            secureArea.sign(
                alias = eq(alias),
                dataToSign = eq(dataToSign),
                unlockReason = any()
            )
        }

        // Verify credential policy was applied
        coVerify { credential.increaseUsageCount() }
    }

    @Test
    fun `test signConsumingCredential uses provided keyUnlockData`() = runTest {
        // Setup - Create test data, credential, and unlock data
        val dataToSign = "test-data".toByteArray()
        val expectedSignature = mockk<EcSignature>()
        val mockUnlockData = mockk<KeyUnlockData>()
        val alias = "sign-credential"

        val secureArea = mockk<SecureArea> {
            coEvery {
                sign(eq(alias), eq(dataToSign), any<Reason>())
            } returns expectedSignature
            coEvery { getKeyInvalidated(any()) } returns false
        }

        val credential = createMockCredential(
            alias = alias,
            usageCount = 0
        )

        // Override the secureArea for our credential
        every { credential.secureArea } returns secureArea

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = listOf(credential)
            )
        )

        // Act - Sign the data with unlock data
        val result = issuedDocument.signConsumingCredential(dataToSign, mockUnlockData)

        // Assert - The signature is as expected
        assert(result.isSuccess)
        assert(result.getOrNull() == expectedSignature)

        // Verify the signing was called (unlock data is now handled via provider)
        coVerify {
            secureArea.sign(
                alias = eq(alias),
                dataToSign = eq(dataToSign),
                unlockReason = any()
            )
        }
    }

    @Test
    fun `test signConsumingCredential returns failure when signing fails`() = runTest {
        // Setup - Create test data and a credential whose secureArea throws an exception
        val dataToSign = "test-data".toByteArray()
        val testException = RuntimeException("Signing failed")
        val alias = "sign-credential"

        val secureArea = mockk<SecureArea> {
            coEvery {
                sign(eq(alias), eq(dataToSign), any())
            } throws testException
            coEvery { getKeyInvalidated(any()) } returns false
        }

        val credential = createMockCredential(
            alias = alias,
            usageCount = 0
        )

        // Override the secureArea for our credential
        every { credential.secureArea } returns secureArea

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = listOf(credential)
            )
        )

        // Act - Attempt to sign the data
        val result = issuedDocument.signConsumingCredential(dataToSign)

        // Assert - The result is a failure with the expected exception
        assert(result.isFailure)
        assert(result.exceptionOrNull() == testException)
    }

    // keyAgreementConsumingCredential tests
    @Test
    fun `test keyAgreementConsumingCredential establishes shared secret`() = runTest {
        // Setup - Create public key and expected shared secret
        val otherPublicKey = "other-party-key".toByteArray()
        val otherPublicKeyDataItem = mockk<DataItem>()
        mockkObject(Cbor)
        every { Cbor.decode(otherPublicKey) } returns otherPublicKeyDataItem
        mockkObject(EcPublicKey.Companion)
        every { EcPublicKey.Companion.fromDataItem(otherPublicKeyDataItem) } returns mockk()
        val expectedSharedSecret = byteArrayOf(1, 2, 3, 4) // SharedSecret is a ByteArray type alias
        val alias = "key-agreement-credential"

        val secureArea = mockk<SecureArea> {
            coEvery {
                keyAgreement(eq(alias), any(), any())
            } returns expectedSharedSecret
            coEvery { getKeyInvalidated(any()) } returns false
        }

        val credential = createMockCredential(
            alias = alias,
            usageCount = 0
        )

        // Override the secureArea for our credential
        every { credential.secureArea } returns secureArea

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                credentialPolicy = CreateDocumentSettings.CredentialPolicy.OnceOnly(),
                certifiedCredentials = listOf(credential)
            )
        )

        // Setup tracking for policy application
        var policyApplied = false
        coEvery {
            issuedDocument.baseDocument.deleteCredential(any())
        } answers {
            policyApplied = true
        }

        // Act - Perform key agreement
        val result = issuedDocument.keyAgreementConsumingCredential(otherPublicKey)

        // Assert - The shared secret is as expected
        assert(result.isSuccess) { "Expected result to be success but was: $result" }
        assert(
            result.getOrNull()?.contentEquals(expectedSharedSecret) == true
        ) { "Expected shared secret doesn't match" }

        // Verify key agreement was performed with correct parameters
        coVerify {
            secureArea.keyAgreement(
                alias = eq(alias),
                otherKey = any(), // Can't directly compare the converted key
                unlockReason = any()
            )
        }

        // Verify OnceOnly policy was applied
        assert(policyApplied) { "OnceOnly policy wasn't applied" }

        unmockkObject(Cbor, EcPublicKey.Companion)
    }

    @Test
    fun `test keyAgreementConsumingCredential uses provided keyUnlockData`() = runTest {
        // Setup - Create public key, unlock data, and expected shared secret

        val otherPublicKey = "other-party-key".toByteArray()
        val otherPublicKeyDataItem = mockk<DataItem>()
        mockkObject(Cbor)
        every { Cbor.decode(otherPublicKey) } returns otherPublicKeyDataItem
        mockkObject(EcPublicKey.Companion)
        every { EcPublicKey.Companion.fromDataItem(otherPublicKeyDataItem) } returns mockk()
        val expectedSharedSecret = byteArrayOf(1, 2, 3, 4) // SharedSecret is a ByteArray type alias
        val mockUnlockData = mockk<KeyUnlockData>()
        val alias = "key-agreement-credential"

        val secureArea = mockk<SecureArea> {
            coEvery {
                keyAgreement(eq(alias), any(), any<Reason>())
            } returns expectedSharedSecret
            coEvery { getKeyInvalidated(any()) } returns false
        }

        val credential = createMockCredential(
            alias = alias,
            usageCount = 0
        )

        // Override the secureArea for our credential
        every { credential.secureArea } returns secureArea

        val issuedDocument = createMockIssuedDocument(
            baseDocument = createMockBaseDocument(
                certifiedCredentials = listOf(credential)
            )
        )

        // Act - Perform key agreement with unlock data
        val result = issuedDocument.keyAgreementConsumingCredential(otherPublicKey, mockUnlockData)

        // Assert - The shared secret is as expected
        assert(result.isSuccess) { "Expected result to be success but was: $result" }
        assert(
            result.getOrNull()?.contentEquals(expectedSharedSecret) == true
        ) { "Expected shared secret doesn't match" }

        // Verify key agreement was called (unlock data is now handled via provider)
        coVerify {
            secureArea.keyAgreement(
                alias = eq(alias),
                otherKey = any(), // Can't directly compare the converted key
                unlockReason = any()
            )
        }

        unmockkObject(Cbor, EcPublicKey.Companion)
    }

    @Test
    fun `test keyAgreementConsumingCredential returns failure when key agreement fails`() =
        runTest {
            // Setup - Create public key and a credential whose secureArea throws an exception
            val otherPublicKey = "other-party-key".toByteArray()
            val otherPublicKeyDataItem = mockk<DataItem>()
            mockkObject(Cbor)
            every { Cbor.decode(otherPublicKey) } returns otherPublicKeyDataItem
            mockkObject(EcPublicKey.Companion)
            every { EcPublicKey.Companion.fromDataItem(otherPublicKeyDataItem) } returns mockk()
            val testException = RuntimeException("Out of bounds decoding data")
            val alias = "key-agreement-credential"

            val secureArea = mockk<SecureArea> {
                coEvery {
                    keyAgreement(eq(alias), any(), any())
                } throws testException
                coEvery { getKeyInvalidated(any()) } returns false
            }

            val credential = createMockCredential(
                alias = alias,
                usageCount = 0
            )

            // Override the secureArea for our credential
            every { credential.secureArea } returns secureArea

            val issuedDocument = createMockIssuedDocument(
                baseDocument = createMockBaseDocument(
                    certifiedCredentials = listOf(credential)
                )
            )

            // Act - Attempt to perform key agreement
            val result = issuedDocument.keyAgreementConsumingCredential(otherPublicKey)

            // Assert - The result is a failure with the expected exception
            assert(result.isFailure) { "Expected result to be a failure but was: $result" }
            assert(result.exceptionOrNull() is RuntimeException) { "Expected exception to be RuntimeException but was: ${result.exceptionOrNull()?.javaClass?.name}" }
            assert(result.exceptionOrNull()?.message == "Out of bounds decoding data") { "Expected exception message to be 'Out of bounds decoding data' but was: ${result.exceptionOrNull()?.message}" }

            unmockkObject(Cbor, EcPublicKey.Companion)
        }

}
