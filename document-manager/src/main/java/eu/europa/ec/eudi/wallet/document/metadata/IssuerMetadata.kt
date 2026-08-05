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

package eu.europa.ec.eudi.wallet.document.metadata

import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata.Companion.fromJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.Locale

/**
 * Document metadata domain object for storage.
 * @property documentConfigurationIdentifier the document configuration identifier
 * @property display the display properties of the document
 * @property claims metadata for the claims of the document
 * @property credentialIssuerIdentifier the credential issuer identifier
 * @property issuerDisplay the display properties of the issuer that issued the document
 */
@Serializable
data class IssuerMetadata(
    val documentConfigurationIdentifier: String,
    val display: List<Display>,
    val claims: List<Claim>?,
    val credentialIssuerIdentifier: String,
    val issuerDisplay: List<IssuerDisplay>?
) {

    /**
     * Convert the object to a JSON string.
     * @return the JSON string representation of the object
     * @throws SerializationException if cannot be serialized to JSON.
     */
    fun toJson(): String = Json.encodeToString(serializer(), this)

    /**
     * Convert the object to a byte array of json string.
     * @return the byte array representation of the object
     * @see [toJson]
     * @throws SerializationException if cannot be serialized to JSON.
     */
    internal fun toByteArray(): ByteArray = toJson().toByteArray()

    companion object {
        val Json = Json {
            ignoreUnknownKeys = true
        }

        /**
         * Create a [IssuerMetadata] object from a JSON string.
         * @param json the JSON string representation of the object
         * @return the [IssuerMetadata] object
         * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [IssuerMetadata]
         * @throws SerializationException if the given JSON string is not a valid JSON input
         */
        fun fromJson(json: String): Result<IssuerMetadata> =
            runCatching { Json.decodeFromString(serializer(), json) }

        /**
         * Create a [IssuerMetadata] object from a byte array of json string.
         * @param jsonByteArray the byte array representation of the object
         * @return the [IssuerMetadata] object
         * @see [fromJson]
         * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [IssuerMetadata]
         * @throws SerializationException if the given bytearray of JSON string is not a valid JSON input
         */
        internal fun fromByteArray(jsonByteArray: ByteArray): Result<IssuerMetadata> =
            fromJson(jsonByteArray.decodeToString())
    }

    /**
     * Display properties of a supported credential type for a certain language.
     * @property name the name of the document
     * @property locale the locale of the current display
     * @property logo the logo of the document
     * @property description the description of the document
     * @property backgroundColor the background color of the document
     * @property textColor the text color of the document
     * @property backgroundImageUri the URI of the background image
     * @see Logo
     * @see Locale
     */
    @Serializable
    data class Display(
        val name: String,
        @Serializable(with = LocaleSerializer::class)
        val locale: Locale? = null,
        val logo: Logo? = null,
        val description: String? = null,
        val backgroundColor: String? = null,
        val textColor: String? = null,
        @Serializable(URISerializer::class)
        @SerialName("backgroundImageUri")
        val backgroundImageUri: URI? = null,
    )

    /**
     * Display properties of the issuer that issued the document.
     * @property name the name of the issuer
     * @property locale the locale of the current display
     * @property logo the logo of the issuer
     */
    @Serializable
    data class IssuerDisplay(
        val name: String,
        @Serializable(with = LocaleSerializer::class)
        val locale: Locale? = null,
        val logo: Logo? = null
    )

    /**
     * Logo information.
     * @property uri the URI of the logo
     * @property alternativeText the alternative text of the logo
     * @see URI
     */
    @Serializable
    data class Logo(
        @Serializable(with = URISerializer::class)
        val uri: URI? = null,
        val alternativeText: String? = null,
    )

    /**
     * Claim properties.
     * @property path namespace,element identifier in case of mso_mdoc, and claim path in case sd-jwt-vc
     * @property mandatory whether the claim is mandatory
     * @property display the display properties of the claim
     * @see Display
     */
    @Serializable
    data class Claim(
        @SerialName("path") val path: List<String>,
        @SerialName("mandatory") val mandatory: Boolean? = false,
        @SerialName("display") val display: List<Display> = emptyList(),
    ) {

        /**
         * Display properties of a Claim.
         * @property name the name of the claim
         * @property locale the locale of the current display
         * @see Locale
         */
        @Serializable
        data class Display(
            @SerialName("name") val name: String? = null,
            @Serializable(LocaleSerializer::class)
            @SerialName("locale") val locale: Locale? = null,
        )
    }
}

/**
 * Serializer for [Locale].
 */
internal object LocaleSerializer : KSerializer<Locale> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Locale =
        Locale.forLanguageTag(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Locale) =
        encoder.encodeString(value.toLanguageTag())
}

/**
 * Serializer for [URI].
 */
internal object URISerializer : KSerializer<URI> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URI {
        return URI.create(decoder.decodeString())
    }
}