/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import android.content.Intent
import eu.europa.ec.eudi.wallet.document.DocumentManager
import java.util.concurrent.Executor

/**
 * OpenId4VciManager is the main entry point to issue documents using the OpenId4Vci protocol
 * It provides methods to issue documents using a document type or an offer, and to resolve an offer
 * @see[OpenId4VciManager.Config] for the configuration options
 */
interface OpenId4VciManager {

    /**
     * Issue a document using a document type
     * @param docType the document type to issue
     * @param config the [Config] to use. Optional, if [OpenId4VciManager] implementation has a default config
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onResult the callback to be called when the document is issued
     * @see[IssueDocumentResult] on how to handle the result
     * @see[IssueDocumentResult.UserAuthRequired] on how to handle user authentication
     *
     * Example:
     * ```kotlin
     * openId4VciManager.issueDocumentByDocType(docType) { result ->
     *   when (result) {
     *      is IssueDocumentResult.Success -> {
     *          val documentId = result.documentId
     *          // handle document
     *      }
     *      is IssueDocumentResult.Failure -> {
     *          val error = result.throwable
     *          // handle error
     *      }
     *      is IssueDocumentResult.UserAuthRequired -> {
     *          val cryptoObject = result.cryptoObject
     *          // handle user auth
     *      }
     *  }
     */
    fun issueDocumentByDocType(
        docType: String,
        config: Config? = null,
        executor: Executor? = null,
        onResult: OnIssueDocument
    )

    /**
     * Issue a document using an offer
     * @param offer the offer to issue
     * @param config the [Config] to use. Optional, if [OpenId4VciManager] implementation has a default config
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onResult the callback to be called when the document is issued. This callback may be called multiple times, each for every document in the offer
     *
     * @see[IssueDocumentResult] on how to handle the result
     * @see[IssueDocumentResult.UserAuthRequired] on how to handle user authentication
     *
     * Example:
     * ```kotlin
     * openId4VciManager.issueDocumentByOffer(offer) { result ->
     *   when (result) {
     *      is IssueDocumentResult.Success -> {
     *          val documentId = result.documentId
     *          // handle document
     *      }
     *      is IssueDocumentResult.Failure -> {
     *          val error = result.throwable
     *          // handle error
     *      }
     *      is IssueDocumentResult.UserAuthRequired -> {
     *          val cryptoObject = result.cryptoObject
     *          // handle user auth
     *      }
     *  }
     */
    fun issueDocumentByOffer(
        offer: Offer,
        config: Config? = null,
        executor: Executor? = null,
        onResult: OnIssueDocument
    )

    /**
     * Resolve an offer using OpenId4Vci protocol
     *
     * @param offerUri the offer URI
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onResult the callback to be called when the offer is resolved
     *
     * Example:
     * ```kotlin
     * openId4VciManager.resolveDocumentOffer(offerUri) { result ->
     *    when (result) {
     *      is OfferResult.Success -> {
     *          val offer = result.offer
     *          // handle offer
     *      }
     *      is OfferResult.Failure -> {
     *          val error = result.error
     *          // handle error
     *      }
     *    }
     * ```
     */
    fun resolveDocumentOffer(offerUri: String, executor: Executor? = null, onResult: OnResolveDocumentOffer)

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param intent the intent that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(intent: Intent)

    fun interface OnResult<T> {
        fun onResult(result: T)
        operator fun invoke(result: T) = onResult(result)
    }

    /**
     * Callback to be called when a document is issued
     */
    fun interface OnIssueDocument : OnResult<IssueDocumentResult>

    /**
     * Callback to be called when an offer is resolved
     */
    fun interface OnResolveDocumentOffer : OnResult<OfferResult>

    /**
     * Builder to create an instance of [OpenId4VciManager]
     * @param context the context
     * @property config the [Config] to use
     * @property documentManager the [DocumentManager] to use
     */
    class Builder(private val context: Context) {
        var config: Config? = null
        var documentManager: DocumentManager? = null

        /**
         * Set the [Config] to use
         */
        fun config(config: Config) = apply { this.config = config }

        /**
         * Set the [DocumentManager] to use
         */
        fun documentManager(documentManager: DocumentManager) = apply { this.documentManager = documentManager }

        /**
         * Build the [OpenId4VciManager]
         * @throws [IllegalStateException] if config or documentManager is not set
         */
        fun build(): OpenId4VciManager {
            checkNotNull(config) { "config is required" }
            checkNotNull(documentManager) { "documentManager is required" }
            return DefaultOpenId4VciManager(context, documentManager!!, config!!)
        }
    }

    companion object {
        /**
         * Create an instance of [OpenId4VciManager]
         */
        operator fun invoke(context: Context, block: Builder.() -> Unit) = Builder(context).apply(block).build()
    }

    /**
     * Configuration for the OpenId4Vci issuer
     * @property issuerUrl the issuer url
     * @property clientId the client id
     * @property authFlowRedirectionURI the redirection URI for the authorization flow
     */
    data class Config(val issuerUrl: String, val clientId: String, val authFlowRedirectionURI: String) {

        /**
         * Builder to create an instance of [Config]
         * @property issuerUrl the issuer url
         * @property clientId the client id
         * @property authFlowRedirectionURI the redirection URI for the authorization flow
         *
         */
        class Builder {
            var issuerUrl: String? = null
            var clientId: String? = null
            var authFlowRedirectionURI: String? = null

            /**
             * Set the issuer url
             */
            fun issuerUrl(issuerUrl: String) = apply { this.issuerUrl = issuerUrl }

            /**
             * Set the client id
             */
            fun clientId(clientId: String) = apply { this.clientId = clientId }

            /**
             * Set the redirection URI for the authorization flow
             */
            fun authFlowRedirectionURI(authFlowRedirectionURI: String) =
                apply { this.authFlowRedirectionURI = authFlowRedirectionURI }

            /**
             * Build the [Config]
             * @throws [IllegalStateException] if issuerUrl, clientId or authFlowRedirectionURI is not set
             */
            fun build(): Config {
                checkNotNull(issuerUrl) { "issuerUrl is required" }
                checkNotNull(clientId) { "clientId is required" }
                checkNotNull(authFlowRedirectionURI) { "authFlowRedirectionURI is required" }

                return Config(issuerUrl!!, clientId!!, authFlowRedirectionURI!!)
            }

            @Deprecated("Use issuerUrl instead", ReplaceWith("issuerUrl(issuerUrl)"))
            fun withIssuerUrl(issuerUrl: String) = issuerUrl(issuerUrl)

            @Deprecated("Use clientId instead", ReplaceWith("clientId(clientId)"))
            fun withClientId(clientId: String) = clientId(clientId)
        }

        companion object {
            /**
             * Create an instance of [Config]
             */
            operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
        }
    }
}