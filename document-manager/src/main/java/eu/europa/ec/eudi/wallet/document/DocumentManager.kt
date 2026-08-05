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

package eu.europa.ec.eudi.wallet.document

import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage

/**
 * The DocumentManager interface is the main entry point to interact with documents in the EUDI Wallet.
 * It is a high-level abstraction that provides a simplified API to manage digital documents
 * like credentials, certificates, and other identity documents.
 *
 * The DocumentManager is responsible for:
 * - Creating new documents with specific formats and security configurations
 * - Managing the document lifecycle (creation, issuance, storage, retrieval, deletion)
 * - Providing secure storage and access to documents
 * - Supporting different document formats and credential types
 *
 * Document creation follows a specific flow:
 * 1. Create an unsigned document with [createDocument]
 * 2. Use the resulting [UnsignedDocument] with an issuer to obtain certified claims
 * 3. Store the document either as:
 *    - An [IssuedDocument] with [storeIssuedDocument] when claims are immediately available
 *    - A [DeferredDocument] with [storeDeferredDocument] when issuance will complete later
 *
 * To create a DocumentManager instance, use the companion object or the [Builder] class.
 *
 * @see [Builder]
 * @see [DocumentManagerImpl] for the default implementation
 *
 * @property identifier Unique identifier for this document manager instance
 * @property storage Storage mechanism for persistent document data
 * @property secureAreaRepository Repository for secure key management and cryptographic operations
 */
interface DocumentManager {

    val identifier: String
    val storage: Storage
    val secureAreaRepository: SecureAreaRepository

    /**
     * Retrieves a document by its unique identifier.
     * 
     * This method searches for a document with the specified ID in the document store.
     * It will only return documents that are managed by this DocumentManager instance
     * (matching the DocumentManager's identifier).
     *
     * @param documentId The unique identifier of the document to retrieve
     * @return The document if found and managed by this DocumentManager, null otherwise
     */
    fun getDocumentById(documentId: DocumentId): Document?

    /**
     * Retrieves all documents managed by this DocumentManager instance.
     *
     * This method returns a list of all documents that are managed by this DocumentManager
     * (matching the DocumentManager's identifier). An optional predicate can be provided
     * to filter the results based on custom criteria.
     *
     * @param predicate Optional filter function that takes a Document and returns a boolean
     *                 indicating whether to include the document in the results
     * @return A list of documents matching the criteria, or an empty list if none found or if an error occurs
     */
    fun getDocuments(predicate: ((Document) -> Boolean)? = null): List<Document>

    /**
     * Deletes a document by its unique identifier.
     *
     * This method attempts to delete a document with the specified ID from the document store.
     * The document will only be deleted if it's managed by this DocumentManager instance.
     * In some cases, a proof of deletion may be returned upon successful deletion.
     *
     * @param documentId The unique identifier of the document to delete
     * @return An [Outcome] containing either:
     *         - A success result with an optional [ProofOfDeletion] object
     *         - A failure result with an exception (typically [IllegalArgumentException] if document not found)
     */
    fun deleteDocumentById(documentId: DocumentId): Outcome<ProofOfDeletion?>

    /**
     * Creates a new document with the specified format and security settings.
     *
     * This method initializes a new document with its security infrastructure (keys) according to the
     * provided format and creation settings. The resulting [UnsignedDocument] contains the necessary
     * keys and means to prove ownership of these keys, which can then be used to interact with an issuer
     * to obtain the document's certified claims.
     *
     * The document creation workflow typically follows these steps:
     * 1. Create an unsigned document using this method
     * 2. Use the unsigned document in an issuance protocol with a trusted issuer
     * 3. Store the resulting document using either [storeIssuedDocument] or [storeDeferredDocument]
     *
     * @param format The format specification for the document (e.g., [MsoMdocFormat], [SdJwtVcFormat])
     * @param createSettings Configuration for document creation, including security settings and credential policies
     * @param issuerMetadata Optional metadata about the issuer, useful for display and verification purposes
     * @return An [Outcome] containing either:
     *         - A success result with the created [UnsignedDocument]
     *         - A failure result with an exception describing what went wrong
     */
    fun createDocument(
        format: DocumentFormat,
        createSettings: CreateDocumentSettings,
        issuerMetadata: IssuerMetadata? = null
    ): Outcome<UnsignedDocument>

