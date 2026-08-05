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
package eu.europa.ec.eudi.wallet.statium

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.wallet.trust.StatusListTrustConfig
import eu.europa.ec.eudi.wallet.trust.StatusListTrustConfigBuilder
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * DSL builder for configuring the document status resolver.
 *
 * Allows configuring both the clock skew and optional ETSI trust verification
 * for status list token signers.
 *
 * Example:
 * ```
 * configureDocumentStatusResolver {
 *     clockSkew(5)
 *     configureTrust {
 *         trustSource(myComposeChainTrust)
 *         classifications(myClassifications)
 *         policy {
 *             default(TrustPolicy.Action.ENFORCE)
 *         }
 *     }
 * }
 * ```
 */
class DocumentStatusResolverConfigBuilder {

    var clockSkew: Duration = Duration.ZERO
        private set

    private var trustConfigBuilder: StatusListTrustConfigBuilder? = null

    /**
     * Sets the allowed clock skew in minutes for status list token verification.
     *
     * @param minutes the clock skew in minutes
     */
    fun clockSkew(minutes: Long) {
        this.clockSkew = minutes.minutes
    }

    /**
     * Configures ETSI trust verification for status list token signers.
     *
     * When configured, the signer's certificate chain will be evaluated against
     * ETSI trusted lists before accepting the status list token.
     *
     * @param block configuration block applied to the [StatusListTrustConfigBuilder]
     */
    fun configureTrust(block: StatusListTrustConfigBuilder.() -> Unit) {
        this.trustConfigBuilder = StatusListTrustConfigBuilder().apply(block)
    }

    /**
     * Builds the [StatusListTrustConfig] from the current builder state, if trust was configured.
     *
     * @return the built [StatusListTrustConfig], or null if [configureTrust] was not called
     */
    internal fun buildTrustConfig(): StatusListTrustConfig? {
        return trustConfigBuilder?.build()
    }

    /**
     * Builds the [StatusListTrustConfig], using the provided defaults for trust source and
     * classifications when they have not been explicitly set inside [configureTrust].
     *
     * When [configureTrust] was not called but ETSI defaults are available, a default
     * [StatusListTrustConfig] is created automatically using the provided defaults.
     *
     * @param defaultSource default EUDIW trust source from [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureEtsiTrust]
     * @param defaultClassifications default classifications from [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureEtsiTrust]
     * @return the built [StatusListTrustConfig], or null if no trust source is available
     */
    internal fun buildTrustConfig(
        defaultSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
        defaultClassifications: AttestationClassifications?,
    ): StatusListTrustConfig? {
        return if (trustConfigBuilder != null) {
            trustConfigBuilder!!.build(defaultSource, defaultClassifications)
        } else if (defaultSource != null && defaultClassifications != null) {
            // Auto-create with ETSI defaults when configureTrust was not called
            StatusListTrustConfigBuilder().build(defaultSource, defaultClassifications)
        } else {
            null
        }
    }
}
