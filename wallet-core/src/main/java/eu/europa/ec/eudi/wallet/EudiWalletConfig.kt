/*
 * Copyright (c) 2023-2026 European Commission
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
@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet

import android.content.Context
import androidx.annotation.RawRes
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.RevocationPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.wallet.EudiWalletConfig.Companion.DEFAULT_DOCUMENT_MANAGER_IDENTIFIER
import eu.europa.ec.eudi.wallet.dcapi.DCAPIConfig
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.KeyProtectionPolicy
import eu.europa.ec.eudi.wallet.internal.getCertificate
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerRegistrationPolicy
import eu.europa.ec.eudi.wallet.registration.relyingparty.WrpRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.relyingparty.WrpRegistrationPolicy
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.statium.DocumentStatusResolverConfigBuilder
import eu.europa.ec.eudi.wallet.trust.EtsiTrustConfig
import eu.europa.ec.eudi.wallet.trust.EtsiTrustConfigBuilder
import eu.europa.ec.eudi.wallet.trust.IssuerTrustConfig
import eu.europa.ec.eudi.wallet.trust.IssuerTrustConfigBuilder
import eu.europa.ec.eudi.wallet.trust.ReaderAuthenticationConfigBuilder
import eu.europa.ec.eudi.wallet.trust.ReaderTrustConfigBuilder
import eu.europa.ec.eudi.wallet.trust.StatusListTrustConfig
import eu.europa.ec.eudi.wallet.trust.asReaderTrustStore
import java.security.cert.TrustAnchor
import org.multipaz.mdoc.zkp.ZkSystemRepository
import java.security.cert.X509Certificate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Eudi wallet config. This config is used to configure the default settings of the Eudi wallet.
 *
 * Custom configuration and implementations of the various components can be provided using the
 * [EudiWallet.Builder] class.
 *
 * Example usage:
 *
 * ```
 * val storageFile = File(applicationContext.noBackupFilesDir.path, "main.db")
 * val config = EudiWalletConfig()
 *     .configureDocumentManager(storageFile.absolutePath)
 *     .configureLogging(
 *         // set log level to info
 *         level = Logger.LEVEL_INFO
 *     )
 *     .configureDocumentKeyCreation(
 *         // require user authentication; building the wallet fails if the device is not secured
 *         userAuthentication = KeyProtectionPolicy.Required,
 *         // use the device's StrongBox if available to store the keys
 *         strongBox = KeyProtectionPolicy.IfSupported,
 *         // set userAuthenticationTimeout to 30 seconds
 *         userAuthenticationTimeout = 30.seconds,
 *     )
 *     .configureReaderTrustStore(
 *         // set the reader trusted certificates for the reader trust store
 *         listOf(readerCertificate)
 *     )
 *     .configureReaderAuthPolicy(
 *         // set the reader authentication enforcement policy
 *         // default is EnforceIfPresent
 *         ReaderAuthPolicy.EnforceIfPresent
 *     )
 *     .configureOpenId4Vci {
 *         withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.None("client-id"))
 *         withAuthFlowRedirectionURI("eudi-openid4ci://authorize")
 *         withParUsage(OpenId4VciManager.Config.ParUsage.Companion.IF_SUPPORTED)
 *     }
 *     .configureProximityPresentation(
 *         enableBlePeripheralMode = true,
 *         enableBleCentralMode = false,
 *         clearBleCache = true,
 *     )
 *     .configureOpenId4Vp {
 *         withEncryptionAlgorithms(
 *             EncryptionAlgorithm.ECDH_ES
 *         )
 *         withEncryptionMethods(
 *             EncryptionMethod.A128CBC_HS256,
 *             EncryptionMethod.A256GCM
 *         )
 *         withClientIdSchemes(
 *             ClientIdScheme.X509SanDns
 *         )
 *         withSchemes(
 *             "openid4vp",
 *             "eudi-openid4vp",
 *             "mdoc-openid4vp"
 *         )
 *     }
 *     .configureDCAPI {
 *         withEnabled(true) // Enable DCAPI, by default it is disabled
 *         withPrivilegedAllowlist("allowlist") // your own allowlist of privileged browsers/apps that you trust
 *         withSupportedProtocols(
 *             DCAPIProtocol.ISO_MDOC,
 *             DCAPIProtocol.OPENID4VP_V1_SIGNED,
 *             DCAPIProtocol.OPENID4VP_V1_UNSIGNED
 *         )
 *     }
 *     .configureZkp(
 *         // To enable ZKP Support provide a ZkSystemRepository, for example:
 *         zkSystemRepository = LongfellowZkSystemRepository(LongfellowCircuits.get(context)).build()
 *      )
 *
 * ```
 *
 * @property openId4VciConfig the OpenID4VCI configuration
 * @property openId4VpConfig the OpenID4VP configuration
 * @property dcapiConfig the DCAPI configuration
 * @property documentManagerIdentifier the document manager identifier
 * @property documentsStoragePath the documents storage path
 * @property enableBlePeripheralMode whether to enable BLE peripheral mode
 * @property enableBleCentralMode whether to enable BLE central mode
 * @property clearBleCache whether to clear the BLE cache
 * @property logLevel the log level
 * @property logSizeLimit the log size limit
 * @property readerTrustedCertificates the reader trusted certificates
 * @property readerAuthPolicy the reader authentication enforcement policy for proximity and DCAPI presentations
 * @property userAuthenticationPolicy whether the document keys require user authentication
 * @property userAuthenticationTimeout the user authentication timeout
 * @property strongBoxPolicy whether the document keys are created in StrongBox
 * @property documentStatusResolverClockSkew the clock skew for the document status resolver
 * @property zkSystemRepository the Zero-Knowledge Proofs (ZKP) system repository
 *
 * @see EudiWallet.Builder
 */

