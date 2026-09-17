/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData

import android.annotation.SuppressLint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Transaction data with which a relying party asks the user to approve a signature it has already
 * prepared, as defined by ETSI TS 119 432 clause B.6.2 and CSC Data Model clause 10.1.
 *
 * The documents themselves are not carried; the relying party sends their digests in
 * [documentDigests] and the signature is created by the trust service provider that holds the
 * signing credential.
 *
 * @property type the transaction data type, always [TYPE]
 * @property credentialIds the DCQL query identifiers of the Credentials that may authorize the
 * signature
 * @property hashAlgorithms the hash algorithms the relying party accepts for the transaction data
 * hashes, as IANA names
 * @property locations the identifiers of the trust service providers the request is addressed to
 * @property credentialId the identifier of the signing credential to use
 * @property signatureQualifier the kind of signature to create, for example `eu_eidas_qes`
 * @property numSignatures the number of signatures to create with the user's authorization
 * @property documentDigests the digests of the documents to be signed
 * @property hashAlgorithmOid the object identifier of the algorithm that produced
 * [DocumentDigest.hash]
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class QesApprovalRequest(
    @SerialName("type")
    val type: String,
    @SerialName("credential_ids")
    val credentialIds: List<String>,
    @SerialName("transaction_data_hashes_alg")
    val hashAlgorithms: List<String>? = null,
    @SerialName("locations")
    val locations: List<String>? = null,
    @SerialName("credentialID")
    val credentialId: String? = null,
    @SerialName("signatureQualifier")
    val signatureQualifier: String? = null,
    @SerialName("numSignatures")
    val numSignatures: Int,
    @SerialName("documentDigests")
    val documentDigests: List<DocumentDigest>,
    @SerialName("hashAlgorithmOID")
    val hashAlgorithmOid: String,
) {
    init {
        require(type == TYPE) { "QesApprovalRequest: 'type' must be '$TYPE', was '$type'" }
        require(credentialIds.isNotEmpty()) {
            "QesApprovalRequest: 'credential_ids' must not be empty"
        }
        require(hashAlgorithms == null || hashAlgorithms.isNotEmpty()) {
            "QesApprovalRequest: 'transaction_data_hashes_alg' must not be empty"
        }
        require(locations == null || locations.isNotEmpty()) {
            "QesApprovalRequest: 'locations' must not be empty"
        }
        require(credentialId != null || signatureQualifier != null) {
            "QesApprovalRequest: either 'credentialID' or 'signatureQualifier' must be present"
        }
        require(numSignatures > 0) { "QesApprovalRequest: 'numSignatures' must be positive" }
        require(documentDigests.isNotEmpty()) {
            "QesApprovalRequest: 'documentDigests' must not be empty"
        }
        require(hashAlgorithmOid.isNotBlank()) {
            "QesApprovalRequest: 'hashAlgorithmOID' must not be blank"
        }
    }

    companion object {
        /** The `type` of transaction data described by [QesApprovalRequest]. */
        const val TYPE: String = "https://cloudsignatureconsortium.org/2025/qes-approval"
    }
}

/**
 * Transaction data with which a relying party asks the wallet to have one or more documents signed,
 * as defined by ETSI TS 119 432 clause A.6.4 and CSC data model bindings clause 6.2.1.
 *
 * Unlike [QesApprovalRequest] the documents are carried in the request, and the wallet drives
 * the signature and returns it to the relying party.
 *
 * @property type the transaction data type, always [TYPE]
 * @property credentialIds the DCQL query identifiers of the Credentials that may authorize the
 * signature
 * @property hashAlgorithms the hash algorithms the relying party accepts for the transaction data
 * hashes, as IANA names
 * @property signatureRequests the signatures to create, one per document
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class QesRequest(
    @SerialName("type")
    val type: String,
    @SerialName("credential_ids")
    val credentialIds: List<String>,
    @SerialName("transaction_data_hashes_alg")
    val hashAlgorithms: List<String>? = null,
    @SerialName("signatureRequests")
    val signatureRequests: List<SignatureRequest>,
) {
    init {
        require(type == TYPE) { "QesRequest: 'type' must be '$TYPE', was '$type'" }
        require(credentialIds.isNotEmpty()) { "QesRequest: 'credential_ids' must not be empty" }
        require(hashAlgorithms == null || hashAlgorithms.isNotEmpty()) {
            "QesRequest: 'transaction_data_hashes_alg' must not be empty"
        }
        require(signatureRequests.isNotEmpty()) {
            "QesRequest: 'signatureRequests' must not be empty"
        }
    }

    companion object {
        /** The `type` of transaction data described by [QesRequest]. */
        const val TYPE: String = "https://cloudsignatureconsortium.org/2025/qes"
    }
}

