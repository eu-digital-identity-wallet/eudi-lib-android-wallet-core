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

import android.content.Context
import androidx.annotation.RawRes
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreImpl
import eu.europa.ec.eudi.iso18013.transfer.readerauth.RevocationPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.wallet.internal.getCertificate
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * DSL builder for unified reader authentication configuration.
 *
 * Replaces the scattered `configureReaderTrustStore` overloads and
 * `configureReaderAuthPolicy` with a single entry point that bundles trust
 * material and enforcement intent together.
 *
 * The builder is stored at configuration time and executed at
 * [EudiWallet.Builder.build()][eu.europa.ec.eudi.wallet.EudiWallet] time,
 * so that deferred trust sources (e.g. ETSI) are available.
 *
 * Example:
 * ```
 * configureReaderAuthentication {
 *     trustedCertificates(cert1, cert2)
 *     revocationPolicy(RevocationPolicy.SoftFail)
 *     enforceIfPresent()
 * }
 * ```
 *
 * @see eu.europa.ec.eudi.wallet.EudiWalletConfig.configureReaderAuthentication
 */
class ReaderAuthenticationConfigBuilder {

    private enum class Enforcement { DoNotEnforce, EnforceIfPresent, AlwaysRequire }

    private var enforcement = Enforcement.EnforceIfPresent

    private var certificates: List<X509Certificate>? = null
    private var customTrustStore: ReaderTrustStore? = null
    private var etsiChainTrust: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>? = null
    private var revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail

    // --- Trust material setters ---

    fun trustedCertificates(certificates: List<X509Certificate>) {
        this.certificates = certificates
    }

    fun trustedCertificates(vararg certificates: X509Certificate) {
        this.certificates = certificates.toList()
    }

    fun trustedCertificates(context: Context, @RawRes vararg certificateRes: Int) {
        this.certificates = certificateRes.map { context.getCertificate(it) }
    }

    fun trustSource(readerTrustStore: ReaderTrustStore) {
        this.customTrustStore = readerTrustStore
    }

    fun trustSource(isChainTrusted: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>) {
        this.etsiChainTrust = isChainTrusted
    }

    // --- Enforcement setters ---

    fun doNotEnforce() {
        enforcement = Enforcement.DoNotEnforce
    }

    fun enforceIfPresent() {
        enforcement = Enforcement.EnforceIfPresent
    }

    fun alwaysRequire() {
        enforcement = Enforcement.AlwaysRequire
    }

    // --- Revocation setter ---

    fun revocationPolicy(policy: RevocationPolicy) {
        this.revocationPolicy = policy
    }

    /**
     * Builds the [ReaderAuthPolicy] from the configured trust sources and enforcement.
     *
     * Trust source resolution priority:
     * 1. Custom [ReaderTrustStore] via [trustSource]
     * 2. Explicit ETSI chain trust via [trustSource] (IsChainTrustedForEUDIW overload)
     * 3. Static certificates via [trustedCertificates]
     * 4. Central ETSI trust source from [configureEtsiTrust][eu.europa.ec.eudi.wallet.EudiWalletConfig.configureEtsiTrust]
     *
     * @param etsiSource the centrally configured ETSI trust source (may be null)
     * @param logger the wallet logger for diagnostic output
     * @return the resolved [ReaderAuthPolicy]
     * @throws IllegalArgumentException if [enforceIfPresent] or [alwaysRequire] is set
     *   but no trust source is available
     */
    internal fun build(
        etsiSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
        logger: Logger,
    ): ReaderAuthPolicy {
        val trustStore: ReaderTrustStore? = when {
            customTrustStore != null -> customTrustStore
            etsiChainTrust != null -> etsiChainTrust!!.asReaderTrustStore()
            certificates != null -> ReaderTrustStoreImpl(
                certificates!!,
                profileValidation = { _, _ -> true },
                revocationPolicy = revocationPolicy,
            )
            etsiSource != null -> etsiSource.asReaderTrustStore()
            else -> null
        }?.also { if (it is EtsiReaderTrustStore) it.logger = logger }

        return when (enforcement) {
            Enforcement.DoNotEnforce -> ReaderAuthPolicy.DoNotEnforce
            Enforcement.EnforceIfPresent -> {
                requireNotNull(trustStore) {
                    "enforceIfPresent() requires trust sources. " +
                        "Call trustedCertificates() or trustSource(), or use doNotEnforce() to opt out."
                }
                ReaderAuthPolicy.EnforceIfPresent(trustStore)
            }
            Enforcement.AlwaysRequire -> {
                requireNotNull(trustStore) {
                    "alwaysRequire() requires trust sources. " +
                        "Call trustedCertificates() or trustSource()."
                }
                ReaderAuthPolicy.AlwaysRequire(trustStore)
            }
        }
    }
}
