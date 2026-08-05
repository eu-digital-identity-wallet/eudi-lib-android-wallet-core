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
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * Configuration for status list token trust verification.
 *
 * Holds the trust source, optional attestation classifications, and the trust policy
 * for verifying the signer of status list tokens (JWT or CWT) used in document
 * revocation checks.
 *
 * @param isChainTrustedForAttestation the ETSI trust source for validating certificate chains
 * @param classifications optional attestation classifications for mapping credentials to verification contexts
 * @param trustPolicy the policy determining how to handle trust verification results
 */
data class StatusListTrustConfig(
    val isChainTrustedForAttestation: IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>,
    val classifications: AttestationClassifications?,
    val trustPolicy: TrustPolicy,
)