/**
 * The digest of a document to be signed, with the information needed to show the document to the
 * user and, when the document can be retrieved, to check what was retrieved.
 *
 * @property label the name of the document, to designate it in the user's approval
 * @property hash the digest of the document, produced with [QesApprovalRequest.hashAlgorithmOid]
 * @property hashType what [hash] was calculated over; one of [HASH_TYPE_SDR], [HASH_TYPE_DTBSR] or
 * [HASH_TYPE_SODR]
 * @property signedProperties the attributes to be included in the signature
 * @property circumstantialData data about the circumstances under which the signature is created
 * @property href where the document can be retrieved from
 * @property checksum the digest with which the retrieved document is checked
 * @property access how the document at [href] is accessed
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DocumentDigest(
    @SerialName("label")
    val label: String? = null,
    @SerialName("hash")
    val hash: String,
    @SerialName("hashType")
    val hashType: String = HASH_TYPE_DTBSR,
    @SerialName("signed_props")
    val signedProperties: List<Attribute>? = null,
    @SerialName("circumstantialData")
    val circumstantialData: String? = null,
    @SerialName("href")
    val href: String? = null,
    @SerialName("checksum")
    val checksum: Checksum? = null,
    @SerialName("access")
    val access: AccessControlMethod? = null,
) {
    init {
        require(label == null || label.isNotBlank()) { "DocumentDigest: 'label' must not be blank" }
        require(hash.isNotBlank()) { "DocumentDigest: 'hash' must not be blank" }
        require(hashType in HASH_TYPES) {
            "DocumentDigest: 'hashType' must be one of $HASH_TYPES, was '$hashType'"
        }
        require(signedProperties == null || signedProperties.isNotEmpty()) {
            "DocumentDigest: 'signed_props' must not be empty"
        }
        require(href == null || href.isNotBlank()) { "DocumentDigest: 'href' must not be blank" }
    }

    companion object {
        /** The digest is the signer's document representation, of the formatted document. */
        const val HASH_TYPE_SDR: String = "sdr"

        /** The digest is the data to be signed representation. */
        const val HASH_TYPE_DTBSR: String = "dtbsr"

        /** The digest is of the signer's original document, before any formatting. */
        const val HASH_TYPE_SODR: String = "sodr"

        /** The values [hashType] accepts. */
        val HASH_TYPES: Set<String> = setOf(HASH_TYPE_SDR, HASH_TYPE_DTBSR, HASH_TYPE_SODR)
    }
}

/**
 * A digest with which a retrieved document is checked.
 *
 * @property value the digest
 * @property algorithmOid the object identifier of the algorithm that produced [value]
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Checksum(
    @SerialName("value")
    val value: String,
    @SerialName("algorithmOID")
    val algorithmOid: String,
) {
    init {
        require(value.isNotBlank()) { "Checksum: 'value' must not be blank" }
        require(algorithmOid.isNotBlank()) { "Checksum: 'algorithmOID' must not be blank" }
    }
}

/**
 * How a document is accessed.
 *
 * @property accessMode the access method, for example [ACCESS_MODE_PUBLIC]
 * @property oneTimePassword the password to access the document, required for
 * [ACCESS_MODE_ONE_TIME_PASSWORD]
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AccessControlMethod(
    @SerialName("type")
    val accessMode: String,
    @SerialName("oneTimePassword")
    val oneTimePassword: String? = null,
) {
    init {
        require(accessMode.isNotBlank()) { "AccessControlMethod: 'type' must not be blank" }
        require(accessMode != ACCESS_MODE_ONE_TIME_PASSWORD || oneTimePassword != null) {
            "AccessControlMethod: 'oneTimePassword' is required for '$ACCESS_MODE_ONE_TIME_PASSWORD'"
        }
    }

    companion object {
        /** The document is accessed without credentials. */
        const val ACCESS_MODE_PUBLIC: String = "public"

        /** The document is accessed with a single use password. */
        const val ACCESS_MODE_ONE_TIME_PASSWORD: String = "OTP"

        /** The document is accessed with HTTP basic authentication. */
        const val ACCESS_MODE_BASIC_AUTHENTICATION: String = "Basic_Auth"

        /** The document is accessed with HTTP digest authentication. */
        const val ACCESS_MODE_DIGEST_AUTHENTICATION: String = "Digest_Auth"

        /** The document is accessed with OAuth 2.0. */
        const val ACCESS_MODE_OAUTH20: String = "OAuth_20"
    }
}

