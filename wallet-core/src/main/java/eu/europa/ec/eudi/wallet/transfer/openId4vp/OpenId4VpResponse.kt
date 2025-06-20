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

import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.DeviceResponseBytes
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.JarmRequirement
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VpContent
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.util.CBOR.cborPrettyPrint
import kotlinx.serialization.Serializable
import org.bouncycastle.util.encoders.Hex.toHexString

/**
 * Defines the OpenID4VP response types and related properties for wallet-core.
 *
 * This sealed interface and its implementations represent the possible responses to an OpenID4VP
 * (OpenID for Verifiable Presentations) request, including device responses and generic DCQL responses.
 * It provides access to the resolved request, consensus, nonce, verifiable presentation content,
 * and encryption parameters for JARM (JWT Secured Authorization Response Mode) requirements.
 *
 * Implementations of this interface are used to return results to relying parties after successful
 * or failed credential presentations.
 */
sealed interface OpenId4VpResponse : Response {
    /**
     * The resolved OpenID4VP request object.
     */
    val resolvedRequestObject: ResolvedRequestObject

    /**
     * The consensus result containing the verifiable presentation token.
     */
    val consensus: Consensus.PositiveConsensus.VPTokenConsensus

    /**
     * The nonce used for MSO mdoc presentations.
     */
    val msoMdocNonce: String

    /**
     * The verifiable presentation content extracted from the consensus.
     */
    val vpContent: VpContent
        get() = consensus.vpContent

    /**
     * The list of responded documents.
     * Can either be index-based or query-based.
     *
     * Indicates which documents were responded to the relying party
     * and where each is positioned in the response.
     */
    val respondedDocuments: List<RespondedDocument>

    /**
     * The encryption parameters for JARM, if required by the relying party.
     * Returns null if encryption is not required.
     */
    val encryptionParameters: EncryptionParameters?
        get() = when (val jarmReq = resolvedRequestObject.jarmRequirement) {
            is JarmRequirement.Encrypted -> constructEncryptedParameters(
                jarmReq.responseEncryptionAlg,
                msoMdocNonce
            )

            is JarmRequirement.SignedAndEncrypted -> constructEncryptedParameters(
                jarmReq.encryptResponse.responseEncryptionAlg,
                msoMdocNonce
            )

            else -> null
        }

    /**
     * Extension function for Logger that prints detailed debug information about the response.
     *
     * @param tag The tag to use for logging the response information
     */
    fun Logger.debugPrint(tag: String)

