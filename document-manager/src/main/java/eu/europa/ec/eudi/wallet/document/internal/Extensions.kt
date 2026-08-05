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

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.buildCborMap
import org.multipaz.crypto.EcPublicKey
import kotlin.time.Duration.Companion.seconds

/**
 * Extension property that converts an EcPublicKey to its CBOR-encoded representation.
 *
 * This property takes the public key, converts it to a COSE key format with no additional
 * parameters (empty map), and then encodes it using CBOR encoding. The resulting ByteArray
 * can be used for storage or transmission in CBOR-based protocols.
 *
 * @return ByteArray containing the CBOR-encoded representation of the public key in COSE key format
 */
internal val EcPublicKey.toCoseBytes: ByteArray
    get() = Cbor.encode(this.toCoseKey(emptyMap()).toDataItem())

/**
 * Extension property that decodes an EcPublicKey from CBOR-encoded bytes.
 *
 * This property takes a ByteArray that contains a CBOR-encoded representation of a public key,
 * decodes it using CBOR decoding, and then creates an EcPublicKey instance from the resulting
 * DataItem. This is the reverse operation of [toCoseBytes].
 *
 * @return EcPublicKey instance reconstructed from the CBOR-encoded bytes
 * @throws Exception if the ByteArray cannot be decoded as a valid CBOR-encoded public key
 */
internal val ByteArray.toEcPublicKey: EcPublicKey
    get() = EcPublicKey.fromDataItem(Cbor.decode(this))

/**
 * Extension property that converts a ByteArray to a SD-JWT VC string.
 *
 * This property takes a ByteArray containing SD-JWT VC data and converts it to a string
 * representation using the US-ASCII charset. This is useful when working with SD-JWT VCs
 * that need to be processed as strings (e.g., for parsing or validation).
 *
 * @return String representation of the SD-JWT VC data encoded using US-ASCII charset
 */
internal val ByteArray.sdJwtVcString: String
    get() = String(this, charset = Charsets.US_ASCII)

/**
 * Converts a CredentialPolicy to a CBOR DataItem for serialization.
 *
 * This function creates a CBOR map containing the Java class name of the CredentialPolicy
 * instance as the "type" field, plus any additional properties for ETSI reuse policy types.
 * This allows for proper deserialization of different CredentialPolicy types.
 *
 * @return DataItem representing the CredentialPolicy as a CBOR map
 */
internal fun CreateDocumentSettings.CredentialPolicy.toDataItem(): DataItem {
    return buildCborMap {
        put("type", this@toDataItem.javaClass.name)
        when (val policy = this@toDataItem) {
            is CreateDocumentSettings.CredentialPolicy.OnceOnly -> {
                put("numberOfCredentials", policy.numberOfCredentials.toLong())
                policy.reissueTriggerUnused?.let { put("reissueTriggerUnused", it.toLong()) }
            }
            is CreateDocumentSettings.CredentialPolicy.LimitedTime -> {
                policy.reissueTriggerLifetimeLeft?.let { put("reissueTriggerLifetimeLeft", it.inWholeSeconds) }
            }
            is CreateDocumentSettings.CredentialPolicy.RotatingBatch -> {
                put("numberOfCredentials", policy.numberOfCredentials.toLong())
                policy.reissueTriggerLifetimeLeft?.let { put("reissueTriggerLifetimeLeft", it.inWholeSeconds) }
            }
        }
    }
}

/**
 * Companion object extension function that reconstructs a CredentialPolicy from a CBOR DataItem.
 *
 * This function extracts the policy type from the CBOR map and instantiates the appropriate
 * CredentialPolicy based on the stored type information.
 *
 * @param dataItem The CBOR DataItem containing the serialized CredentialPolicy
 * @return CredentialPolicy instance based on the type information in the DataItem
 * @throws IllegalArgumentException if the DataItem isn't a CborMap or contains an unknown policy type
 */
internal fun CreateDocumentSettings.CredentialPolicy.Companion.fromDataItem(dataItem: DataItem): CreateDocumentSettings.CredentialPolicy {
    require(dataItem is CborMap) {
        "Expected dataItem to be a CborMap for CredentialPolicy."
    }

    return when (val type = dataItem["type"].asTstr) {
        CreateDocumentSettings.CredentialPolicy.OnceOnly::class.java.name ->
            CreateDocumentSettings.CredentialPolicy.OnceOnly(
                numberOfCredentials = dataItem["numberOfCredentials"].asNumber.toInt(),
                reissueTriggerUnused = dataItem.getOrNull("reissueTriggerUnused")?.asNumber?.toInt(),
            )
        CreateDocumentSettings.CredentialPolicy.LimitedTime::class.java.name ->
            CreateDocumentSettings.CredentialPolicy.LimitedTime(
                reissueTriggerLifetimeLeft = dataItem.getOrNull("reissueTriggerLifetimeLeft")?.asNumber?.toLong()?.seconds,
            )
        CreateDocumentSettings.CredentialPolicy.RotatingBatch::class.java.name ->
            CreateDocumentSettings.CredentialPolicy.RotatingBatch(
                numberOfCredentials = dataItem["numberOfCredentials"].asNumber.toInt(),
                reissueTriggerLifetimeLeft = dataItem.getOrNull("reissueTriggerLifetimeLeft")?.asNumber?.toLong()?.seconds,
            )
        else -> throw IllegalArgumentException("Unknown credential policy type: $type")
    }
}

/**
 * Converts a DocumentFormat to a CBOR DataItem for serialization.
 *
 * This function creates a CBOR map with different fields based on the concrete type of the DocumentFormat:
 * - For MsoMdocFormat, it stores the "docType" field
 * - For SdJwtVcFormat, it stores the "vct" field
 * This allows different document format types to be properly serialized and later deserialized.
 *
 * @return DataItem representing the DocumentFormat as a CBOR map with format-specific fields
 */
internal fun DocumentFormat.toDataItem(): DataItem {
    return buildCborMap {
        when (this@toDataItem) {
            is MsoMdocFormat -> put("docType", docType)
            is SdJwtVcFormat -> put("vct", vct)
        }
    }
}

/**
 * Companion object extension function that reconstructs a DocumentFormat from a CBOR DataItem.
 *
 * This function inspects the CBOR map to determine which type of DocumentFormat it represents:
 * - If it has a "docType" key, it creates a MsoMdocFormat instance
 * - If it has a "vct" key, it creates a SdJwtVcFormat instance
 *
 * @param dataItem The CBOR DataItem containing the serialized DocumentFormat
 * @return DocumentFormat instance of the appropriate concrete type based on the fields in the DataItem
 * @throws IllegalArgumentException if the DataItem isn't a CborMap or doesn't contain recognized format keys
 */
internal fun DocumentFormat.Companion.fromDataItem(dataItem: DataItem): DocumentFormat {
    require(dataItem is CborMap) {
        "Expected dataItem to be a CborMap for DocumentFormat."
    }

    return when {
        dataItem.hasKey("docType") -> MsoMdocFormat(docType = dataItem["docType"].asTstr)
        dataItem.hasKey("vct") -> SdJwtVcFormat(vct = dataItem["vct"].asTstr)
        else -> throw IllegalArgumentException("Unknown DocumentFormat type")
    }
}

