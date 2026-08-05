/*
 *  Copyright (c) 2025-2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.readerauth

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.readerauth.profile.ProfileValidation
import org.bouncycastle.asn1.x500.X500Name
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertPathValidator
import java.security.cert.CertPathValidatorException
import java.security.cert.CertStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.CollectionCertStoreParameters
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.PKIXParameters
import java.security.cert.PKIXRevocationChecker
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.util.Date

class ReaderTrustStoreImpl(
    private val trustedCertificates: List<X509Certificate>,
    private val profileValidation: ProfileValidation,
    private var errorLogger: ((tag: String, message: String, cause: Throwable) -> Unit) = { tag, message, cause ->
        Log.d(tag, message, cause)
    },
    private val revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
) : ReaderTrustStore {

    private val trustedCertMap: Map<X500Name, X509Certificate> by lazy {
        trustedCertificates.associateBy { X500Name(it.subjectX500Principal.name) }
    }

    override fun createCertificationTrustPath(chain: List<X509Certificate>): List<X509Certificate>? {
        for (certificate in chain) {
            val x500Name = X500Name(certificate.issuerX500Principal.name)
            trustedCertMap[x500Name]?.let {
                return listOf(certificate, it)
            }
        }
        return null
    }

    /**
     * Validates the certification trust path of a document signer.
     *
     * This function verifies the certificate chain against a set of trusted certificates,
     * performs revocation checking based on the configured [RevocationPolicy],
     * and performs additional profile validation on the signer's certificate.
     *
     * @param chainToDocumentSigner The certificate chain leading to the document signer's certificate.
     * @return `true` if the certification trust path is valid, `false` otherwise.
     */
    override fun validateCertificationTrustPath(chainToDocumentSigner: List<X509Certificate>): Boolean {
        if (chainToDocumentSigner.isEmpty()) return false

        return try {
            val certStore = CertStore.getInstance(
                "Collection",
                CollectionCertStoreParameters(trustedCertificates),
            )
            val trustAnchors = trustedCertificates.map {
                TrustAnchor(it, null)
            }.toSet()

            val validator = CertPathValidator.getInstance("PKIX")
            val params = PKIXParameters(trustAnchors).apply {
                addCertStore(certStore)
                date = Date()

                when (revocationPolicy) {
                    is RevocationPolicy.NoCheck -> {
                        isRevocationEnabled = false
                    }
                    is RevocationPolicy.HardFail -> {
                        isRevocationEnabled = true
                        val checker = validator.revocationChecker as PKIXRevocationChecker
                        checker.options = setOf(
                            PKIXRevocationChecker.Option.PREFER_CRLS,
                            PKIXRevocationChecker.Option.NO_FALLBACK,
                        )
                        addCertPathChecker(checker)
                    }
                    is RevocationPolicy.SoftFail -> {
                        isRevocationEnabled = true
                        val checker = validator.revocationChecker as PKIXRevocationChecker
                        checker.options = setOf(
                            PKIXRevocationChecker.Option.PREFER_CRLS,
                            PKIXRevocationChecker.Option.NO_FALLBACK,
                            PKIXRevocationChecker.Option.SOFT_FAIL,
                        )
                        addCertPathChecker(checker)
                    }
                }
            }

            val certPath = CertificateFactory.getInstance("X.509")
                .generateCertPath(chainToDocumentSigner)
            val certPathValidationResult = validator
                .validate(certPath, params) as PKIXCertPathValidatorResult
            val trustedRootCA = certPathValidationResult.trustAnchor.trustedCert

            profileValidation.validate(chainToDocumentSigner, trustedRootCA)
        } catch (e: Exception) {
            when (e) {
                is InvalidAlgorithmParameterException ->
                    errorLogger(TAG, "INVALID_ALGORITHM_PARAMETER", e)
                is NoSuchAlgorithmException -> errorLogger(TAG, "NO_SUCH_ALGORITHM", e)
                is CertificateException -> errorLogger(TAG, "CERTIFICATE_ERROR", e)
                is CertPathValidatorException -> errorLogger(TAG, "CERTIFICATE_PATH_ERROR", e)
                else -> errorLogger(TAG, "UNKNOWN_ERROR", e)
            }
            false
        }
    }

    companion object {
        private const val TAG = "ReaderTrustStoreImpl"
    }
}
