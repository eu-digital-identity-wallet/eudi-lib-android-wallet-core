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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.parsing

import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.SelectPath.Default.query
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.VPTokenConsensusJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Parses an OpenID4VP VP token into one [ClaimInfo] per presented credential. Only claim paths are
 * recorded, never values. Handles SD-JWT VC and mdoc presentations; other formats are
 * skipped.
 *
 * @param rawResponse the raw VP token JSON bytes.
 * @return one [ClaimInfo] per presentation (paths only).
 */
fun parseVp(rawResponse: ByteArray): List<ClaimInfo> {
    val vpToken = VPTokenConsensusJson.decodeFromString<Consensus.PositiveConsensus>(
        String(rawResponse)
    )
    return vpToken.verifiablePresentations.value
        .mapKeys { it.key.value }
        .flatMap { (_, vps) ->
            vps.filterIsInstance<VerifiablePresentation.Generic>()
                .mapNotNull { vp -> parseSingleVp(vp) }
        }
}

/**
 * Parses one [VerifiablePresentation.Generic]. Tries mdoc first, then SD-JWT VC; returns `null` if
 * neither fits.
 */
private fun parseSingleVp(vp: VerifiablePresentation.Generic): ClaimInfo? {
    runCatching {
        val mdocBytes = Base64.getUrlDecoder().decode(vp.value)
        parseMsoMdoc(rawResponse = mdocBytes, sessionTranscript = null).firstOrNull()
    }.getOrNull()?.let { return it }
    return parseSdJwtVc(vp)
}

private fun parseSdJwtVc(vp: VerifiablePresentation.Generic): ClaimInfo? {
    val sdJwt: SdJwt<JwtAndClaims> = getSdJwt(vp.valueWithoutKeyBinding) ?: return null
    return claimInfoFromSdJwt(sdJwt.claims)
}

/** The SD-JWT VP without its key-binding segment. */
val VerifiablePresentation.Generic.valueWithoutKeyBinding: String
    get() = value
        .split("~")
        .dropLast(1)
        .joinToString("~")
        .plus("~")

fun getSdJwt(sdJwt: String): SdJwt<JwtAndClaims>? =
    with(DefaultSdJwtOps) { unverifiedIssuanceFrom(sdJwt).getOrNull() }

/**
 * The SD-JWT claims as a map of path to value.
 */
val SdJwt<JwtAndClaims>.claims: Map<List<String>, JsonElement?>
    get() {
        val (jsonObject, claimPath) = with(DefaultSdJwtOps) {
            this@claims.recreateClaimsAndDisclosuresPerClaim()
        }
        return claimPath.keys.associate {
            it.value.toList().map(Any::toString) to jsonObject.query(it).getOrNull()?.toJsonElement()
        }
    }

/**
 * Builds one [ClaimInfo] from an SD-JWT claim map: the credential identifier is the `vct`, and the
 * claims are the leaf paths (parents dropped when a child is present).
 */
private fun claimInfoFromSdJwt(claims: Map<List<String>, JsonElement?>): ClaimInfo {
    val leafPaths = mutableListOf<List<String>>()
    var vct: String? = null
    // Longest paths first, so a parent is skipped when a child is present.
    claims.toList().sortedByDescending { it.first.size }.forEach { (path, value) ->
        if (leafPaths.none { it.take(path.size) == path }) {
            leafPaths.add(path)
        }
        if (vct == null && path.firstOrNull() == "vct") {
            vct = value?.jsonPrimitive?.content
        }
    }
    return ClaimInfo(
        credentialIdentifier = vct.orEmpty(),
        claims = leafPaths.map { p -> ClaimPath(p.map(ClaimPath.Segment::Key)) },
    )
}