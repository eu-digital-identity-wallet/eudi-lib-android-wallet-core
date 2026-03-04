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
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataResolver
import eu.europa.ec.eudi.openid4vci.DeferredIssuer
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.internal.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.internal.wrappedWithLogging
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceConfig
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
import org.multipaz.storage.StorageTableSpec
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
     * Storage for re-issuance metadata.
     * If not configured in [config], uses a default AndroidStorage with a separate database file.
     */
    private val reissuanceStorage: Storage by lazy {
        config.reissuanceMetadataStorage ?: run {
            val storagePath = File(
                context.noBackupFilesDir,
                "reissuance_metadata.db"
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

                val deferredContext = DeferredContext.fromBytes(
                    deferredDocument.relatedData,
                    walletAttestationKeyManager
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
                            walletKeyManager = walletAttestationKeyManager,
                            clientAttestationPopKeyId = clientAttestationPopKeyId,
                            callback = callback,
                            deferredContext = ctx?.let {
                                DeferredContext(
                                    issuanceContext = it,
                                    keyAliases = deferredContext.keyAliases,
                                    clientAttestationPopKeyId = clientAttestationPopKeyId
                                )
                            } ?: deferredContext,
                            logger = logger
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
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        launch(executor, onIssueEvent) { coroutineScope, listener ->
            try {
                // 1. Load re-issuance config from storage
                val reissuanceConfig = loadReissuanceConfig(documentId)
                    ?: throw IllegalStateException("No re-issuance metadata found for document $documentId")

                // 2. Reconstruct AuthorizedRequest from stored config
                var authorizedRequest = ReissuanceIssuer.reconstructAuthorizedRequest(reissuanceConfig)

                // 3. Create Issuer using IssuerCreator (resolves issuer metadata)
                val issuer = issuerCreator.createIssuer(
                    reissuanceConfig.credentialIssuerId,
                    listOf(CredentialConfigurationIdentifier(reissuanceConfig.credentialConfigurationIdentifier))
                )
                val offer = Offer(issuer.credentialOffer)

                // 4. Create a new UnsignedDocument (fresh keys) via DocumentCreator
                //    This fires IssueEvent.DocumentRequiresCreateSettings so the app
                //    can provide CreateDocumentSettings (secure area, number of credentials, etc.)
                val documentCreator = DocumentCreator(
                    documentManager = documentManager,
                    listener = listener,
                    logger = logger
                )
                val requestMap = documentCreator.createDocuments(offer)

                listener(IssueEvent.Started(requestMap.size))

                // 5. Submit the issuance request using stored AuthorizedRequest
                //    (skips the authorization flow - uses refresh token instead)
                val submit = SubmitRequest(config, walletProvider, issuer, authorizedRequest)
                val response = submit.request(requestMap).also {
                    authorizedRequest = submit.authorizedRequest
                }

                // 6. Process the response: store new document, then delete old one
                val issuedDocumentIds = mutableListOf<DocumentId>()
                ProcessResponse(
                    documentManager = documentManager,
                    deferredContextFactory = DeferredContextFactory(issuer, authorizedRequest),
                    walletKeyManager = walletAttestationKeyManager,
                    clientAttestationPopKeyId = issuerCreator.clientAttestationPopKeyId,
                    listener = listener,
                    issuedDocumentIds = issuedDocumentIds,
                    logger = logger,
                    authorizedRequest = authorizedRequest,
                    issuer = issuer,
                    documentToConfigurationMap = requestMap,
                    dpopKeyAlias = issuerCreator.dpopKeyAlias,
                    config = config,
                    clientAuthentication = issuerCreator.clientAuthentication,
                ).process(response)

                // 7. If new document(s) issued successfully, delete the old document
                //    and migrate the re-issuance metadata
                if (issuedDocumentIds.isNotEmpty()) {
                    // Delete the old document
                    documentManager.deleteDocumentById(documentId)
                    logger?.d(TAG, "Deleted old document $documentId after re-issuance")

                    // Migrate re-issuance metadata: delete old entry
                    // (new entries were already stored by ProcessResponse.storeReissuanceMetadata)
                    CoroutineScope(Dispatchers.IO).launch {
                        deleteReissuanceMetadata(documentId)
                    }
                }

                listener(IssueEvent.Finished(issuedDocumentIds))

            } catch (e: Throwable) {
                listener(failure(e))
                coroutineScope.cancel("reissueDocument failed", e)
            }
        }
    }

    /**
     * Loads re-issuance configuration from storage.
     *
     * @param documentId The document ID
     * @return The re-issuance config, or null if not found
     */
    private suspend fun loadReissuanceConfig(documentId: DocumentId): ReissuanceConfig? {
        return runCatching {
            val storage = reissuanceStorage
            val table = storage.getTable(
                StorageTableSpec(
                    name = "reissuance_metadata",
                    supportPartitions = false,
                    supportExpiration = false
                )
            )

            val bytes = table.get(documentId) ?: return null
            ReissuanceConfig.fromByteArray(bytes.toByteArray())
        }.getOrNull()
    }

    /**
     * Deletes re-issuance metadata for the given document ID.
     *
     * @param documentId The document ID whose metadata should be deleted
     */
    private suspend fun deleteReissuanceMetadata(documentId: DocumentId) {
        runCatching {
            val storage = reissuanceStorage
            val table = storage.getTable(
                StorageTableSpec(
                    name = "reissuance_metadata",
                    supportPartitions = false,
                    supportExpiration = false
                )
            )
            table.delete(documentId)
            logger?.d(TAG, "Deleted re-issuance metadata for old document $documentId")
        }.onFailure { error ->
            logger?.d(TAG, "Failed to delete re-issuance metadata for $documentId: ${error.message}")
        }
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
            deferredContextFactory = DeferredContextFactory(issuer, authorizedRequest),
            walletKeyManager = walletAttestationKeyManager,
            clientAttestationPopKeyId = issuerCreator.clientAttestationPopKeyId,
            listener = listener,
            issuedDocumentIds = issuedDocumentIds,
            logger = logger,
            authorizedRequest = authorizedRequest,
            issuer = issuer,
            documentToConfigurationMap = requestMap,
            dpopKeyAlias = issuerCreator.dpopKeyAlias,
            config = config,
            clientAuthentication = issuerCreator.clientAuthentication,
        ).process(response)
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
