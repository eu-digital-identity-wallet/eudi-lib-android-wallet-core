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

package eu.europa.ec.eudi.wallet.document.internal

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata.Companion.fromJson
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborBuilder
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.MapBuilder
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.toDataItem
import org.multipaz.cbor.toDataItemDateTimeString
import org.multipaz.document.AbstractDocumentMetadata
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Interface for application-specific document metadata management.
 *
 * This interface extends [AbstractDocumentMetadata] to provide functionality for storing and
 * managing metadata related to documents in the EUDI wallet application. It handles document format,
 * credentials, issuer information, and the document lifecycle from creation through issuance.
 */
internal interface ApplicationMetadata : AbstractDocumentMetadata {

    /**
     * The unique identifier of the document manager handling this document.
     * @throws IllegalStateException if the document manager ID is not set
     */
    val documentManagerId: String

    /**
     * The format of the document (e.g., MSO MDL or SD-JWT VC).
     * @throws IllegalStateException if the document format is not set
     */
    val format: DocumentFormat

    /**
     * The policy that determines how credentials are handled for this document.
     * @throws IllegalStateException if the credential policy is not set
     */
    val credentialPolicy: CreateDocumentSettings.CredentialPolicy

    /**
     * The initial number of credentials for this document.
     * Derived from [credentialPolicy]'s [CreateDocumentSettings.CredentialPolicy.numberOfCredentials].
     */
    val initialCredentialsCount: Int get() = credentialPolicy.numberOfCredentials

    /**
     * Optional key attestation data in JSON format.
     */
    val keyAttestation: String?

    /**
     * Optional metadata about the issuer of the document.
     */
    val issuerMetadata: IssuerMetadata?

    /**
     * Optional data provided by the issuer during document issuance.
     */
    val issuerProvidedData: ByteArray?

    /**
     * Optional timestamp when the document was issued.
     */
    val issuedAt: Instant?

    /**
     * Optional data related to deferred issuance.
     */
    val deferredRelatedData: ByteArray?

    /**
     * Updates the metadata with issuer-provided data and marks the issuance timestamp.
     * Clears any deferred issuance data.
     *
     * Note: The caller is responsible for persisting changes via [Document.edit].
     *
     * @param issuerProvidedData Data provided by the issuer
     */
    fun issue(issuerProvidedData: ByteString)

    /**
     * Sets deferred issuance related data.
     *
     * Note: The caller is responsible for persisting changes via [Document.edit].
     *
     * @param deferredRelatedData Data related to deferred issuance
     */
    fun issueDeferred(deferredRelatedData: ByteString)

    /**
     * Updates the key attestation data for the document.
     *
     * Note: The caller is responsible for persisting changes via [Document.edit].
     *
     * @param keyAttestation Key attestation data in JSON format
     */
    fun setKeyAttestation(keyAttestation: String)

    companion object {
        /**
         * The factory for [ApplicationMetadata].
         *
         * @param documentId the document to create metadata for.
         * @param serializedData the serialized metadata.
         * @return the created [ApplicationMetadata]
         */
        fun create(
            documentId: String,
            serializedData: ByteString?,
        ): ApplicationMetadata = ApplicationMetadataImpl.create(serializedData)
    }

}


/**
 * Implementation of [ApplicationMetadata].
 *
 * This class manages application-specific document metadata by storing it in a serializable
 * [Data] object. It is a pure data object with no persistence awareness.
 *
 * Mutation methods ([issue], [issueDeferred], [setKeyAttestation]) update the in-memory state only.
 * The caller is responsible for persisting changes via [Document.edit], which automatically
 * calls [serialize] to write the updated metadata to storage.
 */
