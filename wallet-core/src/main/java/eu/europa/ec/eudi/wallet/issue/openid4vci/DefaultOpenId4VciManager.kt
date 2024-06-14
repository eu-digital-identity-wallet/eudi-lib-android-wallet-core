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
import android.util.Log
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

    private val offerUriCache = mutableMapOf<String, Offer>()

    override fun issueDocumentByDocType(
        docType: String,
        executor: Executor?,
        authorizationHandler: AuthorizationHandler,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
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
                doIssueDocumentByOffer(offer, config, authorizationHandler, listener)

            } catch (e: Throwable) {
                Log.e(TAG, "issueDocumentByDocType failed", e)
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByDocType failed", e)
            }
        }
    }

    override fun issueDocumentByOffer(
        offer: Offer,
        executor: Executor?,
        authorizationHandler: AuthorizationHandler,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
            try {
                doIssueDocumentByOffer(offer, config, authorizationHandler, listener)
            } catch (e: Throwable) {
                Log.e(TAG, "issueDocumentByOffer failed", e)
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByOffer failed", e)
            }
        }
    }


    override fun issueDocumentByOfferUri(
        offerUri: String,
        executor: Executor?,
        authorizationHandler: AuthorizationHandler,
        onIssueEvent: OpenId4VciManager.OnIssueEvent
    ) {
        launch(onIssueEvent.wrap(executor)) { coroutineScope, listener ->
            try {
                val offer = offerUriCache[offerUri].also {
                    Log.d(TAG, "OfferUri $offerUri cache hit")
                } ?: CredentialOfferRequestResolver().resolve(offerUri).getOrThrow()
                    .let {
                        DefaultOffer(
                            it, Compose(
                                MsoMdocFormatFilter,
                                ProofTypeFilter(config.proofTypes)
                            )
                        )
                    }.also { offerUriCache[offerUri] = it }
                doIssueDocumentByOffer(offer, config, authorizationHandler, listener)
            } catch (e: Throwable) {
                Log.e(TAG, "issueDocumentByOfferUri failed", e)
                listener(failure(e))
                coroutineScope.cancel("issueDocumentByOffer failed", e)
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
                val credentialOffer = CredentialOfferRequestResolver().resolve(offerUri).getOrThrow()
                val offer = DefaultOffer(credentialOffer, Compose(MsoMdocFormatFilter, ProofTypeFilter(config.proofTypes)))
                offerUriCache[offerUri] = offer
                Log.d(TAG, "OfferUri $offerUri resolved")
                callback(OfferResult.Success(offer))
                coroutineScope.cancel("resolveDocumentOffer succeeded")
            } catch (e: Throwable) {
                offerUriCache.remove(offerUri)
                Log.e(TAG, "OfferUri $offerUri resolution failed", e)
                callback(OfferResult.Failure(e))
                coroutineScope.cancel("resolveDocumentOffer failed", e)
            }
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
        authorizationHandler: AuthorizationHandler,
        onEvent: OpenId4VciManager.OnResult<IssueEvent>
    ) {
        offer as DefaultOffer
        val credentialOffer = offer.credentialOffer
        val issuer = Issuer.make(config.toOpenId4VCIConfig(), credentialOffer).getOrThrow()
        onEvent(IssueEvent.Started(offer.offeredDocuments.size))
        with(issuer) {
            val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
            val authResponse = authorizationHandler.doAuthorization(prepareAuthorizationCodeRequest).getOrThrow()
            val authorizedRequest = prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                AuthorizationCode(authResponse.authorizationCode),
                authResponse.serverState
            ).getOrThrow()

            val addedDocuments = mutableSetOf<DocumentId>()

            offer.offeredDocuments.forEach { item ->
                val issuanceRequest = documentManager
                    .createIssuanceRequest(item, config.useStrongBoxIfSupported)
                    .getOrThrow()

                Log.d(TAG, "Issuing document: ${issuanceRequest.documentId} for ${issuanceRequest.docType}")

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
        Log.d(TAG, "doIssueCredential payload: $payload")
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
        Log.d(TAG, "doRequestSingleNoProof for ${issuanceRequest.documentId}")
        when (val outcome = authRequest.requestSingle(payload).getOrThrow()) {
            is SubmittedRequest.InvalidProof -> {
                Log.d(TAG, "doRequestSingleNoProof invalid proof")
                doRequestSingleWithProof(
                    authRequest.handleInvalidProof(outcome.cNonce),
                    payload,
                    credentialConfiguration,
                    issuanceRequest,
                    addedDocuments,
                    onEvent
                )
            }

            is SubmittedRequest.Failed -> onEvent(IssueEvent.DocumentFailed(issuanceRequest, outcome.error))
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
        Log.d(TAG, "doRequestSingleWithProof for ${issuanceRequest.documentId}")
        val proofSigner = ProofSigner(issuanceRequest, credentialConfiguration, config.proofTypes).getOrThrow()
        Log.d(TAG, "doRequestSingleWithProof proofSigner: ${proofSigner::class.java.name}")
        try {
            when (val outcome = authRequest.requestSingle(payload, proofSigner.popSigner).getOrThrow()) {
                is SubmittedRequest.Failed -> onEvent(IssueEvent.DocumentFailed(issuanceRequest, outcome.error))
                is SubmittedRequest.InvalidProof -> onEvent(
                    IssueEvent.DocumentFailed(
                        issuanceRequest,
                        Exception(outcome.errorDescription)
                    )
                )

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
                    Log.d(TAG, "doRequestSingleWithProof userAuthStatus: $status")
                    val event = object : IssueEvent.DocumentRequiresUserAuth(issuanceRequest, status.cryptoObject) {
                        override fun resume() {
                            Log.d(TAG, "doRequestSingleWithProof resume from user auth")
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
                            Log.e(TAG, "doRequestSingleWithProof cancel from user auth")
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
            is IssuedCredential.Deferred -> onEvent(
                IssueEvent.DocumentFailed(
                    issuanceRequest,
                    Exception("Deferred credential not implemented yet"),
                )
            )

            is IssuedCredential.Issued -> {
                val cbor = Base64.getUrlDecoder().decode(issuedCredential.credential)

                Log.d(TAG, "storeIssuedCredential for ${issuanceRequest.documentId}")
                Log.d(TAG, "storeIssuedCredential cbor: ${Hex.toHexString(cbor)}")

                when (val addResult = documentManager.addDocument(issuanceRequest, cbor)) {
                    is AddDocumentResult.Failure -> {
                        documentManager.deleteDocumentById(issuanceRequest.documentId)
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

    private inline fun <R : OpenId4VciManager.OnResult<V>, reified V> R.logResult(): OpenId4VciManager.OnResult<V> {
        return OpenId4VciManager.OnResult { result: V ->
            when (result) {
                is IssueEvent.DocumentIssued -> Log.d(
                    TAG,
                    "${IssueEvent.DocumentIssued::class.java.name} for ${result.documentId}"
                )

                is IssueEvent.DocumentFailed -> Log.e(
                    TAG,
                    IssueEvent.DocumentFailed::class.java.simpleName,
                    result.cause
                )

                is IssueEvent.DocumentRequiresUserAuth -> Log.d(
                    TAG,
                    IssueEvent.DocumentRequiresUserAuth::class.java.simpleName
                )

                is IssueEvent.Started -> Log.d(
                    TAG,
                    "${IssueEvent.Started::class.java.name} for ${result.total} documents"
                )

                is IssueEvent.Finished -> Log.d(
                    TAG,
                    "${IssueEvent.Finished::class.java.name} for ${result.issuedDocuments}"
                )

                is IssueEvent.Failure -> Log.e(TAG, IssueEvent.Failure::class.java.simpleName, result.cause)
                is OfferResult.Failure -> Log.e(TAG, OfferResult.Failure::class.java.simpleName, result.error)
                is OfferResult.Success -> Log.d(TAG, "${OfferResult.Success::class.java.name} for ${result.offer}")
                else -> Log.d(TAG, V::class.java.simpleName)
            }
            this.onResult(result)
        }
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
