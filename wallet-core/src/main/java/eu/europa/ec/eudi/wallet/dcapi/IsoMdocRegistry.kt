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

package eu.europa.ec.eudi.wallet.dcapi

import android.content.Context
import android.graphics.BitmapFactory
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.multipaz.cbor.Cbor
import java.net.HttpURLConnection
import java.net.URL

/**
 * [DigitalCredentialRegistry] implementation for the `org-iso-mdoc` protocol
 * (ISO/IEC TS 18013-7:2025 Annex C).
 *
 * ```
 * val registry = IsoMdocRegistry(
 *     context = context,
 *     documents = issuedDocuments,
 *     id = "eudi-mdoc-registry-v1"
 * )
 * registryManager.registerCredentials(registry)
 * ```
 */
internal class IsoMdocRegistry private constructor(
    id: String,
    credentials: ByteArray,
    matcher: ByteArray
) : DigitalCredentialRegistry(id, credentials, matcher) {

    companion object {
        private const val TAG = "IsoMdocRegistry"
        private const val DEFAULT_MATCHER_FILE = "identitycredentialmatcher.wasm"

        // Keys consumed by the matcher in the CBOR credentials structure
        private const val TITLE = "title"
        private const val SUBTITLE = "subtitle"
        private const val BITMAP = "bitmap"
        private const val MDOC = "mdoc"
        private const val ID = "id"
        private const val DOC_TYPE = "docType"
        private const val NAMESPACES = "namespaces"

        /**
         * Creates a new [IsoMdocRegistry] for the given documents.
         *
         * @param context Application context used to load the bundled WASM matcher and
         *   to resolve app name / locale for display fields in the credentials structure.
         * @param documents The issued mdoc documents to register. Documents whose format
         *   is not [MsoMdocFormat] are ignored by the matcher anyway, but ideally callers
         *   should filter beforehand.
         * @param id Unique registry identifier, max 64 characters.
         * @param logger Optional logger.
         * @param ioDispatcher Dispatcher used for network I/O (logo download) and asset
         *   reading.
         */
        suspend operator fun invoke(
            context: Context,
            documents: List<IssuedDocument>,
            id: String,
            logger: Logger? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): IsoMdocRegistry = withContext(ioDispatcher) {
            val credentials = documents.toCredentialBytes(context, logger, ioDispatcher)
            val matcher = context.getMatcher(DEFAULT_MATCHER_FILE)
            IsoMdocRegistry(id, credentials, matcher)
        }

        /**
         * Serializes the documents into the CBOR structure expected by the bundled
         * WASM matcher.
         */
        private suspend fun List<IssuedDocument>.toCredentialBytes(
            context: Context,
            logger: Logger?,
            ioDispatcher: CoroutineDispatcher
        ): ByteArray {
            val docsBuilder = CBORObject.NewArray()
            forEach { document ->
                val docType = (document.data.format as MsoMdocFormat).docType
                logger?.d(
                    TAG,
                    "Issued Document with id: ${document.id}, type: $docType is being added as a credential"
                )

                // Try to get document logo provided by issuer else use an empty byte array
                val bitmapBytes = document.issuerMetadata?.let {
                    getBitmapBytes(it, context, ioDispatcher, logger)
                } ?: byteArrayOf(0)

                docsBuilder.Add(CBORObject.NewMap().apply {
                    Add(TITLE, document.name)
                    Add(SUBTITLE, context.getAppName())
                    Add(BITMAP, bitmapBytes)
                    Add(MDOC, CBORObject.NewMap().apply {
                        Add(ID, document.id)
                        Add(DOC_TYPE, docType)
                        Add(NAMESPACES, CBORObject.NewMap().apply {
                            (document.data as MsoMdocData).claims.groupBy { it.nameSpace }
                                .forEach { (nameSpace, elements) ->
                                    val namespaceBuilder = CBORObject.NewMap()
                                    elements.forEach { element ->
                                        val displayName = element.issuerMetadata?.display?.find {
                                            it.locale?.language == context.getLocale().language
                                        }?.name ?: element.identifier
                                        val displayedValue =
                                            if (Cbor.toDiagnostics(element.rawValue).startsWith("h'")) {
                                                "${element.rawValue.size} bytes"
                                            } else {
                                                Cbor.toDiagnostics(element.rawValue)
                                            }
                                        val elementBuilder = CBORObject.NewArray().apply {
                                            Add(displayName)
                                            Add(displayedValue)
                                        }
                                        namespaceBuilder.Add(element.identifier, elementBuilder)
                                    }
                                    Add(nameSpace, namespaceBuilder)
                                }
                        })
                    })
                })
            }
            return docsBuilder.EncodeToBytes()
        }

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
