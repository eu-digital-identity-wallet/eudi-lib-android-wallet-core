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
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

@RunWith(RobolectricTestRunner::class)
class IssuerTrustConfigBuilderTest {

    @Test
    fun buildsConfigFromPreBuiltAttestation() {
        val mockAttestation = mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockAttestation)
            ignoreSignedMetadata()
        }.build()

        assertEquals(mockAttestation, config.isChainTrustedForAttestation)
        assertNull(config.classifications)
    }

    @Test
    fun buildsConfigWithClassifications() {
        val mockEudiw = mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()
        val classifications = AttestationClassifications(
            pids = AttestationIdentifierPredicate.equalsTo(AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1")),
        )

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockEudiw)
            classifications(classifications)
        }.build()

        assertNotNull(config.isChainTrustedForAttestation)
        assertEquals(classifications, config.classifications)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenTrustSourceMissing() {
        IssuerTrustConfigBuilder().build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenClassificationsMissingForEUDIW() {
        val mockEudiw = mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()

        IssuerTrustConfigBuilder().apply {
            trustSource(mockEudiw)
        }.build()
    }

    @Test
    fun customVerifierIsIncluded() {
        val mockAttestation = mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()
        val mockVerifier = mockk<CredentialTrustVerifier>()

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockAttestation)
            credentialTrustVerifier(MsoMdocFormat::class, mockVerifier)
            ignoreSignedMetadata()
        }.build()

        assertEquals(mockVerifier, config.credentialTrustVerifiers[MsoMdocFormat::class])
        // Default verifiers for MsoMdoc (overridden) and SdJwtVc (default)
        assertEquals(2, config.credentialTrustVerifiers.size)
    }

    @Test
    fun defaultPolicyIsEnforce() {
        val mockAttestation = mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockAttestation)
            ignoreSignedMetadata()
        }.build()

        val action = config.trustPolicy.resolve(
            AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL"),
            VerificationContext.PID,
        )
        assertEquals(TrustPolicy.Action.ENFORCE, action)
    }

    @Test
    fun defaultMetadataPolicyIsRequireSigned() {
        val mockEudiw = mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()
        val classifications = AttestationClassifications(
            pids = AttestationIdentifierPredicate.equalsTo(AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1")),
        )

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockEudiw)
            classifications(classifications)
        }.build()

        assertTrue(config.issuerMetadataPolicy is IssuerMetadataPolicy.RequireSigned)
    }

    @Test(expected = IllegalArgumentException::class)
    fun defaultMetadataWithPreBuiltAttestationThrows() {
        val mockAttestation = mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()

        IssuerTrustConfigBuilder().apply {
            trustSource(mockAttestation)
        }.build()
    }

    @Test
    fun preBuiltAttestationWithIgnoreSignedMetadataSucceeds() {
        val mockAttestation = mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockAttestation)
            ignoreSignedMetadata()
        }.build()

        assertEquals(IssuerMetadataPolicy.IgnoreSigned, config.issuerMetadataPolicy)
    }

    @Test
    fun requireSignedMetadataWithEudiwSource() {
        val mockEudiw = mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()
        val classifications = AttestationClassifications(
            pids = AttestationIdentifierPredicate.equalsTo(AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1")),
        )

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockEudiw)
            classifications(classifications)
            requireSignedMetadata()
        }.build()

        assertTrue(config.issuerMetadataPolicy is IssuerMetadataPolicy.RequireSigned)
    }

    @Test
    fun ignoreSignedMetadataOverridesDefault() {
        val mockEudiw = mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()
        val classifications = AttestationClassifications(
            pids = AttestationIdentifierPredicate.equalsTo(AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1")),
        )

        val config = IssuerTrustConfigBuilder().apply {
            trustSource(mockEudiw)
            classifications(classifications)
            ignoreSignedMetadata()
        }.build()

        assertEquals(IssuerMetadataPolicy.IgnoreSigned, config.issuerMetadataPolicy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun requireSignedMetadataWithoutEudiwSourceThrows() {
        val mockAttestation = mockk<IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>>()

        IssuerTrustConfigBuilder().apply {
            trustSource(mockAttestation)
            requireSignedMetadata()
        }.build()
    }
}
