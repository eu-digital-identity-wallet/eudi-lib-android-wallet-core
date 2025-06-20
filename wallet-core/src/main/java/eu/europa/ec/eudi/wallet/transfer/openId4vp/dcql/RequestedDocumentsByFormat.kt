/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.openid4vp.dcql.QueryId

/**
 * Type alias for a map that associates DCQL query identifiers with their corresponding documents and formats.
 *
 * This mapping is crucial for the DCQL document request processing flow as it allows:
 * - Tracking which documents match each specific credential query in the DCQL request
 * - Associating the appropriate format information with each query's documents
 * - Organizing requested documents by the query that requested them
 *
 * Used by [DcqlRequestProcessor] to organize processed requests and by [ProcessedDcqlRequest]
 * to generate properly formatted responses.
 */
typealias RequestedDocumentsByQueryId = Map<QueryId, RequestedDocumentsByFormat>

/**
 * Data structure that associates a document format with a collection of requested documents.
 *
 * This class groups documents by their format (e.g., MSO_MDOC or SD_JWT_VC) to ensure proper
 * handling during the credential presentation flow. Each format requires specific processing
 * for presentation generation.
 *
 * @property format The string identifier of the document format (e.g., "mso_mdoc", "sd_jwt_vc")
 * @property requestedDocuments Collection of documents requested in this format
 */
data class RequestedDocumentsByFormat(
    val format: String,
    val requestedDocuments: RequestedDocuments
)