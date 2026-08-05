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

import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationValidator
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import java.security.cert.X509Certificate

/**
 * Bridges the transfer layer's [WrpRegistrationValidator] to the wallet's WRPRC handling on the
 * proximity and DC-API paths. Authenticates the registration certificate carried in a device request
 * with a [WrprcAuthenticator] and, on success, evaluates the described registration against the
 * request with a [WrpRegistrationEvaluator]. An absent or inauthentic certificate is reported as a
 * [RegistrationCertificateResult.Failed] rather than raised as an error, so the user can be warned and the
 * request can proceed (WRP-VALIDATION-02).
 */
internal class DefaultWrpRegistrationValidator(
    private val authenticator: WrprcAuthenticator,
    private val evaluator: WrpRegistrationEvaluator,
) : WrpRegistrationValidator {

    override suspend fun validate(
        registrationCertificate: ByteArray?,
        readerAccessChain: List<X509Certificate>,
        requestedDocuments: List<RequestedDocument>
    ): WrpRegistrationInfo = validateAttestations(
        registrationCertificate = registrationCertificate,
        readerAccessChain = readerAccessChain,
        requested = requestedDocuments.map { it.toRequestedAttestation() },
    )

    /**
     * Validates against requested attestations whose claim paths are already resolved, as the remote
     * path produces them from the DCQL query.
     */
    internal suspend fun validateAttestations(
        registrationCertificate: ByteArray?,
        readerAccessChain: List<X509Certificate>,
        requested: List<RequestedAttestation>
    ): WrpRegistrationInfo {
        val certificate = registrationCertificate
            ?: return RegistrationCertificateResult.Failed(RegistrationFailureReason.CERTIFICATE_ABSENT)

        return when (val authentication = authenticator.authenticate(certificate)) {
            is WrprcAuthentication.Authentic ->
                evaluator.evaluate(
                    registration = authentication.registration,
                    accessCertificate = readerAccessChain.firstOrNull(),
                    requestedAttestations = requested
                )

            is WrprcAuthentication.Invalid ->
                RegistrationCertificateResult.Failed(authentication.reason)
        }
    }
}
