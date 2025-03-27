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

sealed interface OpenId4VpResponse : Response {
    val resolvedRequestObject: ResolvedRequestObject
    val consensus: Consensus.PositiveConsensus.VPTokenConsensus
    val msoMdocNonce: String
    val vpContent: VpContent
        get() = consensus.vpContent

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

    data class DeviceResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val responseBytes: DeviceResponseBytes,
    ) : OpenId4VpResponse {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DeviceResponse

            if (resolvedRequestObject != other.resolvedRequestObject) return false
            if (consensus != other.consensus) return false
            if (!responseBytes.contentEquals(other.responseBytes)) return false
            if (msoMdocNonce != other.msoMdocNonce) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resolvedRequestObject.hashCode()
            result = 31 * result + consensus.hashCode()
            result = 31 * result + responseBytes.contentHashCode()
            result = 31 * result + msoMdocNonce.hashCode()
            return result
        }
    }

    data class GenericResponse(
        override val resolvedRequestObject: ResolvedRequestObject,
        override val consensus: Consensus.PositiveConsensus.VPTokenConsensus,
        override val msoMdocNonce: String,
        val response: List<String>,
    ) : OpenId4VpResponse
}

private fun constructEncryptedParameters(
    alg: JWEAlgorithm,
    msoMdocNonce: String,
): EncryptionParameters? {
    return if (alg in JWEAlgorithm.Family.ECDH_ES) {
        EncryptionParameters.DiffieHellman(apu = Base64URL.encode(msoMdocNonce))
    } else null
}
