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

import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.ECKey
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.sdjwt.SdJwtVerifier
import eu.europa.ec.eudi.sdjwt.asJwtVerifier
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.StoreDocumentResult
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.documentFailed
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.util.parseCertificateFromSdJwt
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.Closeable
import java.util.Base64
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

    private suspend fun process(
        unsignedDocument: UnsignedDocument,
        outcomeResult: Result<SubmissionOutcome>
    ) {
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

    private fun processSubmittedRequest(
        unsignedDocument: UnsignedDocument,
        outcome: SubmissionOutcome
    ) {
        when (outcome) {
            is SubmissionOutcome.Success -> processSubmittedRequestSuccess(
                outcome,
                unsignedDocument
            )

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

    private fun processSubmittedRequestSuccess(
        outcome: SubmissionOutcome.Success,
        unsignedDocument: UnsignedDocument
    ) {
        when (val credential = outcome.credentials[0]) {
            is IssuedCredential.Issued -> try {
                if (isSdJwt(credential.credential)) {
                    processIssuedSdjwt(credential, unsignedDocument)
                } else {
                    processIssuedMdoc(credential, unsignedDocument)
                }
            } catch (e: Throwable) {
                if (isSdJwt(credential.credential)) {
                    documentManager.deleteDocumentById(unsignedDocument.id)
                } else {
                    DocumentManagerSdJwt.deleteDocument(unsignedDocument.id)
                }
                listener(documentFailed(unsignedDocument, e))
            }

            is IssuedCredential.Deferred -> {
                val contextToStore = deferredContextCreator.create(credential)
                documentManager.storeDeferredDocument(
                    unsignedDocument,
                    contextToStore.toByteArray()
                ).notifyListener(unsignedDocument, isDeferred = true)
            }
        }
    }

    private fun isSdJwt(credential: String): Boolean {
        return try {
            val headerString = credential.split(".").first()
            // try to parse to header json
            JSONObject(String(Base64.getUrlDecoder().decode(headerString)))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun processIssuedSdjwt(
        credential: IssuedCredential.Issued,
        unsignedDocument: UnsignedDocument
    ) {
        val certificate = parseCertificateFromSdJwt(credential.credential)

        val ecKey = ECKey.parse(certificate)
        val jwtSignatureVerifier = ECDSAVerifier(ecKey).asJwtVerifier()

        CoroutineScope(Dispatchers.IO).launch {
            SdJwtVerifier.verifyIssuance(
                jwtSignatureVerifier,
                credential.credential
            ).getOrThrow()

            DocumentManagerSdJwt.storeDocument(
                unsignedDocument.id,
                credential.credential
            )
            documentManager.deleteDocumentById(unsignedDocument.id)
            listener.invoke(
                IssueEvent.DocumentIssued(
                    unsignedDocument.id,
                    unsignedDocument.name,
                    unsignedDocument.docType
                )
            )
        }
    }

    private fun processIssuedMdoc(
        credential: IssuedCredential.Issued,
        unsignedDocument: UnsignedDocument
    ) {
        val cborBytes = Base64.getUrlDecoder().decode(credential.credential)

        documentManager.storeIssuedDocument(
            unsignedDocument,
            cborBytes
        ).notifyListener(unsignedDocument)
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

    private fun StoreDocumentResult.notifyListener(
        unsignedDocument: UnsignedDocument,
        isDeferred: Boolean = false
    ) =
        when (this) {
            is StoreDocumentResult.Success -> {
                issuedDocumentIds.add(documentId)
                if (isDeferred) {
                    listener(
                        IssueEvent.DocumentDeferred(
                            documentId,
                            unsignedDocument.name,
                            unsignedDocument.docType
                        )
                    )
                } else {
                    listener(
                        IssueEvent.DocumentIssued(
                            documentId,
                            unsignedDocument.name,
                            unsignedDocument.docType
                        )
                    )
                }
            }

            is StoreDocumentResult.Failure -> {
                documentManager.deleteDocumentById(unsignedDocument.id)
                listener(documentFailed(unsignedDocument, throwable))
            }
        }
}