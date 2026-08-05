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
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.openid4vci.DeferredCredentialQueryOutcome
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.ProcessDeferredOutcome
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
import org.robolectric.RobolectricTestRunner
import java.security.cert.TrustAnchor

@RunWith(RobolectricTestRunner::class)
class ProcessDeferredOutcomeTrustTest {

    private val documentManager = mockk<DocumentManager>(relaxed = true)
    private val issuedDocument = mockk<IssuedDocument> {
        every { id } returns "doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("test.vct")
    }
    private val deferredDocument = mockk<DeferredDocument> {
        every { id } returns "deferred-doc-id"
        every { name } returns "Test Document"
        every { format } returns SdJwtVcFormat("test.vct")
    }

    private val credential = Credential.Str("test-credential-value")
    private val keyAlias = "key-alias-1"
    private val issuedCredential = IssuedCredential.string("test-credential-value")
    private val trustAnchor = mockk<TrustAnchor>()
    private val verifier = mockk<CredentialTrustVerifier>()

    private fun createOutcome(): DeferredCredentialQueryOutcome.Issued {
        return mockk<DeferredCredentialQueryOutcome.Issued> {
            every { credentials } returns listOf(issuedCredential)
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

    private fun createProcessDeferredOutcome(
        callback: OpenId4VciManager.OnResult<DeferredIssueResult>,
        config: IssuerTrustConfig? = null,
    ) = ProcessDeferredOutcome(
        documentManager = documentManager,
        callback = callback,
        deferredContext = null,
        logger = null,
        issuerTrustConfig = config,
    )

    @Test
    fun storesDeferredDocumentWithTrustResult() = runTest {
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { verifier.verify(any(), any()) } returns trusted
        setupStoreIssuedDocument()

        val outcome = createOutcome()
        var capturedResult: DeferredIssueResult? = null
        val callback = OpenId4VciManager.OnResult<DeferredIssueResult> { capturedResult = it }
        val config = createConfig(TrustPolicy.Action.INFORM)

        createProcessDeferredOutcome(callback, config)
            .process(deferredDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentIssued but got $capturedResult",
            capturedResult is DeferredIssueResult.DocumentIssued)
        val documentIssued = capturedResult as DeferredIssueResult.DocumentIssued
        assertEquals(trusted, documentIssued.issuerTrustResult)
        assertEquals(issuedDocument, documentIssued.document)
    }

    @Test
    fun storesDeferredWithNullTrustWhenNotConfigured() = runTest {
        setupStoreIssuedDocument()

        val outcome = createOutcome()
        var capturedResult: DeferredIssueResult? = null
        val callback = OpenId4VciManager.OnResult<DeferredIssueResult> { capturedResult = it }

        createProcessDeferredOutcome(callback, config = null)
            .process(deferredDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentIssued but got $capturedResult",
            capturedResult is DeferredIssueResult.DocumentIssued)
        val documentIssued = capturedResult as DeferredIssueResult.DocumentIssued
        assertNull(documentIssued.issuerTrustResult)
        assertEquals(issuedDocument, documentIssued.document)
    }

    @Test
    fun rejectsDeferredWithEnforceAndNotTrusted() = runTest {
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted

        val outcome = createOutcome()
        var capturedResult: DeferredIssueResult? = null
        val callback = OpenId4VciManager.OnResult<DeferredIssueResult> { capturedResult = it }
        val config = createConfig(TrustPolicy.Action.ENFORCE)

        createProcessDeferredOutcome(callback, config)
            .process(deferredDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentFailed but got $capturedResult",
            capturedResult is DeferredIssueResult.DocumentFailed)
        val documentFailed = capturedResult as DeferredIssueResult.DocumentFailed
        assertTrue(documentFailed.cause is IssuerNotTrustedException)
        // Document is NOT deleted - the exception propagates to the outer try/catch
        verify(exactly = 0) { documentManager.deleteDocumentById(any()) }
    }

    @Test
    fun storesDeferredWithInformAndNotTrusted() = runTest {
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted
        setupStoreIssuedDocument()

        val outcome = createOutcome()
        var capturedResult: DeferredIssueResult? = null
        val callback = OpenId4VciManager.OnResult<DeferredIssueResult> { capturedResult = it }
        val config = createConfig(TrustPolicy.Action.INFORM)

        createProcessDeferredOutcome(callback, config)
            .process(deferredDocument, listOf(keyAlias), outcome)

        assertTrue("Expected DocumentIssued but got $capturedResult",
            capturedResult is DeferredIssueResult.DocumentIssued)
        val documentIssued = capturedResult as DeferredIssueResult.DocumentIssued
        assertEquals(notTrusted, documentIssued.issuerTrustResult)
        assertEquals(issuedDocument, documentIssued.document)
    }
}
