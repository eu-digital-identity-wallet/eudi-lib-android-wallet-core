/*
 * Copyright (c) 2026 European Commission
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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredContextFactory
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.ProcessResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.storage.Storage
import org.robolectric.RobolectricTestRunner
import java.security.cert.TrustAnchor

@RunWith(RobolectricTestRunner::class)
class ProcessResponseTrustTest {

    private val documentManager = mockk<DocumentManager>(relaxed = true)
    private val deferredContextFactory = mockk<DeferredContextFactory>()
    private val issuedDocumentIds = mutableListOf<DocumentId>()
    private val deferredDocumentIds = mutableListOf<DocumentId>()
    private val issuedDocument = mockk<IssuedDocument> {
        every { id } returns "doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("test.vct")
    }
    private val unsignedDocument = mockk<UnsignedDocument> {
        every { id } returns "unsigned-doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("test.vct")
    }

    private val credential = Credential.Str("test-credential-value")
    private val keyAlias = "key-alias-1"
    private val issuedCredential = IssuedCredential.string("test-credential-value")
    private val trustAnchor = mockk<TrustAnchor>()
    private val verifier = mockk<CredentialTrustVerifier>()

    private fun createOutcome(): SubmissionOutcome.Success {
        return mockk<SubmissionOutcome.Success> {
            every { credentials } returns listOf(issuedCredential)
            every { selectedCredentialReusePolicy } returns null
        }
    }

    private fun setupStoreIssuedDocument() {
        every {
            documentManager.storeIssuedDocument(any(), any())
        } returns Outcome.success(issuedDocument)
    }

    private fun createConfig(action: TrustPolicy.Action): IssuerTrustConfig {
        return IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = TrustPolicy.uniform(action),
            credentialTrustVerifiers = mapOf(SdJwtVcFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )
    }

    private fun createProcessResponse(
        listener: OpenId4VciManager.OnResult<IssueEvent>,
        config: IssuerTrustConfig? = null,
    ) = ProcessResponse(
        documentManager = documentManager,
        deferredContextFactory = deferredContextFactory,
        clientAttestationPopKeyId = null,
        listener = listener,
        issuedDocumentIds = issuedDocumentIds,
        deferredDocumentIds = deferredDocumentIds,
        logger = null,
        authorizedRequest = mockk<AuthorizedRequest>(relaxed = true),
        issuer = mockk<Issuer>(relaxed = true),
        documentToConfigurationMap = emptyMap(),
        dpopKeyAlias = null,
        issuanceMetadataStorage = mockk<Storage>(relaxed = true),
        clientAuthentication = mockk<ClientAuthentication>(relaxed = true),
        issuerTrustConfig = config,
    )

    @Test
    fun storesDocumentWithTrustResult() = runTest {
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { verifier.verify(any(), any()) } returns trusted
        setupStoreIssuedDocument()

        val outcome = createOutcome()
        var capturedEvent: IssueEvent? = null
        val listener = OpenId4VciManager.OnResult<IssueEvent> { capturedEvent = it }
        val config = createConfig(TrustPolicy.Action.INFORM)

        createProcessResponse(listener, config)
            .processSubmittedRequest(unsignedDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentIssued but got $capturedEvent",
            capturedEvent is IssueEvent.DocumentIssued)
        val documentIssued = capturedEvent as IssueEvent.DocumentIssued
        assertEquals(trusted, documentIssued.issuerTrustResult)
        assertEquals(issuedDocument, documentIssued.document)
        assertTrue(issuedDocumentIds.contains("doc-id"))
    }

    @Test
    fun storesDocumentWithNullTrustWhenNotConfigured() = runTest {
        setupStoreIssuedDocument()

        val outcome = createOutcome()
        var capturedEvent: IssueEvent? = null
        val listener = OpenId4VciManager.OnResult<IssueEvent> { capturedEvent = it }

        createProcessResponse(listener, config = null)
            .processSubmittedRequest(unsignedDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentIssued but got $capturedEvent",
            capturedEvent is IssueEvent.DocumentIssued)
        val documentIssued = capturedEvent as IssueEvent.DocumentIssued
        assertNull(documentIssued.issuerTrustResult)
        assertEquals(issuedDocument, documentIssued.document)
    }

    @Test
    fun rejectsDocumentWithEnforceAndNotTrusted() = runTest {
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted

        val outcome = createOutcome()
        var capturedEvent: IssueEvent? = null
        val listener = OpenId4VciManager.OnResult<IssueEvent> { capturedEvent = it }
        val config = createConfig(TrustPolicy.Action.ENFORCE)

        createProcessResponse(listener, config)
            .processSubmittedRequest(unsignedDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentFailed but got $capturedEvent",
            capturedEvent is IssueEvent.DocumentFailed)
        val documentFailed = capturedEvent as IssueEvent.DocumentFailed
        assertTrue(documentFailed.cause is IssuerNotTrustedException)
        verify { documentManager.deleteDocumentById("unsigned-doc-id") }
        assertTrue(issuedDocumentIds.isEmpty())
    }

    @Test
    fun storesDocumentWithInformAndNotTrusted() = runTest {
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted
        setupStoreIssuedDocument()

        val outcome = createOutcome()
        var capturedEvent: IssueEvent? = null
        val listener = OpenId4VciManager.OnResult<IssueEvent> { capturedEvent = it }
        val config = createConfig(TrustPolicy.Action.INFORM)

        createProcessResponse(listener, config)
            .processSubmittedRequest(unsignedDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentIssued but got $capturedEvent",
            capturedEvent is IssueEvent.DocumentIssued)
        val documentIssued = capturedEvent as IssueEvent.DocumentIssued
        assertEquals(notTrusted, documentIssued.issuerTrustResult)
        assertEquals(issuedDocument, documentIssued.document)
    }
}
