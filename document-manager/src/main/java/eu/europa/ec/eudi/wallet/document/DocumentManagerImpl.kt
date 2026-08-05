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

import eu.europa.ec.eudi.wallet.document.credential.CredentialCertification
import eu.europa.ec.eudi.wallet.document.credential.CredentialFactory
import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.internal.ApplicationMetadata
import eu.europa.ec.eudi.wallet.document.internal.ApplicationMetadataImpl
import eu.europa.ec.eudi.wallet.document.internal.applicationMetadata
import eu.europa.ec.eudi.wallet.document.internal.documentManagerId
import eu.europa.ec.eudi.wallet.document.internal.toDocument
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import org.jetbrains.annotations.VisibleForTesting
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.document.buildDocumentStore
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.storage.Storage
import org.multipaz.util.Logger
import kotlin.time.Clock

/**
 * Default implementation of the [DocumentManager] interface for the EUDI Wallet.
 *
 * This implementation provides the core functionality for managing digital documents
 * in the EUDI Wallet ecosystem, including:
 * - Creation of documents with multiple supported formats (MSO mDoc, SD-JWT VC)
 * - Secure storage and retrieval of documents using provided storage mechanisms
 * - Management of document lifecycle and state transitions
 * - Integration with secure area for cryptographic operations and key management
 *
 * The implementation maintains strict document identity boundaries by using a unique
 * document manager identifier to ensure that documents managed by one instance cannot
 * be accessed or modified by another instance.
 *
 * @property identifier Unique identifier for this document manager instance, used to scope document access
 * @property storage Persistent storage implementation for document data
 * @property secureAreaRepository Repository for cryptographic operations and secure key management
 * @property ktorHttpClientFactory Optional factory to provide custom HTTP clients for network operations
 *
 * @constructor Creates a new DocumentManagerImpl with the required dependencies
 * @param identifier Unique identifier for this document manager instance
 * @param storage Storage implementation for persisting document data
 * @param secureAreaRepository Repository for secure key management and cryptographic operations
 * @param ktorHttpClientFactory Optional factory method to create HTTP clients
 */
