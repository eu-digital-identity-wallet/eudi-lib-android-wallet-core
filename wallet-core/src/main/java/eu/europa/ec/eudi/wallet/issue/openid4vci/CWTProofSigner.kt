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
import eu.europa.ec.eudi.openid4vci.CwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import eu.europa.ec.eudi.wallet.document.IssuanceRequest

/**
 * A [ProofSigner] implementation for CWT.
 * @property issuanceRequest the issuance request
 * @property coseAlgorithm the COSE algorithm to use
 * @property coseCurve the COSE curve to use
 * @constructor Creates a CWT proof signer.
 * @param issuanceRequest The issuance request.
 * @param coseAlgorithm The COSE algorithm to use.
 * @param coseCurve The COSE curve to use.
 */
internal class CWTProofSigner(
    private val issuanceRequest: IssuanceRequest,
    private val supportedProofAlgorithm: SupportedProofType.ProofAlgorithm.Cose
) : ProofSigner() {

    /**
     * The JWK of the public key.
     */
    private val jwk = JWK.parseFromPEMEncodedObjects(issuanceRequest.publicKey.pem)
    override val popSigner: PopSigner.Cwt = PopSigner.Cwt(
        algorithm = supportedProofAlgorithm.coseAlgorithm,
        curve = supportedProofAlgorithm.coseCurve,
        bindingKey = CwtBindingKey.CoseKey(jwk),
        sign = this::sign
    )

    /**
     * Signs the signing input with the authentication key from issuance request for the given algorithm and curve.
     * @param signingInput The input to sign.
     * @throws UserAuthRequiredException If user authentication is required.
     * @throws Throwable If an error occurs during signing.
     * @return The signature of the signing input.
     */
    fun sign(signingInput: ByteArray): ByteArray {
        return doSign(issuanceRequest, signingInput, supportedProofAlgorithm.signAlgorithmName)
    }
}