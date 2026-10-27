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
import org.multipaz.cbor.Cbor
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
    fun sdJwtIgnoresNestedPaths() {
        assertTrue(sdJwtClaims.missingMandatoryClaims(listOf(listOf("address", "not_issued"))).isEmpty())
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
}
