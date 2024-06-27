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

import eu.europa.ec.eudi.wallet.document.DocumentId

sealed interface DeferredIssueResult : OpenId4VciResult {
    /**
     * Document issued successfully.
     * @property documentId the id of the issued document
     * @property name the name of the document
     * @property docType the document type
     * @see[DocumentId] for the document id
     */
    data class DocumentIssued(val documentId: DocumentId, val name: String, val docType: String) : DeferredIssueResult

    /**
     * Document issuance failed.
     * @property name the name of the document
     * @property docType the document type
     * @property cause the error that caused the failure
     */
    data class DocumentFailed(val documentId: DocumentId, override val cause: Throwable) : DeferredIssueResult,
        OpenId4VciResult.Erroneous

    /**
     * Document issuance deferred.
     * @property documentId the id of the deferred document
     * @property name the name of the document
     * @property docType the document type
     */
    data class DocumentNotReady(val documentId: DocumentId, val name: String, val docType: String) : DeferredIssueResult
}
