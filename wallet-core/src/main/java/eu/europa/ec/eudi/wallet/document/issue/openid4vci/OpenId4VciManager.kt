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
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizationServerMetadataResolver
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.CredentialIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataResolver
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyGenerationConfig
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.SubmittedRequest
import eu.europa.ec.eudi.openid4vci.internal.formats.findScopeForMsoMdoc
import eu.europa.ec.eudi.wallet.document.AddDocumentResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.document.issue.IssueDocumentResult
import eu.europa.ec.eudi.wallet.document.issue.openid4vci.OpenId4VciManager.AuthorizationCallback
import eu.europa.ec.eudi.wallet.internal.executeOnThread
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.Base64

class OpenId4VciManager(
    context: Context,
    private val config: OpenId4VciConfig,
    private val documentManager: DocumentManager,
) {

    companion object {
        private const val TAG = "OpenId4VCIManager"
    }

    private val context = context.applicationContext

    private val issuerUrl: String
        get() = config.issuerUrl

    private val openId4VCIConfig
        get() = OpenId4VCIConfig(
            clientId = config.clientId,
            authFlowRedirectionURI = URI(config.redirectionUri),
            keyGenerationConfig = KeyGenerationConfig(Curve.P_256, 2048)
        )

    private val issuerIdentifier
        get() = CredentialIssuerId(issuerUrl)

    private val issuerMetadataResolver
        get() = CredentialIssuerMetadataResolver()

    private val authMetadataResolver
        get() = AuthorizationServerMetadataResolver()


    fun issueDocument(docType: String, onResult: (IssueDocumentResult) -> Unit) {
        executeOnThread {
            try {
                val identifier = issuerIdentifier.getOrThrow()
                val issuerMetadata = issuerMetadataResolver.resolve(identifier).getOrThrow()
                val scope = issuerMetadata.findScopeForMsoMdoc(docType)
                    ?: throw IllegalStateException("Scope for $docType not found")
                val authorizationServer = issuerMetadata.authorizationServers.first()
                val authMetadata = authMetadataResolver.resolve(authorizationServer)
                    .getOrThrow()
                val issuer = Issuer.make(authMetadata, issuerMetadata, openId4VCIConfig)
                val credentialIdentifier = CredentialIdentifier(scope)
                val parPlaced =
                    issuer.pushAuthorizationCodeRequest(listOf(credentialIdentifier), null)
                        .getOrThrow()
                val getAuthorizationCodeUri =
                    Uri.parse(parPlaced.getAuthorizationCodeURL.toString())


                OpenId4VciAuthorizeActivity.callback = AuthorizationCallback {
                    it?.let { code ->
                        runBlocking {
                            try {
                                with(issuer) {
                                    val authorizedRequest = parPlaced
                                        .handleAuthorizationCode(AuthorizationCode(code))
                                        .requestAccessToken()
                                        .getOrThrow()
                                    val issuanceRequest =
                                        documentManager.createIssuanceRequest(docType, true)
                                            .result
                                            .getOrThrow()

                                    val result = when (authorizedRequest) {
                                        is AuthorizedRequest.ProofRequired -> handleProofRequired(
                                            issuer,
                                            authorizedRequest,
                                            credentialIdentifier,
                                            issuanceRequest
                                        )

                                        is AuthorizedRequest.NoProofRequired -> handleNoProofRequired(
                                            issuer,
                                            authorizedRequest,
                                            credentialIdentifier,
                                            issuanceRequest
                                        )
                                    }
                                    context.mainExecutor().execute { onResult(result) }
                                }

                            } catch (e: Exception) {
                                context.mainExecutor().execute {
                                    onResult(IssueDocumentResult.Failure(e))
                                }
                            }
                        }
                    }
                }
                context.startActivity(Intent(ACTION_VIEW, getAuthorizationCodeUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

            } catch (e: Exception) {
                context.mainExecutor().execute {
                    onResult(IssueDocumentResult.Failure(e))
                }
            }
        }

    }

    private suspend fun handleProofRequired(
        issuer: Issuer,
        authRequest: AuthorizedRequest.ProofRequired,
        credentialIdentifier: CredentialIdentifier,
        issuanceRequest: IssuanceRequest,
    ): IssueDocumentResult {
        return with(issuer) {
            when (val outcome = authRequest.requestSingle(
                credentialIdentifier,
                null,
                ProofSigner(issuanceRequest, JWSAlgorithm.ES256.name)
            ).getOrThrow()) {
                is SubmittedRequest.Failed -> IssueDocumentResult.Failure(outcome.error)
                is SubmittedRequest.InvalidProof -> IssueDocumentResult.Failure(
                    IllegalStateException(outcome.errorDescription)
                )

                is SubmittedRequest.Success -> addDocument(outcome, issuanceRequest)
            }
        }
    }

    private suspend fun handleNoProofRequired(
        issuer: Issuer,
        authRequest: AuthorizedRequest.NoProofRequired,
        credentialIdentifier: CredentialIdentifier,
        issuanceRequest: IssuanceRequest,
    ): IssueDocumentResult {
        return with(issuer) {
            when (val outcome =
                authRequest.requestSingle(credentialIdentifier, null).getOrThrow()) {
                is SubmittedRequest.Failed -> IssueDocumentResult.Failure(outcome.error)
                is SubmittedRequest.InvalidProof -> handleProofRequired(
                    issuer,
                    authRequest.handleInvalidProof(outcome.cNonce),
                    credentialIdentifier,
                    issuanceRequest
                )

                is SubmittedRequest.Success -> addDocument(outcome, issuanceRequest)
            }
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
                is AddDocumentResult.Failure -> IssueDocumentResult.Failure(addResult.throwable)
            }
        }
    }


    internal fun interface AuthorizationCallback {
        fun onAuthorizationCode(code: String?)
    }
}