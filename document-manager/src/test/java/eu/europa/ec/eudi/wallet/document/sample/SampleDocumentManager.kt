/*
 * Copyright (c) 2023-2024 European Commission
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
@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet.document.sample

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocType
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome

/**
 * An extension of [DocumentManager] that provides methods to load sample data.
 *
 * The sample data is a CBOR file that contains the following information:
 * - A list of documents to be loaded in the document manager.
 * - Each document contains:
 *  - The document's docType
 *  - A list of namespaces and element identifiers with their corresponding values and random values and digest values to be used in the hash computation.
 *
 * A default implementation is provided by [SampleDocumentManagerImpl]. To instantiate a [SampleDocumentManagerImpl], use the [Builder] class.
 *
 * ```
 * val documentManager = SampleDocumentManager.Builder(context)
 *      .hardwareBacked(true)
 *      .build()
 * ```
 *
 * You can also use a different [DocumentManager] implementation, by passing it to the [Builder] class.
 * By default, the [DocumentManager] implementation used is [eu.europa.ec.eudi.wallet.document.DocumentManagerImpl].
 *
 * ```
 * val sampleDocumentManager = SampleDocumentManager.Builder(context)
 *     documentManager = MyDocumentManager()
 * }.build()
 * ```
 *
 */
interface SampleDocumentManager : DocumentManager {
    /**
     * Loads the sample documents that are in mdoc format into the document manager.
     *
     * @param sampleData the sample data in mdoc format to be loaded in cbor format
     * @param createSettings the settings for creating new documents for the sample
     * @param documentNamesMap the names of the documents per docType
     * @return returns the documentIds if successfully loaded, otherwise a error
     *
     * Expected sampleData format is CBOR. The CBOR data must be in the following structure:
     *
     * ```cddl
     * SampleData = {
     *   "documents" : [+Document], ; Returned documents
     * }
     * Document = {
     *   "docType" : DocType, ; Document type returned
     *   "issuerSigned" : IssuerSigned, ; Returned data elements signed by the issuer
     * }
     * IssuerSigned = {
     *   "nameSpaces" : IssuerNameSpaces, ; Returned data elements
     *   "issuerAuth" : IssuerAuth ; Contains the mobile security object (MSO) for issuer data authentication
     * }
     * IssuerNameSpaces = { ; Returned data elements for each namespace
     *   + NameSpace => [ + IssuerSignedItemBytes ]
     * }
     * IssuerSignedItemBytes = #6.24(bstr .cbor IssuerSignedItem)
     * IssuerSignedItem = {
     *   "digestID" : uint, ; Digest ID for issuer data authentication
     *   "random" : bstr, ; Random value for issuer data authentication
     *   "elementIdentifier" : DataElementIdentifier, ; Data element identifier
     *   "elementValue" : DataElementValue ; Data element value
     * }
     * IssuerAuth = COSE_Sign1 ; The payload is MobileSecurityObjectBytes
     * ```
     */
    fun loadMdocSampleDocuments(
        sampleData: ByteArray,
        createSettings: CreateDocumentSettings,
        documentNamesMap: Map<DocType, String>? = null
    ): Outcome<List<DocumentId>>

    /**
     * Builder class to instantiate a SampleDocumentManager.
     *
     * @property setDocumentManager [DocumentManager] implementation to delegate the document management operations
     *
     * example:
     * ```
     * val sampleDocumentManager = SampleDocumentManager.Builder(context)
     *    .setDocumentManager(documentManager)
     *    .build()
     * ```
     */
    class Builder() {

        var documentManager: DocumentManager? = null

        /**
         * Sets the [DocumentManager] implementation to delegate the document management operations.
         */
        fun setDocumentManager(documentManager: DocumentManager) =
            apply { this.documentManager = documentManager }

        /**
         * Builds the SampleDocumentManager.
         *
         * @return [SampleDocumentManager]
         */
        fun build(): SampleDocumentManager {
            requireNotNull(documentManager) {
                "DocumentManager implementation must be set"
            }
            return SampleDocumentManagerImpl(documentManager!!)
        }
    }

    companion object {
        operator fun invoke(configure: Builder.() -> Unit): SampleDocumentManager {
            return Builder().apply(configure).build()
        }

        fun build(configure: DocumentManager.Builder.() -> Unit): SampleDocumentManager {
            return Builder().apply {
                documentManager = DocumentManager.Builder().apply(configure).build()
            }.build()
        }
    }
}
