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

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import java.security.cert.X509Certificate

/**
 * Evaluates the registration of a relying party against a request, after the registration certificate
 * has been authenticated, and produces a [WrpRegistrationResult].
 *
 * Authentication (decoding, signature verification and signer-chain trust) is performed before this
 * evaluation. This evaluation covers the described registration's expiry, its binding to the relying
 * party that signed the request, its revocation status and whether the request stays within the
 * registered scope.
 *
 * It is configured through [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureWrpRegistrationEvaluator].
 */
fun interface WrpRegistrationEvaluator {

    /**
     * Evaluates an authenticated [registration] against the request.
     *
     * @param registration the registration described by the authenticated certificate
     * @param accessCertificate the leaf access certificate that signed the request; null when absent
     * @param requestedAttestations the attestations and claims requested
     * @return the registration result, verified when every check passes and validation-failed with a
     *   [RegistrationFailureReason] otherwise
     */
    suspend fun evaluate(
        registration: RegistrationCertificate,
        accessCertificate: X509Certificate?,
        requestedAttestations: List<RequestedAttestationInfo>,
    ): WrpRegistrationResult
}
