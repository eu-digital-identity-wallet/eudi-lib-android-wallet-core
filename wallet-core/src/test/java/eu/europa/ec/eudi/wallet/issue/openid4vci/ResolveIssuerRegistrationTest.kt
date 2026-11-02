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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CertificateChainTrust
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerEndpoint
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.registration.CertificateTrust
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerRegistrationResolver
import eu.europa.ec.eudi.wallet.trust.IssuerTrustConfig
import eu.europa.ec.eudi.wallet.trust.TrustPolicy
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveIssuerRegistrationTest {

    @Test
    fun `an issuer without a registration certificate is reported as absent`() = runTest {
        val manager = manager(metadataWithoutRegistrationCertificate())

        val result = manager.resolveIssuerRegistration("https://issuer.example", listOf("pid"))

        assertEquals(
            RegistrationFailureReason.CERTIFICATE_ABSENT,
            (result.getOrThrow() as RegistrationCertificateResult.Failed).reason,
        )
    }

    private fun metadataWithoutRegistrationCertificate() = CredentialIssuerMetadata(
        credentialIssuerIdentifier = CredentialIssuerId("https://issuer.example").getOrThrow(),
        credentialEndpoint = CredentialIssuerEndpoint("https://issuer.example/credential").getOrThrow(),
        credentialConfigurationsSupported = mapOf(
            CredentialConfigurationIdentifier("pid") to
                MsoMdocCredential(credentialMetadata = null, docType = "eu.europa.ec.eudi.pid.1"),
        ),
        issuerInfo = null,
    )

    private fun manager(metadata: CredentialIssuerMetadata): DefaultOpenId4VciManager =
        spyk(
            DefaultOpenId4VciManager(
                context = mockk(relaxed = true),
                documentManager = mockk(relaxed = true),
                walletAttestationKeyManager = mockk(),
                config = OpenId4VciManager.Config(
                    clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("client"),
                    authFlowRedirectionURI = "app://redirect",
                    dpopConfig = DPopConfig.Disabled,
                ),
                issuerTrustConfig = IssuerTrustConfig(
                    isChainTrustedForAttestation = mockk(),
                    classifications = null,
                    trustPolicy = TrustPolicy.uniform(TrustPolicy.Action.INFORM),
                    credentialTrustVerifiers = emptyMap(),
                    issuerMetadataPolicy = IssuerMetadataPolicy.RequireSigned(issuerTrust = mockk<CertificateChainTrust>()),
                ),
                issuerRegistrationEnabled = true,
                issuerRegistration = IssuerRegistrationResolver(CertificateTrust { true }, evaluator = mockk()),
            ),
        ) {
            coEvery { getIssuerMetadata(any()) } returns Result.success(metadata)
        }
}
