/*
 * Copyright (c) 2023-2025 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import org.multipaz.crypto.Algorithm
import java.net.URI

/**
 * Configuration for OpenID4VP (OpenID for Verifiable Presentations) transfer operations.
 *
 * This class provides comprehensive configuration options for OpenID4VP protocol implementation,
 * including support for various client identification schemes, encryption algorithms, credential formats,
 * and protocol schemes. It follows the OpenID4VP specification for secure verifiable credential presentations.
 *
 * ## Example Usage:
 * ```kotlin
 * val config = OpenId4VpConfig.Builder()
 *    .withClientIdSchemes(
 *          listOf(
 *              ClientIdScheme.Preregistered(
 *                  listOf(
 *                      PreregisteredVerifier(
 *                      "VerifierClientId",
 *                      "VerifierLegalName",
 *                      "http://example.com")
 *                  )),
 *              ClientIdScheme.X509SanDns,
 *              ClientIdScheme.X509Hash,
 *              ClientIdScheme.RedirectUri
 *          )
 *     )
 *     .withSchemes(
 *         listOf(
 *             "eudi-openid4vp",
 *             "mdoc-openid4vp"
 *         )
 *     )
 *    .withEncryptionAlgorithms(EncryptionAlgorithm.SUPPORTED_ENCRYPTION_ALGORITHMS) //optional; if not set, all supported algorithms will be used
 *    .withEncryptionMethods(EncryptionMethod.SUPPORTED_ENCRYPTION_METHODS) //optional; if not set, all supported methods will be used
 *    .withFormats(Format.MsoMdoc.ES256, Format.SdJwtVc.ES256)
 *    .build()
 * ```
 *
 * @property clientIdSchemes List of supported client identifier schemes that define how verifiers authenticate themselves
 * @property encryptionAlgorithms List of supported encryption algorithms for securing communication channels
 * @property encryptionMethods List of supported encryption methods for content encryption
 * @property schemes URI schemes supported for OpenID4VP requests (default: "mdoc-openid4vp")
 * @property formats Supported credential formats (mDL/mDoc, SD-JWT VC, etc.)
 *
 * @see ClientIdScheme
 * @see EncryptionAlgorithm
 * @see EncryptionMethod
 * @see Format
 * @since 1.0.0
 */
class OpenId4VpConfig private constructor(private val builder: Builder) {

    val clientIdSchemes: List<ClientIdScheme>
        get() = builder.clientIdSchemes

    val encryptionAlgorithms: List<EncryptionAlgorithm>
        get() = builder.encryptionAlgorithms

    val encryptionMethods: List<EncryptionMethod>
        get() = builder.encryptionMethods

    val schemes: List<String>
        get() = builder.schemes

    val formats: List<Format> = builder.formats

    /**
     * Builder for constructing [OpenId4VpConfig] instances with validation and sensible defaults.
     *
     * This builder implements a fluent API pattern and provides comprehensive validation to ensure
     * the resulting configuration is valid and consistent. It enforces business rules such as
     * allowing only one instance of certain client ID schemes and ensuring required fields are populated.
     *
     * ## Default Values:
     * - **Encryption Algorithms**: All supported algorithms from [EncryptionAlgorithm.SUPPORTED_ENCRYPTION_ALGORITHMS]
     * - **Encryption Methods**: All supported methods from [EncryptionMethod.SUPPORTED_ENCRYPTION_METHODS]
     * - **Schemes**: ["mdoc-openid4vp"]
     *
     * ## Validation Rules:
     * - Client ID schemes list cannot be empty and must be initialized
     * - Maximum one [ClientIdScheme.Preregistered] instance allowed
     * - Maximum one [ClientIdScheme.X509SanDns] instance allowed
     * - Schemes list cannot be empty
     * - Encryption algorithms and methods lists cannot be empty
     * - Formats list cannot be empty and cannot contain duplicate format types
     * - SD-JWT VC formats must have non-empty algorithm lists
     *
     * @property clientIdSchemes List of supported client identifier schemes (must be initialized)
     * @property encryptionAlgorithms List of supported encryption algorithms (defaults to all supported)
     * @property encryptionMethods List of supported encryption methods (defaults to all supported)
     * @property schemes List of URI schemes for OpenID4VP (defaults to "mdoc-openid4vp")
     * @property formats List of supported credential formats (must be initialized)
     */
    class Builder {
        lateinit var clientIdSchemes: List<ClientIdScheme>
            private set

