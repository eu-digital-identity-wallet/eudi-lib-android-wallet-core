/*
 * Copyright (c) 2023-2026 European Commission
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
package eu.europa.ec.eudi.wallet.trust

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.etsi119602.consultation.VerifyJwtSignature
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/**
 * Default LoTE JWT verifier.
 *
 * This implementation **only verifies the JWS cryptographic signature** using the leaf
 * certificate's public key from the `x5c` header. It does **not** perform:
 * - Certificate chain validation (PKIX)
 * - CRL / OCSP revocation checking on the signing certificate
 * - Certificate policy or profile constraint checks
 *
 * These additional trust checks will be added in a future release. In the meantime,
 * provide a custom [VerifyJwtSignature] implementation via
 * [EtsiTrustConfig.Builder.jwtSignatureVerifier] if thorough verification is required.
 *
 * @param logger optional logger for diagnostic output
 */
internal class LoteJwtVerifier(
    private val logger: Logger? = null,
) : VerifyJwtSignature {

    override suspend fun invoke(jwt: String): VerifyJwtSignature.Outcome {
        return try {
            val signedJwt = SignedJWT.parse(jwt)
            val header = signedJwt.header

            val x5cChain = header.x509CertChain
            if (x5cChain.isNullOrEmpty()) {
                return VerifyJwtSignature.Outcome.NotVerified(
                    IllegalArgumentException("JWT header does not contain x5c certificate chain")
                )
            }

            // Build X509Certificate from the leaf (first) x5c entry
            val leafCertBytes = x5cChain[0].decode()
            val certFactory = CertificateFactory.getInstance("X.509")
            val leafCert = certFactory.generateCertificate(
                ByteArrayInputStream(leafCertBytes)
            ) as X509Certificate

            logger?.d(TAG, "Verifying LoTE JWT signature: alg=${header.algorithm}, " +
                "leaf=${leafCert.subjectX500Principal}")

            // Acknowledge any critical headers declared in the JWT (e.g., "sigT")
            // so nimbus doesn't reject the signature for unrecognized crit params
            val deferredCritHeaders = header.criticalParams ?: emptySet()

            // Create verifier based on the key type
            val verifier: JWSVerifier = when (val publicKey = leafCert.publicKey) {
                is ECPublicKey -> ECDSAVerifier(publicKey, deferredCritHeaders)
                is RSAPublicKey -> RSASSAVerifier(publicKey, deferredCritHeaders)
                else -> return VerifyJwtSignature.Outcome.NotVerified(
                    UnsupportedOperationException("Unsupported key type: ${publicKey.algorithm}")
                )
            }

            if (signedJwt.verify(verifier)) {
                logger?.d(TAG, "LoTE JWT signature verified successfully")
                VerifyJwtSignature.Outcome.Verified(jwt)
            } else {
                logger?.d(TAG, "LoTE JWT signature verification failed")
                VerifyJwtSignature.Outcome.NotVerified(
                    SecurityException("JWT signature verification failed")
                )
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger?.e(TAG, "LoTE JWT verification error: ${e.message}", e)
            VerifyJwtSignature.Outcome.NotVerified(e)
        }
    }

    private companion object {
        const val TAG = "LoteJwtVerifier"
    }
}
