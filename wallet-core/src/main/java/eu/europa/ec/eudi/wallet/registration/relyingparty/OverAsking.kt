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

import eu.europa.ec.eudi.wallet.registration.ClaimPathElement
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.OverAskedClaim
import eu.europa.ec.eudi.wallet.registration.RegisteredCredential
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.covers

/**
 * An attestation together with the claims a relying party requests from it in a presentation
 * request, checked against the registered scope by a [WrpRegistrationEvaluator].
 *
 * @property format the requested attestation format, for example `dc+sd-jwt` or `mso_mdoc`
 * @property meta the properties that identify the requested attestation type
 * @property claimPaths the paths of the requested claims, one list of [ClaimPathElement] per claim
 */
data class RequestedAttestation(
    val format: String,
    val meta: CredentialMeta? = null,
    val claimPaths: List<List<ClaimPathElement>> = emptyList(),
)

/**
 * Returns the requested claims that fall outside the relying party's registered scope
 * (WRP-OVERASKING-02).
 *
 * A requested claim is reported when no registered credential of the same attestation permits it; a
 * registered credential with no declared claims permits every claim of that attestation. When the
 * registration declares no requestable credentials, every requested claim is reported. A registered
 * credential or a request that does not identify its attestation type is matched by neither, since the
 * type is mandatory in both.
 */
internal fun RegistrationCertificate.findOverAskedClaims(
    requested: List<RequestedAttestation>,
): List<OverAskedClaim> =
    requested.flatMap { attestation ->
        val registered = requestedCredentials.filter { it.matches(attestation) }
        attestation.claimPaths
            .filter { path -> registered.none { it.allows(path) } }
            .map { OverAskedClaim(format = attestation.format, meta = attestation.meta, path = it) }
    }

private fun RegisteredCredential.matches(requested: RequestedAttestation): Boolean {
    if (format != requested.format) return false
    val registeredMeta = meta ?: return false
    val requestedMeta = requested.meta ?: return false
    val docTypeMatch = registeredMeta.doctypeValue != null &&
        registeredMeta.doctypeValue == requestedMeta.doctypeValue
    val vctMatch = !registeredMeta.vctValues.isNullOrEmpty() &&
        !requestedMeta.vctValues.isNullOrEmpty() &&
        registeredMeta.vctValues.any { it in requestedMeta.vctValues }
    return docTypeMatch || vctMatch
}

private fun RegisteredCredential.allows(path: List<ClaimPathElement>): Boolean =
    claims.isEmpty() || claims.any { it.path.covers(path) }