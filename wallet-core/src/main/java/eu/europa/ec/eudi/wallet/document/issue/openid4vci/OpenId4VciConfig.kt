/*
 *  Copyright (c) 2023 European Commission
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

package eu.europa.ec.eudi.wallet.document.issue.openid4vci

/**
 * Configuration for the OpenID4VCI issuer.
 *
 * Use [Builder] to create an instance.
 *
 * Example usage:
 *
 * ```
 * val config = OpenId4VciConfig.Builder()
 *    .withIssuerUrl("https://example.issuer.com")
 *    .withClientId("client-id")
 *    .build()
 *
 * ```
 *
 * @property issuerUrl the issuer url
 * @property clientId the client id
 */
class OpenId4VciConfig private constructor(private val builder: Builder) {
    val issuerUrl: String
        get() = builder.issuerUrl
    val clientId: String
        get() = builder.clientId

    /**
     * Builder for [OpenId4VciConfig].
     *
     * @property issuerUrl the issuer url
     * @property clientId the client id
     */
    class Builder {
        lateinit var issuerUrl: String
            private set
        lateinit var clientId: String
            private set

        /**
         * Sets the issuer url.
         *
         * @param issuerUrl the issuer url
         */
        fun withIssuerUrl(issuerUrl: String) = apply { this.issuerUrl = issuerUrl }

        /**
         * Sets the client id.
         *
         * @param clientId the client id
         */
        fun withClientId(clientId: String) = apply { this.clientId = clientId }

        fun build(): OpenId4VciConfig {
            require(this::issuerUrl.isInitialized && issuerUrl.isNotBlank()) { "issuerUrl must be initialized with non blank value" }
            require(this::clientId.isInitialized && clientId.isNotBlank()) { "clientId must be initialized with non blank value" }

            return OpenId4VciConfig(this)
        }

    }
}