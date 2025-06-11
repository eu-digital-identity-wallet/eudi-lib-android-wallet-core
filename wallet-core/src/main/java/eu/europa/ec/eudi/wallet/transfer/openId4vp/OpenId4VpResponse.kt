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

    fun Logger.debugPrint(tag: String)

    /**
     * Response type for device-based OpenID4VP presentations (e.g., MSO mdoc).
     *
     * @property sessionTranscript The session transcript bytes used in the device response.
     * @property responseBytes The raw device response bytes.
     * @property documentIds The list of document IDs included in the response.
     */
    data class DeviceResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val sessionTranscript: ByteArray,
        val responseBytes: DeviceResponseBytes,
        val documentIds: List<DocumentId>,
    ) : OpenId4VpResponse {


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
            if (documentIds != other.documentIds) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resolvedRequestObject.hashCode()
            result = 31 * result + consensus.hashCode()
            result = 31 * result + msoMdocNonce.hashCode()
            result = 31 * result + sessionTranscript.contentHashCode()
            result = 31 * result + responseBytes.contentHashCode()
            result = 31 * result + documentIds.hashCode()
            return result
        }
    }

    /**
     * Generic response type for OpenID4VP presentations (non-DCQL).
     *
     * @property response The list of verifiable presentation strings returned to the relying party.
     * @property documentIds The list of document IDs included in the response.
     */
    data class GenericResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val response: List<String>,
        val documentIds: List<DocumentId>,
    ) : OpenId4VpResponse {
        override fun Logger.debugPrint(tag: String) {
            d(tag, "Generic Response: ${response.joinToString("\n")}")
            d(tag, "VpContent: $vpContent")
        }
    }

    /**
     * Response type for DCQL-based OpenID4VP presentations.
     *
     * @property response The list of verifiable presentation strings returned to the relying party.
     * @property documentIds A map from query IDs to the document ID disclosed for each query.
     */
    data class DcqlGenericResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val response: List<String>,
        val documentIds: Map<QueryId, DocumentId>,
    ) : OpenId4VpResponse {
        override fun Logger.debugPrint(tag: String) {
            d(tag, "Generic Response: ${response.joinToString("\n")}")
            d(tag, "VpContent: $vpContent")
        }
    }
}

private fun constructEncryptedParameters(
    alg: JWEAlgorithm,
    msoMdocNonce: String,
): EncryptionParameters? {
    return if (alg in JWEAlgorithm.Family.ECDH_ES) {
        EncryptionParameters.DiffieHellman(apu = Base64URL.encode(msoMdocNonce))
    } else null
}
