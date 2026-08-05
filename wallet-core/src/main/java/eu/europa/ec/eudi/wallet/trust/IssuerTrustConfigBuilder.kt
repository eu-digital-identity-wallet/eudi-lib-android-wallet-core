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
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.IssuerTrust
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import kotlin.reflect.KClass

/**
 * DSL builder for constructing an [IssuerTrustConfig].
 *
 * A trust source must be provided via one of the [trustSource] overloads.
 * When using [IsChainTrustedForEUDIW] or [ComposeChainTrust] as the trust source,
 * [classifications] must also be provided so the builder can construct an
 * [IsChainTrustedForAttestation] instance.
 *
 * Example:
 * ```
 * val config = IssuerTrustConfigBuilder().apply {
 *     trustSource(myComposeChainTrust)
 *     classifications(myClassifications)
 *     policy {
 *         default(TrustPolicy.Action.ENFORCE)
 *         forContext(VerificationContext.PID, TrustPolicy.Action.INFORM)
 *     }
 * }.build()
 * ```
 */
class IssuerTrustConfigBuilder {

    private var preBuiltAttestation: IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>? = null
    private var eudiwSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>? = null
    private var classifications: AttestationClassifications? = null
    private var policyBuilder: (TrustPolicy.Builder.() -> Unit)? = null
    private val customVerifiers = mutableMapOf<KClass<out DocumentFormat>, CredentialTrustVerifier>()
    private var metadataPolicyMode: MetadataPolicyMode = MetadataPolicyMode.REQUIRE

    private enum class MetadataPolicyMode { IGNORE, REQUIRE, PREFER }

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
     * Requires signed issuer metadata for OpenID4VCI operations.
     *
     * When set, the wallet will only accept signed JWT metadata from credential issuers.
     * The signing certificate chain from the JWT `x5c` header is validated against the
     * configured ETSI trust source using
     * [VerificationContext.WalletRelyingPartyAccessCertificate].
     *
     * Requires an [IsChainTrustedForEUDIW] trust source (set via [trustSource]).
     */
    fun requireSignedMetadata() {
        this.metadataPolicyMode = MetadataPolicyMode.REQUIRE
    }

    /**
     * Prefers signed issuer metadata for OpenID4VCI operations.
     *
     * The wallet will prefer signed JWT metadata from credential issuers but
     * will fall back to unsigned metadata if signed metadata is not available.
     * Use this to relax the default [requireSignedMetadata] behaviour.
     *
     * Requires an [IsChainTrustedForEUDIW] trust source (set via [trustSource]).
     */
    fun preferSignedMetadata() {
        this.metadataPolicyMode = MetadataPolicyMode.PREFER
    }

    /**
     * Disables signed issuer metadata verification.
     *
     * When set, the wallet will ignore signed metadata and use only unsigned metadata.
     * Use this to explicitly opt out of the default [requireSignedMetadata] behaviour.
     */
    fun ignoreSignedMetadata() {
        this.metadataPolicyMode = MetadataPolicyMode.IGNORE
    }

    /**
     * Registers a custom [CredentialTrustVerifier] for a specific [DocumentFormat] type.
     *
     * @param format the document format class to associate the verifier with
     * @param verifier the credential trust verifier implementation
     */
    fun credentialTrustVerifier(format: KClass<out DocumentFormat>, verifier: CredentialTrustVerifier) {
        customVerifiers[format] = verifier
    }

    /**
     * Builds the [IssuerTrustConfig] from the current builder state.
     *
     * @return a validated [IssuerTrustConfig]
     * @throws IllegalArgumentException if no trust source is configured, or if classifications
     *   are missing when required by the trust source type
     */
    internal fun build(): IssuerTrustConfig = build(null, null)

    /**
     * Builds the [IssuerTrustConfig], using the provided defaults for trust source and
     * classifications when they have not been explicitly set via [trustSource] / [classifications].
     *
     * This is used by [eu.europa.ec.eudi.wallet.EudiWallet.Builder] to inject the centrally
     * configured ETSI trust source from [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureEtsiTrust].
     *
     * @param defaultSource default EUDIW trust source if [trustSource] was not called
     * @param defaultClassifications default classifications if [classifications] was not called
     * @return a validated [IssuerTrustConfig]
     * @throws IllegalArgumentException if no trust source is available
     */
    internal fun build(
        defaultSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
        defaultClassifications: AttestationClassifications?,
        logger: Logger? = null,
    ): IssuerTrustConfig {
        val effectiveEudiwSource = eudiwSource ?: defaultSource
        val effectiveClassifications = classifications ?: defaultClassifications

        val attestation = preBuiltAttestation ?: run {
            val eudiw = requireNotNull(effectiveEudiwSource) {
                "A trust source must be provided via trustSource()"
            }
            val cls = requireNotNull(effectiveClassifications) {
                "AttestationClassifications must be provided when using IsChainTrustedForEUDIW as trust source"
            }
            IsChainTrustedForAttestation(eudiw, cls)
        }

        val trustPolicy = policyBuilder?.let { TrustPolicy.build(it) }
            ?: TrustPolicy.uniform(TrustPolicy.Action.ENFORCE)

        val defaultVerifiers = mapOf<KClass<out DocumentFormat>, CredentialTrustVerifier>(
            MsoMdocFormat::class to MsoMdocCredentialTrustVerifier(attestation, logger),
            SdJwtVcFormat::class to SdJwtVcCredentialTrustVerifier(attestation, logger),
        )
        val verifiers = defaultVerifiers + customVerifiers

        val issuerMetadataPolicy = buildIssuerMetadataPolicy(effectiveEudiwSource, logger)

        return IssuerTrustConfig(
            isChainTrustedForAttestation = attestation,
            classifications = effectiveClassifications,
            trustPolicy = trustPolicy,
            credentialTrustVerifiers = verifiers,
            issuerMetadataPolicy = issuerMetadataPolicy,
        )
    }

    private fun buildIssuerMetadataPolicy(
        effectiveSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>? = eudiwSource,
        logger: Logger? = null,
    ): IssuerMetadataPolicy =
        when (metadataPolicyMode) {
            MetadataPolicyMode.IGNORE -> IssuerMetadataPolicy.IgnoreSigned
            MetadataPolicyMode.REQUIRE,
            MetadataPolicyMode.PREFER -> {
                val source = effectiveSource
                    ?: throw IllegalArgumentException(
                        "Signed metadata verification requires an IsChainTrustedForEUDIW trust source. " +
                            "Call ignoreSignedMetadata() to opt out."
                    )
                val issuerTrust = IssuerTrust.ByCertificateChain(
                    EtsiCertificateChainTrust(source, logger)
                )
                if (metadataPolicyMode == MetadataPolicyMode.REQUIRE) {
                    IssuerMetadataPolicy.RequireSigned(issuerTrust)
                } else {
                    IssuerMetadataPolicy.PreferSigned(issuerTrust)
                }
            }
        }
}
