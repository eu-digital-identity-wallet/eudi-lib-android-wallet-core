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

import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.claimPath
import eu.europa.ec.eudi.wallet.registration.OverAskedClaim
import eu.europa.ec.eudi.wallet.registration.RegisteredClaim
import eu.europa.ec.eudi.wallet.registration.RegisteredCredential
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
private const val MDL_NAMESPACE = "org.iso.18013.5.1"

class OverAskingTest {

    @Test
    fun `claims within the registered scope are not reported`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(registeredMdl("family_name", "given_name", "age_over_18")),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name", "age_over_18"))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a claim outside the registered scope is reported`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(registeredMdl("family_name")),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name", "portrait"))

        assertEquals(listOf(overAsked("portrait")), result)
    }

    @Test
    fun `a registered credential with no declared claims permits every claim of that attestation`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(registeredMdl()),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name", "portrait"))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a request with claims but no registered credentials reports every claim as over-asked`() {
        val registration = RegistrationCertificate(requestedCredentials = emptyList())

        val result = registration.findOverAskedClaims(mdlRequest("family_name", "given_name"))

        assertEquals(listOf(overAsked("family_name"), overAsked("given_name")), result)
    }

    /**
     * A credential query must state the attestation type for both supported formats, `doctype_value`
     * for mdoc and `vct_values` for SD-JWT VC (OpenID4VP clauses B.2.3 and B.3.5). A query that omits
     * it cannot be scoped to a registered type, so its claims are reported.
     */
    @Test
    fun `a request naming no attestation type is outside a registration that names one`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(registeredMdl("family_name")),
        )

        val result = registration.findOverAskedClaims(untypedRequest("family_name"))

        assertEquals(1, result.size)
        assertEquals(claimPath(MDL_NAMESPACE, "family_name"), result.single().path)
    }

    /**
     * The attestation type is mandatory in a registered credential: `meta` has multiplicity [1..1] in
     * ETSI TS 119 475 clause B.2.9, the standard the registration certificate is required to comply
     * with. An entry without it identifies no attestation, so it covers nothing rather than every
     * attestation of its format.
     */
    @Test
    fun `a registration naming no attestation type covers nothing`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(
                RegisteredCredential(format = "mso_mdoc", claims = emptyList()),
            ),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name"))

        assertEquals(listOf(overAsked("family_name")), result)
    }

    @Test
    fun `no registered credentials and no requested claims reports nothing`() {
        val registration = RegistrationCertificate(requestedCredentials = emptyList())

        val result = registration.findOverAskedClaims(emptyList())

        assertTrue(result.isEmpty())
    }

    /**
     * The array wildcard applies to arrays only: a path element of `null` selects all elements of the
     * currently selected array, and OpenID4VP clause 7.1.1 makes it an error against anything else. An
     * mdoc path is exactly two strings (clause 7.2), so a namespace is never covered by a wildcard —
     * "every claim of this attestation" is expressed by registering no claims at all, which the test
     * above covers.
     */
    @Test
    fun `a registered wildcard does not cover an mdoc namespace`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(
                RegisteredCredential(
                    format = "mso_mdoc",
                    meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
                    claims = listOf(RegisteredClaim(path = claimPath(MDL_NAMESPACE, null))),
                ),
            ),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name"))

        assertEquals(listOf(overAsked("family_name")), result)
    }

    @Test
    fun `a registered wildcard does not cover a request with a different prefix`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(
                RegisteredCredential(
                    format = "mso_mdoc",
                    meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
                    claims = listOf(RegisteredClaim(path = claimPath("other.namespace", null))),
                ),
            ),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name"))

        assertEquals(listOf(overAsked("family_name")), result)
    }
}

/** A request whose credential query carries an empty `meta`, so it names no attestation type. */
private fun untypedRequest(vararg elements: String): List<RequestedAttestation> =
    listOf(
        RequestedAttestation(
            format = "mso_mdoc",
            meta = null,
            claimPaths = elements.map { claimPath(MDL_NAMESPACE, it) },
        ),
    )

private fun mdlRequest(vararg elements: String): List<RequestedAttestation> =
    listOf(
        RequestedAttestation(
            format = "mso_mdoc",
            meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
            claimPaths = elements.map { claimPath(MDL_NAMESPACE, it) },
        ),
    )

private fun registeredMdl(vararg elements: String): RegisteredCredential =
    RegisteredCredential(
        format = "mso_mdoc",
        meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
        claims = elements.map { RegisteredClaim(path = claimPath(MDL_NAMESPACE, it)) },
    )

private fun overAsked(element: String): OverAskedClaim =
    OverAskedClaim(
        format = "mso_mdoc",
        meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
        path = claimPath(MDL_NAMESPACE, element),
    )
