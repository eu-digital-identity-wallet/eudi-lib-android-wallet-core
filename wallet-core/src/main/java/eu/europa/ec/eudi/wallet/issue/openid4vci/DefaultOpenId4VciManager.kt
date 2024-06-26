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
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.DefaultHttpClientFactory
import eu.europa.ec.eudi.wallet.document.*
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executor
import io.ktor.client.plugins.logging.Logger as KtorLogger

/**
 * Default implementation of [OpenId4VciManager].
 * @property config the configuration
 * @property userAuthenticationCallback the user authorization callback
 *
 * @constructor Creates a default OpenId4VCI manager.
 * @param context The context.
 * @param documentManager The document manager.
 * @param config The configuration.
 * @param userAuthenticationCallback The user authorization callback.
 * @see OpenId4VciManager
 */
internal class DefaultOpenId4VciManager(
    private val context: Context,
    private val documentManager: DocumentManager,
    var config: OpenId4VciManager.Config,
) : OpenId4VciManager {

    var logger: Logger? = null
    var ktorHttpClientFactory: () -> HttpClient = DefaultHttpClientFactory
        get() = {
            logger?.let { l ->
                field().let { client ->
                    client.config {
                        install(Logging) {
                            logger = object : KtorLogger {
                                override fun log(message: String) {
                                    l.d(TAG, message)
                                }
                            }
                            level = LogLevel.ALL
                        }
                    }
                }
            } ?: field()
        }

    private val offerCreator: OfferCreator by lazy {
        OfferCreator(config, ktorHttpClientFactory)
    }
    private val offerResolver: OfferResolver by lazy {
        OfferResolver(config, ktorHttpClientFactory)
    }
    private val issuerCreator: IssuerCreator by lazy {
        IssuerCreator(config, ktorHttpClientFactory)
    }
    private val issuerAuthorization: IssuerAuthorization by lazy {
        IssuerAuthorization(context, logger)
    }

    override fun issueDocumentByDocType(
        docType: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
            try {
                val offer = offerCreator.createOffer(docType)
                doIssue(offer, txCode, listener)
            } catch (e: Throwable) {
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByDocType failed", e)
            }
        }
    }

    override fun issueDocumentByOffer(
        offer: Offer,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
            try {
                doIssue(offer, txCode, listener)
            } catch (e: Throwable) {
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByOffer failed", e)
            }
        }
    }


    override fun issueDocumentByOfferUri(
        offerUri: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
            try {
                val offer = offerResolver.resolve(offerUri).getOrThrow()
                doIssue(offer, txCode, listener)
            } catch (e: Throwable) {
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByOfferUri failed", e)
            }
        }
    }

    override fun issueDeferredDocument(
        deferredDocument: DeferredDocument,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        val deferredState = deferredDocument.relatedData.let { DeferredState.decode(it) }
        // check tokens for expiration
        // if not expires proceed with deferred issuer call
        TODO("Not yet implemented")
    }

    override fun resolveDocumentOffer(
        offerUri: String,
        executor: Executor?,
        onResolvedOffer: OpenId4VciManager.OnResolvedOffer,
    ) {
        launch(onResolvedOffer.wrap(executor)) { coroutineScope, callback ->
            try {
                val offer = offerResolver.resolveOnceWithoutCache(offerUri).getOrThrow()
                callback(OfferResult.Success(offer))
                coroutineScope.cancel("resolveDocumentOffer succeeded")
            } catch (e: Throwable) {
                callback(OfferResult.Failure(e))
                coroutineScope.cancel("resolveDocumentOffer failed", e)
            }
        }
    }

    override fun resumeWithAuthorization(intent: Intent) = intent.data?.let { uri ->
        resumeWithAuthorization(uri)
    } ?: throw IllegalStateException("No uri to resume from Intent")

    override fun resumeWithAuthorization(uri: String) = resumeWithAuthorization(Uri.parse(uri))

    override fun resumeWithAuthorization(uri: Uri) = issuerAuthorization.resumeFromUri(uri)

    /**
     * Wraps the given [OpenId4VciManager.OnResult] with the given [Executor].
     * @param executor The executor.
     * @receiver The [OpenId4VciManager.OnResult].
     * @return The wrapped [OpenId4VciManager.OnResult].
     */
    private inline fun <R : OpenId4VciManager.OnResult<V>, reified V : OpenId4VciResult> R.wrap(executor: Executor?): OpenId4VciManager.OnResult<V> {
        return OpenId4VciManager.OnResult { result: V ->
            (executor ?: context.mainExecutor()).execute {
                this@wrap.onResult(result)
            }
        }.logResult()
    }

    /**
     * Wraps the given [OpenId4VciManager.OnResult] with debug logging.
     */
    private inline fun <R : OpenId4VciManager.OnResult<V>, reified V : OpenId4VciResult> R.logResult(): OpenId4VciManager.OnResult<V> {
        return when (val l = logger) {
            null -> this
            else -> OpenId4VciManager.OnResult { result: V ->
                when (result) {
                    is OpenId4VciResult.Erroneous -> l.e(TAG, "$result", result.cause)
                    else -> l.d(TAG, "$result")
                }
                this.onResult(result)
            }
        }
    }

    /**
     * Launches a coroutine.
     * @param onResult The result listener.
     * @param block The coroutine block.
     */
    private fun <R : OpenId4VciManager.OnResult<V>, V : OpenId4VciResult> launch(
        onResult: R,
        block: suspend (coroutineScope: CoroutineScope, onResult: R) -> Unit,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { block(scope, onResult) }
    }

    /**
     * Issues the given [Offer].
     */
    private suspend fun doIssue(
        offer: Offer,
        txCode: String?,
        listener: OpenId4VciManager.OnResult<IssueEvent>,
    ) {
        offer as DefaultOffer
        val issuer = issuerCreator.createIssuer(offer)
        var authorizedRequest = issuerAuthorization.authorize(issuer, txCode)

        listener(IssueEvent.Started(offer.offeredDocuments.size))
        val issuedDocumentIds = mutableListOf<DocumentId>()
        val requestMap = offer.offeredDocuments.associateBy { offeredDocument ->
            documentManager.createDocument(offeredDocument, config.useStrongBoxIfSupported)
                .getOrThrow()
        }

        val request = SubmitRequest(config, issuer, authorizedRequest)
        val response = request.request(requestMap).also {
            authorizedRequest = request.authorizedRequest
        }
        val deferredState = DeferredState(config.clientId, issuer, authorizedRequest)
        ProcessResponse(documentManager, listener, issuedDocumentIds, deferredState).use { it.process(response) }
        listener(IssueEvent.Finished(issuedDocumentIds))
    }
}
