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
import com.nimbusds.jose.crypto.impl.ECDSA
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm.Cose
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm.Jws
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config.ProofType as ConfigProofType

/**
 * Supported proof types. It is used to select a proof type.
 * @property algorithms the proof algorithms
 * @property proofType the proof type
 * @property name the name of the supported proof type
 */
internal sealed interface SupportedProofType {
    val algorithms: List<ProofAlgorithm>
    val proofType: ProofType
    val name: String

    /**
     * Proof type using JWT.
     * @property algorithms the proof algorithms
     * @property proofType the proof type
     * @constructor Creates a new [Jwt] instance.
     * @param algorithms the proof algorithms
     * @param proofType the proof type
     */
    data class Jwt(override val algorithms: List<Jws>, override val proofType: ProofType = ProofType.JWT) :
        SupportedProofType {
        override val name: String
            get() = "JWT for [${algorithms.joinToString(", ") { it.name }}]"
    }

    /**
     * Proof type using CWT.
     * @property algorithms the proof algorithms
     * @property proofType the proof type
     * @constructor Creates a new [Cwt] instance.
     * @param algorithms the proof algorithms
     * @param proofType the proof type
     */
    data class Cwt(override val algorithms: List<Cose>, override val proofType: ProofType = ProofType.CWT) :
        SupportedProofType {
        override val name: String
            get() = "CWT for [${algorithms.joinToString(", ") { it.name }}]"
    }

    /**
     * Companion object for [SupportedProofType] instances.
     */
    companion object {

        /**
         * Prioritized proof types.
         */
        private var priority: List<ProofType>? = null

        /**
         * Supported proof types.
         */
        private val supportedProofTypes: List<SupportedProofType>
            get() = listOf(
                Jwt(listOf(Jws.ES256)),
                Cwt(listOf(Cose.ES256_P_256))
            ).prioritize()

        /**
         * Prioritizes the supported proof types.
         * @receiver the supported proof types
         * @return the supported proof types prioritized
         */
        private fun List<SupportedProofType>.prioritize(): List<SupportedProofType> {
            return priority?.let { order ->
                val supportedProofTypesMap = associateBy { it.proofType }
                order.mapNotNull { supportedProofTypesMap[it] }
            } ?: this

        }

        /**
         * Sets the priority of the supported proof types.
         * @param supportedProofTypesPrioritized the supported proof types prioritized
         */
        fun withPriority(supportedProofTypesPrioritized: List<ConfigProofType>) = apply {
            priority = supportedProofTypesPrioritized.map { it.type }
        }

        /**
         * Selects a proof type based on the credential configuration. It takes into account the supported proof types and their algorithms.
         * Also, the selection is based on the given priority
         * @see withPriority to set priority for the selection of a proof type
         * @param credentialConfiguration the credential configuration
         * @return the selected proof type
         * @throws UnsupportedProofTypeException if the proof type is not supported
         */
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

    /**
     * Proof algorithm for the supported proof type.
     * @property name the name of the proof algorithm
     * @property signAlgorithmName the name of the sign algorithm that it is used in the [eu.europa.ec.eudi.wallet.document.UnsignedDocument.signWithAuthKey]
     * @property signatureByteArrayLength the length of the signature byte array to be use when converting the signature from DER to Concat format
     * method
     */
    sealed interface ProofAlgorithm {
        val name: String

        @get:Algorithm
        val signAlgorithmName: String

        val signatureByteArrayLength: Int

        /**
         * Proof algorithm for signing JWT with JWS.
         * @property algorithm the JWS algorithm
         * @constructor Creates a new [Jws] instance.
         * @param algorithm the JWS algorithm
         */
        data class Jws(
            val algorithm: JWSAlgorithm,
            @Algorithm override val signAlgorithmName: String,
            override val name: String = algorithm.name,
            override val signatureByteArrayLength: Int = ECDSA.getSignatureByteArrayLength(algorithm)
        ) : ProofAlgorithm {


            /**
             * Companion object for [Jws] instances.
             * @property ES256 the ES256 proof algorithm
             */
            companion object {
                val ES256 = Jws(JWSAlgorithm.ES256, Algorithm.SHA256withECDSA)
            }
        }

        /**
         * Proof algorithm for signing CWT with COSE.
         * @property coseAlgorithm the COSE algorithm
         * @property coseCurve the COSE curve
         * @constructor Creates a new [Cose] instance.
         * @param coseAlgorithm the COSE algorithm
         * @param coseCurve the COSE curve
         */
        data class Cose(
            val coseAlgorithm: CoseAlgorithm,
            val coseCurve: CoseCurve,
            @Algorithm override val signAlgorithmName: String,
            override val name: String = "${coseAlgorithm.name()}_${coseCurve.name()}",
            override val signatureByteArrayLength: Int
        ) : ProofAlgorithm {

            /**
             * Companion object for [Cose] instances.
             * @property ES256_P_256 the ES256_P_256 proof algorithm for COSE ES256 with P-256 curve
             */
            companion object {
                val ES256_P_256 =
                    Cose(CoseAlgorithm.ES256, CoseCurve.P_256, Algorithm.SHA256withECDSA, signatureByteArrayLength = 64)
            }
        }
    }
}