class DocumentManagerImpl(
    override val identifier: String,
    override val storage: Storage,
    override val secureAreaRepository: SecureAreaRepository,
    val ktorHttpClientFactory: (() -> HttpClient)? = null
    // TODO: list trusted certificates
) : DocumentManager {

    /**
     * Document storage and retrieval system, lazily initialized with configured
     * credential implementations and metadata factory.
     *
     * Supports multiple credential formats including MdocCredential and SdJwtVcCredential.
     */
    @get:VisibleForTesting
    @get:JvmSynthetic
    internal val documentStore by lazy {
        buildDocumentStore(
            storage = storage,
            secureAreaRepository = secureAreaRepository,
        ) {
            setDocumentMetadataFactory(ApplicationMetadata::create)
        }
    }

    /**
     * Retrieve a document by its identifier.
     *
     * @param documentId the identifier of the document
     * @return the document or null if not found
     */
    override fun getDocumentById(documentId: DocumentId): Document? {
        return runBlocking {
            try {
                documentStore.lookupDocument(documentId)
                    ?.takeIf { doc -> doc.documentManagerId == identifier }?.toDocument()
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to lookup document with id $documentId", e)
                null
            }
        }
    }

    /**
     * Retrieve all documents.
     *
     * @param predicate a query to filter the documents
     * @return the list of documents
     */
    override fun getDocuments(predicate: ((Document) -> Boolean)?): List<Document> {
        return runBlocking {
            try {
                documentStore.listDocuments()
                    .mapNotNull {
                        getDocumentById(it.identifier)?.takeIf { doc -> doc.documentManagerId == identifier }
                    }
                    .filter {
                        when (predicate) {
                            null -> true
                            else -> predicate(it)
                        }
                    }
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to get documents", e)
                emptyList()
            }
        }
    }

    /**
     * Delete a document by its identifier.
     *
     * @param documentId the identifier of the document
     * @return the result of the deletion. If successful, it will return a proof of deletion. If not, it will return an error.
     */
    override fun deleteDocumentById(documentId: DocumentId): Outcome<ProofOfDeletion?> {
        return runBlocking {
            documentStore.lookupDocument(documentId)
                ?.let { identityDocument ->
                    val proofOfDeletion = null
                    documentStore.deleteDocument(identityDocument.identifier)
                    Outcome.success(proofOfDeletion)
                }
                ?: Outcome.failure(IllegalArgumentException("Document with $documentId not found"))
        }
    }

    /**
     * Create a new document. This method will create a new document with the given format and keys settings.
     * If the document is successfully created, it will return an [UnsignedDocument]. This [UnsignedDocument]
     * contains the keys and the method to proof the ownership of the keys, that can be used with an issuer
     * to retrieve the document's claims. After that the document can be stored using [storeIssuedDocument] or [storeDeferredDocument].
     *
     * @param format the format of the document
     * @param createSettings the [CreateDocumentSettings] to use for the new document
     * @param issuerMetadata the [IssuerMetadata] data regarding document display
     * @return the result of the creation. If successful, it will return the document. If not, it will return an error.
     */
    override fun createDocument(
        format: DocumentFormat,
        createSettings: CreateDocumentSettings,
        issuerMetadata: IssuerMetadata?
    ): Outcome<UnsignedDocument> {
        var documentId: String? = null
        return runBlocking {
            try {
                val secureArea =
                    secureAreaRepository.getImplementation(createSettings.secureAreaIdentifier)
                        ?: throw IllegalArgumentException("SecureArea '${createSettings.secureAreaIdentifier}' not registered")
                val domain = identifier
                val identityDocument = documentStore.createDocument(
                    displayName = when (format) {
                        is MsoMdocFormat -> format.docType
                        is SdJwtVcFormat -> format.vct
                    },
                    created = Clock.System.now(),
                    metadata = ApplicationMetadataImpl.create(
                        documentManagerId = identifier,
                        format = format,
                        issuerMetadata = issuerMetadata,
                        credentialPolicy = createSettings.credentialPolicy,
                    )
                )

                val factory = CredentialFactory(domain = domain, format = format)
                val (_, keyAttestation) = factory.createCredentials(
                    format = format,
                    document = identityDocument,
                    createDocumentSettings = createSettings,
                    secureArea = secureArea
                )
                keyAttestation?.let {
                    identityDocument.edit {
                        metadata = identityDocument.applicationMetadata.apply {
                            setKeyAttestation(it)
                        }
                    }
                }

                documentId = identityDocument.identifier

                Outcome.success(identityDocument.toDocument())
            } catch (e: Throwable) {
                documentId?.let { documentStore.deleteDocument(it) }
                Outcome.failure(e)
            }
        }
    }

    /**
     * Store an issued document. This method will store the document with its issuer provided data.
     *
     * @param unsignedDocument the unsigned document
     * @param issuerProvidedData the issuer provided data
     * @return the result of the storage. If successful, it will return the [IssuedDocument]. If not, it will return an error.
     */
    override fun storeIssuedDocument(
        unsignedDocument: UnsignedDocument,
        issuerProvidedData: List<IssuerProvidedCredential>
    ): Outcome<IssuedDocument> {
        return runBlocking {
            try {
                require(issuerProvidedData.isNotEmpty()) {
                    "Issuer provided data cannot be empty"
                }
                val identityDocument = unsignedDocument.baseDocument

                val credentials = identityDocument.getPendingCredentials()
                    .filterIsInstance<SecureAreaBoundCredential>()

                // check that for all credentials we have issuer provided data
                require(issuerProvidedData.size == credentials.size) {
                    "Issuer provided data size (${issuerProvidedData.size}) does not match credentials size (${credentials.size})"
                }

                val credentialCertifier = CredentialCertification(unsignedDocument.format)

                for (credential in credentials) {
                    credentialCertifier.certifyCredential(
                        credential = credential,
                        issuedCredential = issuerProvidedData.first { it.publicKeyAlias == credential.alias },
                    )
                }
                identityDocument.applicationMetadata.issue(
                    issuerProvidedData = ByteString(issuerProvidedData.first().data),
                )
                identityDocument.edit {
                    provisioned = true
                    displayName = unsignedDocument.name
                }
                Outcome.success(identityDocument.toDocument())
            } catch (e: Throwable) {
                Outcome.failure(e)
            }
        }
    }

    /**
     * Store an unsigned document for deferred issuance. This method will store the document with the related
     * to the issuance data.
     *
     * @param unsignedDocument the unsigned document
     * @param relatedData the related data
     * @return the result of the storage. If successful, it will return the [DeferredDocument]. If not, it will return an error.
     */
    override fun storeDeferredDocument(
        unsignedDocument: UnsignedDocument,
        relatedData: ByteArray
    ): Outcome<DeferredDocument> {
        return runBlocking {
            try {
                val identityDocument = documentStore.lookupDocument(unsignedDocument.id)
                    ?: throw IllegalArgumentException("Document with ${unsignedDocument.id} not found")

                identityDocument.edit {
                    displayName = unsignedDocument.name
                    metadata = identityDocument.applicationMetadata.apply {
                        issueDeferred(ByteString(relatedData))
                    }
                }
                Outcome.success(identityDocument.toDocument())
            } catch (e: Exception) {
                Outcome.failure(e)
            }
        }
    }

    companion object {
        private const val TAG = "DocumentManagerImpl"
    }
}