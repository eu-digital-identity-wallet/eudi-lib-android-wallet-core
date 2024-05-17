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
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import kotlinx.coroutines.*
import java.net.URI
import java.util.*
import java.util.concurrent.Executor

class DefaultOpenId4VciManager(
    private val context: Context,
    private val documentManager: DocumentManager,
    var config: OpenId4VciManager.Config
) : OpenId4VciManager {

    private var suspendedAuthorization: SuspendedAuthorization? = null
    private val offerUriCache = mutableMapOf<String, Offer>()

    override fun issueDocumentByDocType(
        docType: String,
        config: OpenId4VciManager.Config?,
        executor: Executor?,
        onEvent: OpenId4VciManager.OnIssueDocumentEvent
    ) {
        val configToUse = config ?: this.config
        clearStateThen {
            launch(onEvent.wrap(executor)) { coroutineScope, callback ->
                try {
                    val credentialIssuerId = CredentialIssuerId(configToUse.issuerUrl).getOrThrow()
                    val (credentialIssuerMetadata, authorizationServerMetadata) = DefaultHttpClientFactory()
                        .use { client ->
                            Issuer.metaData(client, credentialIssuerId)
                        }
                    val credentialConfigurationId =
                        credentialIssuerMetadata.credentialConfigurationsSupported.filter { (_, conf) ->
                            listOf(
                                FormatFilter,
                                DocTypeFilterFactory(docType),
                                ProofTypeFilter
                            ).all { filter -> filter(conf) }
                        }.keys.firstOrNull() ?: throw IllegalStateException("No suitable configuration found")

                    val credentialOffer = CredentialOffer(
                        credentialIssuerIdentifier = credentialIssuerId,
                        credentialIssuerMetadata = credentialIssuerMetadata,
                        authorizationServerMetadata = authorizationServerMetadata.first(),
                        credentialConfigurationIdentifiers = listOf(credentialConfigurationId)
                    )

                    val offer = DefaultOffer(credentialOffer)
                    doIssueDocumentByOffer(offer, configToUse, callback)

                } catch (e: Throwable) {
                    callback.onResult(IssueDocumentEvent.Failure(e))
                    coroutineScope.cancel("issueDocumentByDocType failed", e)
                }
            }
        }
    }

    override fun issueDocumentByOffer(
        offer: Offer,
        config: OpenId4VciManager.Config?,
        executor: Executor?,
        onEvent: OpenId4VciManager.OnIssueDocumentEvent
    ) {
        val configToUse = config ?: this.config
        clearStateThen {
            launch(onEvent.wrap(executor)) { coroutineScope, callback ->
                try {
                    doIssueDocumentByOffer(offer, configToUse, callback)
                } catch (e: Throwable) {
                    callback.onResult(IssueDocumentEvent.Failure(e))
                    coroutineScope.cancel("issueDocumentByOffer failed", e)
                }
            }
        }
    }


    override fun issueDocumentByOfferUri(
        offerUri: String,
        config: OpenId4VciManager.Config?,
        executor: Executor?,
        onEvent: OpenId4VciManager.OnIssueDocumentEvent
    ) {
        clearStateThen {
            launch(onEvent.wrap(executor)) { coroutineScope, callback ->
                try {
                    val offer = offerUriCache[offerUri]
                        ?: CredentialOfferRequestResolver().resolve(offerUri).getOrThrow()
                            .let { DefaultOffer(it) }
                    doIssueDocumentByOffer(offer, config, callback)
                } catch (e: Throwable) {
                    callback.onResult(IssueDocumentEvent.Failure(e))
                    coroutineScope.cancel("issueDocumentByOffer failed", e)
                }
            }
        }
    }

    override fun resolveDocumentOffer(
        offerUri: String,
        executor: Executor?,
        onResult: OpenId4VciManager.OnResolveDocumentOffer
    ) {
        launch(onResult.wrap(executor)) { coroutineScope, callback ->
            try {
                val credentialOffer = CredentialOfferRequestResolver().resolve(offerUri).getOrThrow()
                val offer = DefaultOffer(credentialOffer)
                offerUriCache[offerUri] = offer
                callback(OfferResult.Success(offer))
                coroutineScope.cancel("resolveDocumentOffer succeeded")
            } catch (e: Throwable) {
                offerUriCache.remove(offerUri)
                callback(OfferResult.Failure(e))
                coroutineScope.cancel("resolveDocumentOffer failed", e)
            }
        }
    }

    override fun resumeWithAuthorization(intent: Intent) {
        suspendedAuthorization?.use { it.resumeFromIntent(intent) }
            ?: throw IllegalStateException("No authorization request to resume")
    }

    private suspend fun doIssueDocumentByOffer(
        offer: Offer,
        config: OpenId4VciManager.Config?,
        onResult: OpenId4VciManager.OnResult<IssueDocumentEvent>
    ) {
        val configToUse = config ?: this.config
        offer as DefaultOffer
        val credentialOffer = offer.credentialOffer
        val issuer = Issuer.make(configToUse.toOpenId4VCIConfig(), credentialOffer).getOrThrow()
        with(issuer) {
            val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
            val authResponse = openBrowserForAuthorization(prepareAuthorizationCodeRequest).getOrThrow()
            val authorizedRequest = prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                AuthorizationCode(authResponse.authorizationCode),
                authResponse.serverState
            ).getOrThrow()

            offer.offeredDocuments.forEach { item ->
                val issuanceRequest = documentManager
                    .createIssuanceRequest(item)
                    .getOrThrow()

                val result = doIssueCredential(
                    authorizedRequest,
                    item.configurationIdentifier,
                    item.configuration,
                    issuanceRequest
                )
                onResult(result)
            }
            onResult(IssueDocumentEvent.Finished)
        }
    }

    private suspend fun openBrowserForAuthorization(prepareAuthorizationCodeRequest: AuthorizationRequestPrepared): Result<SuspendedAuthorization.Response> {
        val authorizationCodeUri =
            Uri.parse(prepareAuthorizationCodeRequest.authorizationCodeURL.value.toString())

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

    private suspend fun Issuer.doIssueCredential(
        authRequest: AuthorizedRequest,
        credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest
    ): IssueDocumentEvent {
        val payload = IssuanceRequestPayload.ConfigurationBased(credentialConfigurationIdentifier, null)
        return when (authRequest) {
            is AuthorizedRequest.NoProofRequired -> doRequestSingleNoProof(
                authRequest,
                payload,
                credentialConfiguration,
                issuanceRequest
            )

            is AuthorizedRequest.ProofRequired -> doRequestSingleWithProof(
                authRequest,
                payload,
                credentialConfiguration,
                issuanceRequest
            )
        }
    }

    private suspend fun Issuer.doRequestSingleNoProof(
        authRequest: AuthorizedRequest.NoProofRequired,
        payload: IssuanceRequestPayload,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest
    ): IssueDocumentEvent {
        return when (val outcome = authRequest.requestSingle(payload).getOrThrow()) {
            is SubmittedRequest.Failed -> IssueDocumentEvent.DocumentFailure(outcome.error, credentialConfiguration)
            is SubmittedRequest.InvalidProof -> doRequestSingleWithProof(
                authRequest.handleInvalidProof(outcome.cNonce),
                payload,
                credentialConfiguration,
                issuanceRequest
            )

            is SubmittedRequest.Success -> storeIssuedCredential(outcome, issuanceRequest)
        }
    }

    private suspend fun Issuer.doRequestSingleWithProof(
        authRequest: AuthorizedRequest.ProofRequired,
        payload: IssuanceRequestPayload,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest
    ): IssueDocumentEvent {
        val proofSigner = ProofSigner(credentialConfiguration.proofTypesSupported, issuanceRequest).getOrThrow()
        return try {
            when (val outcome = authRequest.requestSingle(payload, proofSigner).getOrThrow()) {
                is SubmittedRequest.Failed -> IssueDocumentEvent.DocumentFailure(outcome.error, credentialConfiguration)
                is SubmittedRequest.InvalidProof -> IssueDocumentEvent.DocumentFailure(
                    IllegalStateException(outcome.errorDescription),
                    credentialConfiguration
                )

                is SubmittedRequest.Success -> storeIssuedCredential(outcome, issuanceRequest)
            }
        } catch (e: Throwable) {
            when (val status = proofSigner.userAuthStatus) {
                is ProofSigner.UserAuthStatus.Required -> {
                    IssueDocumentEvent.UserAuthRequired(
                        credentialConfiguration,
                        status.cryptoObject,
                        onResume = {
                            runBlocking {
                                doRequestSingleWithProof(
                                    authRequest,
                                    payload,
                                    credentialConfiguration,
                                    issuanceRequest
                                )
                            }
                        },
                        onCancel = {
                            documentManager.deleteDocumentById(issuanceRequest.documentId)
                            IssueDocumentEvent.UserAuthCanceled(credentialConfiguration)
                        }
                    )
                }

                else -> IssueDocumentEvent.DocumentFailure(e, credentialConfiguration)
            }
        }
    }

    private fun storeIssuedCredential(
        response: SubmittedRequest.Success,
        issuanceRequest: IssuanceRequest
    ): IssueDocumentEvent {
        return when (val issuedCredential = response.credentials[0]) {
            is IssuedCredential.Deferred -> IssueDocumentEvent.DocumentFailure(
                IllegalStateException("Deferred credential not implemented yet"),
                issuanceRequest
            )

            is IssuedCredential.Issued -> {
                val cbor = Base64.getUrlDecoder().decode(issuedCredential.credential)
                when (val addResult = documentManager.addDocument(issuanceRequest, cbor)) {
                    is AddDocumentResult.Failure -> {
                        documentManager.deleteDocumentById(issuanceRequest.documentId)
                        IssueDocumentEvent.DocumentFailure(addResult.throwable, issuanceRequest)
                    }

                    is AddDocumentResult.Success -> IssueDocumentEvent.DocumentIssued(addResult.documentId)
                }
            }
        }
    }

    private fun <R : OpenId4VciManager.OnResult<V>, V> R.wrap(executor: Executor?): OpenId4VciManager.OnResult<V> {
        return OpenId4VciManager.OnResult { result: V ->
            (executor ?: context.mainExecutor()).execute {
                this@wrap.onResult(result)
            }
        }
    }

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

    companion object {
        private const val TAG = "DefaultOpenId4VciManage"

        private fun OpenId4VciManager.Config.toOpenId4VCIConfig(): OpenId4VCIConfig {
            return OpenId4VCIConfig(
                clientId = clientId,
                authFlowRedirectionURI = URI.create(authFlowRedirectionURI),
                keyGenerationConfig = KeyGenerationConfig(Curve.P_256, 2048),
                credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED
            )
        }
    }
}
