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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.android.identity.crypto.Algorithm
import com.android.identity.securearea.KeyLockedException
import com.android.identity.securearea.KeyUnlockData
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import eu.europa.ec.eudi.wallet.document.UnsignedDocument

/**
 * A [JWSSigner] implementation for signing the proof of possession of the document's key
 * @property popSigner
 * @property keyLockedException
 * @constructor Creates a JWS proof signer.
 * @param document The issuance request.
 * @param algorithm The proof algorithm.
 * @param keyUnlockData The [KeyUnlockData] that will be used for signing
 *
 */
internal class JWSKeyPoPSigner(
    private val document: UnsignedDocument,
    private val algorithm: Algorithm,
    private val keyUnlockData: KeyUnlockData? = null,
) : JWSSigner {

    private val jcaContext = JCAContext()

    /**
     * The JWK of the public key.
     */
    private val jwk = JWK.parseFromPEMEncodedObjects(document.keyInfo.publicKey.toPem())

    private val jwsAlgorithm = JWSAlgorithm.parse(algorithm.jwseAlgorithmIdentifier)

    val popSigner: PopSigner.Jwt = PopSigner.Jwt(
        algorithm = jwsAlgorithm,
        bindingKey = JwtBindingKey.Jwk(jwk),
        jwsSigner = this
    )

    var keyLockedException: KeyLockedException? = null
        private set

    override fun getJCAContext(): JCAContext = jcaContext

    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> {
        return setOf(jwsAlgorithm)
    }

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        try {
            val signature = document.sign(signingInput, algorithm, keyUnlockData)
                .getOrThrow()
                .toCoseEncoded()
            return Base64URL.encode(signature)
        } catch (e: Throwable) {
            when (e) {
                is KeyLockedException -> {
                    keyLockedException = e
                }
            }
            throw (e)
        }
    }
}