        /**
         * Sets the supported client identifier schemes.
         *
         * @param clientIdSchemes list of [ClientIdScheme]
         */
        fun withClientIdSchemes(clientIdSchemes: List<ClientIdScheme>) =
            apply { this.clientIdSchemes = clientIdSchemes }

        /**
         * Sets the supported client identifier schemes.
         *
         * @param clientIdSchemes variable argument of [ClientIdScheme]
         */
        fun withClientIdSchemes(vararg clientIdSchemes: ClientIdScheme) =
            apply { this.clientIdSchemes = clientIdSchemes.toList() }

        var encryptionAlgorithms: List<EncryptionAlgorithm> =
            EncryptionAlgorithm.SUPPORTED_ENCRYPTION_ALGORITHMS
            private set

        /**
         * Sets the list of supported encryption algorithms.
         *
         * @param encryptionAlgorithms list of [EncryptionAlgorithm]
         */
        fun withEncryptionAlgorithms(encryptionAlgorithms: List<EncryptionAlgorithm>) =
            apply { this.encryptionAlgorithms = encryptionAlgorithms }

        /**
         * Sets the list of supported encryption algorithms.
         *
         * @param encryptionAlgorithms variable argument of [EncryptionAlgorithm]
         */
        fun withEncryptionAlgorithms(vararg encryptionAlgorithms: EncryptionAlgorithm) =
            withEncryptionAlgorithms(encryptionAlgorithms.toList())

        var encryptionMethods: List<EncryptionMethod> =
            EncryptionMethod.SUPPORTED_ENCRYPTION_METHODS
            private set

        /**
         * Sets the list of supported encryption methods.
         *
         * @param encryptionMethods list of [EncryptionMethod]
         */
        fun withEncryptionMethods(encryptionMethods: List<EncryptionMethod>) =
            apply { this.encryptionMethods = encryptionMethods }

        /**
         * Sets the list of supported encryption methods.
         *
         * @param encryptionMethods variable argument of [EncryptionMethod]
         */
        fun withEncryptionMethods(vararg encryptionMethods: EncryptionMethod) =
            withEncryptionMethods(encryptionMethods.toList())

        var schemes: List<String> = listOf("mdoc-openid4vp")
            private set

        /**
         * Sets a list of schemes for OpenID4VP.
         * By default, the scheme "mdoc-openid4vp" is supported.
         *
         * @param schemes the list of schemes
         */
        fun withSchemes(schemes: List<String>) = apply {
            this.schemes = schemes
        }

        /**
         * Sets a list of schemes for OpenID4VP.
         *
         * @param schemes variables argument of schemes
         */
        fun withSchemes(vararg schemes: String) = withSchemes(schemes.toList())


        lateinit var formats: List<Format>
            private set

        /**
         * Sets the supported credential formats for the OpenID4VP.
         *
         * @param formats list of [Format]
         */
        fun withFormats(formats: List<Format>) = apply {
            this.formats = formats
        }

        /**
         * Sets the supported credential formats for the OpenID4VP.
         *
         * @param formats variable argument of [Format]
         */
        fun withFormats(vararg formats: Format) =
            withFormats(formats.toList())

        /**
         * Builds the [OpenId4VpConfig].
         * @return the [OpenId4VpConfig]
         */
        fun build(): OpenId4VpConfig {

            require(this::clientIdSchemes.isInitialized && clientIdSchemes.isNotEmpty()) { "OpenId4VpConfig: clientIdSchemes must be initialized with a not empty list" }
            require(this.clientIdSchemes.count { it is ClientIdScheme.Preregistered } <= 1) { "OpenId4VpConfig: clientIdSchemes must contain maximum one Preregistered item" }
            require(this.clientIdSchemes.count { it is ClientIdScheme.X509SanDns } <= 1) { "OpenId4VpConfig: clientIdSchemes must contain maximum one X509SanDns item" }

            require(this.schemes.isNotEmpty()) { "OpenId4VpConfig: schemes must be initialized with a not empty list" }
            require(encryptionAlgorithms.isNotEmpty()) { "OpenId4VpConfig: encryptionAlgorithms must be initialized with a not empty list" }
            require(encryptionMethods.isNotEmpty()) { "OpenId4VpConfig: encryptionMethods must be initialized with a not empty list" }

            require(this.formats.isNotEmpty()) { "OpenId4VpConfig: formats must be initialized with a not empty list" }
            this.formats.groupBy { it.toString() }
                .forEach { (format, instances) ->
                    require(instances.size == 1) {
                        "OpenId4VpConfig: Multiple instances ${instances.size} found for $format."
                    }
                }
            this.formats.forEach {
                if (it is Format.SdJwtVc) {
                    require(it.sdJwtAlgorithms.isNotEmpty()) { "OpenId4VpConfig: sdJwtAlgorithms in SdJwtVc Format must be initialized with a not empty list" }
                }
            }

            return OpenId4VpConfig(this)
        }
    }
}

