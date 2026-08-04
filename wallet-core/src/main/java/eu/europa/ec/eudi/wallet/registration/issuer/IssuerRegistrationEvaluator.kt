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

package eu.europa.ec.eudi.wallet.registration.issuer

import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import java.security.cert.X509Certificate

/**
 * An attestation type a credential issuer offers to issue, used to check an issuance against the
 * registration certificate's registered `provides_attestations` scope.
 *
 * @property format the attestation format, for example `dc+sd-jwt` or `mso_mdoc`
 * @property meta the properties that identify the attestation type
 */
data class OfferedAttestation(
    val format: String,
    val meta: CredentialMeta? = null,
)

/**
 * Evaluates an authenticated issuer registration certificate against an issuance: its binding to the
 * issuer's access certificate, its expiry, its revocation status, and whether the offered
 * attestations stay within the registered `provides_attestations` scope.
 *
 * The certificate is parsed, its signature verified and its signer chain trusted before this runs, so
 * a custom evaluator governs the identity and scope policy only.
 */
fun interface IssuerRegistrationEvaluator {
    suspend fun evaluate(
        registration: RegistrationCertificate,
        accessCertificate: X509Certificate?,
        offeredAttestations: List<OfferedAttestation>,
    ): RegistrationCertificateResult
}
