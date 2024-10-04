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
import eu.europa.ec.eudi.openid4vci.DefaultHttpClientFactory
import eu.europa.ec.eudi.openid4vci.DeferredIssuer
import eu.europa.ec.eudi.wallet.document.*
import eu.europa.ec.eudi.wallet.issue.openidvci.PARResponse
import eu.europa.ec.eudi.wallet.issue.openidvci.PKCEVerifier
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.util.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.util.wrappedWithLogging
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executor

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
        get() = field.wrappedWithLogging(logger).wrappedWithContentNegotiation()

    private val offerCreator: OfferCreator by lazy {
        OfferCreator(config, ktorHttpClientFactory)
    }
    private val offerResolver: OfferResolver by lazy {
        OfferResolver(config.proofTypes, ktorHttpClientFactory)
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
                        DeferredIssueResult.DocumentExpired(
                            documentId = deferredDocument.id,
                            name = deferredDocument.name,
                            docType = deferredDocument.docType
                        )
                    )

                    else -> {
                        val (ctx, outcome) = DeferredIssuer.queryForDeferredCredential(
                            deferredContext,
                            ktorHttpClientFactory
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
                callback(
                    DeferredIssueResult.DocumentFailed(
                        documentId = deferredDocument.id,
                        name = deferredDocument.name,
                        docType = deferredDocument.docType,
                        cause = e
                    )
                )
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

    override suspend fun performPushAuthorizationRequest(docType: String): PARResponse {
        val offer = offerCreator.createOffer(docType)
        val issuer = issuerCreator.createIssuer(offer)
        val parResponse = issuerAuthorization.performPushAuthorizationRequest(issuer)
        return PARResponse(
            authorizationCodeURL = parResponse.authorizationCodeURL.value,
            pkceVerifier = PKCEVerifier(codeVerifier = parResponse.pkceVerifier.codeVerifier, codeVerifierMethod = parResponse.pkceVerifier.codeVerifierMethod),
            state = parResponse.state
        )
    }

    /**
     * Issues the given [Offer].
     */
    private suspend fun doIssue(
        offer: Offer,
        txCode: String?,
        listener: OpenId4VciManager.OnResult<IssueEvent>,
    ) {
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
                    .wrapWithLogging(logger)
            )
        }
    }
}
