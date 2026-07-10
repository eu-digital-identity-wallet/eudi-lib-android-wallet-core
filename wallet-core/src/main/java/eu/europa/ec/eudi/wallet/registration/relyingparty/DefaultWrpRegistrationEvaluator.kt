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

import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import eu.europa.ec.eudi.statium.GetStatus
import eu.europa.ec.eudi.statium.GetStatusListToken
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
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
 * A validity, binding or revocation failure yields a [WrpRegistrationResult.Failed] result with the
 * corresponding [RegistrationFailureReason]; requested claims outside the registered scope are
 * reported as [WrpRegistrationResult.Verified.overAskedClaims] on an otherwise verified result.
 *
 * @param statusTrust trust store for the status list token signer chain; when null the token is
 *   verified against its own x5c chain
 * @param checkRevocation the revocation check; defaults to a status-list check
 */

internal class DefaultWrpRegistrationEvaluator(
    statusTrust: ReaderTrustStore? = null,
    private val logger: Logger? = null,
    httpClientFactory: (() -> HttpClient)? = null,
    private val checkRevocation: suspend (StatusReference) -> RevocationOutcome =
        { checkStatusListRevocation(it, statusTrust, logger, httpClientFactory) },
) : WrpRegistrationEvaluator {

    override suspend fun evaluate(
        registration: RegistrationCertificate,
        accessCertificate: X509Certificate?,
        requestedAttestations: List<RequestedAttestationInfo>,
    ): WrpRegistrationResult {

        // Check Binding
        if (!registration.isBoundTo(accessCertificate)) {
            return failed(RegistrationFailureReason.NOT_BOUND_TO_REQUESTER, registration)
        }

        // Check expiration
        registration.expiresAt?.let { expiresAt ->
            if (expiresAt < Clock.System.now()) return failed(RegistrationFailureReason.EXPIRED, registration)
        }

        // A status list reference is mandatory.
        val statusReference = registration.status
            ?: return failed(RegistrationFailureReason.STATUS_MISSING, registration)

        when (checkRevocation(statusReference)) {
            RevocationOutcome.REVOKED -> return failed(RegistrationFailureReason.REVOKED, registration)
            RevocationOutcome.UNKNOWN, RevocationOutcome.NOT_CHECKED ->
                return failed(RegistrationFailureReason.REVOCATION_STATUS_UNKNOWN, registration)
            RevocationOutcome.VALID -> Unit
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
        return WrpRegistrationResult.Verified(
            registration = registration,
            overAskedClaims = overAskedClaims,
        )
    }

    private fun failed(
        reason: RegistrationFailureReason,
        registration: RegistrationCertificate? = null,
    ): WrpRegistrationResult {
        logger?.d(TAG, "registration evaluation failed: $reason")
        return WrpRegistrationResult.Failed(reason, registration)
    }
}

private const val TAG = "DefaultWrpRegistrationE"

/** The outcome of checking a registration certificate's revocation status. */
internal enum class RevocationOutcome { VALID, REVOKED, UNKNOWN, NOT_CHECKED }

/**
 * Checks the registration certificate's revocation status against its status list.
 *
 * Returns [RevocationOutcome.VALID] or [RevocationOutcome.REVOKED] according to the status list, or
 * [RevocationOutcome.UNKNOWN] when a check is attempted but cannot be completed.
 */
private suspend fun checkStatusListRevocation(
    statusReference: StatusReference,
    statusTrust: ReaderTrustStore?,
    logger: Logger?,
    httpClientFactory: (() -> HttpClient)?,
): RevocationOutcome {
    return try {
        val httpClient = (httpClientFactory ?: { HttpClient() }).invoke()
        val getStatusListToken = GetStatusListToken.usingJwt(
            clock = Clock.System,
            httpClient = httpClient,
            verifyStatusListTokenSignature = WrprcStatusTokenVerifier(statusTrust),
            allowedClockSkew = Duration.ZERO,
        )
        val status = with(GetStatus(getStatusListToken)) { statusReference.currentStatus() }.getOrThrow()
        if (status is Status.Valid) {
            logger?.d(TAG, "registration certificate status is valid")
            RevocationOutcome.VALID
        } else {
            RevocationOutcome.REVOKED
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // An inconclusive check cannot confirm the certificate is not revoked.
        logger?.d(TAG, "registration certificate revocation status could not be determined: ${e.message}")
        RevocationOutcome.UNKNOWN
    }
}
