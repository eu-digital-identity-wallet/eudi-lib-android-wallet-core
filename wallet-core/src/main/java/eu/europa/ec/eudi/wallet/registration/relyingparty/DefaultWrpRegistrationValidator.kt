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
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationInfo
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationValidator
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import java.security.cert.X509Certificate

/**
 * Bridges the transfer layer's [WrpRegistrationValidator] to the wallet's WRPRC handling on the
 * proximity and DC-API paths. Authenticates the registration certificate carried in a device request
 * with a [WrprcAuthenticator] and, on success, evaluates the described registration against the
 * request with a [WrpRegistrationEvaluator]. An absent or inauthentic certificate is rejected.
 */
internal class DefaultWrpRegistrationValidator(
    private val authenticator: WrprcAuthenticator,
    private val evaluator: WrpRegistrationEvaluator,
) : WrpRegistrationValidator {

    override suspend fun validate(
        registrationCertificate: ByteArray?,
        readerAccessChain: List<X509Certificate>,
        requestedAttestations: List<RequestedAttestationInfo>,
    ): WrpRegistrationInfo {
        val certificate = registrationCertificate
            ?: rejected(RegistrationFailureReason.CERTIFICATE_ABSENT)

        return when (val authentication = authenticator.authenticate(certificate)) {
            is WrprcAuthentication.Authentic ->
                evaluator.evaluate(
                    registration = authentication.registration,
                    accessCertificate = readerAccessChain.firstOrNull(),
                    requestedAttestations = requestedAttestations,
                )

            is WrprcAuthentication.Invalid -> rejected(authentication.reason)
        }
    }

    private fun rejected(reason: RegistrationFailureReason): Nothing =
        throw IllegalStateException("relying party registration certificate could not be validated: $reason")
}

internal fun RequestedAttestationInfo.toRequestedAttestation(): RequestedAttestation {
    val meta = if (docType != null || !vctValues.isNullOrEmpty()) {
        CredentialMeta(vctValues = vctValues, doctypeValue = docType)
    } else {
        null
    }
    return RequestedAttestation(format = format, meta = meta, claimPaths = claimPaths)
}
