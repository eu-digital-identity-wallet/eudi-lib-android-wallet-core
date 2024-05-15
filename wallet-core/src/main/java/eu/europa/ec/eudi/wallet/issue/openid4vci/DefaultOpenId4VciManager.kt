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

    override fun issueDocumentByDocType(
        docType: String,
        config: OpenId4VciManager.Config?,
        executor: Executor?,
        onResult: OpenId4VciManager.OnIssueDocument
    ) {
        val configToUse = config ?: this.config
        clearStateThen {
            launch(onResult.wrap(executor)) { coroutineScope, callback ->
                try {
                    val credentialIssuerId = CredentialIssuerId(configToUse.issuerUrl).getOrThrow()
                    val (credentialIssuerMetadata, authorizationServerMetadata) = DefaultHttpClientFactory()
                        .use { client ->
                            Issuer.metaData(client, credentialIssuerId)
                        }
                    val (credentialConfigurationId, credentialConfiguration) =
                        credentialIssuerMetadata.credentialConfigurationsSupported.filter { (_, conf) ->
                            listOf(
                                FormatFilter,
                                DocTypeFilterFactory(docType),
                                ProofTypeFilter
                            ).all { filter -> filter(conf) }
                        }.entries.firstOrNull() ?: throw IllegalStateException("No suitable configuration found")

                    val credentialOffer = CredentialOffer(
                        credentialIssuerIdentifier = credentialIssuerId,
                        credentialIssuerMetadata = credentialIssuerMetadata,
                        authorizationServerMetadata = authorizationServerMetadata.first(),
                        credentialConfigurationIdentifiers = listOf(credentialConfigurationId)
                    )

                    val issuer = Issuer.make(configToUse.toOpenId4VCIConfig(), credentialOffer).getOrThrow()

                    with(issuer) {
                        val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
                        val authResponse = openBrowserForAuthorization(prepareAuthorizationCodeRequest).getOrThrow()
                        val authorizedRequest = prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                            AuthorizationCode(authResponse.authorizationCode),
                            authResponse.serverState
                        ).getOrThrow()

                        val issuanceRequest = documentManager
                            .createIssuanceRequest(credentialConfiguration)
                            .getOrThrow()
                        issueCredential(
                            authorizedRequest,
                            credentialConfigurationId,
                            credentialConfiguration,
                            issuanceRequest,
                            onResult
                        )
                    }
                } catch (e: Throwable) {
                    callback.onResult(IssueDocumentResult.Failure(e))
                    coroutineScope.cancel("issueDocumentByDocType failed", e)
                }
            }
        }
    }

    override fun issueDocumentByOffer(
        offer: Offer,
        config: OpenId4VciManager.Config?,
        executor: Executor?,
        onResult: OpenId4VciManager.OnIssueDocument
    ) {
        val configToUse = config ?: this.config
        clearStateThen {
            launch(onResult.wrap(executor)) { coroutineScope, callback ->
                try {
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

                            issueCredential(
                                authorizedRequest,
                                item.configurationIdentifier,
                                item.configuration,
                                issuanceRequest,
                                onResult
                            )

                        }
                    }
                } catch (e: Throwable) {
                    callback.onResult(IssueDocumentResult.Failure(e))
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
                callback(OfferResult.Success(DefaultOffer(credentialOffer)))
                coroutineScope.cancel("resolveDocumentOffer succeeded")
            } catch (e: Throwable) {
                callback(OfferResult.Failure(e))
                coroutineScope.cancel("resolveDocumentOffer failed", e)
            }
        }
    }

    override fun resumeWithAuthorization(intent: Intent) {
        suspendedAuthorization?.use { it.resumeFromIntent(intent) }
            ?: throw IllegalStateException("No authorization request to resume")
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

    private suspend fun Issuer.issueCredential(
        authRequest: AuthorizedRequest,
        credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest,
        onResult: OpenId4VciManager.OnIssueDocument
    ) {
        when (authRequest) {
            is AuthorizedRequest.NoProofRequired -> requestSingleNoProof(
                authRequest,
                credentialConfigurationIdentifier,
                credentialConfiguration,
                issuanceRequest,
                onResult
            )

            is AuthorizedRequest.ProofRequired -> requestSingleWithProof(
                authRequest,
                credentialConfigurationIdentifier,
                credentialConfiguration,
                issuanceRequest,
                onResult
            )
        }
    }

    private suspend fun Issuer.requestSingleNoProof(
        authRequest: AuthorizedRequest.NoProofRequired,
        credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest,
        onResult: OpenId4VciManager.OnIssueDocument
    ) {
        val payload = IssuanceRequestPayload.ConfigurationBased(credentialConfigurationIdentifier, null)
        when (val outcome = authRequest.requestSingle(payload).getOrThrow()) {
            is SubmittedRequest.Failed -> onResult(IssueDocumentResult.Failure(outcome.error))
            is SubmittedRequest.InvalidProof -> requestSingleWithProof(
                authRequest.handleInvalidProof(outcome.cNonce),
                credentialConfigurationIdentifier,
                credentialConfiguration,
                issuanceRequest,
                onResult
            )

            is SubmittedRequest.Success -> onResult(storeIssuedCredential(outcome, issuanceRequest))
        }
    }

    private suspend fun Issuer.requestSingleWithProof(
        authRequest: AuthorizedRequest.ProofRequired,
        credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
        credentialConfiguration: CredentialConfiguration,
        issuanceRequest: IssuanceRequest,
        onResult: OpenId4VciManager.OnIssueDocument
    ) {
        val proofSigner = ProofSigner(credentialConfiguration.proofTypesSupported, issuanceRequest).getOrThrow()
        try {
            val payload = IssuanceRequestPayload.ConfigurationBased(credentialConfigurationIdentifier, null)
            when (val outcome = authRequest.requestSingle(payload, proofSigner).getOrThrow()) {
                is SubmittedRequest.Failed -> onResult(IssueDocumentResult.Failure(outcome.error))
                is SubmittedRequest.InvalidProof -> onResult(IssueDocumentResult.Failure(IllegalStateException(outcome.errorDescription)))
                is SubmittedRequest.Success -> onResult(storeIssuedCredential(outcome, issuanceRequest))
            }
        } catch (e: Throwable) {
            when (val status = proofSigner.userAuthStatus) {
                is ProofSigner.UserAuthStatus.Required -> {
                    IssueDocumentResult.UserAuthRequired(
                        status.cryptoObject,
                        onResume = {
                            runBlocking {
                                requestSingleWithProof(
                                    authRequest,
                                    credentialConfigurationIdentifier,
                                    credentialConfiguration,
                                    issuanceRequest,
                                    onResult
                                )
                            }
                        },
                        onCancel = { onResult(IssueDocumentResult.Failure(e)) }
                    )
                }

                else -> throw e
            }
        }

    }

    private fun storeIssuedCredential(
        response: SubmittedRequest.Success,
        issuanceRequest: IssuanceRequest
    ): IssueDocumentResult {
        return when (val issuedCredential = response.credentials[0]) {
            is IssuedCredential.Deferred -> IssueDocumentResult.Failure(IllegalStateException("Deferred credential not implemented yet"))
            is IssuedCredential.Issued -> {
                val cbor = Base64.getUrlDecoder().decode(issuedCredential.credential)
                when (val addResult = documentManager.addDocument(issuanceRequest, cbor)) {
                    is AddDocumentResult.Failure -> {
                        documentManager.deleteDocumentById(issuanceRequest.documentId)
                        IssueDocumentResult.Failure(addResult.throwable)
                    }

                    is AddDocumentResult.Success -> IssueDocumentResult.Success(addResult.documentId)
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
