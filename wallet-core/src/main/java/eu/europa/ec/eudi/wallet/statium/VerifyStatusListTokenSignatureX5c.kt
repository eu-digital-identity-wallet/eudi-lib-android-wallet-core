/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.statium

import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.util.X509CertUtils
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.statium.StatusListTokenFormat
import eu.europa.ec.eudi.statium.VerifyStatusListTokenSignature
import kotlinx.datetime.Instant
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

/**
 * Verifies the signature of a status list token using the x5c header.
 */
class VerifyStatusListTokenSignatureX5c : VerifyStatusListTokenSignature {

    /**
     * Verifies the signature of a status list token.
     * It requires the [statusListToken] to be in JWT format and contains an x5c header.
     *
     * @param statusListToken The status list token to verify.
     * @param format The format of the status list token.
     * @param at The time at which the verification is performed.
     * @return A [Result] success or failure.
     */
    override suspend fun invoke(
        statusListToken: String,
        format: StatusListTokenFormat,
        at: Instant,
    ): Result<Unit> = runCatching {
        // only JWT format is supported
        require(format == StatusListTokenFormat.JWT) {
            "Unsupported format: $format"
        }

        val signedJwt = SignedJWT.parse(statusListToken)

        val publicKey = signedJwt.header?.x509CertChain?.firstOrNull()?.let { cert ->
            X509CertUtils.parse(cert.decode())
        }?.publicKey ?: throw IllegalStateException("Missing x5c in JWT header")

        val verifier = when (publicKey) {
            is ECPublicKey -> ECDSAVerifier(publicKey)
            is RSAPublicKey -> RSASSAVerifier(publicKey)
            else -> throw IllegalStateException(
                "Unsupported public key type: ${publicKey.javaClass.name}"
            )
        }

        val isVerified = signedJwt.verify(verifier)

        if (!isVerified) {
            throw SignatureVerificationError()
        }
    }
}

/**
 * Custom exception for signature verification errors.
 */
class SignatureVerificationError : IllegalStateException()

/**
 * Companion object for [VerifyStatusListTokenSignature] to provide a x5c implementation.
 */
val VerifyStatusListTokenSignature.Companion.x5c: VerifyStatusListTokenSignature
    get() = VerifyStatusListTokenSignatureX5c()