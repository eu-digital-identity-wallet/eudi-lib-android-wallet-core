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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate

/**
 * The entitlement URIs a provider may be registered for, as carried in the registration certificate's
 * `entitlements` claim (ETSI TS 119 475; the URIs are those specified in TS5 for the roles of the CIR
 * for Relying Party Registration).
 */
internal object IssuerEntitlements {
    private const val BASE = "https://uri.etsi.org/19475/Entitlement"

    const val PID = "$BASE/PID_Provider"
    const val QEAA = "$BASE/QEAA_Provider"
    const val PUB_EAA = "$BASE/PUB_EAA_Provider"
    const val NON_Q_EAA = "$BASE/Non_Q_EAA_Provider"
}

/**
 * The provider role an offered attestation requires its issuer to be registered for.
 *
 * Issuing a PID requires registration as a PID Provider (ARF ISSU_24a); issuing any other attestation
 * requires registration as a QEAA, PuB-EAA or EAA Provider (ARF ISSU_34a). The latter is a disjunction
 * on purpose: the entitlement is what the registrar asserted, whereas the wallet's own classification
 * of an attestation is local configuration, so requiring the two to agree would refuse providers that
 * are legitimately registered under a different one of the three.
 */
internal enum class EntitlementRequirement { PID_PROVIDER, EAA_PROVIDER }

private val EAA_ENTITLEMENTS = setOf(
    IssuerEntitlements.QEAA,
    IssuerEntitlements.PUB_EAA,
    IssuerEntitlements.NON_Q_EAA,
)

/**
 * Returns the entitlement requirements of the [offered] attestations that this registration does not
 * satisfy. An empty result means the issuer is registered for everything it offers to issue.
 *
 * [isPid] decides which offered attestations are PIDs; it is the classification the wallet is already
 * configured with for trust-area routing. An attestation that identifies no type, and any attestation
 * when no PID classification is configured, counts as a non-PID attestation.
 */
internal fun RegistrationCertificate.findUnmetEntitlements(
    offered: List<OfferedAttestation>,
    isPid: AttestationIdentifierPredicate,
): Set<EntitlementRequirement> =
    offered.map { it.requirement(isPid) }
        .filterNot { it.isMetBy(entitlements) }
        .toSet()

private fun OfferedAttestation.requirement(
    isPid: AttestationIdentifierPredicate,
): EntitlementRequirement =
    if (identifiers().any { isPid.test(it) }) {
        EntitlementRequirement.PID_PROVIDER
    } else {
        EntitlementRequirement.EAA_PROVIDER
    }

/**
 * The attestation type identifiers this offered attestation may be known by. A registered mdoc
 * doctype and each SD-JWT VC type are both projected, so the classification matches on whichever the
 * offer carries.
 */
private fun OfferedAttestation.identifiers(): List<AttestationIdentifier> = buildList {
    meta?.doctypeValue?.let { add(AttestationIdentifier.MDoc(it)) }
    meta?.vctValues?.forEach { add(AttestationIdentifier.SDJwtVc(it)) }
}

private fun EntitlementRequirement.isMetBy(entitlements: List<String>): Boolean = when (this) {
    EntitlementRequirement.PID_PROVIDER -> IssuerEntitlements.PID in entitlements
    EntitlementRequirement.EAA_PROVIDER -> entitlements.any { it in EAA_ENTITLEMENTS }
}
