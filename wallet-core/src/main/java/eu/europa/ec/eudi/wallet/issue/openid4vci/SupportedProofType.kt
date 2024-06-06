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
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm.Cose
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm.Jws
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config.ProofType as ConfigProofType

internal sealed interface SupportedProofType {
    val algorithms: List<ProofAlgorithm>
    val proofType: ProofType
    val name: String

    data class Jwt(override val algorithms: List<Jws>, override val proofType: ProofType = ProofType.JWT) :
        SupportedProofType {
        override val name: String
            get() = "JWT for [${algorithms.joinToString(", ") { it.name }}]"
    }

    data class Cwt(override val algorithms: List<Cose>, override val proofType: ProofType = ProofType.CWT) :
        SupportedProofType {
        override val name: String
            get() = "CWT for [${algorithms.joinToString(", ") { it.name }}]"
    }

    companion object {

        private var priority: List<ProofType>? = null

        private val supportedProofTypes: List<SupportedProofType>
            get() = listOf(
                Jwt(listOf(Jws.ES256)),
                Cwt(listOf(Cose.ES256_P_256))
            ).prioritize()

        private fun List<SupportedProofType>.prioritize(): List<SupportedProofType> {
            return priority?.let { order ->
                val supportedProofTypesMap = associateBy { it.proofType }
                order.mapNotNull { supportedProofTypesMap[it] }
            } ?: this

        }

        fun prioritize(supportedProofTypesPrioritized: List<ConfigProofType>) = apply {
            priority = supportedProofTypesPrioritized.map { it.type }
        }

        @JvmStatic
        fun select(credentialConfiguration: CredentialConfiguration): SelectedProofType {
            val issuerProofTypesSupportedMap =
                credentialConfiguration.proofTypesSupported.values.associateBy { it.type() }
            for (supportedProofType in supportedProofTypes) {
                val issuerProofTypeMeta = issuerProofTypesSupportedMap[supportedProofType.proofType] ?: continue

                when (issuerProofTypeMeta) {
                    is ProofTypeMeta.Cwt -> {
                        val issuerCoseAlgorithms = issuerProofTypeMeta.algorithms
                        val issuerCoseCurves = issuerProofTypeMeta.curves
                        for (supportedProofAlgorithm in supportedProofType.algorithms) {
                            if (supportedProofAlgorithm !is Cose) continue
                            if (supportedProofAlgorithm.coseAlgorithm in issuerCoseAlgorithms &&
                                supportedProofAlgorithm.coseCurve in issuerCoseCurves
                            ) {
                                return SelectedProofType.Cwt(supportedProofAlgorithm)
                            }
                        }
                    }

                    is ProofTypeMeta.Jwt -> {
                        val issuerAlgorithms = issuerProofTypeMeta.algorithms
                        for (supportedProofAlgorithm in supportedProofType.algorithms) {
                            if (supportedProofAlgorithm !is Jws) continue
                            if (supportedProofAlgorithm.algorithm in issuerAlgorithms) {
                                return SelectedProofType.Jwt(supportedProofAlgorithm)
                            }
                        }
                    }

                    else -> continue
                }
            }
            throw UnsupportedProofTypeException(supportedProofTypes)
        }
    }

    sealed interface ProofAlgorithm {
        val name: String

        @get:Algorithm
        val signAlgorithmName: String

        data class Jws(
            val algorithm: JWSAlgorithm,
            @Algorithm override val signAlgorithmName: String,
            override val name: String = algorithm.name
        ) : ProofAlgorithm {

            companion object {
                val ES256 = Jws(JWSAlgorithm.ES256, Algorithm.SHA256withECDSA)
            }
        }

        data class Cose(
            val coseAlgorithm: CoseAlgorithm,
            val coseCurve: CoseCurve,
            @Algorithm override val signAlgorithmName: String,
            override val name: String = "${coseAlgorithm.name()}_${coseCurve.name()}"
        ) : ProofAlgorithm {

            companion object {
                val ES256_P_256 = Cose(CoseAlgorithm.ES256, CoseCurve.P_256, Algorithm.SHA256withECDSA)
            }
        }
    }
}