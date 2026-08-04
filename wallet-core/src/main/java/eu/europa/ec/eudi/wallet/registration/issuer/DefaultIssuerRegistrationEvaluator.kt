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

import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.CertificateTrust
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.OverProvidedAttestation
import eu.europa.ec.eudi.wallet.registration.RevocationOutcome
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.checkStatusListRevocation
import eu.europa.ec.eudi.wallet.registration.validateCertificate
import io.ktor.client.HttpClient
import java.security.cert.X509Certificate

/**
 * Default [IssuerRegistrationEvaluator]. Runs the certificate-validity checks (binding, expiry,
 * mandatory status reference and revocation) and then checks the offered attestations against the
 * certificate's registered `provides_attestations`.
 *
 * A validity failure yields a [RegistrationCertificateResult.Failed]; offered attestations outside the
 * registered scope are reported as [OverProvidedAttestation]s on an otherwise verified result.
 *
 * @param statusTrust trust for the status list token signer chain; when null only the token's own
 *   signature is checked, without establishing that its signer is a trusted status list provider
 * @param checkRevocation the revocation check; defaults to a status-list check
 */
internal class DefaultIssuerRegistrationEvaluator(
    statusTrust: CertificateTrust? = null,
    private val logger: Logger? = null,
    httpClientFactory: (() -> HttpClient)? = null,
    private val checkRevocation: suspend (StatusReference) -> RevocationOutcome =
        { checkStatusListRevocation(it, statusTrust, logger, httpClientFactory) },
) : IssuerRegistrationEvaluator {

    override suspend fun evaluate(
        registration: RegistrationCertificate,
        accessCertificate: X509Certificate?,
        offeredAttestations: List<OfferedAttestation>
    ): RegistrationCertificateResult {
        registration.validateCertificate(accessCertificate, checkRevocation)?.let { failure ->
            logger?.d(TAG, "issuer registration evaluation failed: ${failure.reason}")
            return failure
        }

        val overProvided = registration.findOverProvidedAttestations(offeredAttestations)
        logger?.d(
            TAG,
            if (overProvided.isEmpty()) {
                "issuer '${registration.name}' issuance is within its registered scope"
            } else {
                "issuer '${registration.name}' is OVER-PROVIDING ${overProvided.size} " +
                    "attestation(s): " + overProvided.joinToString {
                        "${it.format}:${it.meta?.doctypeValue ?: it.meta?.vctValues?.joinToString("|") ?: ""}"
                    }
            },
        )
        return RegistrationCertificateResult.Verified(
            registration = registration,
            overProvidedAttestations = overProvided.map {
                OverProvidedAttestation(format = it.format, meta = it.meta)
            }
        )
    }
}

private const val TAG = "DefaultIssuerRegistrationEvaluator"
