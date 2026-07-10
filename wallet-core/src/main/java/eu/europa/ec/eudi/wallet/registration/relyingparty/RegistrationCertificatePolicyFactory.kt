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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy.Authorization
import eu.europa.ec.eudi.openid4vp.RegistrationCertificatePolicy.PolicyViolation
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import kotlinx.serialization.json.Json

private const val TAG = "RegistrationCertVerify"
private val json = Json { ignoreUnknownKeys = true }

/**
 * Builds the [RegistrationCertificatePolicy] that the OpenID4VP library applies to the relying
 * party's registration certificate on the remote (OpenID4VP) path.
 *
 * The library verifies the certificate's presence, signature and signer-chain trust; the [apply]
 * callback hands the described registration to [evaluator]. The result is surfaced as warnings and
 * never denies the request.
 *
 * @param certificateTrust trust store for the registration certificate signer chain
 * @param evaluator the evaluator applied to the authenticated registration
 * @param logger optional logger
 */
internal fun wrpRegistrationCertificatePolicy(
    certificateTrust: ReaderTrustStore,
    evaluator: WrpRegistrationEvaluator,
    logger: Logger? = null,
): RegistrationCertificatePolicy = RegistrationCertificatePolicy(
    trust = { chain -> certificateTrust.validateCertificationTrustPath(chain) },
    apply = { accessCertificate, registrationCertificate, dcql ->
        // A payload that does not match the WRPRC data model is reported as a malformed-certificate
        // warning.
        val registration = runCatching {
            json.decodeFromJsonElement(WrprcPayloadDto.serializer(), registrationCertificate)
                .toWrpRegistration()
        }.getOrNull()
        val result = if (registration == null) {
            logger?.d(TAG, "registration certificate payload is malformed")
            WrpRegistrationResult.Failed(RegistrationFailureReason.MALFORMED)
        } else {
            evaluator.evaluate(
                registration = registration,
                accessCertificate = accessCertificate,
                requestedAttestations = dcql.toRequestedAttestationInfos(),
            ).also {
                logger?.d(
                    TAG,
                    "relying party '${registration.name}' produced ${it.warningCount} warning(s)"
                )
            }
        }
        result.toAuthorization()
    }
)

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