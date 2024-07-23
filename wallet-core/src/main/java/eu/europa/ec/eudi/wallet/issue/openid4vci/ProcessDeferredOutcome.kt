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

import eu.europa.ec.eudi.openid4vci.DeferredCredentialQueryOutcome
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.StoreDocumentResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import org.bouncycastle.util.encoders.Hex
import java.util.*

internal class ProcessDeferredOutcome(
    val documentManager: DocumentManager,
    val callback: OpenId4VciManager.OnResult<DeferredIssueResult>,
    val deferredIssuanceContext: DeferredIssuanceContext?,
    val logger: Logger? = null,
) {

    fun process(deferredDocument: DeferredDocument, outcome: DeferredCredentialQueryOutcome) {
        try {
            when (outcome) {
                is DeferredCredentialQueryOutcome.Errored -> {
                    callback(
                        DeferredIssueResult.DocumentFailed(
                            documentId = deferredDocument.id,
                            name = deferredDocument.name,
                            docType = deferredDocument.docType,
                            cause = IllegalStateException(outcome.error)
                        )
                    )
                }

                is DeferredCredentialQueryOutcome.IssuancePending -> {
                    deferredIssuanceContext?.let { ctx ->
                        documentManager.storeDeferredDocument(deferredDocument, ctx.toByteArray())
                            .notifyListener(deferredDocument, true)
                    } ?: callback(
                        DeferredIssueResult.DocumentNotReady(
                            documentId = deferredDocument.id,
                            name = deferredDocument.name,
                            docType = deferredDocument.docType
                        )
                    )
                }

                is DeferredCredentialQueryOutcome.Issued -> {
                    val cborBytes = Base64.getUrlDecoder().decode(outcome.credential.credential)
                    logger?.d(TAG, "CBOR bytes: ${Hex.toHexString(cborBytes)}")
                    documentManager.storeIssuedDocument(deferredDocument, cborBytes)
                        .notifyListener(deferredDocument)
                }
            }
        } catch (e: Throwable) {
            callback(
                DeferredIssueResult.DocumentFailed(
                    documentId = deferredDocument.id,
                    name = deferredDocument.name,
                    docType = deferredDocument.docType,
                    cause = e
                )
            )
        }
    }


    private fun StoreDocumentResult.notifyListener(deferredDocument: DeferredDocument, isDeferred: Boolean = false) =
        when (this) {
            is StoreDocumentResult.Success -> {
                if (isDeferred) {
                    callback(
                        DeferredIssueResult.DocumentNotReady(
                            documentId = documentId,
                            name = deferredDocument.name,
                            docType = deferredDocument.docType
                        )
                    )
                } else {
                    callback(
                        DeferredIssueResult.DocumentIssued(
                            documentId = documentId,
                            name = deferredDocument.name,
                            docType = deferredDocument.docType
                        )
                    )
                }
            }

            is StoreDocumentResult.Failure -> {
                documentManager.deleteDocumentById(deferredDocument.id)
                callback(
                    DeferredIssueResult.DocumentFailed(
                        documentId = deferredDocument.id,
                        name = deferredDocument.name,
                        docType = deferredDocument.docType,
                        cause = throwable
                    )
                )
            }
        }
}