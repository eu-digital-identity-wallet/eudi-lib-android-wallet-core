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

@file:JvmMultifileClass
package eu.europa.ec.eudi.wallet.dcapi

import android.content.Context
import android.graphics.BitmapFactory
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import com.google.android.gms.identitycredentials.RegistrationRequest
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.util.encoders.Hex
import org.multipaz.cbor.Cbor
import java.net.HttpURLConnection
import java.net.URL

/**
 * [DCAPIIsoMdocRegistration] is responsible for registering MSO MDOC credentials for the Digital
 * Credential API (DCAPI).
 *
 * It retrieves issued documents, converts them to CBOR format, and registers them with
 * the Identity Credential Manager.
 *
 * @property context The application context used for accessing resources and services.
 * @property documentManager The [DocumentManager] instance used to manage documents.
 * @property logger Optional logger for logging events.
 */

class DCAPIIsoMdocRegistration(
    private val context: Context,
    private val documentManager: DocumentManager,
    private var logger: Logger? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): DCAPIRegistration {

    override suspend fun registerCredentials() {
        return withContext(ioDispatcher) {
            try {
                val issuedMsoMdocDocuments =
                    documentManager.getDocuments().filterIsInstance<IssuedDocument>()
                        .filter { it.format is MsoMdocFormat }

                val credentials = issuedMsoMdocDocuments.toCBORBytes(context)
                val client = IdentityCredentialManager.getClient(context)
                val matcher = context.getMatcher(IDENTITY_CREDENTIAL_MATCHER)
                client.registerCredentials(
                    RegistrationRequest(
                        credentials = credentials,
                        matcher = matcher,
                        type = REGISTRATION_TYPE,
                        requestType = "",
                        protocolTypes = emptyList(),
                    )
                ).addOnSuccessListener {
                    logger?.d(TAG, "Registration succeeded (old)")
                }.addOnFailureListener {
                    logger?.d(TAG, "Registration failed  (old) $it")
                }
                client.registerCredentials(
                    RegistrationRequest(
                        credentials = credentials,
                        matcher = matcher,
                        type = TYPE_DIGITAL_CREDENTIAL,
                        requestType = "",
                        protocolTypes = emptyList(),
                    )
                ).addOnSuccessListener {
                    logger?.d(TAG, "Registration succeeded")
                }.addOnFailureListener {
                    logger?.d(TAG, "Registration failed $it")
                }
            } catch (e: Exception) {
                logger?.e(TAG, "Error during registration", e)
            }
        }
    }

    private suspend fun List<IssuedDocument>.toCBORBytes(context: Context): ByteArray {
        val docsBuilder = CBORObject.NewArray()
        forEach { document ->
            val docType = (document.data.format as MsoMdocFormat).docType
            logger?.d(
                TAG,
                "Issued Document with id: ${document.id}, type: $docType is being added as a credential"
            )

            // Try to get document logo provided by issuer else use an empty byte array
            val bitmapBytes = document.issuerMetadata?.display?.find {
                it.locale?.language == context.getLocale().language
            }?.logo?.uri?.let { uri ->
                getLogo(uri.toURL())?.let { logoBytes ->
                    BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size).getIconBytes()
                }
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
        val credentialBytes = docsBuilder.EncodeToBytes()
        logger?.d(TAG, "Register documents with bytes: ${Hex.toHexString(credentialBytes)}")
        return credentialBytes
    }

    private suspend fun getLogo(url: URL): ByteArray? = withContext(ioDispatcher) {
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
            logger?.e(TAG, "Failed to download from URL: $url", e)
            null
        }
    }

    companion object {
        private const val TAG = "DCAPIIsoMdocRegistration"
        private const val REGISTRATION_TYPE = "com.credman.IdentityCredential"
        private const val TYPE_DIGITAL_CREDENTIAL = "androidx.credentials.TYPE_DIGITAL_CREDENTIAL"
        private const val IDENTITY_CREDENTIAL_MATCHER = "identitycredentialmatcher.wasm"
        private const val TITLE = "title"
        private const val SUBTITLE = "subtitle"
        private const val BITMAP = "bitmap"
        private const val MDOC = "mdoc"
        private const val ID = "id"
        private const val DOC_TYPE = "docType"
        private const val NAMESPACES = "namespaces"
    }
}

fun interface DCAPIRegistration {
    suspend fun registerCredentials()
}