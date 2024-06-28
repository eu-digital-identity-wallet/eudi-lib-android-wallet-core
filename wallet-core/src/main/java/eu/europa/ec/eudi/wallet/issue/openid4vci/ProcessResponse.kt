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

import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.StoreDocumentResult
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.documentFailed
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import org.bouncycastle.util.encoders.Hex
import java.io.Closeable
import java.util.*
import kotlin.coroutines.resume

internal class ProcessResponse(
    val documentManager: DocumentManager,
    val deferredContextCreator: DeferredContextCreator,
    val listener: OpenId4VciManager.OnResult<IssueEvent>,
    val issuedDocumentIds: MutableList<DocumentId>,
    val logger: Logger? = null,
) : Closeable {
    private val continuations = mutableMapOf<DocumentId, CancellableContinuation<Boolean>>()
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun process(response: SubmitRequest.Response) {
        response.forEach { (unsignedDocument, result) ->
            process(unsignedDocument, result)
        }
    }

    suspend fun process(unsignedDocument: UnsignedDocument, outcomeResult: Result<SubmissionOutcome>) {
        try {
            processSubmittedRequest(unsignedDocument, outcomeResult.getOrThrow())
        } catch (e: Throwable) {
            when (e) {
                is UserAuthRequiredException -> {
                    listener(e.toIssueEvent(unsignedDocument))
                    val authenticationResult = suspendCancellableCoroutine {
                        it.invokeOnCancellation { listener(IssueEvent.Failure(e)) }
                        continuations[unsignedDocument.id] = it
                    }
                    processSubmittedRequest(unsignedDocument, e.resume(authenticationResult))
                }

                else -> listener(IssueEvent.Failure(e))
            }
        }
    }

    override fun close() {
        continuations.values.forEach { it.cancel() }
    }

    fun processSubmittedRequest(unsignedDocument: UnsignedDocument, outcome: SubmissionOutcome) {
        when (outcome) {
            is SubmissionOutcome.Success -> when (val credential = outcome.credentials[0]) {
                is IssuedCredential.Issued -> try {
                    val cborBytes = Base64.getUrlDecoder().decode(credential.credential)
                    logger?.d(TAG, "CBOR bytes: ${Hex.toHexString(cborBytes)}")
                    documentManager.storeIssuedDocument(unsignedDocument, cborBytes)
                        .notifyListener(unsignedDocument)
                } catch (e: Throwable) {
                    documentManager.deleteDocumentById(unsignedDocument.id)
                    listener(documentFailed(unsignedDocument, e))
                }

                is IssuedCredential.Deferred -> {
                    val contextToStore = deferredContextCreator.create(credential)
                    documentManager.storeDeferredDocument(unsignedDocument, contextToStore.toByteArray())
                        .notifyListener(unsignedDocument, isDeferred = true)
                }
            }

            is SubmissionOutcome.InvalidProof -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(
                    documentFailed(
                        unsignedDocument,
                        IllegalStateException(outcome.errorDescription)
                    )
                )
            }

            is SubmissionOutcome.Failed -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(documentFailed(unsignedDocument, outcome.error))
            }
        }
    }

    private fun UserAuthRequiredException.toIssueEvent(
        unsignedDocument: UnsignedDocument,
    ): IssueEvent.DocumentRequiresUserAuth {
        return IssueEvent.DocumentRequiresUserAuth(
            unsignedDocument,
            cryptoObject = cryptoObject,
            resume = { runBlocking { continuations[unsignedDocument.id]!!.resume(true) } },
            cancel = { runBlocking { continuations[unsignedDocument.id]!!.resume(false) } }
        )
    }

    private fun StoreDocumentResult.notifyListener(unsignedDocument: UnsignedDocument, isDeferred: Boolean = false) =
        when (this) {
            is StoreDocumentResult.Success -> {
                issuedDocumentIds.add(documentId)
                if (isDeferred) {
                    listener(IssueEvent.DocumentDeferred(documentId, unsignedDocument.name, unsignedDocument.docType))
                } else {
                    listener(IssueEvent.DocumentIssued(documentId, unsignedDocument.name, unsignedDocument.docType))
                }
            }

            is StoreDocumentResult.Failure -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(documentFailed(unsignedDocument, throwable))
            }
        }
}