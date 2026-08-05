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

package eu.europa.ec.eudi.wallet.dcapi.registration

import eu.europa.ec.eudi.wallet.dcapi.internal.*

import android.content.Context
import android.graphics.BitmapFactory
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import com.upokecenter.cbor.CBORType
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.cbor.Cbor
import java.net.HttpURLConnection
import java.net.URL

/**
 * Registers the wallet's issued documents with the Credential Manager, for the Digital Credential
 * API (DCAPI).
 *
 * It registers both MSO mdoc and SD-JWT VC documents, encoding their display fields and claims so
 * they can be matched against and presented to verifiers, and advertises the protocols the wallet
 * supports. Each document also carries the exchange protocols it can be presented over, so a
 * document is only offered for a request whose protocol it supports (for example, SD-JWT VC
 * documents are offered over OpenID4VP but not over the ISO mdoc protocol).
 *
 * ```
 * val registry = DCAPICredentialRegistry(
 *     context = context,
 *     documents = issuedDocuments,
 *     id = "eudi-credential-registry-v1",
 *     protocols = supportedProtocols,
 * )
 * registryManager.registerCredentials(registry)
 * ```
 */
internal class DCAPICredentialRegistry private constructor(
    id: String,
    credentials: ByteArray,
    matcher: ByteArray
) : DigitalCredentialRegistry(id, credentials, matcher) {

    companion object {
        private const val TAG = "DCAPICredentialRegistry"
        // Digital Credentials API matcher from the multipaz project (v0.100.0-SNAPSHOT),
        // bundled unmodified.
        private const val DEFAULT_MATCHER_FILE = "identitycredentialmatcher.wasm"
        private const val PROTOCOLS = "protocols"
        private const val CREDENTIALS = "credentials"
        private const val TITLE = "title"
        private const val SUBTITLE = "subtitle"
        private const val BITMAP = "bitmap"
        private const val MDOC = "mdoc"
        private const val SDJWT = "sdjwt"
        private const val DOCUMENT_ID = "documentId"
        private const val DOC_TYPE = "docType"
        private const val NAMESPACES = "namespaces"
        private const val VCT = "vct"
        private const val CLAIMS = "claims"

        /**
         * Creates a new [DCAPICredentialRegistry] for the given documents.
         *
         * @param context application context
         * @param documents the issued documents to register (MSO mdoc and SD-JWT VC).
         * @param id unique registry identifier, at most 64 characters.
         * @param protocols the protocols to advertise for the registered documents.
         * @param logger optional logger.
         * @param ioDispatcher dispatcher used for network I/O (logo download) and asset reading.
         */
        suspend operator fun invoke(
            context: Context,
            documents: List<IssuedDocument>,
            id: String,
            protocols: List<DCAPIProtocol>,
            logger: Logger? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): List<DCAPICredentialRegistry> = withContext(ioDispatcher) {
            val credentials = documents.toCredentialBytes(context, logger, ioDispatcher, protocols)
            val matcher = context.getMatcher(DEFAULT_MATCHER_FILE)
            listOf(DCAPICredentialRegistry(id, credentials, matcher))
        }

        /**
         * Serializes the documents into the CBOR structure the registry expects: a top-level map
         * `{ protocols: [...], credentials: [ { title, subtitle, bitmap, protocols, mdoc | sdjwt } ] }`.
         * The exchange protocols are set per document: MSO mdoc documents over all supported protocols,
         * SD-JWT VC documents over the supported OpenID4VP protocols only. The top-level `protocols` is
         * the fallback for a document that carries none and is intentionally left empty, so any such
         * document is hidden rather than offered over every protocol. Each data element or claim is a
         * 3-element array `[displayName, value, matchValue]`, where `matchValue` is the value used to
         * match the document against a request.
         */
        private suspend fun List<IssuedDocument>.toCredentialBytes(
            context: Context,
            logger: Logger?,
            ioDispatcher: CoroutineDispatcher,
            protocols: List<DCAPIProtocol>
        ): ByteArray {
            val credentialsArray = CBORObject.NewArray()
            forEach { document ->
                // Issuer-provided logo, or an empty placeholder when none is available.
                val bitmapBytes = document.issuerMetadata?.let {
                    getBitmapBytes(it, context, ioDispatcher, logger)
                } ?: byteArrayOf(0)

                val credential = CBORObject.NewMap().apply {
                    Add(TITLE, document.name)
                    Add(SUBTITLE, context.getAppName())
                    Add(BITMAP, bitmapBytes)
                }

                when (val format = document.format) {
                    is MsoMdocFormat -> {
                        logger?.d(TAG, "Adding mdoc credential id=${document.id}, docType=${format.docType}")
                        // MSO mdoc can be presented over every supported protocol.
                        credential.Add(PROTOCOLS, protocols.toProtocolsCbor())
                        credential.Add(MDOC, document.toMdocEntry(format, context))
                    }

                    is SdJwtVcFormat -> {
                        logger?.d(TAG, "Adding SD-JWT VC credential id=${document.id}, vct=${format.vct}")
                        // SD-JWT VC can be presented over OpenID4VP.
                        credential.Add(PROTOCOLS, protocols.filter { it.isOpenId4Vp }.toProtocolsCbor())
                        credential.Add(SDJWT, document.toSdJwtEntry(format, context))
                    }
                }
                credentialsArray.Add(credential)
            }

            val database = CBORObject.NewMap().apply {
                // Intentionally empty: exchange protocols are set per document.
                Add(PROTOCOLS, CBORObject.NewArray())
                Add(CREDENTIALS, credentialsArray)
            }
            return database.EncodeToBytes()
        }

        /** Encodes a list of protocols as a CBOR array of their on-the-wire identifiers. */
        private fun List<DCAPIProtocol>.toProtocolsCbor(): CBORObject =
            CBORObject.NewArray().apply { forEach { Add(it.identifier) } }

        /** Builds the `mdoc` entry: docType + claims grouped by namespace. */
        private fun IssuedDocument.toMdocEntry(format: MsoMdocFormat, context: Context): CBORObject =
            CBORObject.NewMap().apply {
                Add(DOCUMENT_ID, id)
                Add(DOC_TYPE, format.docType)
                Add(NAMESPACES, CBORObject.NewMap().apply {
                    (data as MsoMdocData).claims.groupBy { it.nameSpace }
                        .forEach { (nameSpace, elements) ->
                            val namespaceBuilder = CBORObject.NewMap()
                            elements.forEach { element ->
                                val displayName = element.issuerMetadata?.display?.find {
                                    it.locale?.language == context.getLocale().language
                                }?.name ?: element.dataElementName
                                val displayedValue =
                                    if (Cbor.toDiagnostics(element.rawValue).startsWith("h'")) {
                                        "${element.rawValue.size} bytes"
                                    } else {
                                        Cbor.toDiagnostics(element.rawValue)
                                    }
                                namespaceBuilder.Add(
                                    element.dataElementName,
                                    CBORObject.NewArray().apply {
                                        Add(displayName)
                                        Add(displayedValue)
                                        Add(element.rawValue.toMatchValue())
                                    }
                                )
                            }
                            Add(nameSpace, namespaceBuilder)
                        }
                })
            }

        /**
         * Builds the `sdjwt` entry: vct + claims. Claims are flattened to dot-joined path keys
         * (e.g. `age_equal_or_over`, `age_equal_or_over.18`), because a requested claim path is
         * looked up by its full dot-joined path, so a nested claim must be registered under its
         * full path. Each entry is a 3-element `[displayName, value, matchValue]` array.
         */
        private fun IssuedDocument.toSdJwtEntry(format: SdJwtVcFormat, context: Context): CBORObject =
            CBORObject.NewMap().apply {
                Add(DOCUMENT_ID, id)
                Add(VCT, format.vct)
                Add(
                    CLAIMS,
                    buildSdJwtClaimsCbor(
                        (data as SdJwtVcData).claims,
                        context.getLocale().language
                    )
                )
            }

        /**
         * Recursively flattens an SD-JWT claim tree into the CBOR `claims` map, keyed by dot-joined
         * claim path. Object-key nodes only — array-element and wildcard nodes are skipped for now
         * (nested arrays are a deferred edge case). Visible for testing.
         */
        internal fun buildSdJwtClaimsCbor(
            claims: List<SdJwtVcClaim>,
            localeLanguage: String,
        ): CBORObject = CBORObject.NewMap().apply {
            claims.forEach { addSdJwtClaim(it, prefix = null, localeLanguage = localeLanguage) }
        }

        /** Adds [claim] and (recursively) its children to [this] map under their dot-joined keys. */
        private fun CBORObject.addSdJwtClaim(
            claim: SdJwtVcClaim,
            prefix: String?,
            localeLanguage: String,
        ) {
            val name = claim.claimName ?: return // skip array-element / wildcard nodes (no object-key name)
            val key = if (prefix == null) name else "$prefix.$name"
            val displayName = claim.issuerMetadata?.display?.find {
                it.locale?.language == localeLanguage
            }?.name ?: name
            val displayedValue = claim.value?.toString() ?: claim.rawValue
            val matchValue = when (val v = claim.value) {
                is JsonPrimitive -> v.content
                else -> v?.toString() ?: claim.rawValue
            }.truncatedForMatch()
            Add(
                key,
                CBORObject.NewArray().apply {
                    Add(displayName)
                    Add(displayedValue)
                    Add(matchValue)
                }
            )
            claim.children.forEach { addSdJwtClaim(it, prefix = key, localeLanguage = localeLanguage) }
        }

        /**
         * String form of a CBOR-encoded mdoc element value, used to match the element against a
         * request. A text string matches as its raw content (without surrounding JSON quotes),
         * while booleans, numbers and complex values match as their JSON form. Large values
         * (e.g. portrait bytes) are dropped.
         */
        private fun ByteArray.toMatchValue(): String =
            runCatching {
                val cbor = CBORObject.DecodeFromBytes(this)
                if (cbor.type == CBORType.TextString) cbor.AsString() else cbor.ToJSONString()
            }
                .getOrElse { "" }
                .truncatedForMatch()

        /** Values of 128 characters or more are not used for matching. */
        private fun String.truncatedForMatch(): String = if (length < 128) this else ""

        private suspend fun getBitmapBytes(
            issuerMetadata: IssuerMetadata,
            context: Context,
            ioDispatcher: CoroutineDispatcher,
            logger: Logger?
        ): ByteArray {
            return try {
                issuerMetadata.display.find {
                    it.locale?.language == context.getLocale().language
                }?.logo?.uri?.let { uri ->
                    getLogo(uri.toURL(), ioDispatcher, logger)?.let { logoBytes ->
                        BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size).getIconBytes()
                    }
                } ?: byteArrayOf(0)
            } catch (_: Exception) {
                byteArrayOf(0)
            }
        }

        private suspend fun getLogo(
            url: URL,
            ioDispatcher: CoroutineDispatcher,
            logger: Logger?
        ): ByteArray? = withContext(ioDispatcher) {
            try {
                (url.openConnection() as? HttpURLConnection)?.run {
                    connectTimeout = 5_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                    doInput = true
                    connect()
                    inputStream.use { it.readBytes() }
                }
            } catch (e: Exception) {
                logger?.e(TAG, "Failed to download logo from URL: $url", e)
                null
            }
        }
    }
}
