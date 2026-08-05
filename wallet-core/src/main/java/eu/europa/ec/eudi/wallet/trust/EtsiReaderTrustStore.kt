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
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import kotlin.coroutines.CoroutineContext

/**
 * [ReaderTrustStore] implementation backed by ETSI Trusted Lists.
 *
 * Delegates reader certificate chain validation to the ETSI library's
 * [IsChainTrustedForEUDIW], enabling dynamic trust anchor resolution
 * from LoTE (ETSI TS 119 602)
 *
 * This is a drop-in replacement for
 * [eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreImpl] — configure it
 * via [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureReaderTrustStore] or
 * [eu.europa.ec.eudi.wallet.EudiWallet.setReaderTrustStore].
 *
 * @param isChainTrusted the ETSI chain trust validator (supports LoTE, or combined sources)
 * @param verificationContext the EUDI verification context for reader authentication
 *        (defaults to [VerificationContext.WalletRelyingPartyAccessCertificate])
 * @param coroutineContext the coroutine context for the sync/async bridge
 *        (defaults to [Dispatchers.IO])
 * @param logger optional [Logger] for diagnostic output
 */
class EtsiReaderTrustStore(
    private val isChainTrusted: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>,
    private val verificationContext: VerificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
    @JvmField var logger: Logger? = null,
) : ReaderTrustStore {

    /**
     * Creates a certification trust path by validating the chain against ETSI trusted lists.
     *
     * @param chain the certificate chain to validate
     * @return the chain plus the trust anchor certificate if trusted, null otherwise
     */
    override fun createCertificationTrustPath(
        chain: List<X509Certificate>
    ): List<X509Certificate>? {
        val result = evaluateChain(chain) ?: return null
        return when (result) {
            is CertificationChainValidation.Trusted -> chain + result.trustAnchor.trustedCert
            is CertificationChainValidation.NotTrusted -> null
        }
    }

    /**
     * Validates that the certificate chain is trusted according to ETSI trusted lists.
     *
     * @param chainToDocumentSigner the certificate chain to validate
     * @return true if the chain is trusted, false otherwise
     */
    override fun validateCertificationTrustPath(
        chainToDocumentSigner: List<X509Certificate>
    ): Boolean {
        logger?.d(TAG, "validateCertificationTrustPath: chain has ${chainToDocumentSigner.size} certs, " +
            "leaf=${chainToDocumentSigner.firstOrNull()?.subjectX500Principal}")
        val result = evaluateChain(chainToDocumentSigner) ?: return false
        val trusted = result is CertificationChainValidation.Trusted
        logger?.d(TAG, "validateCertificationTrustPath: result=$result, trusted=$trusted")
        return trusted
    }

    private fun evaluateChain(
        chain: List<X509Certificate>
    ): CertificationChainValidation<TrustAnchor>? = try {
        runBlocking(coroutineContext) {
            isChainTrusted(chain, verificationContext)
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        logger?.e(TAG, "evaluateChain failed: ${e.message}", e)
        null
    }

    private companion object {
        const val TAG = "ReaderTrust"
    }
}