internal class ApplicationMetadataImpl private constructor(
    serializedData: ByteString?
) : ApplicationMetadata {

    private var data: Data = serializedData?.let { Data.fromCbor(it) } ?: Data()

    override fun serialize(): ByteString = data.toCbor()

    override val documentManagerId: String
        get() = data.documentManagerId ?: throw IllegalStateException("Document manager ID not set")
    override val format: DocumentFormat
        get() = data.format ?: throw IllegalStateException("Document format not set")
    override val credentialPolicy: CreateDocumentSettings.CredentialPolicy
        get() = data.credentialPolicy ?: throw IllegalStateException("Credential policy not set")
    override val keyAttestation: String?
        get() = data.keyAttestation
    override val issuerMetadata: IssuerMetadata? get() = data.issuerMetadata
    override val issuerProvidedData: ByteArray? get() = data.issuerProvidedData?.toByteArray()
    override val issuedAt: Instant? get() = data.issuedAt
    override val deferredRelatedData: ByteArray? get() = data.deferredRelatedData?.toByteArray()

    /**
     * Internal data class for storing metadata fields in a serializable format.
     *
     * This class provides methods for CBOR serialization and deserialization of metadata.
     */
    internal data class Data(
        val documentManagerId: String? = null,
        val format: DocumentFormat? = null,
        val credentialPolicy: CreateDocumentSettings.CredentialPolicy? = null,
        val keyAttestation: String? = null,
        val issuerMetadata: IssuerMetadata? = null,
        val issuerProvidedData: ByteString? = null,
        val issuedAt: Instant? = null,
        val deferredRelatedData: ByteString? = null,
    ) {

        /**
         * Converts this Data object to a CBOR encoded ByteString.
         *
         * @return CBOR representation of the metadata
         */
        fun toCbor(): ByteString {
            val builder = CborMap.builder()

            builder.putIfNotNull("documentManagerId", documentManagerId) { Tstr(it) }
            builder.putIfNotNull("format", format) { it.toDataItem() }
            builder.putIfNotNull("credentialPolicy", credentialPolicy) { it.toDataItem() }
            builder.putIfNotNull("keyAttestation", keyAttestation) { Tstr(it) }
            builder.putIfNotNull("issuerMetadata", issuerMetadata) { Tstr(it.toJson()) }
            builder.putIfNotNull(
                "issuerProvidedData",
                issuerProvidedData
            ) { Bstr(it.toByteArray()) }
            builder.putIfNotNull("issuedAt", issuedAt) { it.toDataItemDateTimeString() }
            builder.putIfNotNull(
                "deferredRelatedData",
                deferredRelatedData
            ) { Bstr(it.toByteArray()) }

            val dataItem = builder.end().build()
            return ByteString(Cbor.encode(dataItem))
        }

        companion object {
            /**
             * Creates a Data object from CBOR encoded ByteString.
             *
             * @param cbor The CBOR encoded metadata
             * @return Deserialized Data object
             */
            fun fromCbor(cbor: ByteString): Data {
                val dataItem = Cbor.decode(cbor.toByteArray())

                return Data(
                    format = DocumentFormat.fromDataItem(dataItem["format"]),
                    documentManagerId = dataItem["documentManagerId"].asTstr,
                    credentialPolicy = CreateDocumentSettings.CredentialPolicy.fromDataItem(dataItem["credentialPolicy"]),
                    keyAttestation = dataItem.getValue("keyAttestation") { it.asTstr },
                    issuerMetadata = dataItem.getValue("issuerMetadata") {
                        fromJson(
                            it.asTstr
                        ).getOrNull()
                    },
                    issuerProvidedData = dataItem.getValue("issuerProvidedData") { ByteString(it.asBstr) },
                    issuedAt = dataItem.getValue("issuedAt") { it.asDateTimeString },
                    deferredRelatedData = dataItem.getValue("deferredRelatedData") { ByteString(it.asBstr) },
                )
            }
        }
    }

    /**
     * Updates the metadata with issuer-provided data.
     * Clears any deferred issuance data and sets the issuance timestamp.
     */
    override fun issue(issuerProvidedData: ByteString) {
        data = data.copy(
            issuerProvidedData = issuerProvidedData,
            deferredRelatedData = null,
            issuedAt = Clock.System.now(),
        )
    }

    /**
     * Sets deferred issuance related data.
     */
    override fun issueDeferred(deferredRelatedData: ByteString) {
        data = data.copy(
            deferredRelatedData = deferredRelatedData,
        )
    }

    /**
     * Updates the key attestation data.
     */
    override fun setKeyAttestation(keyAttestation: String) {
        data = data.copy(
            keyAttestation = keyAttestation
        )
    }

    companion object {
        /**
         * Factory method for restoring [ApplicationMetadataImpl] from serialized data.
         *
         * @param serializedData Optional previously serialized metadata
         * @return A new or restored [ApplicationMetadataImpl] instance
         */
        fun create(
            serializedData: ByteString?,
        ): ApplicationMetadataImpl {
            return ApplicationMetadataImpl(serializedData)
        }

        /**
         * Factory method for creating a fully initialized [ApplicationMetadataImpl].
         *
         * @return A new [ApplicationMetadataImpl] instance with all metadata fields set
         */
        fun create(
            documentManagerId: String,
            format: DocumentFormat,
            credentialPolicy: CreateDocumentSettings.CredentialPolicy,
            issuerMetadata: IssuerMetadata? = null,
            keyAttestation: String? = null,
        ): ApplicationMetadataImpl {
            return ApplicationMetadataImpl(null).apply {
                data = Data(
                    documentManagerId = documentManagerId,
                    format = format,
                    credentialPolicy = credentialPolicy,
                    issuerMetadata = issuerMetadata,
                    keyAttestation = keyAttestation,
                )
            }
        }
    }
}

/**
 * Utility extension function to add a key-value pair to a CBOR map builder only if the value is not null.
 *
 * @param key The key for the map entry
 * @param value The value to add if not null
 * @param transform Function to transform the value to a CBOR DataItem
 */
private inline fun <T> MapBuilder<CborBuilder>.putIfNotNull(
    key: String,
    value: T?,
    transform: (T) -> DataItem
) {
    if (value != null) {
        put(key, transform(value))
    }
}

/**
 * Utility extension function to safely extract a value from a CBOR DataItem by key.
 *
 * @param key The key to look up in the CBOR map
 * @param extractor Function to extract and convert the value from the DataItem
 * @return The extracted value, or null if the key is not present
 */
private inline fun <T> DataItem.getValue(key: String, extractor: (DataItem) -> T?): T? =
    if (hasKey(key)) extractor(this[key]) else null