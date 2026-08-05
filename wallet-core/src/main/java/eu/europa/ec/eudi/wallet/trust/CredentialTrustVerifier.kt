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
import java.security.cert.TrustAnchor

/**
 * Per-format credential trust verifier. Implementations extract the certificate
 * chain from the credential and evaluate trust using the ETSI library.
 *
 * Built-in implementations:
 * - [MsoMdocCredentialTrustVerifier] for MsoMdoc credentials (uses multipaz CBOR/COSE)
 * - [SdJwtVcCredentialTrustVerifier] for SD-JWT VC credentials (uses eudi sd-jwt-vc library)
 *
 * @see IssuerTrustConfigBuilder.credentialTrustVerifier for registering custom verifiers
 */
fun interface CredentialTrustVerifier {
    /**
     * Verify the issuer trust for a credential.
     *
     * @param credentialValue the raw credential string (base64url-encoded CBOR for MsoMdoc,
     *   or SD-JWT string for SD-JWT VC)
     * @param attestationIdentifier the attestation identifier derived from the document format
     * @return the trust evaluation result, or `null` if the certificate chain could not be
     *   extracted or the verification context is not configured
     */
    suspend fun verify(
        credentialValue: String,
        attestationIdentifier: AttestationIdentifier,
    ): CertificationChainValidation<TrustAnchor>?
}
