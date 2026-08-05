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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.ComposeChainTrust
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * DSL builder for constructing a [StatusListTrustConfig].
 *
 * A trust source must be provided via one of the [trustSource] overloads.
 * When using [IsChainTrustedForEUDIW] or [ComposeChainTrust] as the trust source,
 * [classifications] must also be provided so the builder can construct an
 * [IsChainTrustedForAttestation] instance.
 *
 * Example:
 * ```
 * val config = StatusListTrustConfigBuilder().apply {
 *     trustSource(myComposeChainTrust)
 *     classifications(myClassifications)
 *     policy {
 *         default(TrustPolicy.Action.ENFORCE)
 *         forContext(VerificationContext.PID, TrustPolicy.Action.INFORM)
 *     }
 * }.build()
 * ```
 */
class StatusListTrustConfigBuilder {

    private var preBuiltAttestation: IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>? = null
    private var eudiwSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>? = null
    private var classifications: AttestationClassifications? = null
    private var policyBuilder: (TrustPolicy.Builder.() -> Unit)? = null

    /**
     * Sets the trust source from a pre-built [IsChainTrustedForAttestation].
     *
     * When using this overload, [classifications] is not required (the attestation
     * already encapsulates the classification logic).
     *
     * @param source the pre-built attestation trust source
     */
    fun trustSource(source: IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>) {
        this.preBuiltAttestation = source
        this.eudiwSource = null
    }

    /**
     * Sets the trust source from a [ComposeChainTrust] instance.
     *
     * When using this overload, [classifications] must be provided before calling [build].
     *
     * @param source the composed chain trust source
     */
    fun trustSource(source: ComposeChainTrust<List<X509Certificate>, VerificationContext, TrustAnchor>) {
        this.eudiwSource = source
        this.preBuiltAttestation = null
    }

    /**
     * Sets the trust source from an [IsChainTrustedForEUDIW] instance.
     *
     * When using this overload, [classifications] must be provided before calling [build].
     *
     * @param source the EUDIW trust source
     */
    fun trustSource(source: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>) {
        this.eudiwSource = source
        this.preBuiltAttestation = null
    }

    /**
     * Sets the attestation classifications used to map credential types to verification contexts.
     *
     * Required when the trust source is an [IsChainTrustedForEUDIW] or [ComposeChainTrust].
     *
     * @param classifications the attestation classifications
     */
    fun classifications(classifications: AttestationClassifications) {
        this.classifications = classifications
    }

    /**
     * Configures the trust policy using the [TrustPolicy.Builder] DSL.
     *
     * If not called, the default policy is [TrustPolicy.Action.ENFORCE] for all credentials.
     *
     * @param block configuration block applied to the [TrustPolicy.Builder]
     */
    fun policy(block: TrustPolicy.Builder.() -> Unit) {
        this.policyBuilder = block
    }

    /**
     * Builds the [StatusListTrustConfig] from the current builder state.
     *
     * @return a validated [StatusListTrustConfig]
     * @throws IllegalArgumentException if no trust source is configured, or if classifications
     *   are missing when required by the trust source type
     */
    internal fun build(): StatusListTrustConfig = build(null, null)

    /**
     * Builds the [StatusListTrustConfig], using the provided defaults for trust source and
     * classifications when they have not been explicitly set via [trustSource] / [classifications].
     *
     * @param defaultSource default EUDIW trust source if [trustSource] was not called
     * @param defaultClassifications default classifications if [classifications] was not called
     * @return a validated [StatusListTrustConfig]
     * @throws IllegalArgumentException if no trust source is available
     */
    internal fun build(
        defaultSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
        defaultClassifications: AttestationClassifications?,
    ): StatusListTrustConfig {
        val effectiveClassifications = classifications ?: defaultClassifications

        val attestation = preBuiltAttestation ?: run {
            val eudiw = eudiwSource ?: defaultSource
                ?: error("A trust source must be provided via trustSource()")
            val cls = effectiveClassifications
                ?: error("AttestationClassifications must be provided when using IsChainTrustedForEUDIW as trust source")
            IsChainTrustedForAttestation(eudiw, cls)
        }

        val trustPolicy = policyBuilder?.let { TrustPolicy.build(it) }
            ?: TrustPolicy.uniform(TrustPolicy.Action.ENFORCE)

        return StatusListTrustConfig(
            isChainTrustedForAttestation = attestation,
            classifications = effectiveClassifications,
            trustPolicy = trustPolicy,
        )
    }
}
