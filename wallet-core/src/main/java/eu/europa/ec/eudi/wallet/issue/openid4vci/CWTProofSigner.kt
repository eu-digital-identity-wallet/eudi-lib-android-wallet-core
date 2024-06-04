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

import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.CoseAlgorithm
import eu.europa.ec.eudi.openid4vci.CoseCurve
import eu.europa.ec.eudi.openid4vci.CwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.document.IssuanceRequest

internal class CWTProofSigner(
    private val issuanceRequest: IssuanceRequest,
    private val coseAlgorithm: CoseAlgorithm,
    private val coseCurve: CoseCurve
) : ProofSigner() {

    private val jwk = JWK.parseFromPEMEncodedObjects(issuanceRequest.publicKey.pem)
    override val popSigner: PopSigner.Cwt = PopSigner.Cwt(
        algorithm = coseAlgorithm,
        curve = coseCurve,
        bindingKey = CwtBindingKey.CoseKey(jwk),
        sign = this::sign
    )

    private val algorithm
        get() = Pair(coseAlgorithm, coseCurve).let {
            CWTAlgorithmMap[it] ?: throw UnsupportedAlgorithmException()
        }

    fun sign(signingInput: ByteArray): ByteArray {
        return doSign(issuanceRequest, signingInput, algorithm)
    }

    companion object {
        private val CWTAlgorithmMap = mapOf(
            Pair(CoseAlgorithm.ES256, CoseCurve.P_256) to Algorithm.SHA256withECDSA,
        )
    }
}