/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.dcapi

import android.content.Intent
import eu.europa.ec.eudi.iso18013.transfer.DeviceResponseBytes
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse

/**
 * A response for the Digital Credential API (DCAPI). There is one subtype per protocol, since the
 * payloads differ:
 *  - [IsoMdocDCAPIResponse] for ISO mdoc — an ISO 18013-5 device response;
 *  - [OpenId4VpDCAPIResponse] for OpenID4VP — a verifiable presentation token.
 *
 * Each carries the [intent] returned to the system. The protocol-specific payload is also kept in
 * structured form, so it remains available even when the
 * [intent] carries an encrypted response.
 */
sealed interface DCAPIResponse : Response {
    val intent: Intent

    companion object {
        @Deprecated(
            message = "Use IsoMdocDCAPIResponse or OpenId4VpDCAPIResponse.",
            replaceWith = ReplaceWith("IsoMdocDCAPIResponse(deviceResponseBytes, documentIds, intent)"),
        )
        operator fun invoke(
            deviceResponseBytes: DeviceResponseBytes,
            intent: Intent,
            documentIds: List<DocumentId> = emptyList(),
        ): DCAPIResponse = IsoMdocDCAPIResponse(deviceResponseBytes, documentIds, intent)
    }
}

/**
 * Response for the ISO mdoc protocol (ISO/IEC TS 18013-7:2025 Annex C).
 *
 * @property deviceResponseBytes the unencrypted ISO 18013-5 device response.
 * @property intent the result intent returned to the system, carrying the encrypted response.
 */
data class IsoMdocDCAPIResponse(
    val deviceResponseBytes: DeviceResponseBytes,
    val documentIds: List<DocumentId>,
    override val intent: Intent,
) : DCAPIResponse {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IsoMdocDCAPIResponse
        if (!deviceResponseBytes.contentEquals(other.deviceResponseBytes)) return false
        if (documentIds != other.documentIds) return false
        if (intent != other.intent) return false
        return true
    }

    override fun hashCode(): Int {
        var result = deviceResponseBytes.contentHashCode()
        result = 31 * result + documentIds.hashCode()
        result = 31 * result + intent.hashCode()
        return result
    }
}

/**
 * Response for the OpenID4VP protocols.
 *
 * @property vpToken the verifiable presentation token produced for the request.
 * @property respondedDocuments the documents included in the response, grouped by query.
 * @property intent the result intent returned to the system, carrying the assembled response
 *   (encrypted when the verifier requested an encrypted response).
 */
data class OpenId4VpDCAPIResponse(
    val vpToken: Consensus.PositiveConsensus,
    val respondedDocuments: Map<QueryId, List<OpenId4VpResponse.RespondedDocument>>,
    override val intent: Intent,
) : DCAPIResponse
