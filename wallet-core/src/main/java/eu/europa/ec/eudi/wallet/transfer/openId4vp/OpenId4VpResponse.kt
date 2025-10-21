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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentId
import kotlinx.serialization.Serializable

/**
 * Represents an OpenID4VP (OpenID for Verifiable Presentations) response.
 *
 * This class encapsulates the complete response to an OpenID4VP request, including
 * the resolved request object, consensus result, nonce for MSO mdoc presentations,
 * and the list of documents that were included in the response.
 *
 * @property resolvedRequestObject The resolved OpenID4VP request object that was processed
 * @property vpToken The consensus result containing the verifiable presentation token
 * @property msoMdocNonce The nonce used for MSO mdoc presentations
 * @property respondedDocuments The list of responded documents with their metadata
 * @property encryptionParameters The encryption parameters, if required by the relying party
 */
class OpenId4VpResponse(
    val resolvedRequestObject: ResolvedRequestObject,
    val vpToken: Consensus.PositiveConsensus.VPTokenConsensus,
    val msoMdocNonce: String,
    val respondedDocuments: Map<QueryId, List<RespondedDocument>>,
) : Response {
    /**
     * The encryption parameters for JARM, if required by the relying party.
     * Returns null if encryption is not required.
     */
    val encryptionParameters: EncryptionParameters?
        get() = resolvedRequestObject.responseEncryptionSpecification
            ?.let { _ ->
                EncryptionParameters.DiffieHellman(apu = Base64URL.encode(msoMdocNonce))
            }

    /**
     * Represents a document that was included in an OpenID4VP response.
     *
     * This data class provides metadata about documents that were presented
     * in response to an OpenID4VP request, supporting both index-based and
     * query-based document identification.
     *
     * @property documentId The unique identifier of the document that was responded
     * @property format The format of the document (e.g., "mso_mdoc", "sd_jwt_vc")
     */
    @Serializable
    data class RespondedDocument(
        /**
         * The unique identifier of the document that was responded.
         */
        val documentId: DocumentId,

        /**
         * The format of the document (e.g., "mso_mdoc", "sd_jwt_vc").
         * Indicates the credential format used for this specific document.
         */
        val format: String,
    )
}