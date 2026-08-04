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
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import java.security.cert.X509Certificate

/**
 * Holds the relying party registration evaluated while a request is being resolved, for the request
 * processor to reuse.
 *
 * At most one evaluation is held. It is taken only by a request whose inputs are the ones it was
 * produced from, and taking it removes it; a request whose inputs differ evaluates the certificate
 * itself. [clear] scopes an evaluation to the request being resolved and is to be called whenever a
 * request resolution begins.
 */
internal class ResolvedWrpRegistration {

    private var evaluation: Evaluation? = null

    /** Discards the published evaluation, if any. */
    @Synchronized
    fun clear() {
        evaluation = null
    }

    /**
     * Publishes [result] as the evaluation of [certificate] for the given request inputs.
     */
    @Synchronized
    fun publish(
        certificate: ByteArray,
        accessCertificate: X509Certificate?,
        requestedAttestations: List<RequestedAttestationInfo>,
        result: RegistrationCertificateResult,
    ) {
        evaluation = Evaluation(certificate, accessCertificate, requestedAttestations, result)
    }

    /**
     * Returns the published evaluation when it was produced from the given request inputs, or null
     * when there is none or it was produced from different ones. The published evaluation is removed
     * either way.
     */
    @Synchronized
    fun take(
        certificate: ByteArray?,
        accessCertificate: X509Certificate?,
        requestedAttestations: List<RequestedAttestationInfo>,
    ): RegistrationCertificateResult? {
        val published = evaluation ?: return null
        evaluation = null
        return published.result.takeIf {
            certificate != null &&
                published.certificate.contentEquals(certificate) &&
                published.accessCertificate == accessCertificate &&
                published.requestedAttestations == requestedAttestations
        }
    }

    private class Evaluation(
        val certificate: ByteArray,
        val accessCertificate: X509Certificate?,
        val requestedAttestations: List<RequestedAttestationInfo>,
        val result: RegistrationCertificateResult,
    )
}