class EudiWalletConfig {
    /**
     * Configuration for OpenID4VCI operations. This can be set using [configureOpenId4Vci] methods.
     * When null, OpenID4VCI functionality requires configuration to be passed directly to methods that use it,
     * such as [EudiWallet.createOpenId4VciManager].
     */
    var openId4VciConfig: OpenId4VciManager.Config? = null
        private set

    /**
     * Configure OpenID for Verifiable Credential Issuance (OpenID4VCI).
     * This configuration is used by [EudiWallet.createOpenId4VciManager] when no specific config is provided.
     *
     * @see OpenId4VciManager.Config
     * @see OpenId4VciManager.Config.Builder
     *
     * @param openId4VciConfig the OpenID4VCI configuration
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vci(openId4VciConfig: OpenId4VciManager.Config) = apply {
        this.openId4VciConfig = openId4VciConfig
    }

    /**
     * Configure OpenID for Verifiable Credential Issuance (OpenID4VCI) using a builder pattern.
     * This configuration is used by [EudiWallet.createOpenId4VciManager] when no specific config is provided.
     *
     * @see OpenId4VciManager.Config
     * @see OpenId4VciManager.Config.Builder
     *
     * @param openId4VciConfig the OpenID4VCI configuration lambda with [OpenId4VciManager.Config.Builder] as receiver
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vci(openId4VciConfig: OpenId4VciManager.Config.Builder.() -> Unit) = apply {
        this.openId4VciConfig = OpenId4VciManager.Config.Builder().apply(openId4VciConfig).build()
    }

    var openId4VpConfig: OpenId4VpConfig? = null
        private set

    /**
     * Configure OpenID4VP.
     *
     * @see OpenId4VpConfig
     * @see OpenId4VpConfig.Builder
     *
     * @param openId4VpConfig the OpenID4VP configuration
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vp(openId4VpConfig: OpenId4VpConfig) = apply {
        this.openId4VpConfig = openId4VpConfig
    }

    /**
     * Configure OpenID4VP using a [OpenId4VpConfig.Builder] as a lambda with receiver.
     *
     * @see OpenId4VpConfig
     * @see OpenId4VpConfig.Builder
     *
     * @param openId4VpConfig the OpenID4VP configuration lambda
     * @return the [EudiWalletConfig] instance
     */
    fun configureOpenId4Vp(openId4VpConfig: OpenId4VpConfig.Builder.() -> Unit) = apply {
        this.openId4VpConfig = OpenId4VpConfig.Builder().apply(openId4VpConfig).build()
    }

    /**
     * Configuration for the Digital Credential API (DCAPI).
     */
    var dcapiConfig: DCAPIConfig? = null
        private set

    /**
     * Configure the DCAPI.
     *
     * @see DCAPIConfig
     * @see DCAPIConfig.Builder
     *
     * @param dcapiConfig the DCAPI configuration
     * @return the [EudiWalletConfig] instance
     */
    fun configureDCAPI(dcapiConfig: DCAPIConfig) = apply {
        this.dcapiConfig = dcapiConfig
    }

    /**
     * Configure the DCAPI using a [DCAPIConfig.Builder] as a lambda with receiver.
     *
     * @see DCAPIConfig
     * @see DCAPIConfig.Builder
     *
     * @param dcapiConfig the DCAPI configuration lambda
     * @return the [EudiWalletConfig] instance
     */
    fun configureDCAPI(dcapiConfig: DCAPIConfig.Builder.() -> Unit) = apply {
        this.dcapiConfig = DCAPIConfig.Builder().apply(dcapiConfig).build()
    }

    var documentManagerIdentifier: String = DEFAULT_DOCUMENT_MANAGER_IDENTIFIER
        private set
    var documentsStoragePath: String? = null
        internal set // internal for setting the default value from the builder

