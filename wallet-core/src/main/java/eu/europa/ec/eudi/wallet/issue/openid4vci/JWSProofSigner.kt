/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import eu.europa.ec.eudi.wallet.document.IssuanceRequest

/**
 * A [ProofSigner] and [JWSSigner] implementation for JWS.
 * @property popSigner
 * @constructor Creates a JWS proof signer.
 * @param issuanceRequest The issuance request.
 * @param supportedProofAlgorithm The supported proof algorithm.
 *
 */
internal class JWSProofSigner(
    private val issuanceRequest: IssuanceRequest,
    private val supportedProofAlgorithm: SupportedProofType.ProofAlgorithm.Jws
) : ProofSigner(), JWSSigner {

    private val jcaContext = JCAContext()

    /**
     * The JWK of the public key.
     */
    private val jwk = JWK.parseFromPEMEncodedObjects(issuanceRequest.publicKey.pem)

    override val popSigner: PopSigner.Jwt = PopSigner.Jwt(
        algorithm = supportedProofAlgorithm.algorithm,
        bindingKey = JwtBindingKey.Jwk(jwk),
        jwsSigner = this
    )

    override fun getJCAContext(): JCAContext = jcaContext

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
        return mutableSetOf(supportedProofAlgorithm.algorithm)
    }

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        if (header.algorithm != supportedProofAlgorithm.algorithm) {
            throw JOSEException(
                AlgorithmSupportMessage.unsupportedJWSAlgorithm(
                    header.algorithm,
                    supportedJWSAlgorithms()
                )
            )
        }
        return doSign(issuanceRequest, signingInput, supportedProofAlgorithm.signAlgorithmName).let { signature ->
            Base64URL.encode(signature.derToConcat(supportedProofAlgorithm))
        }
    }
}