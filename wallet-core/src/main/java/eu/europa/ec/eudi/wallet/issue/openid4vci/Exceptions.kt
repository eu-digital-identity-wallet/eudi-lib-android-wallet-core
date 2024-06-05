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

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.CoseAlgorithm
import eu.europa.ec.eudi.openid4vci.CoseCurve
import eu.europa.ec.eudi.openid4vci.ProofType

internal class UserAuthRequiredException : Throwable()
class UnsupportedAlgorithmException internal constructor(supportedAlgorithms: Collection<Any>? = null) : Throwable(
    message = supportedAlgorithms?.joinToString(", ") {
        when (it) {
            is Pair<*, *> -> when (val alg = it.first) {
                is CoseAlgorithm -> when (val crv = it.second) {
                    is CoseCurve -> "${alg.name()}:${crv.name()}"
                    else -> ""
                }

                else -> null
            }

            is JWSAlgorithm -> it.name
            is String -> it
            else -> null
        }?.let { msg -> "Supported algorithms are: $msg" } ?: ""
    }
)

class UnsupportedProofTypeException internal constructor(supportedProofTypes: Collection<ProofType>? = null) :
    Throwable(
        message = "Supported proof types are: " + supportedProofTypes?.joinToString(", ") { it.name }
    )