/**
 * Sealed interface defining the supported client identifier schemes for OpenID4VP verifier authentication.
 *
 * Client identifier schemes determine how verifiers authenticate themselves to the wallet during
 * the OpenID4VP flow. Each scheme provides different levels of trust and verification mechanisms,
 * from pre-registered trusted verifiers to certificate-based authentication.
 *
 * ## Supported Schemes:
 * - **[Preregistered]**: Verifiers known and trusted in advance by the wallet
 * - **[X509SanDns]**: Certificate-based authentication using DNS Subject Alternative Names
 * - **[X509Hash]**: Certificate-based authentication using certificate hash verification
 * - **[RedirectUri]**: Authentication based on redirect URI validation
 *
 * @see OpenId4VpConfig
 * @since 1.0.0
 */
sealed interface ClientIdScheme {

    /**
     * Client identifier scheme for pre-registered verifiers that are known and trusted by the wallet.
     *
     * This scheme allows wallets to maintain a list of trusted verifiers that have been vetted
     * and approved in advance. It provides the highest level of trust as verifiers are explicitly
     * whitelisted with their credentials and metadata.
     *
     * @property preregisteredVerifiers List of pre-approved verifier configurations with their
     *   client IDs, legal names, API endpoints, and cryptographic parameters
     * @see PreregisteredVerifier
     */
    data class Preregistered(var preregisteredVerifiers: List<PreregisteredVerifier>) :
        ClientIdScheme

    /**
     * Client identifier scheme using X.509 certificate validation with DNS Subject Alternative Names.
     *
     * This scheme validates verifiers using X.509 certificates where the client identifier
     * matches a DNS name in the certificate's Subject Alternative Name (SAN) extension.
     * Provides strong cryptographic authentication based on PKI infrastructure.
     */
    data object X509SanDns : ClientIdScheme

    /**
     * Client identifier scheme using X.509 certificate hash validation.
     *
     * This scheme validates verifiers by comparing the hash of their X.509 certificate
     * against expected values. Provides certificate-based authentication with additional
     * integrity verification through hash comparison.
     */
    data object X509Hash : ClientIdScheme

    /**
     * Client identifier scheme using redirect URI validation.
     *
     * This scheme validates verifiers based on their registered redirect URIs.
     * The client identifier must match or be associated with a valid redirect URI
     * that the verifier is authorized to use.
     */
    data object RedirectUri : ClientIdScheme
}

/**
 * Preregistered verifier for the [ClientIdScheme.Preregistered] client identifier scheme.
 * @property clientId the client identifier
 * @property legalName the legal name of the client
 * @property verifierApi the verifier API
 * @property jwsAlgorithm the JWS algorithm. Default is [Algorithm.ESP256]
 * @property jwkSetSource the JWK set source. Default is the [verifierApi] with the path "/wallet/public-keys.json"
 */
data class PreregisteredVerifier(
    var clientId: ClientId,
    var legalName: LegalName,
    var verifierApi: VerifierApi,
    var jwsAlgorithm: Algorithm = Algorithm.ESP256,
    var jwkSetSource: URI = URI("$verifierApi/wallet/public-keys.json"),
) {
    init {
        requireNotNull(jwsAlgorithm.joseAlgorithmIdentifier) {
            "JWS algorithm $jwsAlgorithm does not have a JOSE algorithm identifier"
        }
    }
}

