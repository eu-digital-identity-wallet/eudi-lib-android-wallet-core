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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.DIGEST_SHA256
import android.security.keystore.KeyProperties.PURPOSE_SIGN
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.PopSigner
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.time.Instant
import java.util.*

/**
 * A [JWSSigner] implementation for DPoP.
 * @property popSigner the DPoP signer [PopSigner.Jwt]
 */
internal class JWSDPoPSigner private constructor() : JWSSigner {

    private val jcaContext = JCAContext()

    override fun getJCAContext(): JCAContext = jcaContext

    private val keyStore: KeyStore
        get() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private val jwk: JWK
        get() = JWK.parseFromPEMEncodedObjects(keyStore.getCertificate(KEY_ALIAS).publicKey.pem)

    val popSigner: PopSigner.Jwt
        get() = PopSigner.Jwt(
            algorithm = SupportedAlgorithms.keys.first(),
            bindingKey = JwtBindingKey.Jwk(jwk),
            jwsSigner = this
        )

    init {
        generateKeyPair()
    }

    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        val algorithm = SupportedAlgorithms[header.algorithm]
            ?: throw JOSEException(
                AlgorithmSupportMessage.unsupportedJWSAlgorithm(
                    header.algorithm,
                    supportedJWSAlgorithms()
                )
            )
        val privateKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry).privateKey
        val signature = Signature.getInstance(algorithm).apply {
            initSign(privateKey)
            update(signingInput)
        }.sign()
        return Base64URL.encode(signature.derToJose(header.algorithm))
    }

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> = SupportedAlgorithms.keys.toMutableSet()

    private fun generateKeyPair() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
        val now = Instant.now()
        val notBefore = Date.from(now)
        val notAfter = Date.from(now.plusSeconds(180L))
        val keySpec = KeyGenParameterSpec.Builder(KEY_ALIAS, PURPOSE_SIGN)
            .setDigests(DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .setKeyValidityStart(notBefore)
            .setKeyValidityEnd(notAfter)
            .setCertificateNotBefore(notBefore)
            .setCertificateNotAfter(notAfter)
            .build()
        with(KeyPairGenerator.getInstance("EC", "AndroidKeyStore")) {
            initialize(keySpec)
            generateKeyPair()
        }
    }

    /**
     * Companion object for the JWSDPoPSigner class.
     */
    companion object {
        private const val KEY_ALIAS = "eu.europa.ec.eudi.wallet.issue.openid4vci.DPoPKey"


        private val SupportedAlgorithms: Map<JWSAlgorithm, String>
            get() = mapOf(JWSAlgorithm.ES256 to "SHA256withECDSA")

        /**
         * Creates a [PopSigner.Jwt] instance.
         * @return [Result<PopSigner.Jwt>] The result of the operation. If successful, the result contains the [PopSigner.Jwt] instance.
         * If unsuccessful, the result contains the exception that occurred.
         */
        operator fun invoke(): Result<PopSigner.Jwt> {
            return try {
                Result.success(JWSDPoPSigner().popSigner)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}