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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.metaMsoMdoc
import eu.europa.ec.eudi.openid4vp.dcql.metaSdJwtVc
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.openid.dcql.DcqlClaimSet
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim

/**
 * Converts a [CredentialQuery] into a [DcqlCredentialQuery] for the matching pipeline.
 *
 * Throws [IllegalArgumentException] if the format is not supported (anything other than
 * `mso_mdoc` / `dc+sd-jwt`) or if format-specific metadata is missing.
 */
internal fun CredentialQuery.toDcqlCredentialQuery(): DcqlCredentialQuery {
    val mdocDocType: String?
    val vctValues: List<String>?
    when (format) {
        Format.MsoMdoc -> {
            mdocDocType = requireNotNull(metaMsoMdoc?.doctypeValue?.value) {
                "DocType is missing for query with id $id"
            }
            vctValues = null
        }
        Format.SdJwtVc -> {
            mdocDocType = null
            val vct = requireNotNull(metaSdJwtVc?.vctValues) {
                "vct_values is missing for query with id $id"
            }
            require(vct.isNotEmpty()) { "vct_values is empty for query with id $id" }
            vctValues = vct
        }
        else -> throw IllegalArgumentException("Unsupported format ${format.value}")
    }

    val requestedClaims: List<RequestedClaim> = claims.orEmpty().map { claim ->
        when (format) {
            Format.MsoMdoc -> claim.toMdocRequestedClaim(mdocDocType!!)
            Format.SdJwtVc -> claim.toJsonRequestedClaim(vctValues!!)
            else -> throw IllegalArgumentException("Unsupported format ${format.value}")
        }
    }

    val dcqlClaimSets = claimSets.orEmpty().map { set ->
        DcqlClaimSet(claimIdentifiers = set.value.map { it.value })
    }

    val claimIdToClaim: Map<String, RequestedClaim> = requestedClaims
        .mapNotNull { rc -> rc.id?.let { it to rc } }
        .toMap()

    return DcqlCredentialQuery(
        id = id.value,
        format = format.value,
        meta = meta,
        mdocDocType = mdocDocType,
        vctValues = vctValues,
        claims = requestedClaims,
        claimSets = dcqlClaimSets,
        claimIdToClaim = claimIdToClaim,
    )
}

private fun eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery.toMdocRequestedClaim(
    docType: String,
): MdocRequestedClaim {
    // ClaimsQuery.ensureMsoMdoc guarantees path.size == 2 and both elements are Claim.
    val elements = path.value
    require(elements.size == 2) {
        "Mdoc claim path must have exactly two elements (namespace, dataElementName)"
    }
    val namespace = (elements[0] as? ClaimPathElement.Claim)?.name
        ?: error("Mdoc claim path[0] must be a Claim element")
    val dataElement = (elements[1] as? ClaimPathElement.Claim)?.name
        ?: error("Mdoc claim path[1] must be a Claim element")
    return MdocRequestedClaim(
        id = id?.value,
        docType = docType,
        namespaceName = namespace,
        dataElementName = dataElement,
        intentToRetain = intentToRetain ?: false,
        values = values
    )
}

private fun eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery.toJsonRequestedClaim(
    vctValues: List<String>,
): JsonRequestedClaim = JsonRequestedClaim(
    id = id?.value,
    vctValues = vctValues,
    claimPath = path.toJsonArray(),
    values = values
)

/**
 * Converts a [ClaimPath] into a [JsonArray] per OpenID4VP §6.4 / SD-JWT VC claim path
 * semantics:
 *
 *  - [ClaimPathElement.Claim] -> string [JsonPrimitive]
 *  - [ClaimPathElement.ArrayElement] -> integer [JsonPrimitive]
 *  - [ClaimPathElement.AllArrayElements] -> [JsonNull]
 */
internal fun ClaimPath.toJsonArray(): JsonArray = JsonArray(
    value.map { element ->
        when (element) {
            is ClaimPathElement.Claim -> JsonPrimitive(element.name)
            is ClaimPathElement.ArrayElement -> JsonPrimitive(element.index)
            ClaimPathElement.AllArrayElements -> JsonNull
        }
    }
)
