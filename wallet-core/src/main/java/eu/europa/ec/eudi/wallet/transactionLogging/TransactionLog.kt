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

package eu.europa.ec.eudi.wallet.transactionLogging

import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.DataFormat.Cbor
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.DataFormat.Json
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.Status.Completed
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.Status.Error
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.Status.Incomplete
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.Type.Issuance
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.Type.Presentation
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.Type.Signing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


/**
 * Represents a transaction log entry.
 *
 * @property timestamp The timestamp of the transaction.
 * @property status The status of the transaction.
 * @property type The type of the transaction.
 *
 * The following properties are optional and should be present in case of type [Type.Presentation]
 * @property relyingParty The relying party involved in the transaction.
 * @property rawRequest The raw request data.
 * @property rawResponse The raw response data.
 * @property dataFormat The format of the data (e.g., CBOR, JSON).
 * @property sessionTranscript The session transcript data.
 * @property metadata Additional metadata related to the transaction each item is a json-encoded [Metadata] object
 */
@Serializable
data class TransactionLog(
    val timestamp: Long,
    val status: Status,
    val type: Type,
    val relyingParty: RelyingParty?,
    val rawRequest: ByteArray?,
    val rawResponse: ByteArray?,
    val dataFormat: DataFormat?,
    val sessionTranscript: ByteArray?,
    val metadata: List<String>?
) {
    /**
     * Represents the status of the transaction.
     *
     * - [Incomplete] indicates that the transaction is incomplete.
     * - [Completed] indicates that the transaction is completed successfully.
     * - [Error] indicates that there was an error during the transaction.
     */
    @Serializable
    enum class Status {
        Incomplete,
        Completed,
        Error;
    }

    /**
     * Represents the type of the transaction.
     *
     * - [Presentation] indicates that the transaction is related to a presentation of documents.
     * - [Issuance] indicates that the transaction is related to the issuance of documents.
     * - [Signing] indicates that the transaction is related to the signing of documents.
     */
    @Serializable
    enum class Type {
        Presentation,
        Issuance,
        Signing;
    }

    /**
     * Represents the relying party involved in the transaction.
     *
     * Appears in the context of [Type.Presentation] and contains information about the relying party.
     *
     * @property name The name of the relying party.
     * @property isVerified Indicates whether the relying party is verified.
     * @property certificateChain The certificate chain of the relying party.
     * @property readerAuth The reader authentication data.
     */
    @Serializable
    data class RelyingParty(
        val name: String,
        val isVerified: Boolean,
        val certificateChain: List<String>,
        val readerAuth: String?,
    )

    /**
     * Represents the format of the data in the [eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog.rawResponse]
     *
     * - [Cbor] indicates that the data is in CBOR format.
     * - [Json] indicates that the data is in JSON format.
     */
    @Serializable
    enum class DataFormat {
        Cbor,
        Json,
    }

    @Serializable
    sealed interface Metadata {
        val issuerMetadata: String?
        val format: String


        companion object {
            val Json = Json {
                classDiscriminator = "type"
                ignoreUnknownKeys = true
                allowStructuredMapKeys = true
            }

            fun fromJson(jsonStr: String): Metadata {
                return Json.decodeFromString(jsonStr)
            }
        }

        fun toJson(): String {
            return Json.encodeToString(this)
        }

        @Serializable
        data class IndexBased(
            val index: Int,
            override val format: String,
            override val issuerMetadata: String?
        ) : Metadata {
            override fun toString(): String {
                return issuerMetadata ?: ""
            }
        }

        @Serializable
        data class QueryBased(
            val queryId: String,
            override val format: String,
            override val issuerMetadata: String?
        ) : Metadata {
            override fun toString(): String {
                return issuerMetadata ?: ""
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionLog

        if (timestamp != other.timestamp) return false
        if (status != other.status) return false
        if (type != other.type) return false
        if (relyingParty != other.relyingParty) return false
        if (!rawRequest.contentEquals(other.rawRequest)) return false
        if (!rawResponse.contentEquals(other.rawResponse)) return false
        if (dataFormat != other.dataFormat) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (relyingParty?.hashCode() ?: 0)
        result = 31 * result + (rawRequest?.contentHashCode() ?: 0)
        result = 31 * result + (rawResponse?.contentHashCode() ?: 0)
        result = 31 * result + (dataFormat?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}


