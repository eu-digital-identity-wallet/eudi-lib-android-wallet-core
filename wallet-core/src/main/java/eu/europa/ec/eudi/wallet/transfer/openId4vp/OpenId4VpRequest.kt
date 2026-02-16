/*
 * Copyright (c) 2024 European Commission
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

import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.wallet.internal.generateJarmNonce

data class OpenId4VpRequest(
    val resolvedRequestObject: ResolvedRequestObject
) : Request {
    /**
     * Computes the encryption parameters required to send a response (Success or Error)
     * if the Verifier requested JARM encryption.
     */
    val responseEncryptionParameters: EncryptionParameters?
        get() = resolvedRequestObject.responseEncryptionSpecification?.let { _ ->
            // Generate a fresh random APU (Agreement PartyUInfo) for this specific response
            val randomApu = generateJarmNonce()

            EncryptionParameters.DiffieHellman(
                apu = Base64URL.encode(randomApu)
            )
        }
}