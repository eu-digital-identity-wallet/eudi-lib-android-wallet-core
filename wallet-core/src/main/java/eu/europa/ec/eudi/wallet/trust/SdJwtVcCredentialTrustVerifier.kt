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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.vc.IssuerVerificationMethod
import eu.europa.ec.eudi.sdjwt.vc.TypeMetadataPolicy
import eu.europa.ec.eudi.sdjwt.vc.X509CertificateTrust
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * [CredentialTrustVerifier] for SD-JWT VC credentials.
 *
 * Delegates to the eudi sd-jwt-vc library's verify method, plugging in the ETSI
 * trust evaluator via the [X509CertificateTrust] callback. The x5c certificate
 * chain is extracted from the JWT header by the sd-jwt-vc library during
 * verification; the trust callback captures the evaluation result.
 *
 * @param isChainTrusted the ETSI trust source for validating certificate chains
 */
internal class SdJwtVcCredentialTrustVerifier(
    private val isChainTrusted: IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>,
    private val logger: Logger? = null,
) : CredentialTrustVerifier {

    override suspend fun verify(
        credentialValue: String,
        attestationIdentifier: AttestationIdentifier,
    ): CertificationChainValidation<TrustAnchor>? {
        var result: CertificationChainValidation<TrustAnchor>? = null

        // Create trust callback that captures the evaluation result
        val trust = X509CertificateTrust<List<X509Certificate>> { chain, _ ->
            logger?.d(TAG, "x5c chain has ${chain.size} certs, leaf=${chain.firstOrNull()?.subjectX500Principal}")
            result = isChainTrusted.issuance(chain, attestationIdentifier)
            val trusted = result is CertificationChainValidation.Trusted
            logger?.d(TAG, "issuance() trusted=$trusted")
            trusted
        }

        // Create verifier with UsingX5c method
        val verifier = NimbusSdJwtOps.SdJwtVcVerifier(
            IssuerVerificationMethod.UsingX5c(trust),
            TypeMetadataPolicy.NotUsed,
            checkStatus = null
        )

        verifier.verify(credentialValue)
            .onFailure { e -> logger?.e(TAG, "SD-JWT VC credential trust verification failed", e) }
            .getOrNull()
        return result
    }

    companion object {
        private const val TAG = "SdJwtIssuerTrust"
    }
}
