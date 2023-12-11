/*
 *  Copyright (c) 2023 European Commission
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
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizationServerMetadataResolver
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.CredentialIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataResolver
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyGenerationConfig
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.SubmittedRequest
import eu.europa.ec.eudi.openid4vci.internal.formats.findScopeForMsoMdoc
import eu.europa.ec.eudi.wallet.document.AddDocumentResult
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.issue.IssueDocumentResult
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciManager.AuthorizationCallback
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.Base64
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manager for issuing documents using OpenID4VCI.
 *
 *
 * @param context the application context
 * @param config the configuration for OpenID4VCI
 * @param documentManager the document manager to use for issuing documents
 * @param executor the executor to use for callbacks. If null, the main executor will be used.
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
            authFlowRedirectionURI = URI(DEFAULT_REDIRECTION_URI),
            keyGenerationConfig = KeyGenerationConfig(Curve.P_256, 2048)
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
        val onResultUnderExecutor = { result: IssueDocumentResult ->
            if (result is IssueDocumentResult.Failure || result is IssueDocumentResult.Success) {
                if (ioScope.isActive) ioScope.cancel()
                Log.d(TAG, "Cancel CoroutineScope $ioScope")
            }
            (executor ?: context.mainExecutor()).execute {
                callback.onResult(result)
            }
        }
        ioScope.launch {
            try {
                val issuerMetadata = resolveIssuerMetadata()
                val credentialIdentifier = issuerMetadata.getCredentialIdentifier(docType)
                val issuer = issuerMetadata.getIssuer()
                val authorizedRequest = issuer.authorize(credentialIdentifier)
                val issuanceRequest = documentManager
                    .createIssuanceRequest(docType, true)
                    .result
                    .getOrThrow()
                issuer.handleAuthorizedRequest(
                    authorizedRequest,
                    credentialIdentifier,
                    issuanceRequest,
                    onResultUnderExecutor
                )
            } catch (e: Throwable) {
                onResultUnderExecutor(IssueDocumentResult.Failure(e))
            }
        }
    }

    private suspend fun resolveIssuerMetadata(): CredentialIssuerMetadata {
        val identifier = CredentialIssuerId(config.issuerUrl).getOrThrow()
        return (CredentialIssuerMetadataResolver()).resolve(identifier).getOrThrow()
    }

    private fun CredentialIssuerMetadata.getCredentialIdentifier(
        docType: String
    ): CredentialIdentifier {

        val scope = findScopeForMsoMdoc(docType)
            ?: throw IllegalStateException("Scope for $docType not found")

        credentialsSupported.filterValues { credSup ->
            credSup.scope == scope && credSup.cryptographicSuitesSupported.any { it in ProofSigner.SUPPORTED_ALGORITHMS }
        }
            .isNotEmpty() || throw IllegalStateException("No supported algorithm for $docType. Currently supported algorithms: ${ProofSigner.SUPPORTED_ALGORITHMS}")

        return CredentialIdentifier(scope)
    }

    private suspend fun CredentialIssuerMetadata.getIssuer(): Issuer {
        val authorizationServer = authorizationServers.first()
        val authMetadata = AuthorizationServerMetadataResolver().resolve(authorizationServer)
            .getOrThrow()
        return Issuer.make(authMetadata, this, openId4VCIConfig)
    }

    private suspend fun Issuer.authorize(credentialIdentifier: CredentialIdentifier): AuthorizedRequest {
        val issuer = this
        val parPlaced = pushAuthorizationCodeRequest(listOf(credentialIdentifier), null)
            .getOrThrow()
        val getAuthorizationCodeUri = Uri.parse(parPlaced.getAuthorizationCodeURL.toString())

        return suspendCoroutine { continuation ->
            OpenId4VciAuthorizeActivity.callback = AuthorizationCallback {
                runBlocking {
                    it?.let { code ->
                        with(issuer) {
                            try {
                                val authorizedRequest = parPlaced
                                    .handleAuthorizationCode(AuthorizationCode(code))
                                    .requestAccessToken()
                                    .getOrThrow()
                                continuation.resume(authorizedRequest)
                            } catch (e: Throwable) {
                                continuation.resumeWithException(e)
                            }
                        }
                    } ?: run {
                        continuation.resumeWithException(IllegalStateException("Authorization code is null"))
                    }
                }
            }

            context.startActivity(Intent(ACTION_VIEW, getAuthorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private suspend fun Issuer.handleAuthorizedRequest(
        authorizedRequest: AuthorizedRequest,
        credentialIdentifier: CredentialIdentifier,
        issuanceRequest: IssuanceRequest,
        onResult: (IssueDocumentResult) -> Unit
    ) {
        when (authorizedRequest) {
            is AuthorizedRequest.ProofRequired -> handleProofRequired(
                authorizedRequest,
                credentialIdentifier,
                issuanceRequest,
                onResult,
            )

            is AuthorizedRequest.NoProofRequired -> handleNoProofRequired(
                authorizedRequest,
                credentialIdentifier,
                issuanceRequest,
                onResult,
            )
        }
    }


    private suspend fun Issuer.handleProofRequired(
        authRequest: AuthorizedRequest.ProofRequired,
        credentialIdentifier: CredentialIdentifier,
        issuanceRequest: IssuanceRequest,
        onResult: (IssueDocumentResult) -> Unit
    ) {
        val proofSigner = ProofSigner(issuanceRequest, proofAlgorithm)
        onResult(try {
            when (val outcome = authRequest.requestSingle(
                credentialIdentifier,
                null,
                proofSigner
            ).getOrThrow()) {
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
                                credentialIdentifier,
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
        credentialIdentifier: CredentialIdentifier,
        issuanceRequest: IssuanceRequest,
        onResult: (IssueDocumentResult) -> Unit
    ) {
        when (val outcome =
            authRequest.requestSingle(credentialIdentifier, null).getOrThrow()) {
            is SubmittedRequest.Failed -> onResult(IssueDocumentResult.Failure(outcome.error))
            is SubmittedRequest.InvalidProof -> handleProofRequired(
                authRequest.handleInvalidProof(outcome.cNonce),
                credentialIdentifier,
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

    private val CreateIssuanceRequestResult.result: Result<IssuanceRequest>
        get() = when (this) {
            is CreateIssuanceRequestResult.Success -> Result.success(issuanceRequest)
            is CreateIssuanceRequestResult.Failure -> Result.failure(throwable)
        }

    private companion object {
        private const val TAG = "OpenId4VciManager"
        private const val DEFAULT_REDIRECTION_URI = "eudi-openid4ci://authorize"
    }

    fun interface OnIssueCallback {
        fun onResult(result: IssueDocumentResult)
    }


    internal fun interface AuthorizationCallback {
        fun onAuthorizationCode(code: String?)
    }
}