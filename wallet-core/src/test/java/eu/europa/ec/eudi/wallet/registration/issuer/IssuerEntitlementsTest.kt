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
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import org.junit.Assert.assertEquals
import org.junit.Test

class IssuerEntitlementsTest {

    @Test
    fun `a pid offer is met by the pid provider entitlement`() {
        val unmet = registration(IssuerEntitlements.PID)
            .findUnmetEntitlements(listOf(pidMdoc), isPid)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    @Test
    fun `a pid offer without the pid provider entitlement is unmet`() {
        val unmet = registration(IssuerEntitlements.QEAA)
            .findUnmetEntitlements(listOf(pidMdoc), isPid)

        assertEquals(setOf(EntitlementRequirement.PID_PROVIDER), unmet)
    }

    @Test
    fun `a pid offer identified by vct without the pid provider entitlement is unmet`() {
        val unmet = registration(IssuerEntitlements.PUB_EAA)
            .findUnmetEntitlements(listOf(pidSdJwtVc), isPid)

        assertEquals(setOf(EntitlementRequirement.PID_PROVIDER), unmet)
    }

    @Test
    fun `a non pid offer is met by the qeaa provider entitlement`() {
        val unmet = registration(IssuerEntitlements.QEAA)
            .findUnmetEntitlements(listOf(mdl), isPid)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    @Test
    fun `a non pid offer is met by the pub eaa provider entitlement`() {
        val unmet = registration(IssuerEntitlements.PUB_EAA)
            .findUnmetEntitlements(listOf(mdl), isPid)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    @Test
    fun `a non pid offer is met by the non qualified eaa provider entitlement`() {
        val unmet = registration(IssuerEntitlements.NON_Q_EAA)
            .findUnmetEntitlements(listOf(mdl), isPid)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    @Test
    fun `a non pid offer is not met by the pid provider entitlement alone`() {
        val unmet = registration(IssuerEntitlements.PID)
            .findUnmetEntitlements(listOf(mdl), isPid)

        assertEquals(setOf(EntitlementRequirement.EAA_PROVIDER), unmet)
    }

    @Test
    fun `the service provider entitlement alone meets nothing`() {
        val unmet = registration("https://uri.etsi.org/19475/Entitlement/Service_Provider")
            .findUnmetEntitlements(listOf(pidMdoc, mdl), isPid)

        assertEquals(
            setOf(EntitlementRequirement.PID_PROVIDER, EntitlementRequirement.EAA_PROVIDER),
            unmet,
        )
    }

    @Test
    fun `a mixed offer requires both a pid and an eaa entitlement`() {
        val unmet = registration(IssuerEntitlements.PID)
            .findUnmetEntitlements(listOf(pidMdoc, mdl), isPid)

        assertEquals(setOf(EntitlementRequirement.EAA_PROVIDER), unmet)
    }

    @Test
    fun `a mixed offer is met by a pid and an eaa entitlement together`() {
        val unmet = registration(IssuerEntitlements.PID, IssuerEntitlements.NON_Q_EAA)
            .findUnmetEntitlements(listOf(pidMdoc, mdl), isPid)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    @Test
    fun `an absent entitlements claim leaves the requirement unmet`() {
        val unmet = registration()
            .findUnmetEntitlements(listOf(pidMdoc), isPid)

        assertEquals(setOf(EntitlementRequirement.PID_PROVIDER), unmet)
    }

    @Test
    fun `an offer of nothing requires no entitlement`() {
        val unmet = registration().findUnmetEntitlements(emptyList(), isPid)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    @Test
    fun `an attestation that identifies no type is treated as a non pid attestation`() {
        val untyped = OfferedAttestation(format = "mso_mdoc", meta = null)

        val unmet = registration(IssuerEntitlements.PID)
            .findUnmetEntitlements(listOf(untyped), isPid)

        assertEquals(setOf(EntitlementRequirement.EAA_PROVIDER), unmet)
    }

    @Test
    fun `without configured pid classification a pid offer is treated as a non pid attestation`() {
        val unmet = registration(IssuerEntitlements.NON_Q_EAA)
            .findUnmetEntitlements(listOf(pidMdoc), AttestationIdentifierPredicate.None)

        assertEquals(emptySet<EntitlementRequirement>(), unmet)
    }

    // -- helpers --

    private val isPid = AttestationIdentifierPredicate.any(
        setOf(
            AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1"),
            AttestationIdentifier.SDJwtVc("urn:eudi:pid:1"),
        ),
    )

    private val pidMdoc = OfferedAttestation(
        format = "mso_mdoc",
        meta = CredentialMeta(doctypeValue = "eu.europa.ec.eudi.pid.1"),
    )

    private val pidSdJwtVc = OfferedAttestation(
        format = "dc+sd-jwt",
        meta = CredentialMeta(vctValues = listOf("urn:eudi:pid:1")),
    )

    private val mdl = OfferedAttestation(
        format = "mso_mdoc",
        meta = CredentialMeta(doctypeValue = "org.iso.18013.5.1.mDL"),
    )

    private fun registration(vararg entitlements: String) =
        RegistrationCertificate(name = "Provider", entitlements = entitlements.toList())
}