/**
 * An attribute to be included in the signature, as defined by CSC Data Model clause 7.2.
 *
 * @property name the name of the attribute
 * @property value the value of the attribute
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Attribute(
    @SerialName("attribute_name")
    val name: String,
    @SerialName("attribute_value")
    val value: String? = null,
) {
    init {
        require(name.isNotBlank()) { "Attribute: 'attribute_name' must not be blank" }
    }
}

/**
 * One signature requested by [QesRequest]. The document to sign is either carried in the request,
 * as [WithDocument], or retrieved by the wallet, as [WithDocumentReference].
 *
 * @property label the name of the document, to designate it in the user's approval
 * @property signatureQualifier the kind of signature to create, for example `eu_eidas_qes`
 * @property responseUri where the created signature is sent
 * @property signatureFormat the format of the signature; one of [SIGNATURE_FORMAT_CADES],
 * [SIGNATURE_FORMAT_XADES], [SIGNATURE_FORMAT_PADES] or [SIGNATURE_FORMAT_JADES]
 * @property conformanceLevel the conformance level of the signature, for example `AdES-B-B`
 * @property signedEnvelopeProperty how the signature relates to the signed document
 * @property signedProperties the attributes to be included in the signature
 * @property referenceUri the reference to the signed document, for a detached signature
 * @property circumstantialData data about the circumstances under which the signature is created
 * @property signAlgo the object identifier of the signing algorithm
 * @property signAlgoParams the parameters of the signing algorithm
 */
@Serializable(with = SignatureRequestSerializer::class)
sealed interface SignatureRequest {
    val label: String?
    val signatureQualifier: String
    val responseUri: String?
    val signatureFormat: String?
    val conformanceLevel: String?
    val signedEnvelopeProperty: String?
    val signedProperties: List<Attribute>?
    val referenceUri: String?
    val circumstantialData: String?
    val signAlgo: String
    val signAlgoParams: String?

    /**
     * A signature of a document carried in the request.
     *
     * @property document the document to sign, base64-encoded
     * @property documentType whether [document] is the signer's original document,
     * [DOCUMENT_TYPE_ORIGINAL], or the formatted document, [DOCUMENT_TYPE_FORMATTED]
     */
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class WithDocument(
        @SerialName("signatureQualifier")
        override val signatureQualifier: String,
        @SerialName("responseURI")
        override val responseUri: String? = null,
        @SerialName("signature_format")
        override val signatureFormat: String? = null,
        @SerialName("conformance_level")
        override val conformanceLevel: String? = null,
        @SerialName("signed_envelope_property")
        override val signedEnvelopeProperty: String? = null,
        @SerialName("signed_props")
        override val signedProperties: List<Attribute>? = null,
        @SerialName("referenceUri")
        override val referenceUri: String? = null,
        @SerialName("label")
        override val label: String? = null,
        @SerialName("document")
        val document: String,
        @SerialName("documentType")
        val documentType: String = DOCUMENT_TYPE_ORIGINAL,
        @SerialName("circumstantialData")
        override val circumstantialData: String? = null,
        @SerialName("signAlgo")
        override val signAlgo: String,
        @SerialName("signAlgoParams")
        override val signAlgoParams: String? = null,
    ) : SignatureRequest {
        init {
            validate()
            require(document.isNotBlank()) { "SignatureRequest: 'document' must not be blank" }
            require(documentType in DOCUMENT_TYPES) {
                "SignatureRequest: 'documentType' must be one of $DOCUMENT_TYPES, " +
                        "was '$documentType'"
            }
        }
    }

    /**
     * A signature of a document the wallet retrieves.
     *
     * @property href where the document can be retrieved from
     * @property access how the document at [href] is accessed
     * @property checksum the digest with which the retrieved document is checked
     */
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class WithDocumentReference(
        @SerialName("signatureQualifier")
        override val signatureQualifier: String,
        @SerialName("responseURI")
        override val responseUri: String? = null,
        @SerialName("signature_format")
        override val signatureFormat: String? = null,
        @SerialName("conformance_level")
        override val conformanceLevel: String? = null,
        @SerialName("signed_envelope_property")
        override val signedEnvelopeProperty: String? = null,
        @SerialName("signed_props")
        override val signedProperties: List<Attribute>? = null,
        @SerialName("referenceUri")
        override val referenceUri: String? = null,
        @SerialName("label")
        override val label: String? = null,
        @SerialName("access")
        val access: AccessControlMethod? = null,
        @SerialName("href")
        val href: String,
        @SerialName("checksum")
        val checksum: Checksum? = null,
        @SerialName("circumstantialData")
        override val circumstantialData: String? = null,
        @SerialName("signAlgo")
        override val signAlgo: String,
        @SerialName("signAlgoParams")
        override val signAlgoParams: String? = null,
    ) : SignatureRequest {
        init {
            validate()
            require(href.isNotBlank()) { "SignatureRequest: 'href' must not be blank" }
        }
    }

