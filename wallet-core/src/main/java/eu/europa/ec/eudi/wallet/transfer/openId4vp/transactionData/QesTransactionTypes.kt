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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.DataItem
import org.multipaz.documenttype.TransactionType
import org.multipaz.presentment.TransactionData
import org.multipaz.presentment.TransactionProtocol
import org.multipaz.util.fromBase64Url
import java.security.MessageDigest
import java.util.Base64

// OpenID4VP treats transaction data that is an object of a known type but contains an unknown
// field as invalid_transaction_data, so unknown fields are rejected.
private val json: Json = Json { ignoreUnknownKeys = false }

/**
 * The transaction data type with which a relying party asks the user to approve a signature it has
 * already prepared. Its payload is a [QesApprovalRequest].
 *
 * The type returns the user's approval in a claim of its own, as [keyBindingClaims] describes.
 */
object QesApprovalTransactionType :
    TransactionType<QesApprovalRequest>(
        displayName = "QES approval",
        identifier = QesApprovalRequest.TYPE,
        openId4VpMdocResponseNamespace = QES_APPROVAL_NAMESPACE,
    ),
    TransactionDataKeyBinding,
    TransactionDataDeviceSigned {

    override val nameSpace: String
        get() = getMdocResponseNamespace(TransactionProtocol.OPENID4VP)

    override fun parseOpenId4VpRequest(jsonString: String): QesApprovalRequest =
        json.decodeFromString(QesApprovalRequest.serializer(), jsonString)

    override fun parseJson(serialized: ByteString): TransactionData<QesApprovalRequest> {
        val payload = parseOpenId4VpRequest(serialized.toJsonString())
        return TransactionData(
            type = this,
            payload = payload,
            protocol = TransactionProtocol.OPENID4VP,
            rawBytes = serialized,
            hashAlgorithms = parseJoseHashAlgorithms(payload.hashAlgorithms),
        )
    }

    /**
     * Calculates the `qesApproval` that binds the presentation to the approved transaction data,
     * as defined by CSC Data Model Bindings clause 7.2.1.2.
     *
     * The digest is calculated over the transaction data string as it was received, which for an
     * SD-JWT VC is not base64url decoded first, with the algorithm the request names in
     * `hashAlgorithmOID`, and is encoded with base64 as CSC Data Model clause 5.2 requires.
     *
     * `transaction_data_hashes` covers the same string but takes its algorithm from
     * `transaction_data_hashes_alg` and encodes with base64url, so the two are the same digest
     * only when the request names the same algorithm in both.
     *
     * @throws IllegalArgumentException when [transactionData] holds more than one approval, which
     * a single `qesApproval` cannot represent, or names a hash algorithm the wallet cannot use
     */
    override fun keyBindingClaims(
        transactionData: List<TransactionData<*>>
    ): Map<String, JsonElement> {
        val approval = transactionData.singleOrNull()
            ?: throw IllegalArgumentException(
                "A presentation carries one '$QES_APPROVAL_CLAIM' claim, so it cannot approve " +
                    "the ${transactionData.size} transaction data of type '$identifier' that " +
                    "reference its Credential"
            )
        val payload = approval.payload
        require(payload is QesApprovalRequest) {
            "Transaction data of type '$identifier' carries a payload of another type"
        }
        val digest = MessageDigest.getInstance(messageDigestName(payload.hashAlgorithmOid))
            .digest(approval.rawBytes.toByteArray())
        return mapOf(QES_APPROVAL_CLAIM to JsonPrimitive(base64.encodeToString(digest)))
    }

    /**
     * Calculates the `qesApproval` that binds an ISO/IEC 18013-5 mdoc presentation to the approved
     * transaction data, as defined by CSC Data Model Bindings clause 7.2.1.1.
     *
     * The digest is calculated over the base64url decoded transaction data, always with SHA-256,
     * and is returned as the digest itself, not base64 encoded. The key of the returned map is the
     * data element identifier, within the name space [QES_APPROVAL_NAMESPACE].
     *
     * @throws IllegalArgumentException when [transactionData] holds more than one approval, which
     * a single `qesApproval` cannot represent
     */
    override fun deviceSignedElements(
        transactionData: List<TransactionData<*>>
    ): Map<String, DataItem> {
        val approval = transactionData.singleOrNull()
            ?: throw IllegalArgumentException(
                "A presentation carries one '$QES_APPROVAL_ELEMENT' data element, so it cannot " +
                    "approve the ${transactionData.size} transaction data of type '$identifier' " +
                    "that reference its Credential"
            )
        require(approval.payload is QesApprovalRequest) {
            "Transaction data of type '$identifier' carries a payload of another type"
        }
        val decoded = approval.rawBytes.decodeToString().fromBase64Url()
        val digest = MessageDigest.getInstance(SHA_256).digest(decoded)
        return mapOf(QES_APPROVAL_ELEMENT to Bstr(digest))
    }
}

/**
 * The transaction data type with which a relying party asks the wallet to have one or more documents
 * signed. Its payload is a [QesRequest].
 */
object QesRequestTransactionType : TransactionType<QesRequest>(
    displayName = "QES request",
    identifier = QesRequest.TYPE,
) {
    override fun parseOpenId4VpRequest(jsonString: String): QesRequest =
        json.decodeFromString(QesRequest.serializer(), jsonString)

    override fun parseJson(serialized: ByteString): TransactionData<QesRequest> {
        val payload = parseOpenId4VpRequest(serialized.toJsonString())
        return TransactionData(
            type = this,
            payload = payload,
            protocol = TransactionProtocol.OPENID4VP,
            rawBytes = serialized,
            hashAlgorithms = parseJoseHashAlgorithms(payload.hashAlgorithms),
        )
    }
}

/**
 * Decodes the base64url-encoded transaction data to the JSON it carries.
 */
private fun ByteString.toJsonString(): String = decodeToString().fromBase64Url().decodeToString()

/** The name space of the QES approval data element, per CSC Data Model Bindings clause 7.2.1.1. */
private const val QES_APPROVAL_NAMESPACE = "org.cloudsignatureconsortium.dm.1"

/** The data element identifier of the QES approval, per the same clause. */
private const val QES_APPROVAL_ELEMENT = "qesApproval"

/** The top-level claim of the Key Binding JWT that carries a QES approval. */
private const val QES_APPROVAL_CLAIM = "$QES_APPROVAL_NAMESPACE.$QES_APPROVAL_ELEMENT"

/** The digest an mdoc QES approval always uses, per CSC Data Model Bindings clause 7.2.1.1. */
private const val SHA_256 = "SHA-256"

/** base64 with the standard alphabet and padding, which CSC Data Model clause 5.2 requires. */
private val base64: Base64.Encoder = Base64.getEncoder()

/** The message digest of every hash algorithm a `hashAlgorithmOID` may name. */
private val HASH_ALGORITHM_OIDS: Map<String, String> = mapOf(
    "2.16.840.1.101.3.4.2.1" to "SHA-256",
    "2.16.840.1.101.3.4.2.2" to "SHA-384",
    "2.16.840.1.101.3.4.2.3" to "SHA-512",
)

private fun messageDigestName(hashAlgorithmOid: String): String =
    HASH_ALGORITHM_OIDS[hashAlgorithmOid]
        ?: throw IllegalArgumentException(
            "Unsupported 'hashAlgorithmOID' '$hashAlgorithmOid' for a QES approval"
        )
