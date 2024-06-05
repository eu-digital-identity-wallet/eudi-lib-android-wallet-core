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
@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofAlgorithm.Cose
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofAlgorithm.Jws

internal sealed interface SupportedProofType {
    val algorithms: List<SupportedProofAlgorithm>

    data class Jwt(override val algorithms: List<Jws>) : SupportedProofType {
        val jwsAlgorithms: List<JWSAlgorithm>
            get() = algorithms.map { it.algorithm }.toList()

        override fun toString(): String {
            return name
        }
    }

    data class Cwt(override val algorithms: List<Cose>) : SupportedProofType {
        override fun toString(): String {
            return name
        }

        val coseAlgorithms: List<CoseAlgorithm>
            get() = algorithms.map { it.coseAlgorithm }.toList()

        val coseCurves: List<CoseCurve>
            get() = algorithms.map { it.coseCurve }.toList()
    }

    val proofType: ProofType
        get() = when (this) {
            is Jwt -> ProofType.JWT
            is Cwt -> ProofType.CWT
        }

    val proofTypeMeta: ProofTypeMeta
        get() = when (this) {
            is Jwt -> ProofTypeMeta.Jwt(algorithms.map { it.algorithm })
            is Cwt -> ProofTypeMeta.Cwt(algorithms.map { it.coseAlgorithm }, algorithms.map { it.coseCurve })
        }

    val name: String
        get() = when (this) {
            is Jwt -> "JWT for ${algorithms.map { it.name }}"
            is Cwt -> "CWT for ${algorithms.map { it.name }}"
        }

    fun selectAlgorithm(issuerProofTypesSupported: ProofTypesSupported): SupportedProofAlgorithm? {
        val issuerSupported = issuerProofTypesSupported.values.associateBy { it.type() }
        val credentialProof = issuerSupported[proofType] ?: return null

        return when (this) {
            is Jwt -> credentialProof.takeIf { it is ProofTypeMeta.Jwt }
                ?.let { it as ProofTypeMeta.Jwt }
                ?.let { cProof -> algorithms.firstOrNull { it.algorithm in cProof.algorithms } }

            is Cwt -> credentialProof.takeIf { it is ProofTypeMeta.Cwt }
                ?.let { it as ProofTypeMeta.Cwt }
                ?.let { cProof ->
                    algorithms.firstOrNull { it.coseAlgorithm in cProof.algorithms && it.coseCurve in cProof.curves }
                }
        }
    }

    fun selectAlgorithm(credentialConfiguration: CredentialConfiguration): SupportedProofAlgorithm? =
        selectAlgorithm(credentialConfiguration.proofTypesSupported)

    companion object {
        val SupportedProofTypes: List<SupportedProofType> = listOf(
            Jwt(listOf(Jws.ES256)),
            Cwt(listOf(Cose.ES256_P_256))
        )

        @JvmStatic
        fun selectProofType(issuerProofTypesSupported: ProofTypesSupported): SupportedProofType? {
            val issuerSupported = issuerProofTypesSupported.values.associateBy { it.type() }
            return SupportedProofTypes.firstOrNull { it.proofType in issuerSupported.keys }
        }

        @JvmStatic
        fun selectProofType(credentialConfiguration: CredentialConfiguration): SupportedProofType? =
            selectProofType(credentialConfiguration.proofTypesSupported)
    }
}

sealed interface SupportedProofAlgorithm {
    val name: String

    @get:Algorithm
    val signAlgorithmName: String

    data class Jws(val algorithm: JWSAlgorithm, @Algorithm override val signAlgorithmName: String) :
        SupportedProofAlgorithm {
        override val name: String
            get() = algorithm.name

        companion object {
            val ES256 = Jws(JWSAlgorithm.ES256, Algorithm.SHA256withECDSA)
        }
    }

    data class Cose(
        val coseAlgorithm: CoseAlgorithm,
        val coseCurve: CoseCurve,
        @Algorithm override val signAlgorithmName: String
    ) : SupportedProofAlgorithm {
        override val name: String
            get() = "${coseAlgorithm.name()} with curve ${coseCurve.name()}"

        companion object {
            val ES256_P_256 = Cose(CoseAlgorithm.ES256, CoseCurve.P_256, Algorithm.SHA256withECDSA)
        }
    }


}