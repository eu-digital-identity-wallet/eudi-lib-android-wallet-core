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

import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy.Authorization
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy.PolicyViolation
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

private const val TAG = "RegistrationCertVerify"

/**
 * Builds the [RegistrationCertificatePolicy] that the OpenID4VP library applies to the relying
 * party's registration certificate on the remote (OpenID4VP) path.
 *
 * The library extracts the serialized registration certificate from the request and hands it over
 * without decoding, verifying or establishing trust in it. The certificate is authenticated with
 * [authenticator] and, on success, the described registration is evaluated against the request with
 * [evaluator]. The result is surfaced as warnings and never denies the request.
 *
 * @param authenticator authenticates the serialized registration certificate: decoding, signature
 *   verification and signer-chain trust
 * @param evaluator the evaluator applied to the authenticated registration
 * @param logger optional logger
 */
internal fun wrpRegistrationCertificatePolicy(
    authenticator: WrprcAuthenticator,
    evaluator: WrpRegistrationEvaluator,
    logger: Logger? = null,
): RegistrationCertificatePolicy =
    RegistrationCertificatePolicy { accessCertificate, registrationCertificate, dcql ->
        val serialized = decodeSerializedRegistrationCertificate(registrationCertificate)
        val result = if (serialized == null) {
            WrpRegistrationResult.Failed(RegistrationFailureReason.MALFORMED)
        } else when (val authentication = authenticator.authenticate(serialized)) {
            is WrprcAuthentication.Authentic -> evaluator.evaluate(
                registration = authentication.registration,
                accessCertificate = accessCertificate,
                requestedAttestations = dcql.toRequestedAttestationInfos(),
            )

            is WrprcAuthentication.Invalid ->
                WrpRegistrationResult.Failed(authentication.reason)
        }
        logger?.d(TAG, "registration certificate produced ${result.warningCount} warning(s)")
        result.toAuthorization()
    }

/**
 * Maps a [WrpRegistrationResult] to the library's [Authorization]. The request is always granted; a
 * failed evaluation and each over-asked claim are attached as warnings.
 */
private fun WrpRegistrationResult.toAuthorization(): Authorization {
    val warnings = when (this) {
        is WrpRegistrationResult.Failed ->
            listOf(PolicyViolation("registration validation failed: $reason"))

        is WrpRegistrationResult.Verified ->
            overAskedClaims.map {
                PolicyViolation("over-asking ${it.format}:${it.path.joinToString("/")}")
            }
    }
    return Authorization.Granted(warnings)
}

private val WrpRegistrationResult.warningCount: Int
    get() = when (this) {
        is WrpRegistrationResult.Failed -> 1
        is WrpRegistrationResult.Verified -> overAskedClaims.size
    }