/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.document.format

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.getResourceAsText
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlinx.serialization.json.JsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SdJwtVcDataTest {

    private val sdJwtVcString: String
        get() = getResourceAsText("sample_sd_jwt_vc.txt")
            .replace("\n", "")
            .replace("\r", "")

    private val metadata: IssuerMetadata
        get() = getResourceAsText("sample_sd_jwt_vc_metadata.json")
            .let { IssuerMetadata.fromJson(it) }
            .getOrThrow()


    private lateinit var sdJwtVc: SdJwt<Pair<String, JsonObject>>

    @BeforeTest
    fun setUp() {
        sdJwtVc = DefaultSdJwtOps.unverifiedIssuanceFrom(sdJwtVcString).getOrThrow()
    }


    @Test
    fun `SdJwtVcData claims contains all nested claims from sd-jwt-vc`() {
        val (claims, disclosuresPerClaim) = sdJwtVc.recreateClaimsAndDisclosuresPerClaim()

        val sdJwtVcData = SdJwtVcData(
            format = SdJwtVcFormat(vct = "some vct"),
            issuerMetadata = metadata,
            sdJwtVc = sdJwtVcString,
        )
        val sdJwtVcClaims = sdJwtVcData.claims

        printSdJwtVcClaims(sdJwtVcClaims)

        assertEquals(
            claims.filterNot { it.key in SdJwtVcData.ExcludedIdentifiers }.size,
            sdJwtVcData.claims.size
        )
        assertEquals(
            disclosuresPerClaim.filterNot {
                it.key.head().toString() in SdJwtVcData.ExcludedIdentifiers
            }.size,
            getSdJwtClaims(sdJwtVcClaims).size
        )
    }

    /**
     * Top-level claim by name returns exactly one entry whose value matches the
     * issuer-provided disclosure.
     */
    @Test
    fun `findByPath returns the single top-level claim by name`() {
        val data = sdJwtVcData()

        val result = data.findByPath(
            ClaimPath(ClaimPathElement.Claim("given_name"))
        )

        assertEquals(1, result.size, "expected exactly one match for given_name")
        assertEquals("Tyler", result.first().value)
    }

    /**
     * A nested object path (`place_of_birth.country`) descends through `children`
     * and returns the leaf claim with the inner value.
     */
    @Test
    fun `findByPath descends into nested object members`() {
        val data = sdJwtVcData()

        val result = data.findByPath(
            ClaimPath(
                ClaimPathElement.Claim("place_of_birth"),
                ClaimPathElement.Claim("country"),
            )
        )

        assertEquals(1, result.size, "expected one nested leaf for place_of_birth.country")
        assertEquals("AT", result.first().value)
    }

    /**
     * Indexed array access (`nationalities[0]`) resolves through the parent's
     * `ArrayElement(0)` child.
     */
    @Test
    fun `findByPath resolves an array element by index`() {
        val data = sdJwtVcData()

        val result = data.findByPath(
            ClaimPath(
                ClaimPathElement.Claim("nationalities"),
                ClaimPathElement.ArrayElement(0),
            )
        )

        assertEquals(1, result.size, "expected one match for nationalities[0]")
        assertEquals("AT", result.first().value)
    }

    /**
     * The trailing `AllArrayElements` wildcard fans out across every array-element
     * child. The fixture's `nationalities` array has one entry, so we expect one
     * match — and the matched node is the array-element wrapper, not the parent.
     */
    @Test
    fun `findByPath expands the trailing wildcard to every array element`() {
        val data = sdJwtVcData()

        val result = data.findByPath(
            ClaimPath(
                ClaimPathElement.Claim("nationalities"),
                ClaimPathElement.AllArrayElements,
            )
        )

        assertEquals(1, result.size, "fixture has 1 nationality entry, got ${result.size}")
        assertTrue(
            result.all { it.pathElement is ClaimPathElement.ArrayElement },
            "expected ArrayElement wrappers, got ${result.map { it.pathElement }}",
        )
    }

    /**
     * A path that does not exist anywhere in the credential resolves to the empty
     * list — never an exception.
     */
    @Test
    fun `findByPath returns empty list when nothing matches`() {
        val data = sdJwtVcData()

        val result = data.findByPath(
            ClaimPath(ClaimPathElement.Claim("does_not_exist"))
        )

        assertTrue(result.isEmpty(), "expected no matches, got $result")
    }

    /**
     * Two-level descent into a different nested object (`address.region`) — sanity
     * that the recursive descent works for any object key, not just one fixture
     * field.
     */
    @Test
    fun `findByPath handles deeper nested objects`() {
        val data = sdJwtVcData()

        val result = data.findByPath(
            ClaimPath(
                ClaimPathElement.Claim("address"),
                ClaimPathElement.Claim("region"),
            )
        )

        assertEquals(1, result.size, "expected one match for address.region")
        assertEquals("Lower Austria", result.first().value)
    }

    /**
     * Wrap the same `SdJwtVcData` construction every `findByPath` test uses.
     */
    private fun sdJwtVcData() = SdJwtVcData(
        format = SdJwtVcFormat(vct = "urn:eu.europa.ec.eudi:pid:1"),
        issuerMetadata = metadata,
        sdJwtVc = sdJwtVcString,
    )

    private fun printSdJwtVcClaims(sdJwtVcClaims: List<SdJwtVcClaim>, indent: String = "") {
        for (sdJwtVcClaim in sdJwtVcClaims) {
            println("$indent- PathElement: ${sdJwtVcClaim.pathElement}")
            if (sdJwtVcClaim.value != null) println("$indent  Value: ${sdJwtVcClaim.value}")
            if (sdJwtVcClaim.rawValue.isNotEmpty()) println("$indent  Raw Value: ${sdJwtVcClaim.rawValue}")
            println("$indent  Selectively Disclosable: ${sdJwtVcClaim.selectivelyDisclosable}")
            if (sdJwtVcClaim.issuerMetadata != null) println("$indent  Metadata: ${sdJwtVcClaim.issuerMetadata}")
            if (sdJwtVcClaim.children.isNotEmpty()) {
                println("$indent  Children:")
                printSdJwtVcClaims(sdJwtVcClaim.children, "$indent    ")
            }
        }
    }

    private fun getSdJwtClaims(claims: List<SdJwtVcClaim>): List<SdJwtVcClaim> {
        return claims.flatMap { claim ->
            listOf(claim) + getSdJwtClaims(claim.children)
        }
    }
}