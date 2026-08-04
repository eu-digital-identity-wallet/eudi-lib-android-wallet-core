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

import eu.europa.ec.eudi.openid4vci.RegistrationCertificatePolicy
import eu.europa.ec.eudi.openid4vci.RegistrationCertificatePolicy.Authorization
import eu.europa.ec.eudi.openid4vci.RegistrationCertificatePolicy.PolicyViolation
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.CertificateTrust
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.decodeSerializedRegistrationCertificate

private const val TAG = "IssuerRegistrationCert"

/**
 * Builds the [RegistrationCertificatePolicy] that the OpenID4VCI library applies to the credential
 * issuer's registration certificate carried in the signed issuer metadata.
 *
 * The library extracts the serialized registration certificate from the metadata and hands it over
 * without decoding, verifying or establishing trust in it. It is authenticated and evaluated against
 * the offered credential configurations by [resolver]. The result is surfaced as warnings and never
 * denies the issuance.
 *
 * The certificate is evaluated once per issuance: an outcome already available is passed as
 * [resolvedRegistration] and reused, and otherwise the outcome produced here is published through
 * [onEvaluated].
 *
 * @param resolver authenticates and evaluates the registration certificate
 * @param resolvedRegistration the outcome already evaluated for this issuance, or null to evaluate it
 *   here
 * @param onEvaluated receives the outcome when it is evaluated here
 * @param logger optional logger
 */
internal fun issuerRegistrationCertificatePolicy(
    resolver: IssuerRegistrationResolver,
    resolvedRegistration: RegistrationCertificateResult?,
    onEvaluated: (RegistrationCertificateResult) -> Unit,
    logger: Logger? = null,
): RegistrationCertificatePolicy =
    RegistrationCertificatePolicy { accessCertificate, registrationCertificate, issuanceContext ->
        val result = resolvedRegistration ?: run {
            val serialized = decodeSerializedRegistrationCertificate(registrationCertificate)
            val evaluated = if (serialized == null) {
                RegistrationCertificateResult.Failed(RegistrationFailureReason.MALFORMED)
            } else {
                resolver.evaluate(
                    accessCertificate = accessCertificate,
                    serialized = serialized,
                    offeredAttestations = issuanceContext.mapNotNull { it.toOfferedAttestation() },
                )
            }
            logger?.d(TAG, "issuer registration certificate produced ${evaluated.warningCount} warning(s)")
            onEvaluated(evaluated)
            evaluated
        }
        result.toAuthorization()
    }

/**
 * Maps a [RegistrationCertificateResult] to the library's [Authorization]. Issuance is always
 * granted; a failed validation and each out-of-scope finding are attached as warnings.
 */
private fun RegistrationCertificateResult.toAuthorization(): Authorization {
    val warnings = when (this) {
        is RegistrationCertificateResult.Failed ->
            listOf(PolicyViolation("issuer registration validation failed: $reason"))

        is RegistrationCertificateResult.Verified ->
            overAskedClaims.map { PolicyViolation("over-asking ${it.format}:${it.path.joinToString("/")}") } +
                overProvidedAttestations.map { PolicyViolation("over-providing ${it.format}") }
    }
    return Authorization.Granted(warnings)
}

private val RegistrationCertificateResult.warningCount: Int
    get() = when (this) {
        is RegistrationCertificateResult.Failed -> 1
        is RegistrationCertificateResult.Verified -> overAskedClaims.size + overProvidedAttestations.size
    }
