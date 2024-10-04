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
import android.net.Uri
import androidx.annotation.IntDef
import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config.ParUsage.Companion.IF_SUPPORTED
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config.ParUsage.Companion.NEVER
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Config.ParUsage.Companion.REQUIRED
import eu.europa.ec.eudi.wallet.issue.openidvci.PARResponse
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.*
import java.util.concurrent.Executor
import eu.europa.ec.eudi.openid4vci.ProofType as InternalProofType

/**
 * OpenId4VciManager is the main entry point to issue documents using the OpenId4Vci protocol
 * It provides methods to issue documents using a document type or an offer, and to resolve an offer
 * @see[OpenId4VciManager.Config] for the configuration options
 */
interface OpenId4VciManager {

    /**
     * Issue a document using a document type
     * @param docType the document type to issue
     * @param txCode the transaction code to use for pre-authorized issuing
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onIssueEvent the callback to be called when the document is issued
     * @see[IssueEvent] on how to handle the result
     */
    fun issueDocumentByDocType(
        docType: String,
        txCode: String? = null,
        executor: Executor? = null,
        onIssueEvent: OnIssueEvent,
    )

    /**
     * Issue a document using an offer
     * @param offer the offer to issue
     * @param txCode the transaction code to use for pre-authorized issuing
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onIssueEvent the callback to be called when the document is issued. This callback may be called multiple times, each for every document in the offer
     *
     * @see[IssueEvent] on how to handle the result
     */
    fun issueDocumentByOffer(
        offer: Offer,
        txCode: String? = null,
        executor: Executor? = null,
        onIssueEvent: OnIssueEvent,
    )

    /**
     * Issue a document using an offer URI
     * @param offerUri the offer URI
     * @param txCode the transaction code to use for pre-authorized issuing
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onIssueEvent the callback to be called when the document is issued. This callback may be called multiple times, each for every document in the offer
     * @see[IssueEvent] on how to handle the result
     */
    fun issueDocumentByOfferUri(
        offerUri: String,
        txCode: String? = null,
        executor: Executor? = null,
        onIssueEvent: OnIssueEvent,
    )

    /**
     * Issue a deferred document
     * @param deferredDocument the deferred document to issue
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onIssueResult the callback to be called when the document is issued
     * @see[OnDeferredIssueResult] on how to handle the result
     */
    fun issueDeferredDocument(
        deferredDocument: DeferredDocument,
        executor: Executor?,
        onIssueResult: OnDeferredIssueResult,
    )

    /**
     * Resolve an offer using OpenId4Vci protocol
     *
     * @param offerUri the offer URI
     * @param executor the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread
     * @param onResolvedOffer the callback to be called when the offer is resolved
     *
     */
    fun resolveDocumentOffer(offerUri: String, executor: Executor? = null, onResolvedOffer: OnResolvedOffer)


    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param uri the uri that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(uri: Uri)

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param uri the uri that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(uri: String)

    /**
     * Callback to be called for [OpenId4VciManager.issueDocumentByDocType],
     * [OpenId4VciManager.issueDocumentByOffer], [OpenId4VciManager.issueDocumentByOfferUri]
     * and [OpenId4VciManager.resolveDocumentOffer] methods
     */
    fun interface OnResult<T : OpenId4VciResult> {
        fun onResult(result: T)
        operator fun invoke(result: T) = onResult(result)
    }

    /**
     * Callback to be called when a document is issued
     */
    fun interface OnIssueEvent : OnResult<IssueEvent>

    /**
     * Callback to be called when an offer is resolved
     */
    fun interface OnResolvedOffer : OnResult<OfferResult>

    /**
     * Callback to be called when a deferred document is issued
     */
    fun interface OnDeferredIssueResult : OnResult<DeferredIssueResult>

    /**
     * Builder to create an instance of [OpenId4VciManager]
     * @param context the context
     * @property config the [Config] to use
     * @property documentManager the [DocumentManager] to use
     * requires user authentication
     */
    class Builder(private val context: Context) {
        var config: Config? = null
        var documentManager: DocumentManager? = null
        var logger: Logger? = null
        var ktorHttpClientFactory: (() -> HttpClient)? = null

        /**
         * Set the [Config] to use
         */
        fun config(config: Config) = apply { this.config = config }

        /**
         * Set the [DocumentManager] to use
         */
        fun documentManager(documentManager: DocumentManager) = apply { this.documentManager = documentManager }


        fun logger(logger: Logger) = apply {
            this.logger = logger
        }

        /**
         * Override the Ktor HTTP client factory
         */
        fun ktorHttpClientFactory(factory: () -> HttpClient) = apply {
            this.ktorHttpClientFactory = factory
        }

        /**
         * Build the [OpenId4VciManager]
         * @throws [IllegalStateException] if config or documentManager is not set
         */
        fun build(): OpenId4VciManager {
            checkNotNull(config) { "config is required" }
            checkNotNull(documentManager) { "documentManager is required" }
            return DefaultOpenId4VciManager(context, documentManager!!, config!!).apply {
                this@Builder.logger?.let { this.logger = it }
                this@Builder.ktorHttpClientFactory?.let { this.ktorHttpClientFactory = it }
            }
        }
    }

