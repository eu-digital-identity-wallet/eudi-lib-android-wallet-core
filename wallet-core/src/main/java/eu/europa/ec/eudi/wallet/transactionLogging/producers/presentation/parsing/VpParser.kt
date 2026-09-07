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
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.RFC7519
import eu.europa.ec.eudi.sdjwt.RFC7800
import eu.europa.ec.eudi.sdjwt.RFC9901
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.SdJwtVcSpec
import eu.europa.ec.eudi.sdjwt.TokenStatusListSpec
import eu.europa.ec.eudi.sdjwt.vc.SelectPath.Default.query
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.VPTokenConsensusJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
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
 * The claims that hold the credential together rather than say anything about the User. They are in
 * every SD-JWT payload whether or not anything was disclosed, so they were not presented (TS10 §3.2).
 */
private val STRUCTURAL_CLAIMS: Set<String> = setOf(
    RFC7519.ISSUER,
    RFC7519.SUBJECT,
    RFC7519.AUDIENCE,
    RFC7519.EXPIRATION_TIME,
    RFC7519.NOT_BEFORE,
    RFC7519.ISSUED_AT,
    RFC7519.JWT_ID,
    RFC7800.CNF,
    RFC9901.CLAIM_SD,
    RFC9901.CLAIM_SD_ALG,
    SdJwtVcSpec.VCT,
    SdJwtVcSpec.VCT_INTEGRITY,
    TokenStatusListSpec.STATUS,
)

/**
 * Builds one [ClaimInfo] from an SD-JWT claim map: the credential identifier is the `vct`, and the
 * claims are the leaf paths (parents dropped when a child is present).
 *
 * The recreated payload holds every claim of the credential, so the structural ones are dropped.
 */
private fun claimInfoFromSdJwt(claims: Map<List<String>, JsonElement?>): ClaimInfo {
    val vct = claims.entries
        .firstOrNull { (path, _) -> path == listOf(SdJwtVcSpec.VCT) }
        ?.let { (_, value) -> (value as? JsonPrimitive)?.content }

    val leafPaths = mutableListOf<List<String>>()
    claims.keys
        .filter { it.firstOrNull() !in STRUCTURAL_CLAIMS }
        .sortedByDescending { it.size }
        .forEach { path ->
            if (leafPaths.none { it.take(path.size) == path }) {
                leafPaths.add(path)
            }
        }
    return ClaimInfo(
        credentialIdentifier = vct.orEmpty(),
        claims = leafPaths.map { p -> ClaimPath(p.map(ClaimPath.Segment::Key)) },
    )
}