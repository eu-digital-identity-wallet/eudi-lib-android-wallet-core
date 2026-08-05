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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.etsi1196x2.consultation.predicate
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.cert.TrustAnchor

@RunWith(RobolectricTestRunner::class)
class EvaluateIssuerTrustTest {

    private val document = mockk<Document>()
    private val credential = Credential.Str("test-credential-value")
    private val logger = mockk<Logger>(relaxed = true)
    private val verifier = mockk<CredentialTrustVerifier>()
    private val trustAnchor = mockk<TrustAnchor>()

    @Test
    fun returnsNullWhenConfigIsNull() = runTest {
        val result = evaluateIssuerTrust(
            issuerTrustConfig = null,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertNull(result)
        verify(exactly = 0) { document.format }
    }

    @Test
    fun returnsNullWhenNoVerifierForFormat() = runTest {
        every { document.format } returns MsoMdocFormat("test.doc")

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = TrustPolicy.uniform(TrustPolicy.Action.ENFORCE),
            credentialTrustVerifiers = emptyMap(),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        val result = evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertNull(result)
    }

    @Test
    fun returnsNullWhenVerifierReturnsNull() = runTest {
        every { document.format } returns MsoMdocFormat("test.doc")
        coEvery { verifier.verify(any(), any()) } returns null

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = TrustPolicy.uniform(TrustPolicy.Action.ENFORCE),
            credentialTrustVerifiers = mapOf(MsoMdocFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        val result = evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertNull(result)
        coVerify { verifier.verify("test-credential-value", AttestationIdentifier.MDoc("test.doc")) }
    }

    @Test
    fun returnsTrustedResultFromVerifier() = runTest {
        every { document.format } returns MsoMdocFormat("test.doc")
        val trusted = CertificationChainValidation.Trusted(trustAnchor)
        coEvery { verifier.verify(any(), any()) } returns trusted

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = TrustPolicy.uniform(TrustPolicy.Action.ENFORCE),
            credentialTrustVerifiers = mapOf(MsoMdocFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        val result = evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertEquals(trusted, result)
    }

    @Test
    fun returnsNotTrustedWithInformPolicy() = runTest {
        every { document.format } returns SdJwtVcFormat("test.vct")
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = TrustPolicy.uniform(TrustPolicy.Action.INFORM),
            credentialTrustVerifiers = mapOf(SdJwtVcFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        val result = evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertEquals(notTrusted, result)
    }

    @Test(expected = IssuerNotTrustedException::class)
    fun throwsIssuerNotTrustedWithEnforcePolicy() = runTest {
        every { document.format } returns MsoMdocFormat("test.doc")
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = TrustPolicy.uniform(TrustPolicy.Action.ENFORCE),
            credentialTrustVerifiers = mapOf(MsoMdocFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )
    }

    @Test
    fun enforcePolicyUsesPerAttestationOverride() = runTest {
        every { document.format } returns MsoMdocFormat("test.doc")
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted

        val policy = TrustPolicy.build {
            default(TrustPolicy.Action.ENFORCE)
            forDocType("test.doc", TrustPolicy.Action.INFORM)
        }

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = null,
            trustPolicy = policy,
            credentialTrustVerifiers = mapOf(MsoMdocFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        // Should NOT throw because per-attestation override is INFORM
        val result = evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertEquals(notTrusted, result)
    }

    @Test
    fun enforcePolicyUsesPerContextOverride() = runTest {
        every { document.format } returns MsoMdocFormat("test.doc")
        val cause = IllegalStateException("not trusted")
        val notTrusted = CertificationChainValidation.NotTrusted(cause)
        coEvery { verifier.verify(any(), any()) } returns notTrusted

        val classifications = AttestationClassifications(
            pids = AttestationIdentifier.MDoc("test.doc").predicate,
        )

        val policy = TrustPolicy.build {
            default(TrustPolicy.Action.ENFORCE)
            forContext(VerificationContext.PID, TrustPolicy.Action.INFORM)
        }

        val config = IssuerTrustConfig(
            isChainTrustedForAttestation = mockk(),
            classifications = classifications,
            trustPolicy = policy,
            credentialTrustVerifiers = mapOf(MsoMdocFormat::class to verifier),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
        )

        // Should NOT throw because per-context override for PID is INFORM
        val result = evaluateIssuerTrust(
            issuerTrustConfig = config,
            document = document,
            credential = credential,
            logger = logger,
        )

        assertEquals(notTrusted, result)
    }
}