    /**
     * Response type for device-based OpenID4VP presentations (e.g., MSO mdoc).
     *
     * This class represents responses for ISO 18013-5 mobile driving license (mDL) formatted presentations
     * and other device-based credential disclosures using the MSO mdoc format.
     *
     * @property resolvedRequestObject The original OpenID4VP request that was resolved and processed
     * @property consensus The positive consensus containing the verifiable presentation token
     * @property msoMdocNonce The nonce used for MSO mdoc presentations to ensure freshness
     * @property sessionTranscript The session transcript bytes used in the device response for binding the presentation
     * @property responseBytes The raw CBOR-encoded device response bytes to be transmitted to the verifier
     * @property respondedDocuments List of documents included in this response with their metadata
     */
    data class DeviceResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val sessionTranscript: ByteArray,
        val responseBytes: DeviceResponseBytes,
        override val respondedDocuments: List<RespondedDocument>
    ) : OpenId4VpResponse {

        /**
         * The list of document IDs included in the response, sorted by their index.
         * This is derived from the [respondedDocuments] property, filtering for IndexBased documents.
         */
        val documentIds: List<DocumentId>
            get() = respondedDocuments
                .filterIsInstance<RespondedDocument.IndexBased>()
                .sortedBy { it.index }
                .map { it.documentId }

        /**
         * Prints detailed debug information about the device response.
         *
         * Outputs include:
         * - The raw response bytes in hexadecimal format
         * - The CBOR-pretty-printed response
         * - The verifiable presentation content
         *
         * @param tag The tag to use for logging
         */
        override fun Logger.debugPrint(tag: String) {
            d(tag, "Device Response (hex): ${toHexString(responseBytes)}")
            d(tag, "Device Response (cbor): ${cborPrettyPrint(responseBytes)}")
            d(tag, "VpContent: $vpContent")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DeviceResponse

            if (resolvedRequestObject != other.resolvedRequestObject) return false
            if (consensus != other.consensus) return false
            if (msoMdocNonce != other.msoMdocNonce) return false
            if (!sessionTranscript.contentEquals(other.sessionTranscript)) return false
            if (!responseBytes.contentEquals(other.responseBytes)) return false
            if (respondedDocuments != other.respondedDocuments) return false
            if (documentIds != other.documentIds) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resolvedRequestObject.hashCode()
            result = 31 * result + consensus.hashCode()
            result = 31 * result + msoMdocNonce.hashCode()
            result = 31 * result + sessionTranscript.contentHashCode()
            result = 31 * result + responseBytes.contentHashCode()
            result = 31 * result + respondedDocuments.hashCode()
            result = 31 * result + documentIds.hashCode()
            return result
        }
    }

    /**
     * Generic response type for OpenID4VP presentations (non-DCQL).
     *
     * This class represents responses for verifiable presentation exchanges that use
     * presentation exchange formats rather than DCQL, typically with formats like
     * Presentation Exchange or SD-JWT VC.
     *
     * @property resolvedRequestObject The original OpenID4VP request that was resolved and processed
     * @property consensus The positive consensus containing the verifiable presentation token
     * @property msoMdocNonce The nonce used for MSO mdoc presentations (if applicable)
     * @property response The list of verifiable presentation strings returned to the relying party
     * @property respondedDocuments List of documents included in this response with their metadata
     */
    data class GenericResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val response: List<String>,
        override val respondedDocuments: List<RespondedDocument>
    ) : OpenId4VpResponse {
        /**
         * The list of document IDs included in the response, sorted by their index.
         * This is derived from the [respondedDocuments] property, filtering for IndexBased documents.
         */
        val documentIds: List<DocumentId>
            get() = respondedDocuments
                .filterIsInstance<RespondedDocument.IndexBased>()
                .sortedBy { it.index }
                .map { it.documentId }

        /**
         * Prints detailed debug information about the generic response.
         *
         * @param tag The tag to use for logging
         */
        override fun Logger.debugPrint(tag: String) {
            d(tag, "Generic Response: ${response.joinToString("\n")}")
            d(tag, "VpContent: $vpContent")
        }
    }

    /**
     * Response type for DCQL-based OpenID4VP presentations.
     *
     * This class represents responses for DCQL (Digital Credentials Query Language) format
     * credential presentations, which include query-based document references and structured
     * verification responses.
     *
     * @property resolvedRequestObject The original OpenID4VP request that was resolved and processed
     * @property consensus The positive consensus containing the verifiable presentation token
     * @property msoMdocNonce The nonce used for MSO mdoc presentations (if applicable)
     * @property response Map of query IDs to their corresponding verifiable presentation strings
     * @property respondedDocuments List of documents included in this response with their metadata
     */
    data class DcqlResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val response: Map<QueryId, String>,
        override val respondedDocuments: List<RespondedDocument>
    ) : OpenId4VpResponse {
        /**
         * Prints detailed debug information about the DCQL response.
         *
         * Outputs a formatted list of query IDs and their corresponding responses,
         * along with the verifiable presentation content.
         *
         * @param tag The tag to use for logging
         */
        override fun Logger.debugPrint(tag: String) {
            val message = "DCQL Response: ${
                response.map { (queryId, respStr) -> "$queryId: $respStr" }.joinToString("\n")
            }"
            d(tag, message)
            d(tag, "VpContent: $vpContent")
        }
    }

    /**
     * Interface representing a document that was included in an OpenID4VP response.
     *
     * This can be either index-based (positioned by index in the response) or
     * query-based (associated with a specific query ID).
     */
    @Serializable
    sealed interface RespondedDocument {
        /**
         * The identifier of the document that was responded
         */
        val documentId: DocumentId

        /**
         * The format of the document (e.g., "mso_mdoc", "sd_jwt_vc")
         */
        val format: String

        /**
         * Index-based representation of a responded document.
         *
         * Used when documents are positioned by index in the response,
         * typically for device-based or presentation exchange responses.
         *
         * @property documentId The identifier of the document
         * @property format The format of the document
         * @property index The position of this document in the response array
         */
        @Serializable
        data class IndexBased(
            override val documentId: DocumentId,
            override val format: String,
            val index: Int
        ) : RespondedDocument

        /**
         * Query-based representation of a responded document.
         *
         * Used when documents are associated with specific query IDs,
         * typically for DCQL-based responses.
         *
         * @property documentId The identifier of the document
         * @property format The format of the document
         * @property queryId The query identifier this document responds to
         */
        @Serializable
        data class QueryBased(
            override val documentId: DocumentId,
            override val format: String,
            val queryId: String
        ) : RespondedDocument
    }
}

/**
 * Constructs encryption parameters for JARM responses based on the specified algorithm.
 *
 * For ECDH-ES family algorithms, this creates Diffie-Hellman parameters with the
 * provided nonce as the Agreement PartyUInfo (APU).
 *
 * @param alg The JWE algorithm to use for encryption
 * @param msoMdocNonce The nonce to use as the APU for Diffie-Hellman key agreement
 * @return EncryptionParameters for the specified algorithm, or null if not supported
 */
private fun constructEncryptedParameters(
    alg: JWEAlgorithm,
    msoMdocNonce: String,
): EncryptionParameters? {
    return if (alg in JWEAlgorithm.Family.ECDH_ES) {
        EncryptionParameters.DiffieHellman(apu = Base64URL.encode(msoMdocNonce))
    } else null
}
