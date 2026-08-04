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
package eu.europa.ec.eudi.wallet.registration

import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo

/**
 * The outcome of validating a registration certificate: the relying party's on the presentation path
 * and the credential issuer's on the issuance path. It resolves either to a [Verified] registration,
 * described by a certificate whose validation succeeded, or to a [Failed] validation carrying the
 * reason the certificate could not be validated.
 */
sealed interface RegistrationCertificateResult : WrpRegistrationInfo {

    /**
     * A registration described by a validated certificate: its signature, trust chain, revocation
     * status and binding to the entity's access certificate were all verified.
     *
     * @property registration the registration described by the certificate
     * @property overAskedClaims the requested claims not covered by the registration, on the
     *   presentation (relying-party) path; the user is to be warned about these before sharing
     *   (WRP-OVERASKING-02)
     * @property overProvidedAttestations the offered attestation types outside the issuer's
     *   registered scope, on the issuance path; the user is to be warned about these before issuance.
     *   Attestation-level, since `provides_attestations` has no claims (ETSI TS 119 472-3)
     */
    data class Verified(
        val registration: RegistrationCertificate,
        val overAskedClaims: List<OverAskedClaim> = emptyList(),
        val overProvidedAttestations: List<OverProvidedAttestation> = emptyList(),
    ) : RegistrationCertificateResult

    /**
     * A certificate that could not be validated, because it was absent or a verification check failed.
     * The entity is not to be presented as validated and the user is to be warned
     * (WRP-VALIDATION-02).
     *
     * @property reason the cause of the validation failure
     * @property registration the registration described by the certificate when it was parsed before
     *   the failing check; null when the certificate was absent, malformed, unverifiable or from an
     *   untrusted provider
     */
    data class Failed(
        val reason: RegistrationFailureReason,
        val registration: RegistrationCertificate? = null
    ) : RegistrationCertificateResult
}

/**
 * Whether the user must give explicit approval before proceeding: true when the certificate could not
 * be validated or the request goes beyond the registered scope.
 */
val RegistrationCertificateResult.requiresExplicitApproval: Boolean
    get() = when (this) {
        is RegistrationCertificateResult.Failed -> true
        is RegistrationCertificateResult.Verified ->
            overAskedClaims.isNotEmpty() || overProvidedAttestations.isNotEmpty()
    }

/**
 * A claim requested by the relying party that is not covered by its registration.
 *
 * @property format the format of the attestation the claim was requested from
 * @property meta the properties that identify the attestation type the claim was requested from
 * @property path the path pointer of the requested claim
 */
data class OverAskedClaim(
    val format: String,
    val meta: CredentialMeta? = null,
    val path: List<String>
)

/**
 * An attestation type a credential issuer offers to issue but is not registered to provide, from its
 * `provides_attestations` (ETSI TS 119 472-3). Unlike [OverAskedClaim] this is attestation-level:
 * `provides_attestations` declares only the types an issuer may issue, without claims.
 *
 * @property format the format of the over-provided attestation
 * @property meta the properties that identify the over-provided attestation type
 */
data class OverProvidedAttestation(
    val format: String,
    val meta: CredentialMeta? = null,
)