    /**
     * Configure the built-in document manager.
     *
     * Allowing to configure the documents storage path and the document manager identifier.
     * The default document manager identifier is set to
     * [DEFAULT_DOCUMENT_MANAGER_IDENTIFIER].
     *
     * @see eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
     * @see org.multipaz.storage.Storage
     *
     * @param storagePath the documents storage path
     * @param identifier the document manager identifier
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureDocumentManager(
        storagePath: String,
        identifier: String? = null
    ) = apply {
        documentsStoragePath = storagePath
        identifier?.let { documentManagerIdentifier = it }
    }

    var enableBlePeripheralMode: Boolean = true
        private set
    var enableBleCentralMode: Boolean = false
        private set
    var clearBleCache: Boolean = true
        private set
    var nfcEngagementServiceClass: Class<out NfcEngagementService>? = null
        private set

    /**
     * Configure the proximity presentation. This allows to configure the BLE peripheral mode,
     * the BLE central mode and whether to clear the BLE cache. Also, it allows to set the NFC
     * engagement service class an implementation of [NfcEngagementService], which is used to
     * handle the NFC engagement.
     *
     * The default values are:
     * - enableBlePeripheralMode: true
     * - enableBleCentralMode: false
     * - clearBleCache: true
     * - nfcEngagementServiceClass: null
     *
     * @param enableBlePeripheralMode whether to enable BLE peripheral mode
     * @param enableBleCentralMode whether to enable BLE central mode
     * @param clearBleCache whether to clear the BLE cache
     * @param nfcEngagementServiceClass the NFC engagement service class
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureProximityPresentation(
        enableBlePeripheralMode: Boolean = true,
        enableBleCentralMode: Boolean = false,
        clearBleCache: Boolean = true,
        nfcEngagementServiceClass: Class<out NfcEngagementService>? = null,
    ) = apply {
        this.enableBlePeripheralMode = enableBlePeripheralMode
        this.enableBleCentralMode = enableBleCentralMode
        this.clearBleCache = clearBleCache
        this.nfcEngagementServiceClass = nfcEngagementServiceClass
    }

    @Logger.Level
    var logLevel: Int = Logger.LEVEL_INFO
        private set
    var logSizeLimit: Int = 1000
        private set

    /**
     * Configure the built-in logging. This allows to configure the log level and the log size limit.
     *
     * The default log level is set to [Logger.LEVEL_INFO] and the default log size limit is set to
     * 1000.
     *
     * @param level the log level
     * @param sizeLimit the log size limit
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureLogging(level: Int, sizeLimit: Int? = null) = apply {
        logLevel = level
        sizeLimit?.let { logSizeLimit = it }
    }

    var readerTrustedCertificates: List<X509Certificate>? = null
        private set

    internal var readerTrustStore: ReaderTrustStore? = null
        private set

    /**
     * Configure the built-in [ReaderTrustStore] with a list of trusted certificates.
     *
     * The [revocationPolicy] controls certificate revocation checking (CRL/OCSP)
     * during trust path validation. The default is [RevocationPolicy.HardFail].
     * For ETSI/LoTE-based trust, use [configureEtsiTrust] with `pkixRevocationPolicy(...)` instead.
     *
     * Example:
     * ```
     * configureReaderTrustStore(
     *     listOf(certificate1, certificate2),
     *     revocationPolicy = RevocationPolicy.SoftFail
     * )
     * ```
     *
     * @param readerTrustedCertificates the reader trusted certificates
     * @param revocationPolicy the certificate revocation checking policy (default [RevocationPolicy.HardFail])
     * @return the [EudiWalletConfig] instance
     * @see RevocationPolicy
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { trustedCertificates(...) } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { trustedCertificates(readerTrustedCertificates) }"),
    )
    @JvmOverloads
    fun configureReaderTrustStore(
        readerTrustedCertificates: List<X509Certificate>,
        revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
    ) = apply {
        this.readerTrustedCertificates = readerTrustedCertificates
        this.revocationPolicy = revocationPolicy
    }

    /**
     * Configure the built-in [ReaderTrustStore] with trusted certificates.
     *
     * The [revocationPolicy] controls certificate revocation checking (CRL/OCSP)
     * during trust path validation. The default is [RevocationPolicy.HardFail].
     * For ETSI/LoTE-based trust, use [configureEtsiTrust] with `pkixRevocationPolicy(...)` instead.
     *
     * Example:
     * ```
     * configureReaderTrustStore(
     *     certificate1, certificate2,
     *     revocationPolicy = RevocationPolicy.SoftFail
     * )
     * ```
     *
     * @param readerTrustedCertificates the reader trusted certificates
     * @param revocationPolicy the certificate revocation checking policy (default [RevocationPolicy.HardFail])
     * @return the [EudiWalletConfig] instance
     * @see RevocationPolicy
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { trustedCertificates(...) } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { trustedCertificates(*readerTrustedCertificates) }"),
    )
    fun configureReaderTrustStore(
        vararg readerTrustedCertificates: X509Certificate,
        revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
    ) = apply {
        this.readerTrustedCertificates = readerTrustedCertificates.toList()
        this.revocationPolicy = revocationPolicy
    }

    /**
     * Configure the built-in [ReaderTrustStore] with certificates loaded from raw resources.
     *
     * The [revocationPolicy] controls certificate revocation checking (CRL/OCSP)
     * during trust path validation. The default is [RevocationPolicy.HardFail].
     * For ETSI/LoTE-based trust, use [configureEtsiTrust] with `pkixRevocationPolicy(...)` instead.
     *
     * Example:
     * ```
     * configureReaderTrustStore(
     *     context,
     *     R.raw.reader_cert_1, R.raw.reader_cert_2,
     *     revocationPolicy = RevocationPolicy.SoftFail
     * )
     * ```
     *
     * @param context the context
     * @param certificateRes the reader trusted certificates raw resources
     * @param revocationPolicy the certificate revocation checking policy (default [RevocationPolicy.HardFail])
     * @return the [EudiWalletConfig] instance
     * @see RevocationPolicy
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { trustedCertificates(context, ...) } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { trustedCertificates(context, *certificateRes) }"),
    )
    fun configureReaderTrustStore(
        context: Context,
        @RawRes vararg certificateRes: Int,
        revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
    ) = apply {
        this.readerTrustedCertificates = certificateRes.map { context.getCertificate(it) }
        this.revocationPolicy = revocationPolicy
    }

    /**
     * Configure the [ReaderTrustStore] with a custom implementation.
     *
     * Use this to provide any custom [ReaderTrustStore] implementation.
     * This takes priority over certificate-based configuration set via the
     * other [configureReaderTrustStore] overloads.
     *
     * @param readerTrustStore the custom reader trust store implementation
     * @return the [EudiWalletConfig] instance
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { trustStore(readerTrustStore) } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { trustStore(readerTrustStore) }"),
    )
    fun configureReaderTrustStore(readerTrustStore: ReaderTrustStore) = apply {
        this.readerTrustStore = readerTrustStore
    }

    /**
     * Configure the [ReaderTrustStore] with an ETSI-backed trust source.
     *
     * Creates an [eu.europa.ec.eudi.wallet.trust.EtsiReaderTrustStore] that delegates
     * reader certificate chain validation to the given [IsChainTrustedForEUDIW], using the
     * [VerificationContext.WalletRelyingPartyAccessCertificate][eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext.WalletRelyingPartyAccessCertificate]
     * verification context. This takes priority over certificate-based configuration.
     *
     * Example:
     * ```
     * val composeChainTrust: IsChainTrustedForEUDIW<...> = // from ETSI library
     * configureReaderTrustStore(composeChainTrust)
     * ```
     *
     * @param isChainTrusted the ETSI chain trust validator
     * @return the [EudiWalletConfig] instance
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { trustStore(isChainTrusted) } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { trustStore(isChainTrusted) }"),
    )
    fun configureReaderTrustStore(
        isChainTrusted: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>,
    ) = apply {
        this.readerTrustStore = isChainTrusted.asReaderTrustStore()
    }

    /**
     * Configure the [ReaderTrustStore] using the ETSI trust source from [configureEtsiTrust].
     *
     * Creates an [eu.europa.ec.eudi.wallet.trust.EtsiReaderTrustStore] that delegates
     * reader certificate chain validation to the centrally configured ETSI trust source.
     * Requires [configureEtsiTrust] to be called.
     *
     * Example with default policy ([ReaderAuthPolicy.EnforceIfPresent]):
     * ```
     * configureReaderTrustStore { }
     * ```
     *
     * Example requiring reader authentication for all presentations:
     * ```
     * configureReaderTrustStore {
     *     readerAuthPolicy(ReaderAuthPolicy.AlwaysRequire)
     * }
     * ```
     *
     * @param block configuration block applied to the [ReaderTrustConfigBuilder]
     * @return the [EudiWalletConfig] instance
     * @see configureEtsiTrust
     * @see ReaderTrustConfigBuilder
     * @see ReaderAuthPolicy
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { ... } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { alwaysRequire() }"),
    )
    fun configureReaderTrustStore(
        block: ReaderTrustConfigBuilder.() -> Unit,
    ) = apply {
        val builder = ReaderTrustConfigBuilder().apply(block)
        builder.readerAuthPolicy?.let { this.readerAuthPolicy = it }
    }

    /**
     * The reader authentication enforcement policy for proximity and DCAPI presentations.
     * This determines how the wallet handles reader authentication results when generating
     * device responses.
     *
     * The available policies are:
     * - [ReaderAuthPolicy.DoNotEnforce]: Reader trust is evaluated when a trust source is
     *   configured, but never blocks document disclosure. This was the default behavior before
     *   version 0.27.0.
     * - [ReaderAuthPolicy.EnforceIfPresent]: The request is rejected when a reader's certificate
     *   is not trusted (default).
     * - [ReaderAuthPolicy.AlwaysRequire]: The request is rejected unless reader authentication
     *   is present and trusted.
     *
     * The default is [ReaderAuthPolicy.EnforceIfPresent].
     *
     * @see ReaderAuthPolicy
     * @see configureReaderAuthentication
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { ... } instead. " +
            "The policy is now determined by the DSL builder.",
    )
    var readerAuthPolicy: ReaderAuthPolicy? = null
        private set

    /**
     * Configure the reader authentication enforcement policy.
     * This policy controls how reader authentication results affect document disclosure
     * during proximity and DCAPI presentations.
     *
     * When a verifier's DeviceRequest includes reader authentication and the verifier's
     * certificate is not in the configured [ReaderTrustStore], the policy determines whether
     * the document is included in the response or excluded.
     *
     * Per ISO 18013-5, when all documents are excluded due to reader authentication failure,
     * the wallet returns a DeviceResponse with status 10 (General Error).
     *
     * @param readerAuthPolicy the reader authentication enforcement policy
     * @return the [EudiWalletConfig] instance
     *
     * @see ReaderAuthPolicy
     * @see configureReaderAuthentication
     */
    @Deprecated(
        message = "Use configureReaderAuthentication { ... } instead.",
        replaceWith = ReplaceWith("configureReaderAuthentication { /* set enforcement via doNotEnforce(), enforceIfPresent(), or alwaysRequire() */ }"),
    )
    fun configureReaderAuthPolicy(readerAuthPolicy: ReaderAuthPolicy) = apply {
        this.readerAuthPolicy = readerAuthPolicy
    }

