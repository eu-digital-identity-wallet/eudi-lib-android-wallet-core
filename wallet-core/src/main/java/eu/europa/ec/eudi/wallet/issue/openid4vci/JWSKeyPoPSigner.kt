/*
 * Copyright (c) 2024-2025 European Commission
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

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import kotlinx.coroutines.runBlocking
import org.multipaz.securearea.AndroidKeystoreKeyUnlockData
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyLockedException
import org.multipaz.securearea.KeyUnlockData

/**
 * A [JWSSigner] implementation for signing the proof of possession of the document's key
 * @property popSigner
 * @property keyLockedException
 * @constructor Creates a JWS proof signer.
 * @param proofSigner The [ProofOfPossessionSigner] that will be used for signing
 * @param keyUnlockData The [KeyUnlockData] that will be used for signing
 *
 */
internal class JWSKeyPoPSigner(
    val proofSigner: ProofOfPossessionSigner,
    private val keyUnlockData: KeyUnlockData?
) : JWSSigner {

    private val jcaContext = JCAContext()

    private val keyInfo: KeyInfo by lazy {
        runBlocking { proofSigner.getKeyInfo() }
    }

    /**
     * The JWK of the public key.
     */
    private val jwk by lazy {
        JWK.parseFromPEMEncodedObjects(keyInfo.publicKey.toPem())
    }

    private val jwsAlgorithm by lazy {
        JWSAlgorithm.parse(keyInfo.algorithm.joseAlgorithmIdentifier)
    }

    val popSigner: PopSigner.Jwt by lazy {
        PopSigner.Jwt(
            algorithm = jwsAlgorithm,
            bindingKey = JwtBindingKey.Jwk(jwk),
            jwsSigner = this
        )
    }

    var keyLockedException: KeyLockedException? = null
        private set

    override fun getJCAContext(): JCAContext = jcaContext

    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> {
        return setOf(jwsAlgorithm)
    }

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        return runBlocking {
            try {
                val signature = proofSigner.signPoP(signingInput, keyUnlockData)
                    .toCoseEncoded()
                Base64URL.encode(signature)
            } catch (e: KeyLockedException) {
                keyLockedException = e
                throw e
            }
        }
    }
}