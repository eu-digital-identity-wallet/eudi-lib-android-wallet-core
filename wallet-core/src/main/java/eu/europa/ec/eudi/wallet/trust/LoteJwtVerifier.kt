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
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.NonEmptyList
import eu.europa.ec.eudi.etsi1196x2.consultation.ValidateCertificateChain
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

/**
 * Default LoTE JWT verifier.
 *
 * A LoTE is verified when:
 * - its `x5c` header carries the certificate chain of its signer;
 * - the signing certificate, the first certificate of `x5c`, is valid now;
 * - [validateSigner] trusts the `x5c` chain against [signerTrustAnchors]; and
 * - the JWS signature verifies with the public key of the signing certificate.
 *
 * @param signerTrustAnchors the trust anchors of the LoTE signers
 * @param validateSigner validates the `x5c` chain against [signerTrustAnchors]
 * @param logger optional logger for diagnostic output
 */
internal class LoteJwtVerifier(
    private val signerTrustAnchors: NonEmptyList<TrustAnchor>,
    private val validateSigner: ValidateCertificateChain<List<X509Certificate>, TrustAnchor>,
    private val logger: Logger? = null
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

            val certFactory = CertificateFactory.getInstance("X.509")
            val chain = x5cChain.map {
                certFactory.generateCertificate(ByteArrayInputStream(it.decode())) as X509Certificate
            }
            val signingCert = chain.first()
            signingCert.checkValidity()

            logger?.d(TAG, "Verifying LoTE JWT signature: alg=${header.algorithm}, " +
                "signer=${signingCert.subjectX500Principal}")

            when (val validation = validateSigner(chain, signerTrustAnchors)) {
                is CertificationChainValidation.NotTrusted -> {
                    logger?.d(TAG, "LoTE JWT signer is not trusted")
                    VerifyJwtSignature.Outcome.NotVerified(validation.cause)
                }

                is CertificationChainValidation.Trusted -> verifySignature(signedJwt, signingCert, jwt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger?.e(TAG, "LoTE JWT verification error: ${e.message}", e)
            VerifyJwtSignature.Outcome.NotVerified(e)
        }
    }

    private fun verifySignature(
        signedJwt: SignedJWT,
        signingCert: X509Certificate,
        jwt: String,
    ): VerifyJwtSignature.Outcome {
        val verifier: JWSVerifier = when (val publicKey = signingCert.publicKey) {
            is ECPublicKey -> ECDSAVerifier(publicKey)
            is RSAPublicKey -> RSASSAVerifier(publicKey)
            else -> return VerifyJwtSignature.Outcome.NotVerified(
                UnsupportedOperationException("Unsupported key type: ${publicKey.algorithm}")
            )
        }

        return if (signedJwt.verify(verifier)) {
            logger?.d(TAG, "LoTE JWT signature verified successfully")
            VerifyJwtSignature.Outcome.Verified(jwt)
        } else {
            logger?.d(TAG, "LoTE JWT signature verification failed")
            VerifyJwtSignature.Outcome.NotVerified(
                SecurityException("JWT signature verification failed")
            )
        }
    }

    private companion object {
        const val TAG = "LoteJwtVerifier"
    }
}
