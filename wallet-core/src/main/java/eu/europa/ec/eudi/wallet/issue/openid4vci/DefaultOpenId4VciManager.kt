/*
 * Copyright (c) 2024-2025 European Commission
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
import androidx.core.net.toUri
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuanceError
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataResolver
import eu.europa.ec.eudi.openid4vci.DeferredIssuer
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.internal.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.internal.wrappedWithLogging
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceAuthorizationException
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceIssuer
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.multipaz.storage.Storage
import org.multipaz.storage.android.AndroidStorage
import java.io.File
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
    private val walletProvider: WalletAttestationsProvider?,
    private val walletAttestationKeyManager: WalletKeyManager,
    var config: OpenId4VciManager.Config,
    var logger: Logger? = null,
    var ktorHttpClientFactory: (() -> HttpClient)? = null,
) : OpenId4VciManager {

    internal val httpClientFactory
        get() = (ktorHttpClientFactory ?: DefaultHttpClientFactory)
            .wrappedWithLogging(logger)
            .wrappedWithContentNegotiation()

    private val offerResolver: OfferResolver by lazy {
        OfferResolver(httpClientFactory)
    }
    private val issuerCreator: IssuerCreator by lazy {
        IssuerCreator(context, config, httpClientFactory, walletProvider, walletAttestationKeyManager, logger)
    }
    private val issuerAuthorization: IssuerAuthorization by lazy {
        val handler = config.authorizationHandler ?: BrowserAuthorizationHandler(context, logger)
        IssuerAuthorization(handler, logger)
    }

    /**
     * Storage for issuance metadata.
     * If not configured in [config], uses a default AndroidStorage with a separate database file.
     */
    private val issuanceMetadataStorage: Storage by lazy {
        config.issuanceMetadataStorage ?: run {
            val storagePath = File(
                context.noBackupFilesDir,
                "issuance_metadata.db"
            ).absolutePath
            AndroidStorage(storagePath)
        }
    }

    override suspend fun getIssuerMetadata(): Result<CredentialIssuerMetadata> {
        return CredentialIssuerId(config.issuerUrl).mapCatching {
            CredentialIssuerMetadataResolver(httpClientFactory()).resolve(
                issuer = it,
                policy = IssuerMetadataPolicy.IgnoreSigned
            ).getOrThrow()
        }
    }

    @Deprecated("Use issueDocumentByConfigurationIdentifiers that accepts a list of identifiers")
    override fun issueDocumentByConfigurationIdentifier(
        credentialConfigurationId: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        issueDocumentByConfigurationIdentifiers(
            listOf(credentialConfigurationId),
            txCode,
            executor,
            onIssueEvent
        )
    }

    override fun issueDocumentByConfigurationIdentifiers(
        credentialConfigurationIds: List<String>,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(executor, onIssueEvent) { coroutineScope, listener ->
            try {
                val issuer = issuerCreator.createIssuer(
                    config.issuerUrl,
                    credentialConfigurationIds.map{ id -> CredentialConfigurationIdentifier(id) }
                )
                doIssue(issuer, Offer(issuer.credentialOffer), txCode, listener)
            } catch (e: Throwable) {
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByConfigurationIdentifier failed", e)
            }
        }
    }

    override fun issueDocumentByFormat(
        format: DocumentFormat,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(executor, onIssueEvent) { coroutineScope, listener ->
            try {
                val issuer = issuerCreator.createIssuer(config.issuerUrl, format)
                val offer = Offer(issuer.credentialOffer)
                doIssue(issuer, offer, txCode, listener)
            } catch (e: Throwable) {
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByDocType failed", e)
            }
        }
    }

    @Deprecated("Use issueDocumentByConfigurationIdentifier or issueDocumentByFormat instead")
    override fun issueDocumentByDocType(
        docType: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        issueDocumentByFormat(
            format = MsoMdocFormat(
                docType = docType
            ),
            txCode = txCode,
            executor = executor,
            onIssueEvent = onIssueEvent
        )
    }

    override fun issueDocumentByOffer(
        offer: Offer,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) {
        launch(executor, onIssueEvent) { coroutineScope, listener ->
            try {
                val issuer = issuerCreator.createIssuer(offer)
                doIssue(issuer, offer, txCode, listener)
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
                val issuer = issuerCreator.createIssuer(offer)
                doIssue(issuer, offer, txCode, listener)
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

                // Resolve DPopConfig so DeferredContext can recreate the DPoP signer
                val resolvedDpopConfig = when (val cfg = config.dpopConfig) {
                    DPopConfig.Disabled -> null
                    DPopConfig.Default -> DPopConfig.Default.make(context)
                    is DPopConfig.Custom -> cfg
                }

                val deferredContext = DeferredContext.fromBytes(
                    deferredDocument.relatedData,
                    walletAttestationKeyManager,
                    dpopConfig = resolvedDpopConfig,
                    logger = logger,
                )
                val clientAttestationPopKeyId = deferredContext.clientAttestationPopKeyId
                when {
                    deferredContext.issuanceContext.hasExpired -> callback(
                        DeferredIssueResult.DocumentExpired(deferredDocument)
                    )

                    else -> {
                        val (ctx, outcome) = DeferredIssuer.queryForDeferredCredential(
                            ctx = deferredContext.issuanceContext,
                            httpClient = httpClientFactory(),
                            responseEncryptionKey = null // TODO handle encrypted responses
                        ).getOrThrow()

                        ProcessDeferredOutcome(
                            documentManager = documentManager,
                            callback = callback,
                            deferredContext = ctx?.let {
                                DeferredContext(
                                    issuanceContext = it,
                                    keyAliases = deferredContext.keyAliases,
                                    clientAttestationPopKeyId = clientAttestationPopKeyId,
                                    dPoPKeyAlias = deferredContext.dPoPKeyAlias,
                                    credentialConfigurationIdentifier = deferredContext.credentialConfigurationIdentifier,
                                    credentialEndpoint = deferredContext.credentialEndpoint,
                                    replacesDocumentId = deferredContext.replacesDocumentId,
                                )
                            } ?: deferredContext,
                            logger = logger,
                            issuanceMetadataStorage = issuanceMetadataStorage,
                        ).process(deferredDocument, deferredContext.keyAliases, outcome)
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
        resumeWithAuthorization(uri.toUri())
    }

    override fun reissueDocument(
        documentId: DocumentId,
        allowAuthorizationFallback: Boolean,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        launch(executor, onIssueEvent) { coroutineScope, listener ->
            try {
                //  Load issuance metadata from storage
                val issuanceMetadata = loadIssuanceMetadata(documentId)
                    ?: throw IllegalStateException("No issuance metadata found for document $documentId")

                logger?.d(TAG, "Loaded issuanceMetadata: credentialIssuerId=${issuanceMetadata.credentialIssuerId}")

                //  Reconstruct AuthorizedRequest from stored metadata
                var authorizedRequest = ReissuanceIssuer().reconstructAuthorizedRequest(issuanceMetadata)

                //  Create Issuer using IssuerCreator (resolves issuer metadata)
                //  Pass existing DPoP key alias so the same key is reused
                //  (access token is bound to original key's thumbprint)
                val issuer = issuerCreator.createIssuer(
                    issuanceMetadata.credentialIssuerId,
                    listOf(CredentialConfigurationIdentifier(issuanceMetadata.credentialConfigurationIdentifier)),
                    existingDpopKeyAlias = issuanceMetadata.dPoPKeyAlias
                )
                val offer = Offer(issuer.credentialOffer)

                //  Create a new UnsignedDocument (fresh keys) via DocumentCreator
                //    This fires IssueEvent.DocumentRequiresCreateSettings so the app
                //    can provide CreateDocumentSettings (secure area, number of credentials, etc.)
                val documentCreator = DocumentCreator(
                    documentManager = documentManager,
                    listener = listener,
                    logger = logger
                )
                val requestMap = documentCreator.createDocuments(offer)

                listener(IssueEvent.Started(requestMap.size))

                //  Submit the issuance request using stored AuthorizedRequest
                //  (skips the authorization flow - uses refresh token instead)
                val submit = SubmitRequest(config, walletProvider, issuer, authorizedRequest)
                var response = submit.request(requestMap).also {
                    authorizedRequest = submit.authorizedRequest
                }

                //  Check for authentication failure (401 / InvalidToken)
                //    This happens when both stored access token AND refresh token are invalid.
                //    Fall back to full OAuth authorization flow and retry with fresh tokens.
                if (isAuthenticationFailure(response)) {
                    if (!allowAuthorizationFallback) {
                        throw ReissuanceAuthorizationException(
                            "Re-issuance of document $documentId requires user authorization (tokens expired)"
                        )
                    }
                    logger?.d(TAG, "Re-issuance token expired for $documentId, falling back to full authorization")
                    authorizedRequest = issuerAuthorization.authorize(issuer, null)
                    val retrySubmit = SubmitRequest(config, walletProvider, issuer, authorizedRequest)
                    response = retrySubmit.request(requestMap).also {
                        authorizedRequest = retrySubmit.authorizedRequest
                    }
                }

                //  Process the response: store new document, then delete old one
                val issuedDocumentIds = mutableListOf<DocumentId>()
                val deferredDocumentIds = mutableListOf<DocumentId>()
                ProcessResponse(
                    documentManager = documentManager,
                    deferredContextFactory = DeferredContextFactory(issuer, authorizedRequest, issuerCreator.dpopKeyAlias),
                    clientAttestationPopKeyId = issuerCreator.clientAttestationPopKeyId,
                    listener = listener,
                    issuedDocumentIds = issuedDocumentIds,
                    deferredDocumentIds = deferredDocumentIds,
                    logger = logger,
                    authorizedRequest = authorizedRequest,
                    issuer = issuer,
                    documentToConfigurationMap = requestMap,
                    dpopKeyAlias = issuerCreator.dpopKeyAlias,
                    issuanceMetadataStorage = issuanceMetadataStorage,
                    clientAuthentication = issuerCreator.clientAuthentication,
                    replacesDocumentId = documentId,
                ).process(response)

                //  If new document(s) issued successfully, delete the old document.
                //  If the outcome was deferred, the old document stays alive — the
                //  replacesDocumentId stored in the deferred context will trigger
                //  deletion when issueDeferredDocument() eventually succeeds.
                if (issuedDocumentIds.isNotEmpty()) {
                    documentManager.deleteDocumentById(documentId)
                    logger?.d(TAG, "Deleted old document $documentId after re-issuance")
                }

                listener(IssueEvent.Finished(issuedDocumentIds + deferredDocumentIds))

            } catch (e: Throwable) {
                logger?.e(TAG, "Something went wrong with reissuance of $documentId", e)
                listener(failure(e))
                coroutineScope.cancel("reissueDocument failed", e)
            }
        }
    }

    /**
     * Checks if a submission response contains an authentication failure,
     * indicating the stored access token is no longer valid and fresh
     * authorization is needed.
     *
     * Handles two scenarios:
     * - Clean path: Server returns 401 with proper JSON body → parsed as
     *   [SubmissionOutcome.Failed] with [CredentialIssuanceError.InvalidToken]
     * - Fallback path: Server returns 401 without content-type → Ktor throws
     *   NoTransformationFoundException containing "401 Unauthorized" in its message
     */
    private fun isAuthenticationFailure(response: SubmitRequest.Response): Boolean {
        return response.values.any { result ->
            // Clean path: SubmissionOutcome.Failed with InvalidToken
            val outcome = result.outcome.getOrNull()
            if (outcome is SubmissionOutcome.Failed && outcome.error is CredentialIssuanceError.InvalidToken) {
                return@any true
            }
            // Fallback path: exception when the 401 response cannot be parsed as a typed error
            // (e.g. response has no content-type or no JSON body)
            val exception = result.outcome.exceptionOrNull() ?: return@any false
            exception is CredentialIssuanceError.InvalidToken
                || exception.message?.contains("401 Unauthorized") == true
        }
    }

    /**
     * Loads issuance metadata from storage.
     *
     * @param documentId The document ID
     * @return The issuance metadata, or null if not found
     */
    private suspend fun loadIssuanceMetadata(documentId: DocumentId): IssuanceMetadata? {
        val table = issuanceMetadataStorage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
        val bytes = table.get(documentId) ?: return null
        return IssuanceMetadata.fromByteArray(bytes.toByteArray())
    }

    /**
     * Issues the given [Offer].
     */
    private suspend fun doIssue(
        issuer: Issuer,
        offer: Offer,
        txCode: String?,
        listener: OpenId4VciManager.OnResult<IssueEvent>,
    ) {
        var authorizedRequest = issuerAuthorization.authorize(issuer, txCode)
        listener(IssueEvent.Started(offer.offeredDocuments.size))
        val issuedDocumentIds = mutableListOf<DocumentId>()
        val deferredDocumentIds = mutableListOf<DocumentId>()
        val documentCreator = DocumentCreator(
            documentManager = documentManager,
            listener = listener,
            logger = logger
        )
        val requestMap = documentCreator.createDocuments(offer)

        val submit = SubmitRequest(config, walletProvider, issuer, authorizedRequest)
        val response = submit.request(requestMap).also {
            authorizedRequest = submit.authorizedRequest
        }
        ProcessResponse(
            documentManager = documentManager,
            deferredContextFactory = DeferredContextFactory(issuer, authorizedRequest, issuerCreator.dpopKeyAlias),
            clientAttestationPopKeyId = issuerCreator.clientAttestationPopKeyId,
            listener = listener,
            issuedDocumentIds = issuedDocumentIds,
            deferredDocumentIds = deferredDocumentIds,
            logger = logger,
            authorizedRequest = authorizedRequest,
            issuer = issuer,
            documentToConfigurationMap = requestMap,
            dpopKeyAlias = issuerCreator.dpopKeyAlias,
            issuanceMetadataStorage = issuanceMetadataStorage,
            clientAuthentication = issuerCreator.clientAuthentication,
        ).process(response)
        listener(IssueEvent.Finished(issuedDocumentIds + deferredDocumentIds))
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

    companion object {
        private val DefaultHttpClientFactory: () -> HttpClient = {
            HttpClient {
                install(ContentNegotiation) {
                    json(
                        json = Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        },
                    )
                }
            }
        }
    }

}
