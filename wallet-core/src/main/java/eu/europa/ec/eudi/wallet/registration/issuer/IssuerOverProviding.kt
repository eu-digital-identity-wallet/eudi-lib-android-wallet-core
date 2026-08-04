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

import eu.europa.ec.eudi.wallet.registration.ProvidedAttestation
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate

/**
 * Returns the offered attestation types that fall outside the issuer's registered
 * `provides_attestations` scope.
 *
 * An offered attestation is reported when no registered provided attestation of the same format and
 * type covers it; a provided attestation with no declared type covers every attestation of that
 * format. When the registration declares no provided attestations, every offered attestation is
 * reported.
 */
internal fun RegistrationCertificate.findOverProvidedAttestations(
    offered: List<OfferedAttestation>,
): List<OfferedAttestation> =
    offered.filter { attestation -> providedAttestations.none { it.covers(attestation) } }

private fun ProvidedAttestation.covers(offered: OfferedAttestation): Boolean {
    if (format != offered.format) return false
    val registeredMeta = meta ?: return true
    val offeredMeta = offered.meta ?: return true
    val docTypeMatch = registeredMeta.doctypeValue != null &&
        registeredMeta.doctypeValue == offeredMeta.doctypeValue
    val vctMatch = !registeredMeta.vctValues.isNullOrEmpty() &&
        !offeredMeta.vctValues.isNullOrEmpty() &&
        registeredMeta.vctValues.any { it in offeredMeta.vctValues }
    return docTypeMatch || vctMatch
}
