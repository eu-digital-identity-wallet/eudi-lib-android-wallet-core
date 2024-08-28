/*
 *  Copyright (c) 2023-2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet.transfer.openid4vp

import eu.europa.ec.eudi.openid4vp.DefaultHttpClientFactory
import eu.europa.ec.eudi.wallet.transfer.openid4vp.OpenId4VpConfig.Builder
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config as OpenId4VciConfig

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

        lateinit var encryptionAlgorithms: List<EncryptionAlgorithm>
            private set

        /**
         * Sets the issuer url.
         *
         * @param issuerUrl the issuer url
         */
        fun withEncryptionAlgorithms(encryptionAlgorithms: List<EncryptionAlgorithm>) =
            apply { this.encryptionAlgorithms = encryptionAlgorithms }

        lateinit var encryptionMethods: List<EncryptionMethod>
            private set

        /**
         * Sets the issuer url.
         *
         * @param issuerUrl the issuer url
         */
        fun withEncryptionMethods(encryptionMethods: List<EncryptionMethod>) =
            apply { this.encryptionMethods = encryptionMethods }

        var schemes: List<String> = listOf("mdoc-openid4vp")
            private set

        /**
         * Sets the scheme for openId4Vp.
         * By default, the scheme "mdoc-openid4vp" is supported
         *
         * @param scheme the scheme
         */
        fun withScheme(scheme: String) = apply {
            this.schemes = listOf(scheme)
        }

        /**
         * Sets a list of schemes for openId4Vp.
         * By default, the scheme "mdoc-openid4vp" is supported
         *
         * @param schemes the list of schemes
         */
        fun withScheme(schemes: List<String>) = apply {
            this.schemes = schemes
        }


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
    data class Preregistered(var preregisteredVerifiers: List<PreregisteredVerifier>) : ClientIdScheme

    object X509SanDns : ClientIdScheme

    object X509SanUri : ClientIdScheme
}

data class PreregisteredVerifier(
    var clientId: ClientId,
    var legalName: LegalName,
    var verifierApi: VerifierApi,
)

typealias ClientId = String
typealias LegalName = String
typealias VerifierApi = String

sealed interface EncryptionAlgorithm {

    val name: String

    object ECDH_ES : EncryptionAlgorithm {
        override val name: String = "ECDH-ES"
    }

    object ECDH_ES_A128KW : EncryptionAlgorithm {
        override val name: String = "ECDH-ES+A128KW"
    }

    object ECDH_ES_A192KW : EncryptionAlgorithm {
        override val name: String = "ECDH-ES+A192KW"
    }

    object ECDH_ES_A256KW : EncryptionAlgorithm {
        override val name: String = "ECDH-ES+A256KW"
    }

    object ECDH_1PU : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU"
    }

    object ECDH_1PU_A128KW : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU+A128KW"
    }

    object ECDH_1PU_A192KW : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU+A192KW"
    }

    object ECDH_1PU_A256KW : EncryptionAlgorithm {
        override val name: String = "ECDH-1PU+A256KW"
    }
}

sealed interface EncryptionMethod {

    val name: String

    object A128CBC_HS256 : EncryptionMethod {
        override val name: String = "A128CBC-HS256"
    }

    object A192CBC_HS384 : EncryptionMethod {
        override val name: String = "A192CBC-HS384"
    }

    object A256CBC_HS512 : EncryptionMethod {
        override val name: String = "A256CBC-HS512"
    }

    object A128GCM : EncryptionMethod {
        override val name: String = "A128GCM"
    }

    object A192GCM : EncryptionMethod {
        override val name: String = "A192GCM"
    }

    object A256GCM : EncryptionMethod {
        override val name: String = "A256GCM"
    }
}