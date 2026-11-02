/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.registration

import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreImpl
import eu.europa.ec.eudi.iso18013.transfer.readerauth.RevocationPolicy
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

private const val TAG = "CertificateTrust"

/**
 * Establishes whether a certificate chain is trusted.
 *
 */
fun interface CertificateTrust {

    /**
     * Returns whether the given certificate [chain] is trusted, the leaf certificate first.
     */
    suspend fun isTrusted(chain: List<X509Certificate>): Boolean
}

/** Adapts a [ReaderTrustStore] to a [CertificateTrust]. */
internal fun ReaderTrustStore.asCertificateTrust(): CertificateTrust =
    CertificateTrust { chain -> validateCertificationTrustPath(chain) }

/**
 * Adapts an ETSI [IsChainTrustedForEUDIW] to a [CertificateTrust] for the given
 * [verificationContext].
 */
internal fun IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>.asCertificateTrust(
    verificationContext: VerificationContext,
    logger: Logger? = null,
): CertificateTrust = CertificateTrust { chain ->
    try {
        this(chain, verificationContext) is CertificationChainValidation.Trusted
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        logger?.e(TAG, "chain trust evaluation failed: ${e.message}", e)
        false
    }
}

/**
 * The trust for the signer of a registration certificate: the configured [trustedCertificates], else
 * the ETSI registration certificate context of [etsiSource].
 */
internal fun registrationCertificateTrust(
    trustedCertificates: List<X509Certificate>?,
    revocationPolicy: RevocationPolicy,
    etsiSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
    logger: Logger? = null,
): CertificateTrust? =
    trustedCertificates?.asCertificateTrust(revocationPolicy)
        ?: etsiSource?.asCertificateTrust(
            VerificationContext.WalletRelyingPartyRegistrationCertificate,
            logger = logger
        )

/**
 * The trust for the signer of a registration certificate's status list: the configured
 * [statusListTrustedCertificates], else the ETSI registration certificate status context of
 * [etsiSource].
 */
internal fun registrationStatusTrust(
    statusListTrustedCertificates: List<X509Certificate>?,
    revocationPolicy: RevocationPolicy,
    etsiSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
    logger: Logger? = null,
): CertificateTrust? =
    statusListTrustedCertificates?.asCertificateTrust(revocationPolicy)
        ?: etsiSource?.asCertificateTrust(
            VerificationContext.WalletRelyingPartyRegistrationCertificateStatus,
            logger = logger,
        )

/**
 * Validates chains against these certificates as trust anchors, without the ISO/IEC 18013-5 reader
 * certificate profile checks.
 */
private fun List<X509Certificate>.asCertificateTrust(revocationPolicy: RevocationPolicy): CertificateTrust =
    ReaderTrustStoreImpl(this, profileValidation = { _, _ -> true }, revocationPolicy = revocationPolicy)
        .asCertificateTrust()
