/*
 *  Copyright (c) 2023-2024 European Commission
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

package eu.europa.ec.eudi.wallet.document.issue.openid4vci

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.AddDocumentResult
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.issue.IssueDocumentResult
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciManager.AuthorizationCallback
import eu.europa.ec.eudi.wallet.internal.coseBytes
import eu.europa.ec.eudi.wallet.internal.coseDebug
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.internal.openId4VciAuthorizationRedirectUri
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Manager for issuing documents using OpenID4VCI.
 *
 *
 * @param context the application context
 * @param config the configuration for OpenID4VCI
 * @param documentManager the document manager to use for issuing documents
 */
class OpenId4VciManager(
    context: Context,
    private val config: OpenId4VciConfig,
    private val documentManager: DocumentManager,
) {
    private val context = context.applicationContext

    private val openId4VCIConfig
        get() = OpenId4VCIConfig(
            clientId = config.clientId,
            authFlowRedirectionURI = context.openId4VciAuthorizationRedirectUri,
            keyGenerationConfig = KeyGenerationConfig(Curve.P_256, 2048),
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED
        )

    private val proofAlgorithm
        get() = JWSAlgorithm.ES256.name
    /**
     * Issues a document of the given type.
     *
     * @param docType the type of the document to issue (e.g. "eu.europa.ec.eudiw.pid.1")
     * @param executor the executor in which the callback will be invoked.
     * @param callback the callback to be invoked when the result is available
     */
    @JvmOverloads
    fun issueDocument(
        docType: String,
        executor: Executor? = null,
        callback: OnIssueCallback
    ) {
        val ioScope = CoroutineScope(Job() + Dispatchers.IO)
        val onResult = { result: IssueDocumentResult ->
            if (result is IssueDocumentResult.Failure || result is IssueDocumentResult.Success) {
                if (ioScope.isActive) {
                    ioScope.cancel()
                    Log.d(TAG, "Cancel CoroutineScope $ioScope")
                }
            }
            (executor ?: context.mainExecutor()).execute {
                callback.onResult(result)
            }
        }
        ioScope.launch {
            try {
                val credentialIssuerId = CredentialIssuerId(config.issuerUrl).getOrThrow()

                val (credentialIssuerMetadata, authorizationServerMetadata) = issuerMetadata(
                    credentialIssuerId
                ).getOrThrow()

                val (credentialConfigurationId, credentialConfiguration) =
                    credentialIssuerMetadata.getCredentialConfigurationByDocType(
                        docType
                    ).getOrThrow()

                val credentialOffer = CredentialOffer(
                    credentialIssuerIdentifier = credentialIssuerId,
                    credentialIssuerMetadata = credentialIssuerMetadata,
                    authorizationServerMetadata = authorizationServerMetadata.first(),
                    credentialConfigurationIdentifiers = listOf(credentialConfigurationId)
                )

                val issuer = Issuer.make(openId4VCIConfig, credentialOffer).getOrThrow()

                val prepareAuthorizationCodeRequest: AuthorizationRequestPrepared =
                    issuer.prepareAuthorizationRequest().getOrThrow()

                val (authorizationCode, serverState) =
                    prepareAuthorizationCodeRequest.openBrowserForAuthorizationCode().getOrThrow()

                val authorizedRequest = with(issuer) {
                    prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                        AuthorizationCode(authorizationCode), serverState
                    )
                }.getOrThrow()

                val issuanceRequest = credentialConfiguration.createIssuanceRequest(docType)

                val issuanceRequestPayload = IssuanceRequestPayload.ConfigurationBased(
                    credentialConfigurationId,
                    null
                )

                issuer.handleAuthorizedRequest(
                    authorizedRequest,
                    issuanceRequestPayload,
                    issuanceRequest,
                    onResult
                )

            } catch (e: Throwable) {
                Log.e(TAG, "issueDocument", e)
                onResult(IssueDocumentResult.Failure(e))
            }
        }
    }

    private suspend fun AuthorizationRequestPrepared.openBrowserForAuthorizationCode(): Result<Pair<String, String>> {
        try {
            val authorizationCodeUri =
                Uri.parse(authorizationCodeURL.value.toString())

            return suspendCoroutine { continuation ->
                OpenId4VciAuthorizeActivity.callback =
                    AuthorizationCallback { authorizationCode, severState ->
                        authorizationCode?.let { code ->
                            severState?.let { state ->
                                continuation.resume(Result.success(code to state))
                            } ?: run {
                                continuation.resume(Result.failure(IllegalStateException("No server state received or server state is null")))
                            }
                        } ?: run {
                            continuation.resume(Result.failure(IllegalStateException("No authorization code received or authorization code is null")))
                        }

                    }
                context.startActivity(Intent(ACTION_VIEW, authorizationCodeUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (e: Throwable) {
            return Result.failure(e)
        }
    }

    private fun CredentialConfiguration.createIssuanceRequest(docType: String): IssuanceRequest =
        documentManager
            .createIssuanceRequest(docType, true)
            .result
            .getOrThrow()
            .apply { name = this@createIssuanceRequest.name }
            .also {
                Log.d(TAG, "Document's PublicKey in COSE Bytes: ${it.publicKey.coseBytes}")
                Log.d(TAG, "Document's PublicKey in COSE: ${it.publicKey.coseDebug}")
            }


    private suspend fun Issuer.handleAuthorizedRequest(
        authorizedRequest: AuthorizedRequest,
        issuanceRequestPayload: IssuanceRequestPayload,
        issuanceRequest: IssuanceRequest,
        onResult: (IssueDocumentResult) -> Unit
    ) {
        when (authorizedRequest) {
            is AuthorizedRequest.ProofRequired -> handleProofRequired(
                authorizedRequest,
                issuanceRequestPayload,
                issuanceRequest,
                onResult,
            )

            is AuthorizedRequest.NoProofRequired -> handleNoProofRequired(
                authorizedRequest,
                issuanceRequestPayload,
                issuanceRequest,
                onResult,
            )
        }
    }


    private suspend fun Issuer.handleProofRequired(
        authRequest: AuthorizedRequest.ProofRequired,
        issuanceRequestPayload: IssuanceRequestPayload,
        issuanceRequest: IssuanceRequest,
        onResult: (IssueDocumentResult) -> Unit
    ) {
        val proofSigner = ProofSigner(issuanceRequest, proofAlgorithm)
        onResult(
            try {
                when (val outcome =
                    authRequest.requestSingle(issuanceRequestPayload, proofSigner).getOrThrow()) {
                    is SubmittedRequest.Failed -> IssueDocumentResult.Failure(outcome.error)
                    is SubmittedRequest.InvalidProof -> IssueDocumentResult.Failure(
                        IllegalStateException(outcome.errorDescription)
                    )

                    is SubmittedRequest.Success -> addDocument(outcome, issuanceRequest)
                }
            } catch (e: Throwable) {
                when (val userAuthRequired = proofSigner.userAuthRequired) {
                    ProofSigner.UserAuthRequired.No -> IssueDocumentResult.Failure(e)
                    is ProofSigner.UserAuthRequired.Yes -> IssueDocumentResult.UserAuthRequired(
                        userAuthRequired.cryptoObject,
                        onResume = {
                            runBlocking {
                                handleProofRequired(
                                    authRequest,
                                    issuanceRequestPayload,
                                    issuanceRequest,
                                    onResult
                                )
                            }
                        },
                        onCancel = { onResult(IssueDocumentResult.Failure(e)) }
                    )
                }
            })
    }

    private suspend fun Issuer.handleNoProofRequired(
        authRequest: AuthorizedRequest.NoProofRequired,
        issuanceRequestPayload: IssuanceRequestPayload,
        issuanceRequest: IssuanceRequest,
        onResult: (IssueDocumentResult) -> Unit
    ) {
        when (val outcome =
            authRequest.requestSingle(issuanceRequestPayload).getOrThrow()) {
            is SubmittedRequest.Failed -> onResult(IssueDocumentResult.Failure(outcome.error))
            is SubmittedRequest.InvalidProof -> handleProofRequired(
                authRequest.handleInvalidProof(outcome.cNonce),
                issuanceRequestPayload,
                issuanceRequest,
                onResult
            )

            is SubmittedRequest.Success -> onResult(addDocument(outcome, issuanceRequest))
        }
    }

    private fun addDocument(
        outcome: SubmittedRequest.Success,
        issuanceRequest: IssuanceRequest,
    ) = when (val issuedCred = outcome.credentials[0]) {
        is IssuedCredential.Deferred -> TODO("Deferred issuing is not implemented yet")
        is IssuedCredential.Issued -> {
            val cbor = Base64.getUrlDecoder().decode(issuedCred.credential)
            when (val addResult =
                documentManager.addDocument(issuanceRequest, cbor)) {
                is AddDocumentResult.Success -> IssueDocumentResult.Success(addResult.documentId)
                is AddDocumentResult.Failure -> {
                    documentManager.deleteDocumentById(issuanceRequest.documentId)
                    IssueDocumentResult.Failure(addResult.throwable)
                }
            }
        }
    }

    private val CredentialConfiguration.name: String
        get() = context.resources.configuration.locales[0].let { locale ->
            (display.firstOrNull { it.locale == locale }
                ?: display.first()).name
        }

    private companion object {
        private const val TAG = "OpenId4VciManager"
        private suspend fun issuerMetadata(credentialIssuerId: CredentialIssuerId): Result<Pair<CredentialIssuerMetadata, List<CIAuthorizationServerMetadata>>> {
            return try {
                val httpClient = DefaultHttpClientFactory()
                httpClient.use { client ->
                    Result.success(Issuer.metaData(client, credentialIssuerId))
                }
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

        private fun CredentialIssuerMetadata.getCredentialConfigurationByDocType(docType: String): Result<Pair<CredentialConfigurationIdentifier, CredentialConfiguration>> {
            val credentialConfigurationsSupported =
                credentialConfigurationsSupported.filterValues {
                    it is MsoMdocCredential && it.docType == docType
                }

            if (credentialConfigurationsSupported.isEmpty()) {
                Log.e(TAG, "No supported credential format for docType $docType")
                return Result.failure(IllegalStateException("No supported credential format for docType $docType.\nSupported formats:[${FORMAT_MSO_MDOC}]"))
            }

            val (credentialConfigurationId, credentialConfiguration) = credentialConfigurationsSupported.entries.first()
            Log.d(TAG, "Proceeding issuing for ${credentialConfigurationId.value}")
            return try {
                credentialConfiguration.assertProofType()
                Result.success(Pair(credentialConfigurationId, credentialConfiguration))
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

        private fun CredentialConfiguration.assertProofType() {
            if (proofTypesSupported.isEmpty()) return
            if (!proofTypesSupported.any { (type, algorithms) ->
                    type == ProofType.JWT && algorithms.any { it in ProofSigner.SUPPORTED_ALGORITHMS }
                }) {
                val msg = "Did not find any supported proof types.\nSupported proof types: jwt for algorithms [${
                    ProofSigner.SUPPORTED_ALGORITHMS.joinToString(",") { it.name }
                }]"
                Log.e(TAG, msg)
                throw IllegalStateException(msg)
            }
        }

        private val CreateIssuanceRequestResult.result: Result<IssuanceRequest>
            get() = when (this) {
                is CreateIssuanceRequestResult.Success -> Result.success(issuanceRequest)
                is CreateIssuanceRequestResult.Failure -> Result.failure(throwable)
            }
    }

    fun interface OnIssueCallback {
        fun onResult(result: IssueDocumentResult)
    }


    internal fun interface AuthorizationCallback {
        fun onAuthorizationCode(code: String?, serverState: String?)
    }
}