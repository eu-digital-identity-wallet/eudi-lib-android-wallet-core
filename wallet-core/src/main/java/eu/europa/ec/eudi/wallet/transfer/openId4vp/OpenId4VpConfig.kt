/*
 * Copyright (c) 2023-2024 European Commission
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

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vp.DefaultHttpClientFactory
import eu.europa.ec.eudi.openid4vp.JwkSetSource
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
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
 *    .build()
 * ```
 *
 * @property clientIdSchemes list of [ClientIdScheme] that defines the supported Client Identifier schemes
 * @property encryptionAlgorithms list of [EncryptionAlgorithm] that defines the supported encryption algorithms
 * @property encryptionMethods list of [EncryptionMethod] that defines the supported encryption methods
 * @property schemes optionally you can set one or more schemes. By default, the scheme "mdoc-openid4vp" is used
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

    /**
     * Builder for [OpenId4VciConfig].
     *
     * @property clientIdSchemes list of [ClientIdScheme] that defines the supported Client Identifier schemes
     * @property encryptionAlgorithms list of [EncryptionAlgorithm] that defines the supported encryption algorithms
     * @property encryptionMethods list of [EncryptionMethod] that defines the supported encryption methods
     * @property schemes for OpenId4Vp. Optionally, you can set one or more schemes. By default, "mdoc-openid4vp" is used.
     * @property logLevel the debug logging level. By default, [LogLevel.OFF] is used
     * @property ktorHttpClientFactory the HttpClient factory for the OpenId4Vp. By default, [DefaultHttpClientFactory] is used
     */
    class Builder {
        lateinit var clientIdSchemes: List<ClientIdScheme>
            private set

        /**
         * Sets the issuer url.
         *
         * @param issuerUrl the issuer url
         */
        fun withClientIdSchemes(clientIdSchemes: List<ClientIdScheme>) =
            apply { this.clientIdSchemes = clientIdSchemes }

        /**
         *
         */
        fun withClientIdSchemes(vararg clientIdSchemes: ClientIdScheme) =
            apply { this.clientIdSchemes = clientIdSchemes.toList() }

        lateinit var encryptionAlgorithms: List<EncryptionAlgorithm>
            private set

        /**
         * Sets the issuer url.
         *
         * @param issuerUrl the issuer url
         */
        fun withEncryptionAlgorithms(encryptionAlgorithms: List<EncryptionAlgorithm>) =
            apply { this.encryptionAlgorithms = encryptionAlgorithms }

        fun withEncryptionAlgorithms(vararg encryptionAlgorithms: EncryptionAlgorithm) =
            withEncryptionAlgorithms(encryptionAlgorithms.toList())

        lateinit var encryptionMethods: List<EncryptionMethod>
            private set

        /**
         * Sets the issuer url.
         *
         * @param issuerUrl the issuer url
         */
        fun withEncryptionMethods(encryptionMethods: List<EncryptionMethod>) =
            apply { this.encryptionMethods = encryptionMethods }

        /**
         *
         */
        fun withEncryptionMethods(vararg encryptionMethods: EncryptionMethod) =
            withEncryptionMethods(encryptionMethods.toList())

        var schemes: List<String> = listOf("mdoc-openid4vp")
            private set

        /**
         * Sets a list of schemes for openId4Vp.
         * By default, the scheme "mdoc-openid4vp" is supported
         *
         * @param schemes the list of schemes
         */
        fun withSchemes(schemes: List<String>) = apply {
            this.schemes = schemes
        }

        /**
         * Sets a list of schemes for openId4Vp.
         */
        fun withSchemes(vararg schemes: String) = withSchemes(schemes.toList())


        /**
         * Builds the [OpenId4VpConfig].
         * @return the [OpenId4VpConfig]
         */
        fun build(): OpenId4VpConfig {

            require(this::clientIdSchemes.isInitialized && clientIdSchemes.isNotEmpty()) { "OpenId4VpConfig: clientIdSchemes must be initialized with a not empty list" }
            require(this.clientIdSchemes.count { it is ClientIdScheme.Preregistered } <= 1) { "OpenId4VpConfig: clientIdSchemes must contain maximum one Preregistered item" }
            require(this.clientIdSchemes.count { it is ClientIdScheme.X509SanDns } <= 1) { "OpenId4VpConfig: clientIdSchemes must contain maximum one X509SanDns item" }
            require(this.clientIdSchemes.count { it is ClientIdScheme.X509SanUri } <= 1) { "OpenId4VpConfig: clientIdSchemes must contain maximum one X509SanUri item" }

            require(this.schemes.isNotEmpty()) { "OpenId4VpConfig: schemes must be initialized with a not empty list" }

            require(this::encryptionAlgorithms.isInitialized && encryptionAlgorithms.isNotEmpty()) { "OpenId4VpConfig: encryptionAlgorithms must be initialized with a not empty list" }
            require(this::encryptionMethods.isInitialized && encryptionMethods.isNotEmpty()) { "OpenId4VpConfig: encryptionMethods must be initialized with a not empty list" }

            return OpenId4VpConfig(this)
        }
    }
}

sealed interface ClientIdScheme {
    data class Preregistered(var preregisteredVerifiers: List<PreregisteredVerifier>) :
        ClientIdScheme

    data object X509SanDns : ClientIdScheme

    data object X509SanUri : ClientIdScheme
}

data class PreregisteredVerifier(
    var clientId: ClientId,
    var legalName: LegalName,
    var verifierApi: VerifierApi,
    var jwsAlgorithm: JWSAlgorithm = JWSAlgorithm.RS256,
    var jwkSetSource: JwkSetSource = JwkSetSource.ByReference(URI("$verifierApi/wallet/public-keys.json"))
)

typealias ClientId = String
typealias LegalName = String
typealias VerifierApi = String

sealed interface EncryptionAlgorithm {

    val name: String

    data object ECDH_ES : EncryptionAlgorithm {
        override val name: String = "ECDH-ES"
    }

    data object ECDH_ES_A128KW : EncryptionAlgorithm {
        override val name: String = "ECDH-ES+A128KW"
    }

    data object ECDH_ES_A192KW : EncryptionAlgorithm {
        override val name: String = "ECDH-ES+A192KW"
    }

    data object ECDH_ES_A256KW : EncryptionAlgorithm {
        override val name: String = "ECDH-ES+A256KW"
    }

    data object ECDH_1PU : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU"
    }

    data object ECDH_1PU_A128KW : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU+A128KW"
    }

    data object ECDH_1PU_A192KW : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU+A192KW"
    }

    data object ECDH_1PU_A256KW : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU+A256KW"
    }
}

sealed interface EncryptionMethod {

    val name: String

    data object A128CBC_HS256 : EncryptionMethod {
        override val name: String = "A128CBC-HS256"
    }

    data object A192CBC_HS384 : EncryptionMethod {
        override val name: String = "A192CBC-HS384"
    }

    data object A256CBC_HS512 : EncryptionMethod {
        override val name: String = "A256CBC-HS512"
    }

    data object A128GCM : EncryptionMethod {
        override val name: String = "A128GCM"
    }

    data object A192GCM : EncryptionMethod {
        override val name: String = "A192GCM"
    }

    data object A256GCM : EncryptionMethod {
        override val name: String = "A256GCM"
    }
}