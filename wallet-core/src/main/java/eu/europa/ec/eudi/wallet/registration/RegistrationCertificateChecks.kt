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
package eu.europa.ec.eudi.wallet.registration

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

private const val TAG = "RegistrationCertChecks"

/**
 * Runs the registration certificate validity checks — binding to the access certificate, expiry, a
 * mandatory status reference and revocation. Returns a [RegistrationCertificateResult.Failed] on the
 * first failing check, or null when the certificate is valid.
 */
internal suspend fun RegistrationCertificate.validateCertificate(
    accessCertificate: X509Certificate?,
    checkRevocation: suspend (StatusReference) -> RevocationOutcome,
): RegistrationCertificateResult.Failed? {
    if (!isBoundTo(accessCertificate)) {
        return RegistrationCertificateResult.Failed(RegistrationFailureReason.NOT_BOUND_TO_REQUESTER, this)
    }
    expiresAt?.let {
        if (it < Clock.System.now()) {
            return RegistrationCertificateResult.Failed(RegistrationFailureReason.EXPIRED, this)
        }
    }
    val statusReference = status
        ?: return RegistrationCertificateResult.Failed(RegistrationFailureReason.STATUS_MISSING, this)
    return when (checkRevocation(statusReference)) {
        RevocationOutcome.REVOKED ->
            RegistrationCertificateResult.Failed(RegistrationFailureReason.REVOKED, this)

        RevocationOutcome.UNKNOWN, RevocationOutcome.NOT_CHECKED ->
            RegistrationCertificateResult.Failed(RegistrationFailureReason.REVOCATION_STATUS_UNKNOWN, this)

        RevocationOutcome.VALID -> null
    }
}

/** The outcome of checking a registration certificate's revocation status. */
internal enum class RevocationOutcome { VALID, REVOKED, UNKNOWN, NOT_CHECKED }

/**
 * Checks the registration certificate's revocation status against its status list.
 *
 * Returns [RevocationOutcome.VALID] or [RevocationOutcome.REVOKED] according to the status list, or
 * [RevocationOutcome.UNKNOWN] when a check is attempted but cannot be completed.
 */
internal suspend fun checkStatusListRevocation(
    statusReference: StatusReference,
    statusTrust: CertificateTrust?,
    logger: Logger?,
    httpClientFactory: (() -> HttpClient)?,
): RevocationOutcome {
    return try {
        val httpClient = (httpClientFactory ?: { HttpClient() }).invoke()
        val getStatusListToken = GetStatusListToken.usingJwt(
            clock = Clock.System,
            httpClient = httpClient,
            verifyStatusListTokenSignature = RegistrationStatusTokenVerifier(statusTrust),
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
