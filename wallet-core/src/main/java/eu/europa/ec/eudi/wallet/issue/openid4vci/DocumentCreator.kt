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

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

internal class DocumentCreator(
    val documentManager: DocumentManager,
    val listener: OpenId4VciManager.OnResult<IssueEvent>,
    val logger: Logger? = null,
) {

    val continuations =
        mutableMapOf<Offer.OfferedDocument, CancellableContinuation<CreateDocumentSettings>>()

    suspend fun createDocuments(offer: Offer): Map<UnsignedDocument, Offer.OfferedDocument> =
        offer.offeredDocuments.associateBy { createDocument(it) }

    suspend fun createDocument(offeredDocument: Offer.OfferedDocument): UnsignedDocument {
        listener.onResult(IssueEvent.DocumentRequiresCreateSettings(
            offeredDocument = offeredDocument,
            resume = { createSettings ->
                runBlocking {
                    continuations[offeredDocument]!!.resume(createSettings)
                }
            },
            cancel = { reason ->
                runBlocking {
                    continuations[offeredDocument]!!.cancel(reason?.let {
                        CancellationException(it)
                    })
                }
            }
        ))
        val createDocumentSettings = suspendCancellableCoroutine<CreateDocumentSettings> {

            it.invokeOnCancellation {
                listener(
                    IssueEvent.Failure(
                        RuntimeException("Document creation was cancelled")
                    )
                )
            }
            continuations[offeredDocument] = it

        }
        return documentManager.createDocument(offeredDocument, createDocumentSettings).getOrThrow()
    }
}