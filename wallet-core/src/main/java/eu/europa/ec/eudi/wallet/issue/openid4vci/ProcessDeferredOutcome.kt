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

import eu.europa.ec.eudi.openid4vci.DeferredCredentialQueryOutcome
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger

internal class ProcessDeferredOutcome(
    val documentManager: DocumentManager,
    val callback: OpenId4VciManager.OnResult<DeferredIssueResult>,
    val deferredContext: DeferredContext?,
    val logger: Logger? = null,
) {

    fun process(
        deferredDocument: DeferredDocument,
        keyAliases: List<String>,
        outcome: DeferredCredentialQueryOutcome
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
                        documentManager.storeDeferredDocument(deferredDocument, ctx.toByteArray())
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
}