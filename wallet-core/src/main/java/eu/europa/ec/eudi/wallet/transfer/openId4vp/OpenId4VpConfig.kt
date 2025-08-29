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

import io.ktor.client.plugins.logging.LogLevel
import org.multipaz.crypto.Algorithm
import java.net.URI

/**
 * Configuration for the OpenId4Vp transfer.
 *
 * Use [Builder] to create an instance.
 *
 * Example usage:
 *
 * ```
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
 *              ClientIdScheme.X509SanDns
 *          )
 *     )
 *     .withScheme(
 *         listOf(
 *             "eudi-openid4vp",
 *             "mdoc-openid4vp"
 *         )
 *     )
 *    .withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
 *    .withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
 *    .withFormats(Format.MsoMdoc, Format.SdJwtVc.ES256)
 *    .build()
 * ```
 *
 * @property clientIdSchemes list of [ClientIdScheme] that defines the supported Client Identifier schemes
 * @property encryptionAlgorithms list of [EncryptionAlgorithm] that defines the supported encryption algorithms
 * @property encryptionMethods list of [EncryptionMethod] that defines the supported encryption methods
 * @property schemes optionally you can set one or more schemes. By default, the scheme "mdoc-openid4vp" is used
 * @property formats the supported credential formats
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
     * Builder for [OpenId4VciConfig].
     *
     * @property clientIdSchemes list of [ClientIdScheme] that defines the supported Client Identifier schemes
     * @property encryptionAlgorithms list of [EncryptionAlgorithm] that defines the supported encryption algorithms
     * @property encryptionMethods list of [EncryptionMethod] that defines the supported encryption methods
     * @property schemes for OpenId4Vp. Optionally, you can set one or more schemes. By default, "mdoc-openid4vp" is used.
     * @property logLevel the debug logging level. By default, [LogLevel.OFF] is used
     * @property ktorHttpClientFactory the HttpClient factory for the OpenId4Vp. By default, [DefaultHttpClientFactory] is used
     * @property formats the supported credential formats
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

sealed interface ClientIdScheme {
    data class Preregistered(var preregisteredVerifiers: List<PreregisteredVerifier>) :
        ClientIdScheme

    data object X509SanDns : ClientIdScheme

    data object RedirectUri : ClientIdScheme
}

/**
 * Preregistered verifier for the [ClientIdScheme.Preregistered] client identifier scheme.
 * @property clientId the client identifier
 * @property legalName the legal name of the client
 * @property verifierApi the verifier API
 * @property jwsAlgorithm the JWS algorithm. Default is [JwsAlgorithm.ES256]
 * @property jwkSetSource the JWK set source. Default is the [verifierApi] with the path "/wallet/public-keys.json"
 */
data class PreregisteredVerifier(
    var clientId: ClientId,
    var legalName: LegalName,
    var verifierApi: VerifierApi,
    var jwsAlgorithm: JwsAlgorithm = JwsAlgorithm.ES256,
    var jwkSetSource: URI = URI("$verifierApi/wallet/public-keys.json"),
)

typealias ClientId = String
typealias LegalName = String
typealias VerifierApi = String

sealed interface Format {

    data class SdJwtVc(
        val sdJwtAlgorithms: List<Algorithm>,
        val kbJwtAlgorithms: List<Algorithm>,
    ) : Format {
        companion object {
            val ES256 = SdJwtVc(listOf(Algorithm.ESP256), listOf(Algorithm.ESP256))
        }
    }

    data object MsoMdoc : Format
}

/**
 * Algorithm enumeration for [PreregisteredVerifier.jwsAlgorithm]
 * @see PreregisteredVerifier
 */
enum class JwsAlgorithm {
    HS256,
    HS384,
    HS512,
    RS256,
    RS384,
    RS512,
    ES256,
    ES256K,
    ES384,
    ES512,
    PS256,
    PS384,
    PS512,
    EdDSA,
    Ed25519,
    Ed448;
}

enum class EncryptionAlgorithm {
    ECDH_ES,
    ECDH_ES_A128KW,
    ECDH_ES_A192KW,
    ECDH_ES_A256KW;

    companion object {
        val SUPPORTED_ENCRYPTION_ALGORITHMS = entries
    }
}

enum class EncryptionMethod {
    A128CBC_HS256,
    A192CBC_HS384,
    A256CBC_HS512,
    A128GCM,
    A192GCM,
    A256GCM,
    A128CBC_HS256_DEPRECATED,
    A256CBC_HS512_DEPRECATED,
    XC20P;

    companion object {
        val SUPPORTED_ENCRYPTION_METHODS = entries
    }
}