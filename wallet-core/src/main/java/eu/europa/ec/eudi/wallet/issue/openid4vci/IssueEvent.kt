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

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea

/**
 * Events related to document issuance.
 */
sealed interface IssueEvent : OpenId4VciResult {

    /**
     * The issuance was started.
     * @property total the total number of documents to issue
     */
    data class Started(val total: Int) : IssueEvent

    /**
     * The issuance has finished.
     * @property issuedDocuments the ids of the issued documents
     * @see[DocumentId] for the document id
     */
    data class Finished(val issuedDocuments: List<DocumentId>) : IssueEvent

    /**
     * The issuance failed.
     * @property cause the error that caused the failure
     */
    data class Failure(override val cause: Throwable) : IssueEvent, OpenId4VciResult.Erroneous

    /**
     * Document issued successfully.
     * @property document the issued document
     * @property documentId the id of the issued document
     * @property name the name of the document
     * @property docType the document type
     * @see[DocumentId] for the document id
     */
    data class DocumentIssued(val document: IssuedDocument) :
        IssueEvent,
        DocumentDetails by DocumentDetails(document)

    /**
     * Document issuance failed.
     * @property name the name of the document
     * @property docType the document type
     * @property cause the error that caused the failure
     */
    data class DocumentFailed(
        private val document: UnsignedDocument,
        override val cause: Throwable,
    ) : IssueEvent,
        DocumentDetails by DocumentDetails(document),
        OpenId4VciResult.Erroneous

    /**
     * Issuing requires [CreateDocumentSettings] to create the document that will be issued
     * for the [offeredDocument].
     * @property offeredDocument the offered document
     * @property resume the callback to resume the issuance with the [CreateDocumentSettings]
     *  that will be used to create the document
     * @property cancel the callback to cancel the issuance with an optional reason
     */
    data class DocumentRequiresCreateSettings(
        val offeredDocument: Offer.OfferedDocument,
        val resume: (createDocumentSettings: CreateDocumentSettings) -> Unit,
        val cancel: (reason: String?) -> Unit,
    ) : IssueEvent

    /**
     * Document requires user authentication to unlock the key for signing the proof of possession.
     * @property document the document that requires user authentication
     * @property resume the callback to resume the issuance with the [KeyUnlockData] that will be
     *  used to unlock the key
     * @property cancel the callback to cancel the issuance with an optional reason
     * @property documentId the id of the document
     * @property name the name of the document
     * @property docType the document type
     */
    data class DocumentRequiresUserAuth(
        val document: UnsignedDocument,
        val signingAlgorithm: Algorithm,
        val keysRequireAuth: Map<KeyAlias, SecureArea>,
        val resume: (keyUnlockData: Map<KeyAlias, KeyUnlockData?>) -> Unit,
        val cancel: (reason: String?) -> Unit,
    ) : IssueEvent,
        DocumentDetails by DocumentDetails(document)

    /**
     * Document issuance deferred.
     * @property document the deferred document
     * @property documentId the id of the deferred document
     * @property name the name of the document
     * @property docType the document type
     */
    data class DocumentDeferred(val document: DeferredDocument) :
        IssueEvent,
        DocumentDetails by DocumentDetails(document)

    companion object {
        internal fun failure(
            cause: Throwable,
            unsignedDocument: UnsignedDocument? = null,
        ): IssueEvent =
            when (unsignedDocument) {
                null -> Failure(cause)
                else -> DocumentFailed(unsignedDocument, cause)
            }
    }
}