    /**
     * The stored reader authentication DSL block. When non-null, it is executed at
     * [EudiWallet.Builder.build()][eu.europa.ec.eudi.wallet.EudiWallet] time to produce
     * the [ReaderAuthPolicy] with embedded trust material.
     */
    internal var readerAuthenticationBlock: (ReaderAuthenticationConfigBuilder.() -> Unit)? = null
        private set

    /**
     * Configure reader authentication using the unified DSL.
     *
     * This replaces the scattered `configureReaderTrustStore` overloads and
     * `configureReaderAuthPolicy` with a single entry point that bundles trust
     * material and enforcement intent together.
     *
     * The block is stored at configuration time and executed at
     * [EudiWallet.Builder.build()][eu.europa.ec.eudi.wallet.EudiWallet] time so that
     * deferred trust sources (e.g. from [configureEtsiTrust]) are available.
     *
     * The default enforcement is `enforceIfPresent()`. When enforcement is set to
     * `enforceIfPresent()` or `alwaysRequire()` but no trust material is configured
     * (and no central ETSI trust source is available), an [IllegalArgumentException]
     * is thrown at build time. Call `doNotEnforce()` to explicitly opt out.
     *
     * Example with static certificates:
     * ```
     * configureReaderAuthentication {
     *     trustedCertificates(cert1, cert2)
     *     revocationPolicy(RevocationPolicy.SoftFail)
     *     enforceIfPresent()
     * }
     * ```
     *
     * Example with ETSI trust (from [configureEtsiTrust]):
     * ```
     * configureEtsiTrust { ... }
     * configureReaderAuthentication {
     *     alwaysRequire()
     * }
     * ```
     *
     * Example opting out of enforcement:
     * ```
     * configureReaderAuthentication {
     *     trustedCertificates(cert1)
     *     doNotEnforce()
     * }
     * ```
     *
     * @param block configuration block applied to [ReaderAuthenticationConfigBuilder]
     * @return the [EudiWalletConfig] instance
     * @see ReaderAuthenticationConfigBuilder
     * @see ReaderAuthPolicy
     */
    fun configureReaderAuthentication(
        block: ReaderAuthenticationConfigBuilder.() -> Unit,
    ) = apply {
        this.readerAuthenticationBlock = block
    }