/**
 * Type alias for client identifier strings used in OpenID4VP authentication.
 *
 * Represents the unique identifier for a verifier client in the OpenID4VP ecosystem.
 * This identifier is used to distinguish different verifier applications and services.
 */
typealias ClientId = String

/**
 * Type alias for legal name strings of verifier organizations.
 *
 * Represents the official legal name of the organization or entity operating
 * the verifier service, used for display and trust establishment purposes.
 */
typealias LegalName = String

/**
 * Type alias for verifier API endpoint URLs.
 *
 * Represents the base URL of the verifier's API endpoint used for
 * OpenID4VP communication and credential verification operations.
 */
typealias VerifierApi = String

/**
 * Sealed interface defining the supported credential formats for OpenID4VP presentations.
 *
 * This interface encapsulates the different types of verifiable credentials that can be
 * presented through the OpenID4VP protocol. Each format defines specific requirements
 * and capabilities for credential presentation and verification.
 *
 * ## Supported Formats:
 * - **[SdJwtVc]**: Selective Disclosure JWT Verifiable Credentials with configurable algorithms
 * - **[MsoMdoc]**: Mobile Security Object documents (ISO 18013-5 mDL format)
 *
 * @see OpenId4VpConfig.formats
 * @since 1.0.0
 */
sealed interface Format {

    /**
     * Selective Disclosure JWT Verifiable Credential format configuration.
     *
     * This format supports SD-JWT (Selective Disclosure JSON Web Token) verifiable credentials,
     * which allow holders to selectively disclose specific claims from their credentials
     * while maintaining cryptographic integrity. The format requires configuration of
     * algorithms for both the SD-JWT itself and optional Key Binding JWTs.
     *
     * @property sdJwtAlgorithms List of algorithms supported for SD-JWT signature and verification
     * @property kbJwtAlgorithms List of algorithms supported for Key Binding JWT operations
     *
     * @see Algorithm
     */
    data class SdJwtVc(
        val sdJwtAlgorithms: List<Algorithm>,
        val kbJwtAlgorithms: List<Algorithm>,
    ) : Format {

        init {
            for (algorithm in sdJwtAlgorithms) {
                requireNotNull(algorithm.joseAlgorithmIdentifier) {
                    "SdJwtAlgorithm $algorithm does not have a JOSE algorithm identifier"
                }
            }
            for (algorithm in kbJwtAlgorithms) {
                requireNotNull(algorithm.joseAlgorithmIdentifier) {
                    "KbJwtAlgorithm $algorithm does not have a JOSE algorithm identifier"
                }
            }
        }

        companion object {
            /**
             * Pre-configured SD-JWT VC format using ES256 algorithm for both SD-JWT and Key Binding.
             *
             * This is a convenient default configuration that uses the ES256 (ECDSA using P-256 curve
             * and SHA-256 hash) algorithm, which provides a good balance of security and performance
             * for most OpenID4VP use cases.
             */
            val ES256 = SdJwtVc(listOf(Algorithm.ESP256), listOf(Algorithm.ESP256))
        }
    }

    /**
     * Mobile Security Object document format (ISO 18013-5 mDL).
     *
     * This format represents ISO 18013-5 mobile driving license documents and similar
     * mobile security objects. It provides a standardized way to present identity
     * credentials in mobile environments with strong cryptographic security.
     */
    data class MsoMdoc(
        val issuerAuthAlgorithms: List<Algorithm>,
        val deviceAuthAlgorithms: List<Algorithm>,
    ) : Format {

        init {
            for (algorithm in issuerAuthAlgorithms) {
                requireNotNull(algorithm.coseAlgorithmIdentifier) {
                    "IssuerAuthAlgorithm $algorithm does not have a COSE algorithm identifier"
                }
            }

            for (algorithm in deviceAuthAlgorithms) {
                requireNotNull(algorithm.coseAlgorithmIdentifier) {
                    "DeviceAuthAlgorithm $algorithm does not have a COSE algorithm identifier"
                }
            }
        }

        companion object {
            val ES256 = MsoMdoc(
                issuerAuthAlgorithms = listOf(Algorithm.ES256, Algorithm.ESP256),
                deviceAuthAlgorithms = listOf(Algorithm.ES256, Algorithm.ESP256)
            )
        }
    }
}

