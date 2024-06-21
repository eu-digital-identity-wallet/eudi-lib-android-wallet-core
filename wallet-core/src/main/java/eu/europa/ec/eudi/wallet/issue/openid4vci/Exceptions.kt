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

import androidx.biometric.BiometricPrompt.CryptoObject
import eu.europa.ec.eudi.openid4vci.SubmittedRequest

/**
 * Exception thrown when user authentication is required.
 */
internal class UserAuthRequiredException(
    val cryptoObject: CryptoObject?,
    val resume: suspend (Boolean) -> SubmittedRequest
) : Throwable()

/**
 * Exception thrown when the proof type is not supported.
 * @constructor Creates a new [UnsupportedProofTypeException] instance.
 * @param supportedProofTypes supported proof types
 */
class UnsupportedProofTypeException internal constructor(supportedProofTypes: Collection<SupportedProofType>? = null) :
    Throwable(
        message = "Supported proof types are: " + supportedProofTypes?.joinToString(", ") { it.name }
    )