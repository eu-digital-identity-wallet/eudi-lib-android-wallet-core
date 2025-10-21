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

package eu.europa.ec.eudi.wallet.statium

import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat

/**
 * Interface for extracting revocation status data from documents
 */
fun interface StatusReferenceExtractor {
    /**
     * Extracts revocation status data from the provided document
     * @param document The document to extract revocation status data from
     * @return Result containing the extracted revocation status data or an error
     */
    suspend fun extractStatusReference(document: IssuedDocument): Result<StatusReference>
}

/**
 * Default implementation of [StatusReferenceExtractor]
 * It supports the following formats:
 * - [MsoMdocFormat]
 * - [SdJwtVcFormat]
 *
 * It delegates the extraction to the appropriate extractor based on the document format.
 *
 * @see [MsoMdocStatusReferenceExtractor]
 * @see [SdJwtStatusReferenceExtractor]
 */
object DefaultStatusReferenceExtractor : StatusReferenceExtractor {
    /**
     * Extracts status reference from the provided document
     *
     * @param document The document to extract status reference from
     * @return Result containing the extracted status reference or an error
     */
    override suspend fun extractStatusReference(document: IssuedDocument): Result<StatusReference> {
        return when (document.format) {
            is MsoMdocFormat -> MsoMdocStatusReferenceExtractor
            is SdJwtVcFormat -> SdJwtStatusReferenceExtractor
        }.extractStatusReference(document)
    }

}
