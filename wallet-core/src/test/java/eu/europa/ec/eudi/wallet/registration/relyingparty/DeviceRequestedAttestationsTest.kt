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

import eu.europa.ec.eudi.iso18013.transfer.response.MSO_MDOC_FORMAT
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.wallet.registration.ClaimPathElement
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.RegisteredClaim
import eu.europa.ec.eudi.wallet.registration.RegisteredCredential
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.claimPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
private const val MDL_NAMESPACE = "org.iso.18013.5.1"
private const val MDL_EU_NAMESPACE = "eu.europa.ec.eudi.mdl.1"

/**
 * Projecting a document requested over ISO/IEC 18013-5 onto the attestation the registered scope is
 * checked against, as the proximity and DC-API paths do.
 */
class DeviceRequestedAttestationsTest {

    /**
     * An mdoc claim path is the namespace and the data element identifier, both of type string
     * (OpenID4VP clause 7.2), so neither is an array index or the wildcard.
     */
    @Test
    fun `a path is the namespace and the data element, both as names`() {
        val requested = mdlRequest(MDL_NAMESPACE to setOf("family_name")).toRequestedAttestation()

        assertEquals(
            listOf(
                listOf(
                    ClaimPathElement.Claim(MDL_NAMESPACE),
                    ClaimPathElement.Claim("family_name"),
                ),
            ),
            requested.claimPaths
        )
    }

    @Test
    fun `the document type identifies the requested attestation`() {
        val requested = mdlRequest().toRequestedAttestation()

        assertEquals(MSO_MDOC_FORMAT, requested.format)
        assertEquals(CredentialMeta(doctypeValue = MDL_DOCTYPE), requested.meta)
    }

    @Test
    fun `every element of every requested namespace is projected`() {
        val requested = mdlRequest(
            MDL_NAMESPACE to setOf("family_name", "age_over_18"),
            MDL_EU_NAMESPACE to setOf("portrait"),
        ).toRequestedAttestation()

        assertEquals(
            setOf(
                claimPath(MDL_NAMESPACE, "family_name"),
                claimPath(MDL_NAMESPACE, "age_over_18"),
                claimPath(MDL_EU_NAMESPACE, "portrait")
            ),
            requested.claimPaths.toSet(),
        )
    }

    @Test
    fun `a document requesting nothing has no claim paths`() {
        assertEquals(emptyList<List<ClaimPathElement>>(), mdlRequest().toRequestedAttestation().claimPaths)
    }

    @Test
    fun `a projected request within the registered scope is not reported`() {
        val overAsked = registeredForMdl("family_name").findOverAskedClaims(
            listOf(mdlRequest(MDL_NAMESPACE to setOf("family_name")).toRequestedAttestation()),
        )

        assertTrue("reported as over-asked: $overAsked", overAsked.isEmpty())
    }

    @Test
    fun `a projected request outside the registered scope is reported`() {
        val overAsked = registeredForMdl("family_name").findOverAskedClaims(
            listOf(mdlRequest(MDL_NAMESPACE to setOf("portrait")).toRequestedAttestation()),
        )

        assertEquals(claimPath(MDL_NAMESPACE, "portrait"), overAsked.single().path)
    }
}

private fun mdlRequest(vararg nameSpaces: Pair<String, Set<String>>): RequestedDocument =
    RequestedDocument(docType = MDL_DOCTYPE, nameSpaces = nameSpaces.toMap())

private fun registeredForMdl(vararg elements: String): RegistrationCertificate =
    RegistrationCertificate(
        requestedCredentials = listOf(
            RegisteredCredential(
                format = MSO_MDOC_FORMAT,
                meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
                claims = elements.map { RegisteredClaim(path = claimPath(MDL_NAMESPACE, it)) },
            )
        )
    )
