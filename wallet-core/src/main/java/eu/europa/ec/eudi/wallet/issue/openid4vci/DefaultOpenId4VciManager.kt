/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataResolver
import eu.europa.ec.eudi.openid4vci.DefaultHttpClientFactory
import eu.europa.ec.eudi.openid4vci.DeferredIssuer
import eu.europa.ec.eudi.openid4vci.KtorHttpClientFactory
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.internal.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.internal.wrappedWithLogging
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Default implementation of [OpenId4VciManager].
 * @property config the configuration
 *
 * @constructor Creates a default OpenId4VCI manager.
 * @param context The context.
 * @param documentManager The document manager.
 * @param config The configuration.
 * @see OpenId4VciManager
 */
internal class DefaultOpenId4VciManager(
    private val context: Context,
    private val documentManager: DocumentManager,
    var config: OpenId4VciManager.Config,
    var logger: Logger? = null,
    var ktorHttpClientFactory: KtorHttpClientFactory? = null,
) : OpenId4VciManager {

    internal val httpClientFactory
        get() = (ktorHttpClientFactory ?: DefaultHttpClientFactory)
            .wrappedWithLogging(logger)
            .wrappedWithContentNegotiation()

    private val offerCreator: OfferCreator by lazy {
        OfferCreator(config, httpClientFactory)
    }
    private val offerResolver: OfferResolver by lazy {
        OfferResolver(httpClientFactory)
    }
    private val issuerCreator: IssuerCreator by lazy {
        IssuerCreator(config, httpClientFactory)
    }
    private val issuerAuthorization: IssuerAuthorization by lazy {
        IssuerAuthorization(context, logger)
    }

    override suspend fun getIssuerMetadata(): Result<CredentialIssuerMetadata> {
        return CredentialIssuerId(config.issuerUrl).mapCatching {
            CredentialIssuerMetadataResolver(httpClientFactory).resolve(it).getOrThrow()
        }
    }

    override fun issueDocumentByDocType(
        docType: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(executor, onIssueEvent) { coroutineScope, listener ->
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
        launch(executor, onIssueEvent) { coroutineScope, listener ->
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
        launch(executor, onIssueEvent) { coroutineScope, listener ->
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
        onIssueResult: OpenId4VciManager.OnDeferredIssueResult,
    ) {
        launch(executor, onIssueResult) { coroutineScope, callback ->
            try {
                val deferredContext = deferredDocument.relatedData.toDeferredIssuanceContext()
                when {
                    deferredContext.hasExpired -> callback(
                        DeferredIssueResult.DocumentExpired(deferredDocument)
                    )

                    else -> {
                        val (ctx, outcome) = DeferredIssuer.queryForDeferredCredential(
                            deferredContext,
                            httpClientFactory
                        )
                            .getOrThrow()
                        ProcessDeferredOutcome(
                            documentManager = documentManager,
                            callback = callback,
                            deferredIssuanceContext = ctx,
                            logger = logger
                        ).process(deferredDocument, outcome)
                    }
                }
            } catch (e: Throwable) {
                callback(DeferredIssueResult.DocumentFailed(deferredDocument, e))
                coroutineScope.cancel("issueDeferredDocument failed", e)
            }
        }

    }

    override fun resolveDocumentOffer(
        offerUri: String,
        executor: Executor?,
        onResolvedOffer: OpenId4VciManager.OnResolvedOffer,
    ) {
        launch(executor, onResolvedOffer) { coroutineScope, callback ->
            try {
                val offer = offerResolver.resolve(offerUri, useCache = false).getOrThrow()
                callback(OfferResult.Success(offer))
                coroutineScope.cancel("resolveDocumentOffer succeeded")
            } catch (e: Throwable) {
                callback(OfferResult.Failure(e))
                coroutineScope.cancel("resolveDocumentOffer failed", e)
            }
        }
    }

    override fun resumeWithAuthorization(uri: Uri) = issuerAuthorization.resumeFromUri(uri)

    override fun resumeWithAuthorization(uri: String) {
        resumeWithAuthorization(Uri.parse(uri))
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
        val documentCreator = DocumentCreator(
            documentManager = documentManager,
            listener = listener,
            logger = logger
        )
        val requestMap = documentCreator.createDocuments(offer)

        val submit = SubmitRequest(config, issuer, authorizedRequest)
        val response = submit.request(requestMap).also {
            authorizedRequest = submit.authorizedRequest
        }
        ProcessResponse(
            documentManager = documentManager,
            deferredContextCreator = DeferredContextCreator(issuer, authorizedRequest),
            listener = listener,
            issuedDocumentIds = issuedDocumentIds,
            logger = logger
        ).use { it.process(response) }
        listener(IssueEvent.Finished(issuedDocumentIds))
    }

    /**
     * Launches a coroutine.
     * @param onResult The result listener.
     * @param block The coroutine block.
     */
    private inline fun <reified V : OpenId4VciResult> launch(
        executor: Executor?,
        onResult: OpenId4VciManager.OnResult<V>,
        crossinline block: suspend (coroutineScope: CoroutineScope, onResult: OpenId4VciManager.OnResult<V>) -> Unit,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            block(
                scope, onResult
                    .runOn(executor ?: context.mainExecutor())
                    .wrapLogging(logger)
            )
        }
    }
}
