/*
 * Copyright (c) 2024 European Commission
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

import com.android.identity.securearea.KeyUnlockData
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Companion.failure
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
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

    suspend fun process(response: SubmitRequest.Response) {
        response.forEach { (unsignedDocument, result) ->
            process(unsignedDocument, result)
        }
    }

    suspend fun process(
        unsignedDocument: UnsignedDocument,
        outcomeResult: Result<SubmissionOutcome>,
    ) {
        try {
            processSubmittedRequest(unsignedDocument, outcomeResult.getOrThrow())
        } catch (e: Throwable) {
            when (e) {
                is UserAuthRequiredException -> {

                    val keyUnlockData = suspendCancellableCoroutine { cont ->
                        cont.invokeOnCancellation { listener(IssueEvent.Failure(e)) }
                        listener(
                            e.toIssueEvent(
                                unsignedDocument = unsignedDocument,
                                signingAlgorithm = e.signingAlgorithm,
                                continuation = cont
                            )
                        )
                    }
                    processSubmittedRequest(unsignedDocument, e.resume(keyUnlockData))
                }

                else -> listener(IssueEvent.Failure(e))
            }
        }
    }

    override fun close() {
    }

    fun processSubmittedRequest(unsignedDocument: UnsignedDocument, outcome: SubmissionOutcome) {
        when (outcome) {
            is SubmissionOutcome.Success -> when (val credential =
                outcome.credentials.first().credential) {
                is Credential.Json -> TODO("Not supported yet")
                is Credential.Str -> try {
                    val issuerData = when (unsignedDocument.format) {
                        is MsoMdocFormat -> Base64.getUrlDecoder().decode(credential.value)
                            .also {
                                logger?.d(TAG, "CBOR bytes: ${Hex.toHexString(it)}")
                            }

                        is SdJwtVcFormat -> credential.value.also {
                            logger?.d(TAG, "SD-JWT-VC: $it")
                        }.toByteArray(charset = Charsets.US_ASCII)
                    }

                    documentManager.storeIssuedDocument(unsignedDocument, issuerData)
                        .kotlinResult
                        .onSuccess { issuedDocumentIds.add(unsignedDocument.id) }
                        .notifyListener(unsignedDocument)

                } catch (e: Throwable) {
                    documentManager.deleteDocumentById(unsignedDocument.id)
                    listener(failure(e, unsignedDocument))
                }
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

            is SubmissionOutcome.Deferred -> {
                val contextToStore = deferredContextCreator.create(outcome)
                documentManager.storeDeferredDocument(
                    unsignedDocument = unsignedDocument,
                    relatedData = contextToStore.toByteArray()
                )
                    .kotlinResult
                    .onSuccess { issuedDocumentIds.add(unsignedDocument.id) }
                    .notifyListener(unsignedDocument)

            }
        }
    }

    private fun UserAuthRequiredException.toIssueEvent(
        continuation: CancellableContinuation<KeyUnlockData>,
        unsignedDocument: UnsignedDocument,
        signingAlgorithm: com.android.identity.crypto.Algorithm,
    ): IssueEvent.DocumentRequiresUserAuth {
        return IssueEvent.DocumentRequiresUserAuth(
            document = unsignedDocument,
            signingAlgorithm = signingAlgorithm,
            resume = { keyUnlockData ->
                runBlocking {
                    continuation.resume(
                        keyUnlockData
                    )
                }
            },
            cancel = {
                runBlocking {
                    continuation.cancel(
                        IllegalStateException("Canceled")
                    )
                }
            }
        )
    }

    private fun Result<Document>.notifyListener(unsignedDocument: UnsignedDocument) =

        onSuccess { document ->
            when (document) {
                is DeferredDocument -> listener(IssueEvent.DocumentDeferred(document))
                is IssuedDocument -> listener(IssueEvent.DocumentIssued(document))
                else -> listener(
                    IssueEvent.DocumentFailed(
                        unsignedDocument,
                        IllegalStateException("Unexpected document state")
                    )
                )
            }
        }.onFailure { throwable ->
            documentManager.deleteDocumentById(unsignedDocument.id)
            listener(failure(throwable, unsignedDocument))
        }
}