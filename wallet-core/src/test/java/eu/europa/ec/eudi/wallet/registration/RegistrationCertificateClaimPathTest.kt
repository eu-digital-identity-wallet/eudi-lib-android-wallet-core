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

import eu.europa.ec.eudi.wallet.registration.relyingparty.RequestedAttestation
import eu.europa.ec.eudi.wallet.registration.relyingparty.findOverAskedClaims
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PID_VCT = "urn:eudi:pid:1"
private const val SD_JWT_VC_FORMAT = "dc+sd-jwt"

private val json = Json { ignoreUnknownKeys = true }

/**
 * Reading the claim paths a registration certificate declares for the credentials it may request
 * (ETSI TS 119 475 clause 5.2.4 `credentials`). An element of a path is a claim name, an array index,
 * or the wildcard for all elements of an array (OpenID4VP clause 7.1).
 */
class RegistrationCertificateClaimPathTest {

    @Test
    fun `path elements are read as names, indices and the wildcard`() {
        val claims = registeredClaims(
            """["family_name"]""",
            """["nationalities", null]""",
            """["nationalities", 0]""",
            """["addresses", 0, "street"]""",
        )

        assertEquals(
            listOf(
                claimPath("family_name"),
                claimPath("nationalities", null),
                claimPath("nationalities", 0),
                claimPath("addresses", 0, "street"),
            ),
            claims.map { it.path },
        )
    }

    @Test
    fun `a number and a quoted number are read differently`() {
        val claims = registeredClaims("""["nationalities", 0]""", """["nationalities", "0"]""")

        assertEquals(ClaimPathElement.ArrayElement(0), claims.first().path.last())
        assertEquals(ClaimPathElement.Claim("0"), claims.last().path.last())
    }

    /**
     * Only a non-negative integer is an array index, so a negative number is read as a claim name. It
     * then matches no requested claim, which reports the request rather than granting it.
     */
    @Test
    fun `a negative number is not an array index`() {
        val claim = registeredClaims("""["nationalities", -1]""").single()

        assertEquals(ClaimPathElement.Claim("-1"), claim.path.last())
    }

    @Test
    fun `a declared wildcard covers the requested elements of that array`() {
        val certificate = certificateRequesting("""["nationalities", null]""")

        val overAsked = certificate.findOverAskedClaims(
            listOf(
                pidRequestFor(
                    claimPath("nationalities", null),
                    claimPath("nationalities", 0),
                ),
            ),
        )

        assertTrue("reported as over-asked: $overAsked", overAsked.isEmpty())
    }

    /**
     * A claim entry with no path selects nothing, so it must not widen the registered scope. An empty
     * path is not a valid claims path pointer (OpenID4VP clause 7.1).
     */
    @Test
    fun `a claim declared with an empty path grants nothing`() {
        val certificate = certificateRequesting("""[]""")

        val overAsked = certificate.findOverAskedClaims(
            listOf(pidRequestFor(claimPath("family_name"))),
        )

        assertEquals(claimPath("family_name"), overAsked.single().path)
    }

    @Test
    fun `a declared index does not cover another index of the same array`() {
        val certificate = certificateRequesting("""["nationalities", 0]""")

        val overAsked = certificate.findOverAskedClaims(
            listOf(pidRequestFor(claimPath("nationalities", 1))),
        )

        assertEquals(claimPath("nationalities", 1), overAsked.single().path)
    }
}

/** Decodes a certificate payload that may request the given claim paths from a PID. */
private fun certificateRequesting(vararg paths: String): RegistrationCertificate =
    json.decodeFromString<RegistrationCertificateDto>(
        """
        {
          "credentials": [
            {
              "format": "$SD_JWT_VC_FORMAT",
              "meta": { "vct_values": ["$PID_VCT"] },
              "claim": [${paths.joinToString(",") { """{"path": $it}""" }}]
            }
          ]
        }
        """.trimIndent(),
    ).toRegistrationCertificate()

private fun registeredClaims(vararg paths: String): List<RegisteredClaim> =
    certificateRequesting(*paths).requestedCredentials.single().claims

private fun pidRequestFor(vararg paths: List<ClaimPathElement>): RequestedAttestation =
    RequestedAttestation(
        format = SD_JWT_VC_FORMAT,
        meta = CredentialMeta(vctValues = listOf(PID_VCT)),
        claimPaths = paths.toList(),
    )