/**
 * Encryption algorithm enumeration for OpenID4VP key agreement and key wrapping operations.
 *
 * This enum defines the supported key agreement algorithms used for encrypting OpenID4VP
 * communications. All algorithms are based on Elliptic Curve Diffie-Hellman Ephemeral Static
 * (ECDH-ES) key agreement, with variations in key derivation and wrapping mechanisms.
 *
 * ## Algorithm Details:
 * - **ECDH_ES**: Direct key agreement without key wrapping
 * - **ECDH_ES_A128KW**: Key agreement with AES-128 key wrapping
 * - **ECDH_ES_A192KW**: Key agreement with AES-192 key wrapping
 * - **ECDH_ES_A256KW**: Key agreement with AES-256 key wrapping
 *
 * All algorithms provide forward secrecy through ephemeral key generation and are suitable
 * for protecting sensitive credential data during OpenID4VP exchanges.
 *
 * @see OpenId4VpConfig.encryptionAlgorithms
 * @since 1.0.0
 */
enum class EncryptionAlgorithm {
    /** Elliptic Curve Diffie-Hellman Ephemeral Static key agreement */
    ECDH_ES,

    /** ECDH-ES with AES-128 Key Wrap algorithm */
    ECDH_ES_A128KW,

    /** ECDH-ES with AES-192 Key Wrap algorithm */
    ECDH_ES_A192KW,

    /** ECDH-ES with AES-256 Key Wrap algorithm */
    ECDH_ES_A256KW;

    companion object {
        /**
         * List of all supported encryption algorithms available for OpenID4VP configuration.
         *
         * This property provides convenient access to all enum entries, useful for
         * default configurations or when all algorithms should be supported.
         */
        val SUPPORTED_ENCRYPTION_ALGORITHMS = entries
    }
}

/**
 * Encryption method enumeration for OpenID4VP content encryption operations.
 *
 * This enum defines the supported symmetric encryption methods used for protecting
 * the actual content (payload) in OpenID4VP communications. The methods include
 * both authenticated encryption (AEAD) modes and traditional cipher modes with
 * separate authentication.
 *
 * ## Method Categories:
 * - **AES-CBC + HMAC**: Combined cipher and MAC modes (A128CBC_HS256, A192CBC_HS384, A256CBC_HS512)
 * - **AES-GCM**: Authenticated encryption modes (A128GCM, A192GCM, A256GCM)
 * - **Deprecated**: Legacy methods for backward compatibility
 * - **XC20P**: ChaCha20-Poly1305 authenticated encryption
 *
 * ## Security Recommendations:
 * - Prefer GCM modes for new implementations due to better performance and security
 * - Avoid deprecated methods unless required for backward compatibility
 * - XC20P provides good security with different performance characteristics than AES
 *
 * @see OpenId4VpConfig.encryptionMethods
 * @since 1.0.0
 */
enum class EncryptionMethod {
    /** AES-128 in CBC mode with HMAC-SHA256 authentication */
    A128CBC_HS256,

    /** AES-192 in CBC mode with HMAC-SHA384 authentication */
    A192CBC_HS384,

    /** AES-256 in CBC mode with HMAC-SHA512 authentication */
    A256CBC_HS512,

    /** AES-128 in Galois/Counter Mode (authenticated encryption) */
    A128GCM,

    /** AES-192 in Galois/Counter Mode (authenticated encryption) */
    A192GCM,

    /** AES-256 in Galois/Counter Mode (authenticated encryption) */
    A256GCM,

    /**
     * AES-128 in CBC mode with HMAC-SHA256 authentication (deprecated).
     * @deprecated Use A128CBC_HS256 instead
     */
    A128CBC_HS256_DEPRECATED,

    /**
     * AES-256 in CBC mode with HMAC-SHA512 authentication (deprecated).
     * @deprecated Use A256CBC_HS512 instead
     */
    A256CBC_HS512_DEPRECATED,

    /** ChaCha20-Poly1305 authenticated encryption */
    XC20P;

    companion object {
        /**
         * List of all supported encryption methods available for OpenID4VP configuration.
         *
         * This property provides convenient access to all enum entries, including
         * deprecated methods. When configuring for production use, consider filtering
         * out deprecated methods unless backward compatibility is required.
         */
        val SUPPORTED_ENCRYPTION_METHODS = entries
    }
}