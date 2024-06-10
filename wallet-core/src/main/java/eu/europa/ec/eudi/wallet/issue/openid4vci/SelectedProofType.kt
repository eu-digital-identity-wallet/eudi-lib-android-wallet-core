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

import eu.europa.ec.eudi.wallet.document.IssuanceRequest

/**
 * Selected proof type. It is used to create a proof signer.
 * @property algorithm the proof algorithm
 */
internal sealed interface SelectedProofType {
    val algorithm: SupportedProofType.ProofAlgorithm

    /**
     * Creates a proof signer.
     * @param issuanceRequest the issuance request
     * @return the proof signer
     */
    fun createProofSigner(issuanceRequest: IssuanceRequest): ProofSigner

    /**
     * Proof type using JWT.
     * @property algorithm the proof algorithm
     * @constructor Creates a new [Jwt] instance.
     * @param algorithm the proof algorithm
     */
    data class Jwt(override val algorithm: SupportedProofType.ProofAlgorithm.Jws) : SelectedProofType {
        override fun createProofSigner(issuanceRequest: IssuanceRequest): ProofSigner {
            return JWSProofSigner(issuanceRequest, algorithm)
        }
    }

    /**
     * Proof type using CWT.
     * @property algorithm the proof algorithm
     * @constructor Creates a new [Cwt] instance.
     * @param algorithm the proof algorithm
     */
    data class Cwt(override val algorithm: SupportedProofType.ProofAlgorithm.Cose) : SelectedProofType {
        override fun createProofSigner(issuanceRequest: IssuanceRequest): ProofSigner {
            return CWTProofSigner(issuanceRequest, algorithm)
        }
    }
}