    /**
     * The certificate revocation checking policy used during reader authentication
     * trust path validation.
     *
     * This applies only when the [ReaderTrustStore] is built from static trusted
     * certificates (via [configureReaderTrustStore] with certificate lists or raw
     * resources). ETSI/LoTE-based trust stores handle revocation internally via
     * [configureEtsiTrust] with `pkixRevocationPolicy(...)`.
     *
     * Set this through the `revocationPolicy` parameter of the certificate-based
     * [configureReaderTrustStore] overloads.
     *
     * The available policies are:
     * - [RevocationPolicy.NoCheck]: No revocation checking is performed.
     * - [RevocationPolicy.HardFail]: Validation fails if a certificate is revoked
     *   **or** if the CRL/OCSP responder cannot be reached (default).
     * - [RevocationPolicy.SoftFail]: Validation fails if a certificate is revoked,
     *   but tolerates CRL/OCSP unavailability.
     *
     * The default is [RevocationPolicy.HardFail].
     *
     * @see RevocationPolicy
     * @see configureReaderTrustStore
     */
    var revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail
        private set

    /**
     * The policy for handling the relying party's registration certificate during presentation
     * requests.
     *
     * - [WrpRegistrationPolicy.Disabled]: the registration certificate is not validated or surfaced.
     * - [WrpRegistrationPolicy.Enabled] (default): the registration certificate is validated and the user
     *   is informed of the outcome — warned when it could not be validated and when requested
     *   attributes fall outside the registered scope — without blocking the presentation. On the
     *   OpenID4VP path, the OpenID4VP library also applies its own checks to the registration
     *   certificate and may reject the request.
     *
     * The registration certificate is validated against the certificates set through
     * [configureWrpRegistrationTrust], else against the ETSI Trusted Lists of [configureEtsiTrust]. The
     * reader authentication trust ([configureReaderAuthentication]) is not used. With the policy enabled
     * and neither [configureWrpRegistrationTrust] nor [configureEtsiTrust] called, building the wallet
     * fails with an [IllegalArgumentException]. When the ETSI Trusted Lists are the source and the
     * Wallet-Relying Party registration certificate provider list (`wrprcProviders`) is not configured,
     * the wallet builds and registration certificates fail with
     * [eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason.UNTRUSTED_PROVIDER].
     *
     * @see WrpRegistrationPolicy
     */
    var wrpRegistrationPolicy: WrpRegistrationPolicy = WrpRegistrationPolicy.Enabled
        private set

    /**
     * Configure the relying party registration policy.
     *
     * @param wrpRegistrationPolicy the relying party registration policy
     * @return the [EudiWalletConfig] instance
     * @see WrpRegistrationPolicy
     */
    fun configureWrpRegistrationPolicy(wrpRegistrationPolicy: WrpRegistrationPolicy) = apply {
        this.wrpRegistrationPolicy = wrpRegistrationPolicy
    }

    /**
     * A custom evaluator for the relying party's registration, applied once the registration
     * certificate has been authenticated. It receives the described registration, the requester's
     * access certificate and the requested attestations, and returns a
     * [eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult].
     *
     * This is the recommended extension point for customizing the registration checks: the same
     * evaluator governs every transport, since the wallet applies it after authenticating the
     * certificate on the proximity and DC-API paths and supplies it to the OpenID4VP library on the
     * remote path. When null, the built-in evaluator is used (validity, binding, revocation and
     * over-asking).
     *
     * @see WrpRegistrationEvaluator
     */
    var wrpRegistrationEvaluator: WrpRegistrationEvaluator? = null
        private set

    /**
     * Configure a custom relying party registration evaluator.
     *
     * @param evaluator the custom evaluator applied across all presentation channels
     * @return the [EudiWalletConfig] instance
     * @see WrpRegistrationEvaluator
     */
    fun configureWrpRegistrationEvaluator(evaluator: WrpRegistrationEvaluator) = apply {
        this.wrpRegistrationEvaluator = evaluator
    }

    internal var wrpRegistrationTrustedCertificates: List<X509Certificate>? = null
        private set

    internal var wrpRegistrationStatusListTrustedCertificates: List<X509Certificate>? = null
        private set

    internal var wrpRegistrationRevocationPolicy: RevocationPolicy = RevocationPolicy.HardFail
        private set

    /**
     * Configure the trust anchors — the certificate authority certificates of the registration
     * certificate providers — for relying party registration certificates and for the status lists
     * that carry their revocation status. They take precedence over the ETSI Trusted Lists set with
     * [configureEtsiTrust].
     *
     * When not configured, the registration certificate signer and the status list signer are
     * validated against the ETSI Trusted Lists; without them, building the wallet fails while
     * [wrpRegistrationPolicy] is [WrpRegistrationPolicy.Enabled].
     *
     * @param trustedCertificates the trust anchors for the registration certificate signer chain
     * @param trustedStatusListCertificates the trust anchors for the status list signer chain; defaults to
     * [trustedCertificates]. Set it when the status lists are signed under a different certificate
     * authority, otherwise their revocation status cannot be confirmed (`REVOCATION_STATUS_UNKNOWN`).
     * @param revocationPolicy the certificate revocation checking policy (default [RevocationPolicy.HardFail])
     * @return the [EudiWalletConfig] instance
     * @throws IllegalArgumentException if [trustedCertificates] or [trustedStatusListCertificates] is empty
     */
    @JvmOverloads
    fun configureWrpRegistrationTrust(
        trustedCertificates: List<X509Certificate>,
        trustedStatusListCertificates: List<X509Certificate> = trustedCertificates,
        revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
    ) = apply {
        require(trustedCertificates.isNotEmpty()) { "trustedCertificates must not be empty" }
        require(trustedStatusListCertificates.isNotEmpty()) { "trustedStatusListCertificates must not be empty" }
        this.wrpRegistrationTrustedCertificates = trustedCertificates
        this.wrpRegistrationStatusListTrustedCertificates = trustedStatusListCertificates
        this.wrpRegistrationRevocationPolicy = revocationPolicy
    }

