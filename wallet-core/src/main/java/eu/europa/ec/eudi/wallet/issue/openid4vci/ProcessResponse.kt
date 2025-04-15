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

import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume

internal class ProcessResponse(
    val documentManager: DocumentManager,
    val deferredContextFactory: DeferredContextFactory,
    val listener: OpenId4VciManager.OnResult<IssueEvent>,
    val issuedDocumentIds: MutableList<DocumentId>,
    val logger: Logger? = null,
) {

    suspend fun process(response: SubmitRequest.Response) {
        response.forEach { (unsignedDocument, result) ->
            process(unsignedDocument, result.first, result.second)
        }
    }

    suspend fun process(
        unsignedDocument: UnsignedDocument,
        keyAliases: List<String>,
        outcomeResult: Result<SubmissionOutcome>
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
        outcome: SubmissionOutcome
    ) {
        when (outcome) {
            is SubmissionOutcome.Success -> runCatching {
                val credentials = outcome.credentials.map { it.credential }.zip(keyAliases)
                documentManager.storeIssuedDocument(unsignedDocument, credentials) { message ->
                    logger?.d(TAG, message)
                }.getOrThrow()
            }.onSuccess { document ->
                issuedDocumentIds.add(document.id)
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
                val contextToStore = deferredContextFactory(keyAliases, outcome)
                documentManager.storeDeferredDocument(
                    unsignedDocument = unsignedDocument,
                    relatedData = contextToStore.toByteArray()
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
}