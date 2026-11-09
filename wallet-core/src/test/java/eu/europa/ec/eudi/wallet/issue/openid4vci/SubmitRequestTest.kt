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

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.ProofSpecification
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigners
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.IssuanceProofProfile
import eu.europa.ec.eudi.wallet.provider.WalletKeyAttestationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * A Credential Issuer without a Nonce Endpoint: the OpenID4VCI library passes no `c_nonce` to the
 * proof that contains the key attestation.
 */
class SubmitRequestTest {

    private val keyAttestationProvider = mockk<WalletKeyAttestationProvider> {
        coEvery { getKeyAttestation(any(), any()) } returns
            Result.failure(IllegalStateException("key attestation requested"))
    }

    private val unsignedDocument = mockk<UnsignedDocument>(relaxed = true) {
        coEvery { getPoPSigners() } returns ProofOfPossessionSigners(
            listOf(mockk<ProofOfPossessionSigner>(relaxed = true) { every { keyAlias } returns "key" })
        )
    }

    private val issuerWithoutNonceEndpoint = mockk<Issuer> {
        coEvery { with(this@mockk) { any<AuthorizedRequest>().request(any(), any()) } } coAnswers {
            when (val proof = thirdArg<ProofSpecification>()) {
                is ProofSpecification.AttestationProof -> proof.attestationProvider(null, null)
                is ProofSpecification.JwtProofWithKeyAttestation -> proof.proofSignerProvider(null, null)
                else -> error("Unexpected proof specification $proof")
            }
            error("The proof was built")
        }
    }

    private val keyAttestationRequirement = KeyAttestationRequirement(null, null, null)
    private val attestationProof = ProofTypeMeta.Attestation(listOf(JWSAlgorithm.ES256), keyAttestationRequirement)
    private val jwtProofWithKeyAttestation = ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256), keyAttestationRequirement)

    @Test
    fun `Etsi does not request a key attestation for an attestation proof without a c_nonce`() {
        val outcome = request(IssuanceProofProfile.Etsi, attestationProof)

        assertNoNonceFailure(outcome)
        coVerify(exactly = 0) { keyAttestationProvider.getKeyAttestation(any(), any()) }
    }

    @Test
    fun `Etsi does not request a key attestation for a JWT proof without a c_nonce`() {
        val outcome = request(IssuanceProofProfile.Etsi, jwtProofWithKeyAttestation)

        assertNoNonceFailure(outcome)
        coVerify(exactly = 0) { keyAttestationProvider.getKeyAttestation(any(), any()) }
    }

    @Test
    fun `Standard requests a key attestation without a c_nonce`() {
        request(IssuanceProofProfile.Standard, attestationProof)

        coVerify(exactly = 1) { keyAttestationProvider.getKeyAttestation(any(), isNull()) }
    }

    private fun request(profile: IssuanceProofProfile, proofType: ProofTypeMeta): Result<*> = runBlocking {
        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { configuration } returns mockk<CredentialConfiguration> {
                every { proofTypesSupported } returns ProofTypesSupported(setOf(proofType))
            }
        }
        val payload = mockk<IssuanceRequestPayload>(relaxed = true)
        SubmitRequest(keyAttestationProvider, issuerWithoutNonceEndpoint, mockk(relaxed = true), profile)
            .request(mapOf(unsignedDocument to (offeredDocument to payload)))
            .getValue(unsignedDocument)
            .outcome
    }

    private fun assertNoNonceFailure(outcome: Result<*>) {
        val error = assertIs<IllegalStateException>(outcome.exceptionOrNull())
        assertTrue(error.message.orEmpty().contains("no Nonce Endpoint"), error.message)
    }
}