    companion object {
        /** The name of the field that carries the document in [WithDocument]. */
        const val DOCUMENT: String = "document"

        /** The name of the field that carries the document location in [WithDocumentReference]. */
        const val HREF: String = "href"

        /** A CAdES signature. */
        const val SIGNATURE_FORMAT_CADES: String = "C"

        /** An XAdES signature. */
        const val SIGNATURE_FORMAT_XADES: String = "X"

        /** A PAdES signature. */
        const val SIGNATURE_FORMAT_PADES: String = "P"

        /** A JAdES signature. */
        const val SIGNATURE_FORMAT_JADES: String = "J"

        /** The values [signatureFormat] accepts. */
        val SIGNATURE_FORMATS: Set<String> = setOf(
            SIGNATURE_FORMAT_CADES,
            SIGNATURE_FORMAT_XADES,
            SIGNATURE_FORMAT_PADES,
            SIGNATURE_FORMAT_JADES,
        )

        /** The values [signedEnvelopeProperty] accepts. */
        val SIGNED_ENVELOPE_PROPERTIES: Set<String> = setOf(
            "Detached",
            "Attached",
            "Parallel",
            "Certification",
            "Revision",
            "Enveloped",
            "Enveloping",
        )

        /** The values [conformanceLevel] accepts. */
        val CONFORMANCE_LEVELS: Set<String> = setOf(
            "AdES-B-B",
            "AdES-B-T",
            "AdES-B-LT",
            "AdES-B-LTA",
            "AdES-B",
            "AdES-T",
            "AdES-LT",
            "AdES-LTA",
        )

        /** The document is the signer's original document, before any formatting. */
        const val DOCUMENT_TYPE_ORIGINAL: String = "sod"

        /** The document is the signer's formatted document. */
        const val DOCUMENT_TYPE_FORMATTED: String = "sfd"

        /** The values [WithDocument.documentType] accepts. */
        val DOCUMENT_TYPES: Set<String> = setOf(DOCUMENT_TYPE_ORIGINAL, DOCUMENT_TYPE_FORMATTED)

        private fun SignatureRequest.validate() {
            val label = label
            val responseUri = responseUri
            val signatureFormat = signatureFormat
            val conformanceLevel = conformanceLevel
            val signedEnvelopeProperty = signedEnvelopeProperty
            val signedProperties = signedProperties
            require(label == null || label.isNotBlank()) {
                "SignatureRequest: 'label' must not be blank"
            }
            require(signatureQualifier.isNotBlank()) {
                "SignatureRequest: 'signatureQualifier' must not be blank"
            }
            require(responseUri == null || responseUri.isNotBlank()) {
                "SignatureRequest: 'responseURI' must not be blank"
            }
            require(signatureFormat == null || signatureFormat in SIGNATURE_FORMATS) {
                "SignatureRequest: 'signature_format' must be one of $SIGNATURE_FORMATS, " +
                        "was '$signatureFormat'"
            }
            require(conformanceLevel == null || conformanceLevel in CONFORMANCE_LEVELS) {
                "SignatureRequest: 'conformance_level' must be one of $CONFORMANCE_LEVELS, " +
                        "was '$conformanceLevel'"
            }
            require(
                signedEnvelopeProperty == null ||
                        signedEnvelopeProperty in SIGNED_ENVELOPE_PROPERTIES
            ) {
                "SignatureRequest: 'signed_envelope_property' must be one of " +
                        "$SIGNED_ENVELOPE_PROPERTIES, was '$signedEnvelopeProperty'"
            }
            require(signedProperties == null || signedProperties.isNotEmpty()) {
                "SignatureRequest: 'signed_props' must not be empty"
            }
            require(signAlgo.isNotBlank()) { "SignatureRequest: 'signAlgo' must not be blank" }
        }
    }
}

/**
 * Chooses the [SignatureRequest] to read from the field that carries the document. Exactly one of
 * [SignatureRequest.DOCUMENT] and [SignatureRequest.HREF] is present.
 */
internal object SignatureRequestSerializer :
    JsonContentPolymorphicSerializer<SignatureRequest>(SignatureRequest::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out SignatureRequest> {
        val fields = element.jsonObject
        val hasDocument = SignatureRequest.DOCUMENT in fields
        val hasHref = SignatureRequest.HREF in fields
        require(hasDocument xor hasHref) {
            "SignatureRequest: exactly one of '${SignatureRequest.DOCUMENT}' and " +
                    "'${SignatureRequest.HREF}' must be present"
        }
        return if (hasDocument) {
            SignatureRequest.WithDocument.serializer()
        } else {
            SignatureRequest.WithDocumentReference.serializer()
        }
    }
}
