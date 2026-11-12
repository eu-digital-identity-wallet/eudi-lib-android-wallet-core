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
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.DERIA5String
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.CRLDistPoint
import org.bouncycastle.asn1.x509.DistributionPointName
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.IssuingDistributionPoint
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.cert.CRLException
import java.security.cert.CertPath
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
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ReaderTrustStoreImpl(
    private val trustedCertificates: List<X509Certificate>,
    private val profileValidation: ProfileValidation,
    private var errorLogger: ((tag: String, message: String, cause: Throwable) -> Unit) = { tag, message, cause ->
        Log.d(tag, message, cause)
    },
    private val revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
    private val revocationCheckTimeout: Duration = DEFAULT_REVOCATION_CHECK_TIMEOUT,
    private val crlCache: CrlCache? = null,
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

        val firstTrustedIndex = (1 until chainToDocumentSigner.size)
            .firstOrNull { chainToDocumentSigner[it] in trustedCertificates }
        val certificationPath =
            firstTrustedIndex?.let { chainToDocumentSigner.take(it) } ?: chainToDocumentSigner

        return try {
            val validator = CertPathValidator.getInstance("PKIX")
            // A trust anchor is not part of the path (RFC 5280 6.1), so a signer that is one has no revocation status.
            val checker =
                if (chainToDocumentSigner.first() in trustedCertificates) null else revocationChecker(validator, revocationPolicy)
            val certPath = CertificateFactory.getInstance("X.509")
                .generateCertPath(certificationPath)
            // The platform fetches CRLs without a timeout, so bound the path validation.
            val validation = validationExecutor.submit<PKIXCertPathValidatorResult> {
                if (checker == null || crlCache == null) {
                    return@submit validator.validate(certPath, pkixParameters(checker, emptyList())) as PKIXCertPathValidatorResult
                }
                // Only the distribution points of a trusted path are contacted.
                validator.validate(certPath, pkixParameters(null, emptyList()))
                val now = Date()
                val crls = crlCache.crlsFor(certificationPath) { certificate, crl ->
                    crl.covers(certificationPath, certificationPath.indexOf(certificate), now)
                }
                val withoutCrl = firstWithoutCrl(certificationPath, crls, now)
                if (withoutCrl < 0) {
                    validator.validate(certPath, pkixParameters(checker, crls)) as PKIXCertPathValidatorResult
                } else {
                    // PKIX would download the missing CRL itself, bypassing the cache's HTTP client.
                    val unavailable = CertPathValidatorException(
                        "No CRL available", null, certPath, withoutCrl,
                        CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS,
                    )
                    if (revocationPolicy !is RevocationPolicy.SoftFail) throw unavailable
                    errorLogger(TAG, "CERTIFICATE_PATH_CRL_UNAVAILABLE_SOFT_FAIL", unavailable)
                    validateOffline(certPath)
                }
            }
            val certPathValidationResult = try {
                validation.get(revocationCheckTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } catch (e: ExecutionException) {
                val cause = e.cause ?: e
                if (revocationPolicy !is RevocationPolicy.SoftFail || !cause.isUnreadableCrl()) throw cause
                // The platform treats only network errors as soft; an unreadable CRL is unavailability too.
                errorLogger(TAG, "CERTIFICATE_PATH_CRL_UNREADABLE_SOFT_FAIL", cause)
                validateOffline(certPath)
            } catch (e: TimeoutException) {
                validation.cancel(true)
                if (revocationPolicy !is RevocationPolicy.SoftFail) throw e
                // A stalled CRL download is unavailability, which SoftFail tolerates.
                errorLogger(TAG, "CERTIFICATE_PATH_TIMEOUT_SOFT_FAIL", e)
                validateOffline(certPath)
            }
            val trustedRootCA = certPathValidationResult.trustAnchor.trustedCert

            profileValidation.validate(certificationPath, trustedRootCA)
        } catch (e: Exception) {
            when (e) {
                is InvalidAlgorithmParameterException ->
                    errorLogger(TAG, "INVALID_ALGORITHM_PARAMETER", e)
                is NoSuchAlgorithmException -> errorLogger(TAG, "NO_SUCH_ALGORITHM", e)
                is CertificateException -> errorLogger(TAG, "CERTIFICATE_ERROR", e)
                is CertPathValidatorException -> errorLogger(TAG, "CERTIFICATE_PATH_ERROR", e)
                is TimeoutException -> errorLogger(TAG, "CERTIFICATE_PATH_TIMEOUT", e)
                else -> errorLogger(TAG, "UNKNOWN_ERROR", e)
            }
            false
        }
    }

    private fun validateOffline(certPath: CertPath): PKIXCertPathValidatorResult {
        val result = CertPathValidator.getInstance("PKIX")
            .validate(certPath, pkixParameters(null, emptyList())) as PKIXCertPathValidatorResult
        crlCache?.let { checkCachedCrls(certPath, result.trustAnchor.trustedCert, it) }
        return result
    }

    // PKIX would download the CRLs that are not cached, so the newest cached one is checked here as PKIX would.
    private fun checkCachedCrls(certPath: CertPath, anchor: X509Certificate, cache: CrlCache) {
        val certificates = certPath.certificates.map { it as X509Certificate }
        val now = Date()
        certificates.forEachIndexed { index, certificate ->
            val issuer = certificates.getOrNull(index + 1) ?: anchor
            val crl = cache.cachedCrlsFor(listOf(certificate))
                .filter { !it.thisUpdate.after(now) && it.isIssuedFor(certificate, listOf(issuer)) }
                .maxByOrNull { it.thisUpdate }
            val revokedAt = crl?.getRevokedCertificate(certificate)?.revocationDate
            if (revokedAt != null && revokedAt.before(now)) {
                throw CertPathValidatorException(
                    "Certificate is revoked by a cached CRL", null, certPath, index,
                    CertPathValidatorException.BasicReason.REVOKED,
                )
            }
        }
    }

    /**
     * The index of the first certificate in [chain] that no CRL of [crls] covers completely, or -1.
     * PKIX looks up further CRLs itself for a certificate a CRL only partly covers, so a covering CRL
     * is a complete one, current, from an issuer that may sign CRLs, for a certificate whose
     * distribution points are not limited to some reasons or another CRL issuer. An issuing
     * distribution point may only limit the CRL to the certificate's distribution point and kind.
     */
    private fun firstWithoutCrl(chain: List<X509Certificate>, crls: List<X509CRL>, now: Date): Int =
        chain.indices.firstOrNull { index ->
            chain[index].hasPartialDistributionPoint() || crls.none { it.covers(chain, index, now) }
        } ?: -1

    private fun X509CRL.covers(chain: List<X509Certificate>, index: Int, now: Date): Boolean {
        val certificate = chain[index]
        val issuers = (listOfNotNull(chain.getOrNull(index + 1)) + trustedCertificates).filter { it.maySignCrls() }
        return isCompleteFor(certificate, now) && isIssuedFor(certificate, issuers)
    }

    private fun X509CRL.isCompleteFor(certificate: X509Certificate, now: Date): Boolean =
        criticalExtensionOIDs.orEmpty().all { it == Extension.issuingDistributionPoint.id } &&
            getExtensionValue(Extension.deltaCRLIndicator.id) == null &&
            !thisUpdate.after(now) && nextUpdate?.after(now) == true &&
            coversScopeOf(certificate)

    private fun X509CRL.coversScopeOf(certificate: X509Certificate): Boolean {
        val extension = getExtensionValue(Extension.issuingDistributionPoint.id) ?: return true
        return runCatching {
            val scope = IssuingDistributionPoint.getInstance(
                ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(extension).octets)
            )
            val isCa = certificate.basicConstraints >= 0
            val sameDistributionPoint = scope.distributionPoint?.let { name ->
                val uris = name.uris() ?: return@runCatching false
                certificate.distributionPointUris().any { it in uris }
            } ?: true
            scope.onlySomeReasons == null && !scope.isIndirectCRL && !scope.onlyContainsAttributeCerts() &&
                !(scope.onlyContainsUserCerts() && isCa) && !(scope.onlyContainsCACerts() && !isCa) &&
                sameDistributionPoint
        }.getOrDefault(false)
    }

    private fun X509Certificate.distributionPointUris(): List<String> {
        val extension = getExtensionValue(Extension.cRLDistributionPoints.id) ?: return emptyList()
        return CRLDistPoint.getInstance(ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(extension).octets))
            .distributionPoints.flatMap { it.distributionPoint?.uris().orEmpty() }
    }

    // The URIs of a full name, or null for a relative name or one without a URI.
    private fun DistributionPointName.uris(): List<String>? =
        takeIf { type == DistributionPointName.FULL_NAME }
            ?.let { GeneralNames.getInstance(name).names }
            ?.filter { it.tagNo == GeneralName.uniformResourceIdentifier }
            ?.map { DERIA5String.getInstance(it.name).string }
            ?.takeIf { it.isNotEmpty() }

    private fun X509Certificate.maySignCrls(): Boolean = keyUsage?.getOrNull(CRL_SIGN) != false

    private fun X509Certificate.hasPartialDistributionPoint(): Boolean {
        val extension = getExtensionValue(Extension.cRLDistributionPoints.id) ?: return false
        return runCatching {
            CRLDistPoint.getInstance(ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(extension).octets))
                .distributionPoints.any { it.reasons != null || it.crlIssuer != null }
        }.getOrDefault(true)
    }

    private fun X509CRL.isIssuedFor(certificate: X509Certificate, issuers: List<X509Certificate>): Boolean =
        issuerX500Principal == certificate.issuerX500Principal && issuers.any { issuer ->
            issuer.subjectX500Principal == issuerX500Principal && runCatching { verify(issuer.publicKey) }.isSuccess
        }

    private fun pkixParameters(revocationChecker: PKIXRevocationChecker?, crls: List<X509CRL>): PKIXParameters {
        val certStore = CertStore.getInstance(
            "Collection",
            CollectionCertStoreParameters(trustedCertificates + crls),
        )
        val trustAnchors = trustedCertificates.map {
            TrustAnchor(it, null)
        }.toSet()

        return PKIXParameters(trustAnchors).apply {
            addCertStore(certStore)
            date = Date()
            isRevocationEnabled = revocationChecker != null
            revocationChecker?.let { addCertPathChecker(it) }
        }
    }

    private fun revocationChecker(validator: CertPathValidator, policy: RevocationPolicy): PKIXRevocationChecker? {
        val options = when (policy) {
            is RevocationPolicy.NoCheck -> return null
            is RevocationPolicy.HardFail -> setOf(
                PKIXRevocationChecker.Option.PREFER_CRLS,
                PKIXRevocationChecker.Option.NO_FALLBACK,
            )
            is RevocationPolicy.SoftFail -> setOf(
                PKIXRevocationChecker.Option.PREFER_CRLS,
                PKIXRevocationChecker.Option.NO_FALLBACK,
                PKIXRevocationChecker.Option.SOFT_FAIL,
            )
        }
        return (validator.revocationChecker as PKIXRevocationChecker).apply { this.options = options }
    }

    private fun Throwable.isUnreadableCrl(): Boolean =
        generateSequence(this) { it.cause }.any { it is CRLException }

    companion object {
        private const val TAG = "ReaderTrustStoreImpl"

        /** The `cRLSign` bit of [X509Certificate.getKeyUsage]. */
        private const val CRL_SIGN = 6

        /**
         * Upper bound for the PKIX path validation with revocation checking, including any CRL
         * download. The offline validation that SoftFail falls back to is not bounded by it.
         */
        val DEFAULT_REVOCATION_CHECK_TIMEOUT: Duration = 10.seconds

        private val validationExecutor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "ReaderTrustStore-validation").apply { isDaemon = true }
        }
    }
}
