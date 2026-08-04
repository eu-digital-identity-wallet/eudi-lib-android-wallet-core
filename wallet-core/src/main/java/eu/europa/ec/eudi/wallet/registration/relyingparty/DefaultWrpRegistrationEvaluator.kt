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

import eu.europa.ec.eudi.wallet.registration.CertificateTrust
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.RevocationOutcome
import eu.europa.ec.eudi.wallet.registration.checkStatusListRevocation
import eu.europa.ec.eudi.wallet.registration.validateCertificate

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import eu.europa.ec.eudi.statium.GetStatus
import eu.europa.ec.eudi.statium.GetStatusListToken
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.HttpClient
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import java.security.cert.X509Certificate

/**
 * Default [WrpRegistrationEvaluator]. Evaluates an authenticated registration against the request:
 * its binding to the relying party that signed the request, its expiry, its revocation status and
 * whether the request stays within the registered scope.
 *
 * A status list reference is mandatory; a registration without one is rejected. Revocation is checked
 * against the certificate's status list. A check that cannot be completed is
 * treated as a failure (REVOCATION_STATUS_UNKNOWN).
 *
 * A validity, binding or revocation failure yields a [RegistrationCertificateResult.Failed] result with the
 * corresponding [RegistrationFailureReason]; requested claims outside the registered scope are
 * reported as [RegistrationCertificateResult.Verified.overAskedClaims] on an otherwise verified result.
 *
 * @param statusTrust trust store for the status list token signer chain; when null only the token's
 *   own signature is checked, without establishing that its signer is a trusted status list provider
 * @param checkRevocation the revocation check; defaults to a status-list check
 */

internal class DefaultWrpRegistrationEvaluator(
    statusTrust: CertificateTrust? = null,
    private val logger: Logger? = null,
    httpClientFactory: (() -> HttpClient)? = null,
    private val checkRevocation: suspend (StatusReference) -> RevocationOutcome =
        { checkStatusListRevocation(it, statusTrust, logger, httpClientFactory) },
) : WrpRegistrationEvaluator {

    override suspend fun evaluate(
        registration: RegistrationCertificate,
        accessCertificate: X509Certificate?,
        requestedAttestations: List<RequestedAttestationInfo>,
    ): RegistrationCertificateResult {

        registration.validateCertificate(accessCertificate, checkRevocation)?.let { failure ->
            logger?.d(TAG, "registration evaluation failed: ${failure.reason}")
            return failure
        }

        // over-asking
        val overAskedClaims = registration.findOverAskedClaims(
            requestedAttestations.map { it.toRequestedAttestation() },
        )
        logger?.d(
            TAG,
            if (overAskedClaims.isEmpty()) {
                "relying party '${registration.name}' request is within its registered scope"
            } else {
                "relying party '${registration.name}' is OVER-ASKING ${overAskedClaims.size} " +
                    "claim(s): " + overAskedClaims.joinToString {
                        "${it.format}:${it.path.joinToString("/")}"
                    }
            },
        )
        return RegistrationCertificateResult.Verified(
            registration = registration,
            overAskedClaims = overAskedClaims,
        )
    }
}

private const val TAG = "DefaultWrpRegistrationE"