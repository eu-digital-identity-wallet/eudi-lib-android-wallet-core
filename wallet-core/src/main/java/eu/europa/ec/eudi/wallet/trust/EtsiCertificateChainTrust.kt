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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.openid4vci.CertificateChainTrust
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * Adapts the ETSI [IsChainTrustedForEUDIW] to the OpenID4VCI [CertificateChainTrust] interface
 * for validating signed issuer metadata certificate chains.
 *
 * Uses [VerificationContext.WalletRelyingPartyAccessCertificate] as the ETSI verification context,
 * which is the correct context for metadata signing certificates per the EUDI specification.
 *
 * @param isChainTrusted the ETSI chain trust validator
 * @param logger optional [Logger] for diagnostic output
 * @see EtsiReaderTrustStore for the analogous adapter for reader authentication
 */
internal class EtsiCertificateChainTrust(
    private val isChainTrusted: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>,
    private val logger: Logger? = null,
) : CertificateChainTrust {

    override suspend fun isTrusted(chain: List<X509Certificate>): Boolean {
        logger?.d(TAG, "isTrusted: chain has ${chain.size} certs, " +
            "leaf=${chain.firstOrNull()?.subjectX500Principal}")
        return try {
            val result = isChainTrusted(chain, VerificationContext.WalletRelyingPartyAccessCertificate)
            val trusted = result is CertificationChainValidation.Trusted
            logger?.d(TAG, "isTrusted: result=$result, trusted=$trusted")
            trusted
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger?.e(TAG, "isTrusted failed: ${e.message}", e)
            false
        }
    }

    private companion object {
        const val TAG = "MetadataTrust"
    }
}
