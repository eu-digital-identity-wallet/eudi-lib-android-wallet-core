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

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo

import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.metaMsoMdoc
import eu.europa.ec.eudi.openid4vp.dcql.metaSdJwtVc

/**
 * Projects the credential queries of a DCQL query onto a [RequestedAttestationInfo] list.
 */
internal fun DCQL.toRequestedAttestationInfos(): List<RequestedAttestationInfo> =
    credentials.value.map { it.toRequestedAttestationInfo() }

private fun CredentialQuery.toRequestedAttestationInfo(): RequestedAttestationInfo {
    val claimPaths = claims.orEmpty().map { claimsQuery ->
        claimsQuery.path.value.mapNotNull { (it as? ClaimPathElement.Claim)?.name }
    }

    return RequestedAttestationInfo(
        format = format.value,
        docType = if (format == Format.MsoMdoc) metaMsoMdoc?.doctypeValue?.value else null,
        vctValues = if (format == Format.SdJwtVc) metaSdJwtVc?.vctValues else null,
        claimPaths = claimPaths,
    )
}