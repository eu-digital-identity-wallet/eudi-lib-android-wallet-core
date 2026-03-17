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
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.DeferredCredentialQueryOutcome
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.Storage

internal class ProcessDeferredOutcome(
    val documentManager: DocumentManager,
    val callback: OpenId4VciManager.OnResult<DeferredIssueResult>,
    val deferredContext: DeferredContext?,
    val logger: Logger? = null,
    val issuanceMetadataStorage: Storage? = null,
) {

    fun process(
        deferredDocument: DeferredDocument,
        keyAliases: List<String>,
        outcome: DeferredCredentialQueryOutcome,
    ) {
        try {
            when (outcome) {
                is DeferredCredentialQueryOutcome.Errored -> {
                    callback(
                        DeferredIssueResult.DocumentFailed(
                            document = deferredDocument,
                            cause = IllegalStateException(outcome.error)
                        )
                    )
                }

                is DeferredCredentialQueryOutcome.IssuancePending -> {
                    deferredContext?.let { ctx ->
                        val contextToStore = ctx.toBytes()
                        documentManager.storeDeferredDocument(deferredDocument, contextToStore)
                            .kotlinResult.onSuccess { document ->
                                callback(DeferredIssueResult.DocumentNotReady(document))
                            }.onFailure { error ->
                                documentManager.deleteDocumentById(deferredDocument.id)
                                callback(
                                    DeferredIssueResult.DocumentFailed(
                                        document = deferredDocument,
                                        cause = error
                                    )
                                )
                            }
                    } ?: callback(
                        DeferredIssueResult.DocumentNotReady(deferredDocument)
                    )
                }

                is DeferredCredentialQueryOutcome.Issued -> {
                    val credentials = outcome.credentials.map { it.credential }.zip(keyAliases)
                    documentManager.storeIssuedDocument(deferredDocument, credentials) {
                        logger?.d(TAG, message = it)
                    }.onSuccess { document ->
                        // If this deferred credential replaces an old document (deferred re-issuance),
                        // delete the old document now that the new one is successfully issued.
                        deferredContext?.replacesDocumentId?.let { oldDocId ->
                            documentManager.deleteDocumentById(oldDocId)
                            logger?.d(TAG, "Deleted old document $oldDocId after deferred re-issuance")
                        }

                        // Store issuance metadata so the new document can be re-issued later
                        if (issuanceMetadataStorage != null && deferredContext != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                storeIssuanceMetadata(document.id, deferredContext, keyAliases)
                            }
                        }

                        callback(DeferredIssueResult.DocumentIssued(document))
                    }.onFailure { error ->
                        documentManager.deleteDocumentById(deferredDocument.id)
                        callback(
                            DeferredIssueResult.DocumentFailed(
                                document = deferredDocument,
                                cause = error
                            )
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            callback(DeferredIssueResult.DocumentFailed(deferredDocument, e))
        }
    }

    private suspend fun storeIssuanceMetadata(
        documentId: String,
        deferredContext: DeferredContext,
        keyAliases: List<String>,
    ) {
        val storage = issuanceMetadataStorage ?: return
        val credentialConfigId = deferredContext.credentialConfigurationIdentifier ?: return
        val credentialEndpoint = deferredContext.credentialEndpoint ?: return

        runCatching {
            val ctx = deferredContext.issuanceContext
            val cfg = ctx.config
            val tx = ctx.authorizedTransaction

            val (clientId, clientAttestationJwt) = when (val auth = cfg.clientAuthentication) {
                is ClientAuthentication.None -> auth.id to null
                is ClientAuthentication.AttestationBased -> auth.id to auth.attestationJWT.jwt.serialize()
                else -> auth.id to null
            }

            val issuanceMetadata = IssuanceMetadata(
                credentialIssuerId = cfg.credentialIssuerId.toString(),
                credentialConfigurationIdentifier = credentialConfigId,
                credentialEndpoint = credentialEndpoint,
                tokenEndpoint = cfg.tokenEndpoint.toString(),
                authorizationServerId = cfg.authorizationServerId.toString(),
                challengeEndpoint = cfg.challengeEndpoint?.toString(),
                clientId = clientId,
                clientAttestationJwt = clientAttestationJwt,
                clientAttestationPopKeyId = deferredContext.clientAttestationPopKeyId,
                popKeyAliases = keyAliases,
                dPoPKeyAlias = deferredContext.dPoPKeyAlias,
                accessToken = tx.authorizedRequest.accessToken.accessToken,
                accessTokenType = when (tx.authorizedRequest.accessToken) {
                    is AccessToken.DPoP -> "DPoP"
                    is AccessToken.Bearer -> "Bearer"
                },
                refreshToken = tx.authorizedRequest.refreshToken?.refreshToken,
                tokenTimestamp = tx.authorizedRequest.timestamp.epochSecond,
                grantType = if (tx.authorizedRequest.grant::class.simpleName == "PreAuthorizedCodeGrant") {
                    "pre-authorized_code"
                } else {
                    "authorization_code"
                },
            )

            val table = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            table.insert(key = documentId, data = ByteString(issuanceMetadata.toByteArray()))

            logger?.d(TAG, "Stored issuance metadata for deferred document $documentId")
        }.onFailure { error ->
            logger?.log(
                Logger.Record(
                    level = Logger.Companion.LEVEL_ERROR,
                    message = "Failed to store issuance metadata for deferred document $documentId: ${error.message}",
                    sourceClassName = "ProcessDeferredOutcome",
                    sourceMethod = "storeIssuanceMetadata",
                    thrown = error
                )
            )
        }
    }
}