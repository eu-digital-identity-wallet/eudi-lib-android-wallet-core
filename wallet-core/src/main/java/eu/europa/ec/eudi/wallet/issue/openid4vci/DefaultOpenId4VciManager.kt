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
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.AddDocumentResult
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.Compose
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.DocTypeFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.MsoMdocFormatFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.ProofTypeFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.ProofSigner.UserAuthStatus
import kotlinx.coroutines.*
import org.bouncycastle.util.encoders.Hex
import java.net.URI
import java.util.*
import java.util.concurrent.Executor

/**
 * Default implementation of [OpenId4VciManager].
 * @property context the context
 * @property documentManager the document manager
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
    var config: OpenId4VciManager.Config
) : OpenId4VciManager {

    private var suspendedAuthorization: SuspendedAuthorization? = null
    private val offerUriCache = mutableMapOf<String, Offer>()

    override fun issueDocumentByDocType(
        docType: String,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        clearStateThen {
            launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
                try {
                    val credentialIssuerId = CredentialIssuerId(config.issuerUrl).getOrThrow()
                    val (credentialIssuerMetadata, authorizationServerMetadata) = DefaultHttpClientFactory()
                        .use { client ->
                            Issuer.metaData(client, credentialIssuerId)
                        }
                    val credentialConfigurationFilter = Compose(
                        DocTypeFilter(docType),
                        ProofTypeFilter(config.proofTypes)
                    )
                    val credentialConfigurationId =
                        credentialIssuerMetadata.credentialConfigurationsSupported.filterValues { conf ->
                            credentialConfigurationFilter(conf)
                        }.keys.firstOrNull() ?: throw IllegalStateException("No suitable configuration found")

                    val credentialOffer = CredentialOffer(
                        credentialIssuerIdentifier = credentialIssuerId,
                        credentialIssuerMetadata = credentialIssuerMetadata,
                        authorizationServerMetadata = authorizationServerMetadata.first(),
                        credentialConfigurationIdentifiers = listOf(credentialConfigurationId)
                    )

                    val offer = DefaultOffer(credentialOffer, credentialConfigurationFilter)
                    doIssueDocumentByOffer(offer, config, listener)

                } catch (e: Throwable) {
                    logError("issueDocumentByDocType failed", e)
                    listener(failure(e))
                    coroutineScope.cancel("issueDocumentByDocType failed", e)
                }
            }
        }
    }

    override fun issueDocumentByOffer(
        offer: Offer,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        clearStateThen {
            launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
                try {
                    doIssueDocumentByOffer(offer, config, listener)
                } catch (e: Throwable) {
                    logError("issueDocumentByOffer failed", e)
                    listener(failure(e))
                    coroutineScope.cancel("issueDocumentByOffer failed", e)
                }
            }
        }
    }


    override fun issueDocumentByOfferUri(
        offerUri: String,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        clearStateThen {
            launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
                try {
                    val offer = offerUriCache[offerUri].also {
                        logDebug("OfferUri $offerUri cache hit")
                    } ?: CredentialOfferRequestResolver().resolve(offerUri).getOrThrow()
                        .let {
                            DefaultOffer(
                                it, Compose(
                                    MsoMdocFormatFilter,
                                    ProofTypeFilter(config.proofTypes)
                                )
                            )
                        }.also { offerUriCache[offerUri] = it }
                    doIssueDocumentByOffer(offer, config, listener)
                } catch (e: Throwable) {
                    logError("issueDocumentByOfferUri failed", e)
                    listener(failure(e))
                    coroutineScope.cancel("issueDocumentByOffer failed", e)
                }
            }
        }
    }

    override fun resolveDocumentOffer(
        offerUri: String,
        executor: Executor?,
        onResolvedOffer: OpenId4VciManager.OnResolvedOffer
    ) {
        launch(onResolvedOffer.wrap(executor)) { coroutineScope, callback ->
            try {
                val credentialOffer =
                    CredentialOfferRequestResolver(config.ktorHttpClientFactoryWithLogging).resolve(offerUri)
                        .getOrThrow()
                val offer =
                    DefaultOffer(credentialOffer, Compose(MsoMdocFormatFilter, ProofTypeFilter(config.proofTypes)))
                offerUriCache[offerUri] = offer
                logDebug("OfferUri $offerUri resolved")
                callback(OfferResult.Success(offer))
                coroutineScope.cancel("resolveDocumentOffer succeeded")
            } catch (e: Throwable) {
                offerUriCache.remove(offerUri)
                logError("OfferUri $offerUri resolution failed", e)
                callback(OfferResult.Failure(e))
                coroutineScope.cancel("resolveDocumentOffer failed", e)
            }
        }
    }

    override fun resumeWithAuthorization(intent: Intent) {
        logDebug("resumeWithAuthorization: ${intent.data}")
        suspendedAuthorization?.use { it.resumeFromIntent(intent) }
            ?: run {
                throw IllegalStateException("No authorization request to resume")
            }
    }

    override fun resumeWithAuthorization(uri: String) {
        logDebug("resumeWithAuthorization: $uri")
        suspendedAuthorization?.use { it.resumeFromUri(uri) }
            ?: run {
                throw IllegalStateException("No authorization request to resume")
            }
    }

    override fun resumeWithAuthorization(uri: Uri) {
        logDebug("resumeWithAuthorization: $uri")
        suspendedAuthorization?.use { it.resumeFromUri(uri) }
            ?: run {
                throw IllegalStateException("No authorization request to resume")
            }
    }

    /**
     * Issues a document by the given offer.
     * @param offer The offer.
     * @param config The configuration.
     * @param onEvent The event listener.
     */
    private suspend fun doIssueDocumentByOffer(
        offer: Offer,
        config: OpenId4VciManager.Config,
        onEvent: OpenId4VciManager.OnResult<IssueEvent>
    ) {
        offer as DefaultOffer
        val credentialOffer = offer.credentialOffer
        val issuer =
            Issuer.make(config.toOpenId4VCIConfig(), credentialOffer, config.ktorHttpClientFactoryWithLogging)
                .getOrThrow()
        onEvent(IssueEvent.Started(offer.offeredDocuments.size))
        with(issuer) {
            val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
            val authResponse = openBrowserForAuthorization(prepareAuthorizationCodeRequest).getOrThrow()
            val authorizedRequest = prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                AuthorizationCode(authResponse.authorizationCode),
                authResponse.serverState
            ).getOrThrow()

            val addedDocuments = mutableSetOf<DocumentId>()

            offer.offeredDocuments.forEach { item ->
                val issuanceRequest = documentManager
                    .createIssuanceRequest(item, config.useStrongBoxIfSupported)
                    .getOrThrow()

                logDebug("Issuing document: ${issuanceRequest.documentId} for ${issuanceRequest.docType}")

                doIssueCredential(
                    authorizedRequest,
                    item.configurationIdentifier,
                    item.configuration,
                    issuanceRequest,
                    addedDocuments,
                    onEvent
                )
            }
            onEvent(IssueEvent.Finished(addedDocuments.toList()))
        }
    }

    /**
     * Opens a browser for authorization.
     * @param prepareAuthorizationCodeRequest The prepared authorization request.
     * @return The authorization response wrapped in a [Result].
     */
    private suspend fun openBrowserForAuthorization(prepareAuthorizationCodeRequest: AuthorizationRequestPrepared): Result<SuspendedAuthorization.Response> {
        val authorizationCodeUri =
            Uri.parse(prepareAuthorizationCodeRequest.authorizationCodeURL.value.toString())
        logDebug("openBrowserForAuthorization: $authorizationCodeUri")
        return suspendCancellableCoroutine { continuation ->
            suspendedAuthorization = SuspendedAuthorization(continuation)
            continuation.invokeOnCancellation {
                suspendedAuthorization = null
            }
            context.startActivity(Intent(ACTION_VIEW, authorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Issues a credential.
     * @param authRequest The authorized request.
     * @param credentialConfigurationIdentifier The credential configuration identifier.
     * @param credentialConfiguration The credential configuration.
     * @param issuanceRequest The issuance request.
     * @param addedDocuments The added documents.
     * @param onEvent The event listener.
     * @throws Exception If an error occurs during the issuance.
     * @receiver The issuer.
     */
    private suspend fun Issuer.doIssueCredential(
        authRequest: AuthorizedRequest,
        credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest,
        addedDocuments: MutableSet<DocumentId>,
        onEvent: OpenId4VciManager.OnResult<IssueEvent>
    ) {
        val claimSet = null
        val payload = IssuanceRequestPayload.ConfigurationBased(credentialConfigurationIdentifier, claimSet)
        logDebug("doIssueCredential payload: $payload")
        when (authRequest) {
            is AuthorizedRequest.NoProofRequired -> doRequestSingleNoProof(
                authRequest,
                payload,
                credentialConfiguration,
                issuanceRequest,
                addedDocuments,
                onEvent
            )

            is AuthorizedRequest.ProofRequired -> doRequestSingleWithProof(
                authRequest,
                payload,
                credentialConfiguration,
                issuanceRequest,
                addedDocuments,
                onEvent
            )
        }
    }

    /**
     * Requests a single document without proof.
     * @param authRequest The authorized request.
     * @param payload The issuance request payload.
     * @param credentialConfiguration The credential configuration.
     * @param issuanceRequest The issuance request.
     * @param addedDocuments The added documents.
     * @param onEvent The event listener.
     * @receiver The issuer.
     * @throws Exception If an error occurs during the issuance.
     */
    private suspend fun Issuer.doRequestSingleNoProof(
        authRequest: AuthorizedRequest.NoProofRequired,
        payload: IssuanceRequestPayload,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest,
        addedDocuments: MutableSet<DocumentId>,
        onEvent: OpenId4VciManager.OnResult<IssueEvent>
    ) {
        logDebug("doRequestSingleNoProof for ${issuanceRequest.documentId}")
        when (val outcome = authRequest.requestSingle(payload).getOrThrow()) {
            is SubmittedRequest.InvalidProof -> {
                logDebug("doRequestSingleNoProof invalid proof")
                doRequestSingleWithProof(
                    authRequest.handleInvalidProof(outcome.cNonce),
                    payload,
                    credentialConfiguration,
                    issuanceRequest,
                    addedDocuments,
                    onEvent
                )
            }

            is SubmittedRequest.Failed -> {
                clearFailedIssuance(issuanceRequest)
                onEvent(IssueEvent.DocumentFailed(issuanceRequest, outcome.error))
            }

            is SubmittedRequest.Success -> storeIssuedCredential(
                outcome.credentials[0],
                issuanceRequest,
                onEvent,
                addedDocuments
            )
        }
    }

    /**
     * Requests a single document with proof.
     * @param authRequest The authorized request.
     * @param payload The issuance request payload.
     * @param credentialConfiguration The credential configuration.
     * @param issuanceRequest The issuance request.
     * @param addedDocuments The added documents.
     * @param onEvent The event listener.
     * @receiver The issuer.
     * @throws Exception If an error occurs during the issuance.
     */
    private suspend fun Issuer.doRequestSingleWithProof(
        authRequest: AuthorizedRequest.ProofRequired,
        payload: IssuanceRequestPayload,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest,
        addedDocuments: MutableSet<DocumentId>,
        onEvent: OpenId4VciManager.OnResult<IssueEvent>
    ) {
        logDebug("doRequestSingleWithProof for ${issuanceRequest.documentId}")
        val proofSigner = ProofSigner(issuanceRequest, credentialConfiguration, config.proofTypes).getOrThrow()
        logDebug("doRequestSingleWithProof proofSigner: ${proofSigner::class.java.name}")
        try {
            when (val outcome = authRequest.requestSingle(payload, proofSigner.popSigner).getOrThrow()) {
                is SubmittedRequest.Failed -> {
                    clearFailedIssuance(issuanceRequest)
                    onEvent(IssueEvent.DocumentFailed(issuanceRequest, outcome.error))
                }

                is SubmittedRequest.InvalidProof -> {
                    clearFailedIssuance(issuanceRequest)
                    onEvent(
                        IssueEvent.DocumentFailed(
                            issuanceRequest,
                            Exception(outcome.errorDescription)
                        )
                    )
                }

                is SubmittedRequest.Success -> storeIssuedCredential(
                    outcome.credentials[0],
                    issuanceRequest,
                    onEvent,
                    addedDocuments
                )
            }

        } catch (e: Throwable) {
            when (val status = proofSigner.userAuthStatus) {
                is UserAuthStatus.Required -> {
                    logDebug("doRequestSingleWithProof userAuthStatus: $status")
                    val event = object : IssueEvent.DocumentRequiresUserAuth(issuanceRequest, status.cryptoObject) {
                        override fun resume() {
                            logDebug("doRequestSingleWithProof resume from user auth")
                            runBlocking {
                                doRequestSingleWithProof(
                                    authRequest,
                                    payload,
                                    credentialConfiguration,
                                    issuanceRequest,
                                    addedDocuments,
                                    onEvent
                                )
                            }
                        }

                        override fun cancel() {
                            logError("doRequestSingleWithProof cancel from user auth", e)
                            onEvent(IssueEvent.DocumentFailed(issuanceRequest, e.cause ?: e))
                        }
                    }
                    onEvent(event)
                }

                else -> onEvent(IssueEvent.DocumentFailed(issuanceRequest, e))
            }
        }
    }

    /**
     * Stores the issued credential.
     * @param issuedCredential The issued credential.
     * @param issuanceRequest The issuance request.
     * @param onEvent The event listener.
     * @param addedDocuments The added documents.
     */
    private fun storeIssuedCredential(
        issuedCredential: IssuedCredential,
        issuanceRequest: IssuanceRequest,
        onEvent: OpenId4VciManager.OnResult<IssueEvent>,
        addedDocuments: MutableSet<DocumentId>
    ) {
        when (issuedCredential) {
            is IssuedCredential.Deferred -> {
                clearFailedIssuance(issuanceRequest)
                onEvent(
                    IssueEvent.DocumentFailed(
                        issuanceRequest,
                        Exception("Deferred credential not implemented yet"),
                    )
                )
            }

            is IssuedCredential.Issued -> {
                val cbor = Base64.getUrlDecoder().decode(issuedCredential.credential)

                logDebug("storeIssuedCredential for ${issuanceRequest.documentId}")
                logDebug("storeIssuedCredential cbor: ${Hex.toHexString(cbor)}")

                when (val addResult = documentManager.addDocument(issuanceRequest, cbor)) {
                    is AddDocumentResult.Failure -> {
                        clearFailedIssuance(issuanceRequest)
                        onEvent(IssueEvent.DocumentFailed(issuanceRequest, addResult.throwable))
                    }

                    is AddDocumentResult.Success -> {
                        addedDocuments += addResult.documentId
                        onEvent(IssueEvent.DocumentIssued(issuanceRequest, addResult.documentId))
                    }
                }
            }
        }
    }

    /**
     * Deletes the pending issuance request. Ensures that the document is not left in a pending state.
     * @param issuanceRequest The issuance request.
     */
    private fun clearFailedIssuance(issuanceRequest: IssuanceRequest) {
        val document = documentManager.getDocumentById(issuanceRequest.documentId)
        if (document == null || document.nameSpaces.isEmpty()) {
            logDebug("clearFailedIssuance for ${issuanceRequest.documentId}")
            documentManager.deleteDocumentById(issuanceRequest.documentId)
        }
    }

    /**
     * Wraps the given [OpenId4VciManager.OnResult] with the given [Executor].
     * @param executor The executor.
     * @receiver The [OpenId4VciManager.OnResult].
     * @return The wrapped [OpenId4VciManager.OnResult].
     */
    private inline fun <R : OpenId4VciManager.OnResult<V>, reified V> R.wrap(executor: Executor?): OpenId4VciManager.OnResult<V> {
        return OpenId4VciManager.OnResult { result: V ->
            (executor ?: context.mainExecutor()).execute {
                this@wrap.onResult(result)
            }
        }.logResult()
    }

    /**
     * Wraps the given [OpenId4VciManager.OnResult] with debug logging.
     */
    private inline fun <R : OpenId4VciManager.OnResult<V>, reified V> R.logResult(): OpenId4VciManager.OnResult<V> {
        return if (config.errorLoggingStatus) {
            OpenId4VciManager.OnResult { result: V ->
                when (result) {
                    is IssueEvent.Started,
                    is IssueEvent.DocumentIssued,
                    is IssueEvent.DocumentRequiresUserAuth,
                    is IssueEvent.Finished,
                    is OfferResult.Success -> logDebug("$result")

                    is IssueEvent.DocumentFailed -> logError("$result", result.cause)
                    is IssueEvent.Failure -> logError("$result", result.cause)
                    is OfferResult.Failure -> logError("$result", result.error)
                    else -> logDebug(V::class.java.simpleName)
                }
                this.onResult(result)
            }
        } else this
    }

    /**
     * Launches a coroutine.
     * @param onResult The result listener.
     * @param block The coroutine block.
     */
    private fun <R : OpenId4VciManager.OnResult<V>, V> launch(
        onResult: R,
        block: suspend (coroutineScope: CoroutineScope, onResult: R) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch { block(scope, onResult) }
    }

    private fun clearStateThen(block: () -> Unit) {
        suspendedAuthorization?.close()
        suspendedAuthorization = null
        block()
    }

    private fun logDebug(message: String) {
        if (config.basicLoggingStatus) {
            debugLog(TAG, message)
        }
    }

    private fun logError(message: String, throwable: Throwable) {
        if (config.errorLoggingStatus) {
            debugLog(TAG, message, throwable)
        }
    }

    companion object {
        internal const val TAG = "DefaultOpenId4VciManage"

        /**
         * Converts the [OpenId4VciManager.Config] to [OpenId4VCIConfig].
         * @receiver The [OpenId4VciManager.Config].
         * @return The [OpenId4VCIConfig].
         */
        private fun OpenId4VciManager.Config.toOpenId4VCIConfig(): OpenId4VCIConfig {
            return OpenId4VCIConfig(
                clientId = clientId,
                authFlowRedirectionURI = URI.create(authFlowRedirectionURI),
                keyGenerationConfig = KeyGenerationConfig(Curve.P_256, 2048),
                credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
                dPoPSigner = if (useDPoPIfSupported) JWSDPoPSigner().getOrNull() else null,
                parUsage = when (parUsage) {
                    OpenId4VciManager.Config.ParUsage.IF_SUPPORTED -> ParUsage.IfSupported
                    OpenId4VciManager.Config.ParUsage.REQUIRED -> ParUsage.Required
                    OpenId4VciManager.Config.ParUsage.NEVER -> ParUsage.Never
                    else -> ParUsage.IfSupported
                }
            )
        }
    }
}
