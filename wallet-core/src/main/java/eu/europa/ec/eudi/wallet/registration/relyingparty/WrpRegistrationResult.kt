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
package eu.europa.ec.eudi.wallet.registration.relyingparty

import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

/**
 * The relying party registration information resolved for a request. A request resolves either to a
 * [Verified] registration, described by a certificate whose validation succeeded, or to a [Failed]
 * validation carrying the reason it could not be validated.
 */
sealed interface WrpRegistrationResult : WrpRegistrationInfo {

    /**
     * A registration described by a validated certificate: its signature, trust chain, revocation
     * status and binding to the relying party's access certificate were all verified.
     *
     * @property registration the registration described by the certificate
     * @property overAskedClaims the requested claims not covered by the registration; the user is to
     *   be warned about these before sharing (WRP-OVERASKING-02)
     */
    data class Verified(
        val registration: RegistrationCertificate,
        val overAskedClaims: List<OverAskedClaim> = emptyList()
    ) : WrpRegistrationResult

    /**
     * A certificate that could not be validated, because it was absent or a verification check failed.
     * The relying party is not to be presented as validated and the user is to be warned
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
    ) : WrpRegistrationResult
}

/**
 * Whether the user must give explicit approval before sharing data with the relying party: true when
 * the certificate could not be validated or the request asks for attributes outside the registered
 * scope.
 */
val WrpRegistrationResult.requiresExplicitApproval: Boolean
    get() = when (this) {
        is WrpRegistrationResult.Failed -> true
        is WrpRegistrationResult.Verified -> overAskedClaims.isNotEmpty()
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