    /**
     * Stores a document that has completed the issuance process with an issuer.
     *
     * This method finalizes the document issuance process by storing the document along with
     * the issuer-provided credentials. It completes the document lifecycle from unsigned to
     * fully issued status. The document becomes ready for use in verification scenarios.
     *
     * The method performs the following key operations:
     * 1. Validates that the issuer-provided credentials match the document's pending credentials
     * 2. Certifies each credential using the appropriate credential certification handler
     * 3. Updates the document metadata (name, issuance timestamp)
     * 4. Clears any deferred issuance data that may exist
     *
     * @param unsignedDocument The unsigned document to be transformed into an issued document
     * @param issuerProvidedData List of credentials provided by the issuer containing the certified claims
     * @return An [Outcome] containing either:
     *         - A success result with the stored [IssuedDocument]
     *         - A failure result with an exception describing what went wrong
     */
    fun storeIssuedDocument(
        unsignedDocument: UnsignedDocument,
        issuerProvidedData: List<IssuerProvidedCredential>
    ): Outcome<IssuedDocument>

    /**
     * Stores an unsigned document for deferred issuance processing.
     *
     * This method is used when the document issuance process cannot be completed immediately
     * and requires additional steps or time to complete. It stores the unsigned document 
     * along with additional data needed to resume and complete the issuance process later.
     *
     * Deferred issuance is useful in scenarios where:
     * - The issuer requires multi-step verification
     * - The issuance process has a time delay
     * - Additional user actions are needed before issuance can complete
     *
     * @param unsignedDocument The unsigned document that is undergoing deferred issuance
     * @param relatedData Binary data containing information needed to resume the issuance process
     * @return An [Outcome] containing either:
     *         - A success result with the stored [DeferredDocument]
     *         - A failure result with an exception describing what went wrong
     */
    fun storeDeferredDocument(
        unsignedDocument: UnsignedDocument,
        relatedData: ByteArray
    ): Outcome<DeferredDocument>


    /**
     * Builder class to create a [DocumentManager] instance.
     * @property identifier the identifier of the document manager
     * @property storage the storage to use for storing/retrieving documents
     * @property secureAreaRepository the secure area repository
     */
    class Builder {
        var identifier: String? = null
        var storage: Storage? = null
        var secureAreaRepository: SecureAreaRepository ? = null

        /**
         * Set the identifier of the document manager.
         * @param identifier the identifier
         * @return this builder
         */
        fun setIdentifier(identifier: String): Builder = apply {
            this.identifier = identifier
        }

        /**
         * Set the storage to use for storing/retrieving documents.
         * @param storage the storage
         * @return this builder
         */
        fun setStorage(storage: Storage): Builder = apply {
            this.storage = storage
        }

        /**
         * Sets the [secureAreaRepository] that will be used for documents' keys management
         * @param secureAreaRepository the secure area repository
         * @return this builder
         */
        fun setSecureAreaRepository(secureAreaRepository: SecureAreaRepository) = apply {
            this.secureAreaRepository = secureAreaRepository
        }

        /**
         * Build a [DocumentManager] instance.
         * @throws IllegalArgumentException if the storage engine or secure area is not set
         * @return the document manager
         */
        fun build(): DocumentManager {
            requireNotNull(identifier) { "Identifier is required" }
            requireNotNull(storage) { "Storage is required" }
            requireNotNull(secureAreaRepository) { "SecureAreaRepository is required" }
            return DocumentManagerImpl(
                identifier = identifier!!,
                storage = storage!!,
                secureAreaRepository = secureAreaRepository!!
            )
        }
    }

    /**
     * Companion object to create a [DocumentManager] instance.
     */
    companion object {
        /**
         * Create a [DocumentManager] instance.
         * @param configure the builder configuration
         * @throws IllegalArgumentException if the storage engine or secure area is not set
         * @return the document manager
         */
        @JvmStatic
        operator fun invoke(configure: Builder.() -> Unit): DocumentManager {
            return Builder().apply(configure).build()
        }
    }
}