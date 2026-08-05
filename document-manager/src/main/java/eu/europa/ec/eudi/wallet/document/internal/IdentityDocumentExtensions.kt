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

package eu.europa.ec.eudi.wallet.document.internal

import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlin.time.Instant
import org.multipaz.document.Document as IdentityDocument

/**
 * The application metadata
 */
internal val IdentityDocument.applicationMetadata: ApplicationMetadata
    @JvmSynthetic
    get() = metadata as ApplicationMetadata

/**
 * The document format stored in application metadata
 */
internal val IdentityDocument.format: DocumentFormat
    @JvmSynthetic
    get() = applicationMetadata.format

/**
 * The document name from the Document's native displayName field.
 * Falls back to format-specific identifiers if no display name is set.
 */
internal val IdentityDocument.documentName: String
    @JvmSynthetic
    get() = displayName ?: when (val fmt = format) {
        is MsoMdocFormat -> fmt.docType
        is SdJwtVcFormat -> fmt.vct
    }

/**
 * The issuer metadata stored in application metadata under the key "issuerMetadata"
 */
internal val IdentityDocument.issuerMetaData: IssuerMetadata?
    @JvmSynthetic
    /**
     * Get the metadata from applicationData under the key "issuerMetadata"
     * @return the metadata or null if not found or invalid
     */
    get() = runCatching { applicationMetadata.issuerMetadata }
        .onFailure { it.printStackTrace() }
        .getOrNull()

/**
 * The creation date of the document from the Document's native created field.
 */
internal val IdentityDocument.createdAt: Instant
    @JvmSynthetic
    get() = created

/**
 * The document manager id stored in application metadata
 */
internal val IdentityDocument.documentManagerId: String
    @JvmSynthetic
    get() = applicationMetadata.documentManagerId

/**
 * The issued date of the document stored in application metadata
 */
internal val IdentityDocument.issuedAt: Instant
    @JvmSynthetic
    get() = applicationMetadata.issuedAt ?: createdAt

/**
 * Deferred related data stored in application metadata under the key "deferredRelatedData"
 * set by [eu.europa.ec.eudi.wallet.document.DocumentManager.storeDeferredDocument]
 * method
 *
 * If not present, an empty byte array is returned
 */
internal val IdentityDocument.deferredRelatedData: ByteArray?
    @JvmSynthetic
    get() = runCatching { applicationMetadata.deferredRelatedData }.getOrNull()

/**
 * Extension function to convert an IdentityDocument to the appropriate Document implementation.
 *
 * This function analyzes the state of the document to determine the correct Document
 * implementation to use based on specific conditions:
 *
 * 1. If issuerMetadata is available (not null), returns an [IssuedDocument]
 *    which represents a fully issued and usable document.
 *
 * 2. If the document has deferred related data, returns a [DeferredDocument]
 *    which represents a document that's in the process of being issued.
 *
 * 3. Otherwise, returns an [UnsignedDocument] which represents a document that
 *    has been created but not yet issued.
 *
 * @param D The specific Document type to return (inferred from context)
 * @return A Document implementation of the specified type D
 * @throws ClassCastException if the determined document type cannot be cast to D
 */
@JvmSynthetic
internal inline fun <reified D : Document> IdentityDocument.toDocument(): D {
    return when {
        applicationMetadata.issuerProvidedData != null -> IssuedDocument(this)

        deferredRelatedData?.isNotEmpty() == true -> DeferredDocument(
            baseDocument = this,
            relatedData = deferredRelatedData!!
        )

        else -> UnsignedDocument(this)
    } as D
}