    companion object {
        internal const val TAG = "OpenId4VciManager"

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
     * @property useStrongBoxIfSupported use StrongBox for document keys if supported
     * @property useDPoPIfSupported flag that if set will enable the use of DPoP JWT
     * @property parUsage if PAR should be used
     * @property proofTypes the proof types to use
     * @property debugLogging flag to enable debug logging
     * @property ktorHttpClientFactory the Ktor HTTP client factory
     */
    data class Config(
        val issuerUrl: String,
        val clientId: String,
        val authFlowRedirectionURI: String,
        val useStrongBoxIfSupported: Boolean,
        val useDPoPIfSupported: Boolean,
        @ParUsage val parUsage: Int,
        val proofTypes: List<ProofType>,
    ) {

        /**
         * PAR usage for the OpenId4Vci issuer
         * @property IF_SUPPORTED use PAR if supported
         * @property REQUIRED use PAR always
         * @property NEVER never use PAR
         */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(value = [IF_SUPPORTED, REQUIRED, NEVER])
        annotation class ParUsage {
            companion object {
                const val IF_SUPPORTED = 2
                const val REQUIRED = 4
                const val NEVER = 0
            }
        }

        /**
         * Proof type for the OpenId4Vci issuer
         */
        enum class ProofType(@JvmSynthetic internal val type: InternalProofType) {
            /**
             * JWT proof type
             */
            JWT(InternalProofType.JWT),

            /**
             * CWT proof type
             */
            CWT(InternalProofType.CWT)
        }

        /**
         * Builder to create an instance of [Config]
         * @property issuerUrl the issuer url
         * @property clientId the client id
         * @property authFlowRedirectionURI the redirection URI for the authorization flow
         * @property useStrongBoxIfSupported use StrongBox for document keys if supported
         * @property useDPoPIfSupported flag that if set will enable the use of DPoP JWT
         * @property parUsage if PAR should be used
         * @property debugLogging flag to enable debug logging. Default is false
         * @property ktorHttpClientFactory the Ktor HTTP client factory. If not set, the default factory will be used
         *
         */
        class Builder {
            var issuerUrl: String? = null
            var clientId: String? = null
            var authFlowRedirectionURI: String? = null
            var useStrongBoxIfSupported: Boolean = false
            var useDPoPIfSupported: Boolean = false

            @ParUsage
            var parUsage: Int = IF_SUPPORTED

            private var proofTypes: List<ProofType> = listOf(ProofType.JWT, ProofType.CWT)


            /**
             * Set the issuer url
             * @param issuerUrl the issuer url
             */
            fun issuerUrl(issuerUrl: String) = apply { this.issuerUrl = issuerUrl }

            /**
             * Set the client id
             * @param clientId the client id
             */
            fun clientId(clientId: String) = apply { this.clientId = clientId }

            /**
             * Set the redirection URI for the authorization flow
             * @param authFlowRedirectionURI the redirection URI for the authorization flow
             */
            fun authFlowRedirectionURI(authFlowRedirectionURI: String) =
                apply { this.authFlowRedirectionURI = authFlowRedirectionURI }

            /**
             * Set the flag that if set will enable the use of StrongBox for document keys if supported
             * @param useStrongBoxIfSupported the flag that if set will enable the use of StrongBox for document keys if supported
             */
            fun useStrongBoxIfSupported(useStrongBoxIfSupported: Boolean) =
                apply { this.useStrongBoxIfSupported = useStrongBoxIfSupported }

            /**
             * Set the flag that if set will enable the use of DPoP JWT
             * @param useDPoP the flag that if set will enable the use of DPoP JWT
             */
            fun useDPoP(useDPoP: Boolean) = apply { this.useDPoPIfSupported = useDPoP }

            /**
             * Set the PAR usage
             * @param parUsage the PAR usage
             */
            fun parUsage(@ParUsage parUsage: Int) = apply { this.parUsage = parUsage }

            /**
             * Set the proof types. The supported proof types are [ProofType].
             * The order of the proof types is the order in which they will be used.
             * @param proofType the proof types
             * @throws [IllegalArgumentException] if a proof type is provided more than once
             */
            fun proofTypes(vararg proofType: ProofType) = apply {
                val distinctList = proofType.toList().distinct()
                require(distinctList.size <= ProofType.values().size) {
                    "Only 2 proof types are supported. You provided ${proofType.toList().size}"
                }
                this.proofTypes = distinctList
            }

            /**
             * Build the [Config]
             * @throws [IllegalStateException] if issuerUrl, clientId or authFlowRedirectionURI is not set
             */
            fun build(): Config {
                checkNotNull(issuerUrl) { "issuerUrl is required" }
                checkNotNull(clientId) { "clientId is required" }
                checkNotNull(authFlowRedirectionURI) { "authFlowRedirectionURI is required" }

                return Config(
                    issuerUrl!!,
                    clientId!!,
                    authFlowRedirectionURI!!,
                    useStrongBoxIfSupported,
                    useDPoPIfSupported,
                    parUsage,
                    proofTypes,
                )
            }

            fun withIssuerUrl(issuerUrl: String) = issuerUrl(issuerUrl)

            fun withClientId(clientId: String) = clientId(clientId)

            fun withAuthFlowRedirectionURI(authFlowRedirectionURI: String) =
                authFlowRedirectionURI(authFlowRedirectionURI)
        }

        companion object {
            /**
             * Create an instance of [Config]
             * @param block the block to configure the [Builder]
             */
            operator fun invoke(block: Builder.() -> Unit) = make(block)

            /**
             * Create an instance of [Config]
             * @param block the block to configure the [Builder]
             */
            fun make(block: Builder.() -> Unit) = Builder().apply(block).build()
        }
    }

    suspend fun performPushAuthorizationRequest(docType: String): PARResponse
}