    /**
     * The issuer registration certificate policy applied during OpenID4VCI issuance;
     * [IssuerRegistrationPolicy.Enabled] by default.
     *
     * @see IssuerRegistrationPolicy
     */
    var issuerRegistrationPolicy: IssuerRegistrationPolicy = IssuerRegistrationPolicy.Enabled
        private set

    /**
     * Configure whether the wallet validates a credential issuer's registration certificate during
     * issuance. Enabled by default; call with [IssuerRegistrationPolicy.Disabled] to opt out.
     *
     * The registration certificate is carried in the signed issuer metadata, so validation only runs
     * when signed metadata is required ([configureIssuerTrust], whose default is
     * `requireSignedMetadata()`); otherwise it is skipped. When it runs, the OpenID4VCI library stops
     * the issuance if the issuer metadata carries no registration certificate, more than one, or one
     * that is not a string. The issuance also stops when the certificate fails validation or the
     * offered attestations are outside its registered scope; nothing is then requested from the issuer.
     *
     * With the policy enabled and signed metadata required, trust for the registration certificate
     * signer must be configured ([configureIssuerRegistrationTrust] or [configureEtsiTrust]); otherwise
     * building the wallet fails with an [IllegalArgumentException].
     *
     * @param policy the issuer registration policy
     * @return the [EudiWalletConfig] instance
     * @see IssuerRegistrationPolicy
     */
    fun configureIssuerRegistrationPolicy(policy: IssuerRegistrationPolicy) = apply {
        this.issuerRegistrationPolicy = policy
    }

    /**
     * A custom evaluator applied to an authenticated issuer registration certificate during issuance.
     * When null, the built-in evaluator is used (validity, binding, revocation and over-providing).
     *
     * @see IssuerRegistrationEvaluator
     */
    var issuerRegistrationEvaluator: IssuerRegistrationEvaluator? = null
        private set

    /**
     * Configure a custom issuer registration evaluator.
     *
     * @param evaluator the custom evaluator applied during issuance
     * @return the [EudiWalletConfig] instance
     * @see IssuerRegistrationEvaluator
     */
    fun configureIssuerRegistrationEvaluator(evaluator: IssuerRegistrationEvaluator) = apply {
        this.issuerRegistrationEvaluator = evaluator
    }

    internal var issuerRegistrationTrustedCertificates: List<X509Certificate>? = null
        private set

    internal var issuerRegistrationStatusListTrustedCertificates: List<X509Certificate>? = null
        private set

    internal var issuerRegistrationRevocationPolicy: RevocationPolicy = RevocationPolicy.HardFail
        private set

    /**
     * Configure the trust anchors — the certificate authority certificates of the registration
     * certificate providers — for credential issuer registration certificates and for the status lists
     * that carry their revocation status. They take precedence over the ETSI Trusted Lists set with
     * [configureEtsiTrust].
     *
     * When not configured, the registration certificate signer and the status list signer are
     * validated against the ETSI Trusted Lists; without them, building the wallet fails while
     * [issuerRegistrationPolicy] is [IssuerRegistrationPolicy.Enabled] and signed metadata is required.
     *
     * The registration certificate is carried in the signed issuer metadata, so it is validated only
     * when signed metadata is required ([configureIssuerTrust], whose default is
     * `requireSignedMetadata()`).
     *
     * @param trustedCertificates the trust anchors for the registration certificate signer chain
     * @param trustedStatusListCertificates the trust anchors for the status list signer chain; defaults to
     * [trustedCertificates]. Set it when the status lists are signed under a different certificate
     * authority, otherwise their revocation status cannot be confirmed (`REVOCATION_STATUS_UNKNOWN`).
     * @param revocationPolicy the certificate revocation checking policy (default [RevocationPolicy.HardFail])
     * @return the [EudiWalletConfig] instance
     * @throws IllegalArgumentException if [trustedCertificates] or [trustedStatusListCertificates] is empty
     */
    @JvmOverloads
    fun configureIssuerRegistrationTrust(
        trustedCertificates: List<X509Certificate>,
        trustedStatusListCertificates: List<X509Certificate> = trustedCertificates,
        revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail,
    ) = apply {
        require(trustedCertificates.isNotEmpty()) { "trustedCertificates must not be empty" }
        require(trustedStatusListCertificates.isNotEmpty()) { "trustedStatusListCertificates must not be empty" }
        this.issuerRegistrationTrustedCertificates = trustedCertificates
        this.issuerRegistrationStatusListTrustedCertificates = trustedStatusListCertificates
        this.issuerRegistrationRevocationPolicy = revocationPolicy
    }

    var userAuthenticationPolicy: KeyProtectionPolicy = KeyProtectionPolicy.Required
        private set
    var userAuthenticationTimeout: Duration = 0.milliseconds
        private set
    var strongBoxPolicy: KeyProtectionPolicy = KeyProtectionPolicy.IfSupported
        private set

    /**
     * Whether user authentication was requested for the document keys. The value that applies to
     * the keys is [EudiWallet.keyProtectionState].
     */
    @Deprecated(
        "Returns the requested policy, not the value that applies to the keys. Use " +
            "EudiWallet.keyProtectionState.userAuthenticationRequired for the value that applies to the keys, " +
            "or userAuthenticationPolicy for the requested policy",
        level = DeprecationLevel.ERROR,
    )
    val userAuthenticationRequired: Boolean
        get() = userAuthenticationPolicy != KeyProtectionPolicy.Disabled

