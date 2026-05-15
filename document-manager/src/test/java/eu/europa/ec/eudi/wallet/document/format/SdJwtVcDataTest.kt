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
import eu.europa.ec.eudi.wallet.document.getResourceAsText
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlinx.serialization.json.JsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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