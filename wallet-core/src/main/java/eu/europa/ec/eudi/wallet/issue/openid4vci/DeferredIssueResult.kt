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

import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument

/**
 * Result of a deferred document issuance.
 * @property documentId the id of the document
 * @property name the name of the document
 * @property docType the document type
 */
sealed interface DeferredIssueResult : OpenId4VciResult {

    val document: Document
    val documentId: DocumentId
    val name: String
    val docType: String

    /**
     * Document issued successfully.
     * @property documentId the id of the issued document
     * @property name the name of the document
     * @property docType the document type
     * @see[DocumentId] for the document id
     */
    data class DocumentIssued(
        override val document: IssuedDocument,
    ) : DeferredIssueResult, DocumentDetails by DocumentDetails(document)

    /**
     * Document issuance failed.
     * @property documentId the id of the failed document
     * @property name the name of the document
     * @property docType the document type
     * @property cause the error that caused the failure
     */
    data class DocumentFailed(
        override val document: Document,
        override val cause: Throwable,
    ) : DeferredIssueResult,
        DocumentDetails by DocumentDetails(document),
        OpenId4VciResult.Erroneous

    /**
     * Document issuance deferred.
     * @property documentId the id of the deferred document
     * @property name the name of the document
     * @property docType the document type
     */
    data class DocumentNotReady(
        override val document: DeferredDocument,
    ) : DeferredIssueResult, DocumentDetails by DocumentDetails(document)

    /**
     * Document issuance expired.
     * @property documentId the id of the expired document
     * @property name the name of the document
     * @property docType the document type
     */
    data class DocumentExpired(
        override val document: DeferredDocument,
    ) : DeferredIssueResult, DocumentDetails by DocumentDetails(document)
}
