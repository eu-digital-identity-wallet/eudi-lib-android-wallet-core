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

    @Test
    fun `no registered credentials and no requested claims reports nothing`() {
        val registration = RegistrationCertificate(requestedCredentials = emptyList())

        val result = registration.findOverAskedClaims(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a registered null wildcard path covers any requested element at that position`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(
                RegisteredCredential(
                    format = "mso_mdoc",
                    meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
                    claims = listOf(RegisteredClaim(path = listOf(MDL_NAMESPACE, null))),
                ),
            ),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name", "portrait"))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a registered wildcard does not cover a request with a different prefix`() {
        val registration = RegistrationCertificate(
            requestedCredentials = listOf(
                RegisteredCredential(
                    format = "mso_mdoc",
                    meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
                    claims = listOf(RegisteredClaim(path = listOf("other.namespace", null))),
                ),
            ),
        )

        val result = registration.findOverAskedClaims(mdlRequest("family_name"))

        assertEquals(listOf(overAsked("family_name")), result)
    }
}

private fun mdlRequest(vararg elements: String): List<RequestedAttestation> =
    listOf(
        RequestedAttestation(
            format = "mso_mdoc",
            meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
            claimPaths = elements.map { listOf(MDL_NAMESPACE, it) },
        ),
    )

private fun registeredMdl(vararg elements: String): RegisteredCredential =
    RegisteredCredential(
        format = "mso_mdoc",
        meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
        claims = elements.map { RegisteredClaim(path = listOf(MDL_NAMESPACE, it)) },
    )

private fun overAsked(element: String): OverAskedClaim =
    OverAskedClaim(
        format = "mso_mdoc",
        meta = CredentialMeta(doctypeValue = MDL_DOCTYPE),
        path = listOf(MDL_NAMESPACE, element),
    )
