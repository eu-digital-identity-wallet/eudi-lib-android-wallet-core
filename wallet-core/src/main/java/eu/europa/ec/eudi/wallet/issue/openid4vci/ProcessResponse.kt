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
import eu.europa.ec.eudi.openid4vci.SubmittedRequest
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.documentFailed
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.documentIssued
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.util.*
import kotlin.coroutines.resume

internal class ProcessResponse(
    val documentManager: DocumentManager,
    val listener: OpenId4VciManager.OnResult<IssueEvent>,
    val issuedDocumentIds: MutableList<DocumentId>,
) : Closeable {
    private val continuations = mutableMapOf<DocumentId, CancellableContinuation<Boolean>>()

    suspend fun process(response: SubmitRequest.Response) {
        response.forEach { (unsignedDocument, result) ->
            process(unsignedDocument, result)
        }
    }

    suspend fun process(unsignedDocument: UnsignedDocument, submittedRequestResult: Result<SubmittedRequest>) {
        try {
            processSubmittedRequest(unsignedDocument, submittedRequestResult.getOrThrow())
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

    fun processSubmittedRequest(unsignedDocument: UnsignedDocument, submittedRequest: SubmittedRequest) {
        when (submittedRequest) {
            is SubmittedRequest.Success -> when (val credential = submittedRequest.credentials[0]) {
                is IssuedCredential.Issued -> try {
                    val cborBytes = Base64.getUrlDecoder().decode(credential.credential)
                    documentManager.storeIssuedDocument(unsignedDocument, cborBytes)
                        .onFailure {
                            documentManager.deleteDocumentById(unsignedDocument.id)
                            listener(documentFailed(unsignedDocument, it))
                        }
                        .onSuccess { id, _ ->
                            issuedDocumentIds.add(id)
                            listener(documentIssued(unsignedDocument))
                        }
                } catch (e: Throwable) {
                    documentManager.deleteDocumentById(unsignedDocument.id)
                    listener(documentFailed(unsignedDocument, e))
                }

                is IssuedCredential.Deferred -> {
                    TODO("Not supported yet")
                }
            }

            is SubmittedRequest.InvalidProof -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(
                    documentFailed(
                        unsignedDocument,
                        IllegalStateException(submittedRequest.errorDescription)
                    )
                )
            }

            is SubmittedRequest.Failed -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(documentFailed(unsignedDocument, submittedRequest.error))
            }
        }
    }

    private fun UserAuthRequiredException.toIssueEvent(
        unsignedDocument: UnsignedDocument
    ): IssueEvent.DocumentRequiresUserAuth {
        return IssueEvent.DocumentRequiresUserAuth(
            unsignedDocument,
            cryptoObject = cryptoObject,
            resume = { runBlocking { continuations[unsignedDocument.id]!!.resume(true) } },
            cancel = { runBlocking { continuations[unsignedDocument.id]!!.resume(false) } }
        )
    }
}