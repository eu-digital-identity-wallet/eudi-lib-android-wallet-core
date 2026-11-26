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

package eu.europa.ec.eudi.wallet.document.credential

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.wallet.document.getResourceAsText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.document.NameSpacedData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class MandatoryClaimsTest {

    private val pidNameSpace = "eu.europa.ec.eudi.pid.1"

    private val sdJwtClaims: JsonObject
        get() = getResourceAsText("sample_sd_jwt_vc.txt")
            .replace("\n", "")
            .replace("\r", "")
            .let { DefaultSdJwtOps.unverifiedIssuanceFrom(it).getOrThrow() }
            .recreateClaimsAndDisclosuresPerClaim()
            .first

    private val mdoc: ByteArray
        get() = getResourceAsText("issuer_data_pid.hex").trim().hexToByteArray(HexFormat.Default)

    private val mdocData: NameSpacedData
        get() = NameSpacedData.fromIssuerProvidedData(mdoc)

    @Test
    fun sdJwtWithAllMandatoryClaimsPasses() {
        val paths = listOf(listOf("family_name"), listOf("given_name"), listOf("birthdate"))
        assertTrue(sdJwtClaims.missingMandatoryClaims(paths).isEmpty())
    }

    @Test
    fun sdJwtMissingMandatoryClaimFails() {
        val paths = listOf(listOf("family_name"), listOf("not_issued"))
        assertEquals(listOf(listOf("not_issued")), sdJwtClaims.missingMandatoryClaims(paths))
    }

    @Test
    fun sdJwtNestedPathInObjectPasses() {
        val paths = listOf(listOf("address", "street_address"))
        assertTrue(sdJwtClaims.missingMandatoryClaims(paths).isEmpty())
    }

    @Test
    fun sdJwtNestedPathMissingInObjectFails() {
        val paths = listOf(listOf("address", "not_issued"))
        assertEquals(paths, sdJwtClaims.missingMandatoryClaims(paths))
    }

    @Test
    fun sdJwtNestedPathBelowPrimitiveFails() {
        val paths = listOf(listOf("family_name", "first"))
        assertEquals(paths, sdJwtClaims.missingMandatoryClaims(paths))
    }

    @Test
    fun sdJwtNestedPathSelectsAllArrayElements() {
        val claims = buildJsonObject {
            putJsonArray("nationalities") {
                addJsonObject { put("country_code", "NL") }
                addJsonObject { put("name", "Netherlands") }
            }
        }
        val present = listOf("nationalities", "country_code")
        val missing = listOf("nationalities", "region")
        assertEquals(listOf(missing), claims.missingMandatoryClaims(listOf(present, missing)))
    }

    @Test
    fun mdocWithAllMandatoryClaimsPasses() {
        val paths = listOf(listOf(pidNameSpace, "family_name"), listOf(pidNameSpace, "given_name"))
        assertTrue(mdocData.missingMandatoryClaims(paths).isEmpty())
    }

    @Test
    fun mdocMissingMandatoryClaimFails() {
        val paths = listOf(listOf(pidNameSpace, "not_issued"), listOf("other.namespace", "family_name"))
        assertEquals(paths, mdocData.missingMandatoryClaims(paths))
    }

    @Test
    fun mdocWithoutNameSpacesFailsAllMandatoryClaims() {
        val withoutNameSpaces = Cbor.encode(buildCborMap {
            put("issuerAuth", Cbor.decode(mdoc)["issuerAuth"])
        })
        val paths = listOf(listOf(pidNameSpace, "family_name"))
        assertEquals(
            paths,
            NameSpacedData.fromIssuerProvidedData(withoutNameSpaces).missingMandatoryClaims(paths),
        )
    }

    private val nestedMdocData: NameSpacedData
        get() = NameSpacedData.Builder()
            .putEntry(pidNameSpace, "family_name", Cbor.encode(Tstr("Doe")))
            .putEntry(pidNameSpace, "place_of_birth", Cbor.encode(buildCborMap {
                put("country", "NL")
            }))
            .putEntry(pidNameSpace, "nationalities", Cbor.encode(buildCborArray {
                add(buildCborMap { put("country_code", "NL") })
                add(buildCborMap { put("name", "Netherlands") })
            }))
            .putEntry(pidNameSpace, "birth_date", Cbor.encode(Tagged(1004, buildCborMap {
                put("year", 1990)
            })))
            .build()

    @Test
    fun mdocNestedPathInMapPasses() {
        val paths = listOf(listOf(pidNameSpace, "place_of_birth", "country"))
        assertTrue(nestedMdocData.missingMandatoryClaims(paths).isEmpty())
    }

    @Test
    fun mdocNestedPathMissingInMapFails() {
        val paths = listOf(listOf(pidNameSpace, "place_of_birth", "locality"))
        assertEquals(paths, nestedMdocData.missingMandatoryClaims(paths))
    }

    @Test
    fun mdocNestedPathSelectsAllArrayElements() {
        val present = listOf(pidNameSpace, "nationalities", "country_code")
        val missing = listOf(pidNameSpace, "nationalities", "region")
        assertEquals(listOf(missing), nestedMdocData.missingMandatoryClaims(listOf(present, missing)))
    }

    @Test
    fun mdocNestedPathBelowPrimitiveFails() {
        val paths = listOf(listOf(pidNameSpace, "family_name", "first"))
        assertEquals(paths, nestedMdocData.missingMandatoryClaims(paths))
    }

    @Test
    fun mdocNestedPathInTaggedValuePasses() {
        val paths = listOf(listOf(pidNameSpace, "birth_date", "year"))
        assertTrue(nestedMdocData.missingMandatoryClaims(paths).isEmpty())
    }

    @Test
    fun mdocNestedPathUnderMissingElementFails() {
        val paths = listOf(listOf(pidNameSpace, "address", "street"))
        assertEquals(paths, nestedMdocData.missingMandatoryClaims(paths))
    }
}