    /**
     * Whether StrongBox was requested for the document keys. The value that applies to the keys is
     * [EudiWallet.keyProtectionState].
     */
    @Deprecated(
        "Returns the requested policy, not the value that applies to the keys. Use " +
            "EudiWallet.keyProtectionState.useStrongBox for the value that applies to the keys, " +
            "or strongBoxPolicy for the requested policy",
        level = DeprecationLevel.ERROR,
    )
    val useStrongBoxForKeys: Boolean
        get() = strongBoxPolicy != KeyProtectionPolicy.Disabled

    /**
     * Configure the protection of the keys created with
     * [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateKeySettings] and
     * [eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings].
     * These methods create the keys in [org.multipaz.securearea.AndroidKeystoreSecureArea]. Keys in
     * other secure areas, or created with a `configure` block, use the settings given by the app.
     *
     * The device support is checked once, when the wallet is built, for the whole wallet. With
     * [KeyProtectionPolicy.Required], building the wallet fails with
     * [eu.europa.ec.eudi.wallet.document.KeyProtectionUnsupportedException] if the device does not
     * support the protection. With [KeyProtectionPolicy.IfSupported], the keys are created without it. The
     * protections that apply are in [EudiWallet.keyProtectionState].
     *
     * Without this call, user authentication is [KeyProtectionPolicy.Required] with a timeout of 0,
     * and StrongBox is [KeyProtectionPolicy.IfSupported].
     *
     * @param userAuthentication whether the keys require user authentication; the device must be
     * secured with a PIN, pattern or password
     * @param strongBox whether the keys are created in StrongBox
     * @param userAuthenticationTimeout if 0, user authentication is required for every use of the
     * key, otherwise it is required within the given duration
     */
    fun configureDocumentKeyCreation(
        userAuthentication: KeyProtectionPolicy,
        strongBox: KeyProtectionPolicy,
        userAuthenticationTimeout: Duration = 0.milliseconds,
    ) = apply {
        this.userAuthenticationPolicy = userAuthentication
        this.strongBoxPolicy = strongBox
        this.userAuthenticationTimeout = userAuthenticationTimeout

        if (userAuthentication != KeyProtectionPolicy.Disabled) {
            require(userAuthenticationTimeout >= 0.milliseconds) { "User authentication timeout must be equal or greater than 0" }
        }
    }

    /**
     * Configure the document key creation. `userAuthenticationRequired = true` maps to
     * [KeyProtectionPolicy.Required], `useStrongBoxForKeys = true` to [KeyProtectionPolicy.IfSupported],
     * and `false` to [KeyProtectionPolicy.Disabled].
     *
     * Use the overload that takes [KeyProtectionPolicy] values instead.
     *
     * @param userAuthenticationRequired whether user authentication is required
     * @param userAuthenticationTimeout  If 0, user authentication is required for every use of the
     * key, otherwise it's required within the given amount of milliseconds
     * @param useStrongBoxForKeys whether to use the strong box for keys
     */
    @Deprecated(
        "Use configureDocumentKeyCreation with KeyProtectionPolicy values. userAuthenticationRequired = true " +
            "maps to KeyProtectionPolicy.Required, useStrongBoxForKeys = true to KeyProtectionPolicy.IfSupported, " +
            "and false to KeyProtectionPolicy.Disabled."
    )
    fun configureDocumentKeyCreation(
        userAuthenticationRequired: Boolean = true,
        userAuthenticationTimeout: Duration = 0.milliseconds,
        useStrongBoxForKeys: Boolean = true,
    ) = configureDocumentKeyCreation(
        userAuthentication = if (userAuthenticationRequired) KeyProtectionPolicy.Required else KeyProtectionPolicy.Disabled,
        strongBox = if (useStrongBoxForKeys) KeyProtectionPolicy.IfSupported else KeyProtectionPolicy.Disabled,
        userAuthenticationTimeout = userAuthenticationTimeout,
    )

    var documentStatusResolverClockSkew: Duration = Duration.ZERO
        internal set

    internal var statusListTrustConfig: StatusListTrustConfig? = null
        internal set

    internal var statusResolverBlock: (DocumentStatusResolverConfigBuilder.() -> Unit)? = null
        private set

    /**
     * Configure the document status resolver clock skew. This allows to configure the clock skew for
     * the provided document status resolver.
     */
    fun configureDocumentStatusResolver(clockSkewInMinutes: Long) = apply {
        this.documentStatusResolverClockSkew = clockSkewInMinutes.minutes
    }

    /**
     * Configure the document status resolver with clock skew and optional ETSI trust verification
     * for status list token signers.
     *
     * When [configureEtsiTrust] is also called, `trustSource()` and `classifications()` inside
     * the `configureTrust` block are optional — they default to the centrally configured
     * ETSI trust source. Explicit calls override the defaults.
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
     *
     * @param block configuration block applied to the [DocumentStatusResolverConfigBuilder]
     * @return the [EudiWalletConfig] instance
     * @see DocumentStatusResolverConfigBuilder
     * @see configureEtsiTrust
     */
    fun configureDocumentStatusResolver(
        block: DocumentStatusResolverConfigBuilder.() -> Unit,
    ) = apply {
        this.statusResolverBlock = block
    }

    var zkSystemRepository: ZkSystemRepository? = null
        private set

    /**
     * The policy that determines behavior when ZK proof generation fails during
     * response generation.
     *
     * The available policies are:
     * - [ZkResponsePolicy.Strict]: Aborts disclosure for the document when ZK proof
     *   generation fails, preventing unintended full document disclosure (default).
     * - [ZkResponsePolicy.FallbackToFullDisclosure]: Falls back to sending the full
     *   document when ZK proof generation fails.
     *
     * The default is [ZkResponsePolicy.Strict].
     *
     * @see ZkResponsePolicy
     */
    var zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.Strict
        private set

