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
