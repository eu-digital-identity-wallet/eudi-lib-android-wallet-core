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

import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaSdJwtVcExtensions
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.claimPath
import eu.europa.ec.eudi.wallet.registration.RegisteredClaim
import eu.europa.ec.eudi.wallet.registration.RegisteredCredential
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PID_VCT = "urn:eudi:pid:1"
private const val SD_JWT_VC_FORMAT = "dc+sd-jwt"

/**
 * Over-asking checked through the DCQL projection, so a requested path keeps every element of the
 * pointer. A claim path is a list of elements that may be a claim name, an array index, or the
 * wildcard for all array elements (OpenID4VP clause 6, ETSI TS 119 475 clause 5.2.4 `credentials`).
 */
class DcqlOverAskingTest {

    @Test
    fun `a wildcard request against a wildcard registration is within scope`() {
        val requested = pidRequest(
            ClaimPath(
                listOf(ClaimPathElement.Claim("nationalities"), ClaimPathElement.AllArrayElements),
            ),
        )

        val result = registeredForNationalitiesWildcard().findOverAskedClaims(requested)

        assertTrue("a wildcard request is covered by a wildcard registration", result.isEmpty())
    }

    @Test
    fun `an indexed request against a wildcard registration is within scope`() {
        val requested = pidRequest(
            ClaimPath(
                listOf(ClaimPathElement.Claim("nationalities"), ClaimPathElement.ArrayElement(0)),
            ),
        )

        val result = registeredForNationalitiesWildcard().findOverAskedClaims(requested)

        assertTrue("an indexed request is covered by a wildcard registration", result.isEmpty())
    }

    @Test
    fun `a nested path keeps its index and stays within scope`() {
        val requested = pidRequest(
            ClaimPath(
                listOf(
                    ClaimPathElement.Claim("addresses"),
                    ClaimPathElement.ArrayElement(0),
                    ClaimPathElement.Claim("street"),
                ),
            ),
        )
        val registration = registeredFor(
            RegisteredClaim(path = claimPath("addresses", null, "street")),
        )

        val result = registration.findOverAskedClaims(requested)

        assertTrue("the index in the middle of the path must be kept", result.isEmpty())
    }

    @Test
    fun `a request outside the registered scope is still reported`() {
        val requested = pidRequest(ClaimPath(listOf(ClaimPathElement.Claim("family_name"))))

        val result = registeredForNationalitiesWildcard().findOverAskedClaims(requested)

        assertEquals(1, result.size)
        assertEquals(SD_JWT_VC_FORMAT, result.single().format)
    }

    /**
     * A registered path that stops at a parent selects the whole subtree below it, so it covers a
     * request that drills into a member of that subtree (OpenID4VP clause 7.3: `["address"]` selects
     * the address claim with its sub-claims as the value).
     */
    @Test
    fun `a request for a member of a registered parent is within scope`() {
        val requested = pidRequest(
            ClaimPath(listOf(ClaimPathElement.Claim("address"), ClaimPathElement.Claim("street_address"))),
        )
        val registration = registeredFor(RegisteredClaim(path = claimPath("address")))

        val result = registration.findOverAskedClaims(requested)

        assertTrue("a registered parent covers a requested member", result.isEmpty())
    }

    @Test
    fun `a request for a sibling of a registered member is reported`() {
        val requested = pidRequest(
            ClaimPath(listOf(ClaimPathElement.Claim("address"), ClaimPathElement.Claim("street_address"))),
        )
        val registration = registeredFor(RegisteredClaim(path = claimPath("address", "postal_code")))

        val result = registration.findOverAskedClaims(requested)

        assertEquals(1, result.size)
        assertEquals(claimPath("address", "street_address"), result.single().path)
    }

    @Test
    fun `a wildcard request against an indexed registration is reported`() {
        val requested = pidRequest(
            ClaimPath(listOf(ClaimPathElement.Claim("nationalities"), ClaimPathElement.AllArrayElements)),
        )
        val registration = registeredFor(RegisteredClaim(path = claimPath("nationalities", 0)))

        val result = registration.findOverAskedClaims(requested)

        assertEquals(1, result.size)
        assertEquals(claimPath("nationalities", null), result.single().path)
    }
}

private fun registeredForNationalitiesWildcard(): RegistrationCertificate =
    registeredFor(RegisteredClaim(path = claimPath("nationalities", null)))

private fun registeredFor(vararg claims: RegisteredClaim): RegistrationCertificate =
    RegistrationCertificate(
        requestedCredentials = listOf(
            RegisteredCredential(
                format = SD_JWT_VC_FORMAT,
                meta = CredentialMeta(vctValues = listOf(PID_VCT)),
                claims = claims.toList(),
            ),
        ),
    )

/** Builds the requested attestations the same way a resolved request does. */
private fun pidRequest(path: ClaimPath): List<RequestedAttestation> =
    DCQL(
        credentials = Credentials(
            listOf(
                CredentialQuery.sdJwtVc(
                    id = QueryId("query_0"),
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(PID_VCT)),
                    claims = listOf(ClaimsQuery.sdJwtVc(path = path)),
                ),
            ),
        ),
        credentialSets = null,
    ).toRequestedAttestations()
