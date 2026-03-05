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

import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.ReissuanceConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.Storage
import kotlin.coroutines.resume

internal class ProcessResponse(
    val documentManager: DocumentManager,
    val deferredContextFactory: DeferredContextFactory,
    val walletKeyManager: WalletKeyManager,
    val clientAttestationPopKeyId: String?,
    val listener: OpenId4VciManager.OnResult<IssueEvent>,
    val issuedDocumentIds: MutableList<DocumentId>,
    val logger: Logger? = null,
    val authorizedRequest: AuthorizedRequest,
    val issuer: Issuer,
    val documentToConfigurationMap: Map<UnsignedDocument, Offer.OfferedDocument>,
    val dpopKeyAlias: String,
    val reissuanceStorage: Storage,
    val clientAuthentication: ClientAuthentication,
) {

    suspend fun process(response: SubmitRequest.Response) {
        response.forEach { (unsignedDocument, result) ->
            process(unsignedDocument, result.keyAliases, result.outcome)
        }
    }

    suspend fun process(
        unsignedDocument: UnsignedDocument,
        keyAliases: List<String>,
        outcomeResult: Result<SubmissionOutcome>,
    ) {
        try {
            processSubmittedRequest(unsignedDocument, keyAliases, outcomeResult.getOrThrow())
        } catch (e: Throwable) {
            when (e) {
                is UserAuthRequiredException -> {

                    val keyUnlockData = suspendCancellableCoroutine { cont ->
                        cont.invokeOnCancellation { listener(IssueEvent.Failure(e)) }
                        listener(
                            IssueEvent.DocumentRequiresUserAuth(
                                document = unsignedDocument,
                                signingAlgorithm = e.signingAlgorithm,
                                keysRequireAuth = e.keysAndSecureAreas,
                                resume = { keyUnlockData ->
                                    runBlocking {
                                        cont.resume(keyUnlockData)
                                    }
                                },
                                cancel = {
                                    runBlocking {
                                        cont.cancel(IllegalStateException("Canceled"))
                                    }
                                }
                            )
                        )
                    }
                    val (keyAliases, outcome) = e.resume(keyUnlockData)
                    processSubmittedRequest(unsignedDocument, keyAliases, outcome)
                }

                else -> listener(IssueEvent.Failure(e))
            }
        }
    }

    fun processSubmittedRequest(
        unsignedDocument: UnsignedDocument,
        keyAliases: List<String>,
        outcome: SubmissionOutcome,
    ) {
        when (outcome) {
            is SubmissionOutcome.Success -> runCatching {
                val credentials = outcome.credentials.map { it.credential }.zip(keyAliases)
                documentManager.storeIssuedDocument(unsignedDocument, credentials) { message ->
                    logger?.d(TAG, message)
                }.getOrThrow()
            }.onSuccess { document ->
                issuedDocumentIds.add(document.id)

                // Store re-issuance metadata in background coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    storeReissuanceMetadata(document.id, unsignedDocument, keyAliases)
                }

                listener(IssueEvent.DocumentIssued(document))
            }.onFailure { error ->
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(IssueEvent.DocumentFailed(unsignedDocument, error))
            }

            is SubmissionOutcome.Failed -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(
                    failure(
                        outcome.error.cause ?: IllegalStateException("CredentialIssuanceError"),
                        unsignedDocument
                    )
                )
            }

            is SubmissionOutcome.Deferred -> runCatching {
                val contextToStore = deferredContextFactory(
                    keyAliases,
                    outcome,
                    clientAttestationPopKeyId
                ).toBytes(walletKeyManager, clientAttestationPopKeyId)

                documentManager.storeDeferredDocument(
                    unsignedDocument = unsignedDocument,
                    relatedData = contextToStore
                ).getOrThrow()
            }.onSuccess { document ->
                issuedDocumentIds.add(document.id)
                listener(IssueEvent.DocumentDeferred(document))
            }.onFailure { error ->
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(IssueEvent.DocumentFailed(unsignedDocument, error))
            }
        }
    }

    /**
     * Stores re-issuance metadata for a successfully issued document.
     *
     * This method captures all necessary metadata to enable credential re-issuance later
     * without requiring the user to go through the full authorization flow again.
     *
     * @param documentId The ID of the successfully issued document
     * @param unsignedDocument The unsigned document that was issued
     * @param keyAliases The PoP key aliases used for this document
     */
    private suspend fun storeReissuanceMetadata(
        documentId: DocumentId,
        unsignedDocument: UnsignedDocument,
        keyAliases: List<String>,
    ) {
        // TODO redundant declare Storage non-nullable
//        val storage = reissuanceStorage ?: return // Re-issuance storage not configured
//        val authRequest = requireNotNull(authorizedRequest) { "AuthorizedRequest is required to store re-issuance metadata" }
//        val currentIssuer = requireNotNull(issuer) { "Issuer is required to store re-issuance metadata" }

        // Only store if refresh token exists (otherwise re-issuance won't work)
        // TODO investigate this maybe we should continue the flow if null
        val refreshToken = authorizedRequest.refreshToken?.refreshToken

        runCatching {
//            val configMap = requireNotNull(documentToConfigurationMap) { "documentToConfigurationMap is required" }
            val credentialConfigurationId = requireNotNull(
                documentToConfigurationMap[unsignedDocument]?.configurationIdentifier?.value
            ) { "Credential configuration identifier not found for document" }

            // Get metadata from credential offer
            val credentialOffer = issuer.credentialOffer
            val issuerMetadata = credentialOffer.credentialIssuerMetadata
            val authServerMetadata = credentialOffer.authorizationServerMetadata

            // Extract client ID and WIA JWT directly from ClientAuthentication
            // Following the pattern from Extensions.kt (DeferredIssuanceContext.serialize)
            val (clientId, clientAttestationJwt) = when (val auth = clientAuthentication) {
                is ClientAuthentication.None -> auth.id to null
                is ClientAuthentication.AttestationBased -> auth.id to auth.attestationJWT.jwt.serialize()
                else -> authServerMetadata.issuer.toString() to null
            }

            // Create re-issuance config
            val reissuanceConfig = ReissuanceConfig(
                credentialIssuerId = issuerMetadata.credentialIssuerIdentifier.toString(),
                credentialConfigurationIdentifier = credentialConfigurationId,
                credentialEndpoint = issuerMetadata.credentialEndpoint.toString(),
                tokenEndpoint = authServerMetadata.tokenEndpointURI.toString(),
                authorizationServerId = authServerMetadata.issuer.toString(),
                challengeEndpoint = null, // Not exposed in CIAuthorizationServerMetadata
                clientId = clientId,
                clientAttestationJwt = clientAttestationJwt,
                clientAttestationPopKeyId = clientAttestationPopKeyId,
                popKeyAliases = keyAliases,
                dPoPKeyAlias = dpopKeyAlias,
                accessToken = authorizedRequest.accessToken.accessToken,
                accessTokenType = when (authorizedRequest.accessToken) {
                    is AccessToken.DPoP -> "DPoP"
                    is AccessToken.Bearer -> "Bearer"
                },
                refreshToken = refreshToken,
                tokenTimestamp = authorizedRequest.timestamp.epochSecond,
                grantType = if (authorizedRequest.grant::class.simpleName == "PreAuthorizedCodeGrant") {
                    "pre-authorized_code"
                } else {
                    "authorization_code"
                },
            )

            val table = reissuanceStorage.getTable(ReissuanceConfig.STORAGE_TABLE_SPEC)

            val bytes = ByteString(reissuanceConfig.toByteArray())
            table.insert(key = documentId, data = bytes)

            logger?.d(TAG, "Stored re-issuance metadata for document $documentId")
        }.onFailure { error ->
            // Log but don't fail the issuance if metadata storage fails
            logger?.log(
                Logger.Record(
                    level = Logger.Companion.LEVEL_ERROR,
                    message = "Failed to store re-issuance metadata for document $documentId: ${error.message}",
                    sourceClassName = "ProcessResponse",
                    sourceMethod = "storeReissuanceMetadata",
                    thrown = error
                )
            )
        }
    }
}