    /**
     * Configure Zero-Knowledge Proofs (ZKP) support.
     * This allows you to enable ZKP support by providing a [ZkSystemRepository]
     * and optionally set the [ZkResponsePolicy] that determines behavior when
     * ZK proof generation fails. The default policy is [ZkResponsePolicy.Strict].
     *
     * @param zkSystemRepository the ZK system repository
     * @param zkResponsePolicy the ZK response policy (default [ZkResponsePolicy.Strict])
     * @return the [EudiWalletConfig] instance
     */
    @JvmOverloads
    fun configureZkp(
        zkSystemRepository: ZkSystemRepository,
        zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.Strict
    ) = apply {
        this.zkSystemRepository = zkSystemRepository
        this.zkResponsePolicy = zkResponsePolicy
    }

    internal var issuerTrustConfig: IssuerTrustConfig? = null
        internal set

    internal var issuerTrustBlock: (IssuerTrustConfigBuilder.() -> Unit)? = null
        private set

    /**
     * Configure issuer trust verification for credentials issued via OpenID4VCI.
     * Trust verification occurs after issuance, before storage.
     *
     * Issuer trust is verified by default. Without this call, the trust source and
     * classifications of [configureEtsiTrust] are used with the `TrustPolicy.Action.ENFORCE`
     * policy, and building the wallet fails when no trust source is available. Call
     * `doNotVerify()` to turn verification off.
     *
     * When [configureEtsiTrust] is also called, `trustSource()` and `classifications()`
     * are optional — they default to the centrally configured ETSI trust source.
     * Explicit calls override the defaults.
     *
     * Example with [configureEtsiTrust] (trust source inherited):
     * ```
     * configureIssuerTrust {
     *     policy {
     *         default(TrustPolicy.Action.ENFORCE)
     *     }
     *     // requireSignedMetadata() is the default — verifies signed issuer metadata JWTs
     *     // ignoreSignedMetadata() to skip metadata signature checks
     * }
     * ```
     *
     * Example with explicit trust source:
     * ```
     * configureIssuerTrust {
     *     trustSource(myComposeChainTrust)
     *     classifications(myClassifications)
     *     policy {
     *         default(TrustPolicy.Action.INFORM)
     *         forContext(VerificationContext.PID, TrustPolicy.Action.ENFORCE)
     *     }
     * }
     * ```
     *
     * @param block configuration block applied to the [IssuerTrustConfigBuilder]
     * @return the [EudiWalletConfig] instance
     * @see IssuerTrustConfigBuilder
     * @see configureEtsiTrust
     */
    fun configureIssuerTrust(
        block: IssuerTrustConfigBuilder.() -> Unit,
    ) = apply {
        this.issuerTrustBlock = block
    }

    internal var etsiTrustConfig: EtsiTrustConfig? = null
        private set

    /**
     * Configure the ETSI LoTE (List of Trusted Entities) trust infrastructure.
     *
     * This centralizes the trust source configuration so it can be shared across all
     * trust verification areas (issuer trust, status list trust, reader authentication).
     * The core builds the underlying trust pipeline internally from the provided parameters.
     *
     * When this is configured, [configureIssuerTrust], [configureDocumentStatusResolver],
     * and [configureReaderTrustStore] no longer require explicit `trustSource()` and
     * `classifications()` calls — they default to the central ETSI trust source.
     *
     * Issuer trust and reader authentication use this trust source by default; status list
     * trust is enabled by calling [configureDocumentStatusResolver].
     *
     * Example:
     * ```
     * configureEtsiTrust {
     *     loteLocations(SupportedLists(
     *         pidProviders = Uri("https://trustedlist.../PIDProviders.jwt"),
     *         wrpacProviders = Uri("https://trustedlist.../WRPACProviders.jwt"),
     *         wrprcProviders = Uri("https://trustedlist.../WRPRCProviders.jwt"),
     *     ))
     *     classifications(AttestationClassifications(
     *         pids = AttestationIdentifierPredicate.any(setOf(
     *             AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1"),
     *         )),
     *     ))
     *     loteSignerTrustAnchors(loteSignerCertificate)
     *     // Optional:
     *     relaxCertificateProfiles()
     * }
     * configureIssuerTrust {
     *     policy { default(TrustPolicy.Action.INFORM) }
     * }
     * configureDocumentStatusResolver { }
     * configureReaderTrustStore()
     * ```
     *
     * @param block configuration block applied to the [EtsiTrustConfigBuilder]
     * @return the [EudiWalletConfig] instance
     * @see EtsiTrustConfigBuilder
     */
    fun configureEtsiTrust(
        block: EtsiTrustConfigBuilder.() -> Unit,
    ) = apply {
        this.etsiTrustConfig = EtsiTrustConfigBuilder().apply(block).build()
    }

    /**
     * Validates configuration that spans more than one section. OpenID4VP over the Digital
     * Credential API reuses the wallet's OpenID4VP configuration, so enabling an OpenID4VP protocol
     * in [DCAPIConfig.supportedProtocols] requires [openId4VpConfig] to be set.
     */
    private fun validateDcApiConfig() {
        val dcapi = dcapiConfig?.takeIf { it.enabled } ?: return
        require(openId4VpConfig != null || dcapi.supportedProtocols.none { it.isOpenId4Vp }) {
            "DCAPIConfig.supportedProtocols includes an OpenID4VP protocol but " +
                "EudiWalletConfig.openId4VpConfig is not set — configure OpenID4VP, or remove " +
                "the OpenID4VP protocols from DCAPIConfig.supportedProtocols."
        }
    }

    companion object {

        const val DEFAULT_DOCUMENT_MANAGER_IDENTIFIER = "EudiWalletDocumentManager"

        /**
         * Create a new EudiWalletConfig instance.
         * @param configure the configuration lambda
         * @return the EudiWalletConfig instance
         */
        operator fun invoke(configure: EudiWalletConfig.() -> Unit): EudiWalletConfig =
            EudiWalletConfig().apply(configure).also {
                it.validateDcApiConfig()
            }
    }
}

