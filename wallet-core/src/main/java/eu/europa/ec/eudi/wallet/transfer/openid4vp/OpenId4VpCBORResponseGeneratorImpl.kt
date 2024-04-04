/*
 *  Copyright (c) 2023 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.transfer.openid4vp

import android.content.Context
import android.util.Log
import com.android.identity.android.securearea.AndroidKeystoreSecureArea
import com.android.identity.android.storage.AndroidStorageEngine
import com.android.identity.credential.CredentialRequest
import com.android.identity.credential.CredentialStore
import com.android.identity.credential.NameSpacedData
import com.android.identity.mdoc.mso.StaticAuthDataParser
import com.android.identity.mdoc.response.DeviceResponseGenerator
import com.android.identity.mdoc.response.DocumentGenerator
import com.android.identity.mdoc.util.MdocUtil
import com.android.identity.securearea.SecureArea
import com.android.identity.securearea.SecureAreaRepository
import com.android.identity.storage.StorageEngine
import com.android.identity.util.Constants
import com.android.identity.util.Timestamp
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.DocRequest
import eu.europa.ec.eudi.iso18013.transfer.DocumentsResolver
import eu.europa.ec.eudi.iso18013.transfer.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.iso18013.transfer.RequestedDocumentData
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseGenerator
import eu.europa.ec.eudi.wallet.internal.Openid4VpX509CertificateTrust

private const val TAG = "OpenId4VpCBORResponseGe"

/**
 * OpenId4VpCBORResponseGeneratorImpl class is used for parsing a request (Presentation Definition) and generating the DeviceResponse
 *
 * @param documentsResolver document manager instance
 * @param storageEngine storage engine used to store documents
 * @param secureArea secure area used to store documents' keys
 */
class OpenId4VpCBORResponseGeneratorImpl(private val documentsResolver: DocumentsResolver,
                                         private val storageEngine: StorageEngine,
                                         private val secureArea: AndroidKeystoreSecureArea
): ResponseGenerator<OpenId4VpRequest>() {

    private var readerTrustStore: ReaderTrustStore? = null
    private val openid4VpX509CertificateTrust = Openid4VpX509CertificateTrust(readerTrustStore)

    /**
     * Set a trust store so that reader authentication can be performed.
     *
     * If it is not provided, reader authentication will not be performed.
     *
     * @param readerTrustStore a trust store for reader authentication, e.g. DefaultReaderTrustStore
     */
    override fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) = apply {
        openid4VpX509CertificateTrust.setReaderTrustStore(readerTrustStore)
        this.readerTrustStore = readerTrustStore
    }

    /**
     * Set a trust store so that reader authentication can be performed.
     *
     * If it is not provided, reader authentication will not be performed.
     *
     * @param readerTrustStore a trust store for reader authentication, e.g. DefaultReaderTrustStore
     */
    fun readerTrustStore(readerTrustStore: ReaderTrustStore) = apply {
        openid4VpX509CertificateTrust.setReaderTrustStore(readerTrustStore)
        this.readerTrustStore = readerTrustStore
    }

    internal fun getOpenid4VpX509CertificateTrust() = openid4VpX509CertificateTrust


    private val secureAreaRepository: SecureAreaRepository by lazy {
        SecureAreaRepository().apply {
            addImplementation(secureArea)
        }
    }

    /** Parses a request and returns the requested document data
     * @param request the received request
     * @return [RequestedDocumentData]
     */
    override fun parseRequest(request: OpenId4VpRequest): RequestedDocumentData {
        return createRequestedDocumentData(
            request.presentationDefinition.inputDescriptors
                .mapNotNull { inputDescriptor ->
                    inputDescriptor.format?.jsonObject()
                        ?.takeIf { it.containsKey("mso_mdoc") } // ignore formats other than "mso_mdoc"
                        ?.run {
                            inputDescriptor.id.value.trim() to inputDescriptor.constraints.fields()
                                .mapNotNull { fieldConstraint ->
                                    // path shall contain a requested data element as: $['<namespace>']['<data element identifier>']
                                    val path = fieldConstraint.paths.first().value
                                    Regex("\\\$\\['(.*?)']\\['(.*?)']").find(path)
                                        ?.let { matchResult ->
                                            val (namespace, elementIdentifier) = matchResult.destructured
                                            if (namespace.isNotBlank() && elementIdentifier.isNotBlank()) {
                                                namespace to elementIdentifier
                                            } else {
                                                null
                                            }
                                        }

                                }.groupBy({ it.first }, { it.second })
                                .mapValues { (_, values) -> values.toList() }
                                .toMap()
                        } ?: run {
                        Log.w(
                            TAG,
                            "Input descriptor with id ${inputDescriptor.id} and format ${inputDescriptor.format} is skipped. Format is not mso_mdoc."
                        )
                        null
                    }
                }.toMap(),
            openid4VpX509CertificateTrust.getReaderAuth()
        )
    }

    /**
     * Creates a response and returns a ResponseResult
     *
     * @param disclosedDocuments a [List] of [DisclosedDocument]
     * @return a [ResponseResult]
     */
    override fun createResponse(
        disclosedDocuments: DisclosedDocuments
    ): ResponseResult {
        try {
            val deviceResponse = DeviceResponseGenerator(Constants.DEVICE_RESPONSE_STATUS_OK)
            disclosedDocuments.documents.forEach { responseDocument ->
                if (responseDocument.docType == "org.iso.18013.5.1.mDL" && responseDocument.selectedDocItems.filter { docItem ->
                        docItem.elementIdentifier.startsWith("age_over_")
                                && docItem.namespace == "org.iso.18013.5.1"
                    }.size > 2) {
                    return ResponseResult.Failure(Exception("Device Response is not allowed to have more than to age_over_NN elements"))
                }
                val addResult =
                    addDocumentToResponse(deviceResponse, responseDocument, byteArrayOf(0))
                if (addResult is AddDocumentToResponse.UserAuthRequired)
                    return ResponseResult.UserAuthRequired(
                        addResult.keyUnlockData.getCryptoObjectForSigning(SecureArea.ALGORITHM_ES256)
                    )
            }
            return ResponseResult.Success(OpenId4VpCBORResponse(deviceResponse.generate()))
        } catch (e: Exception) {
            return ResponseResult.Failure(e)
        }
    }

    @Throws(IllegalStateException::class)
    private fun addDocumentToResponse(
        responseGenerator: DeviceResponseGenerator,
        disclosedDocument: DisclosedDocument,
        transcript: ByteArray
    ): AddDocumentToResponse {
        val dataElements = disclosedDocument.selectedDocItems.map {
            CredentialRequest.DataElement(it.namespace, it.elementIdentifier, false)
        }
        val request = CredentialRequest(dataElements)
        val credentialStore = CredentialStore(storageEngine, secureAreaRepository)
        val credential =
            requireNotNull(credentialStore.lookupCredential(disclosedDocument.documentId))
        val authKey = credential.findAuthenticationKey(Timestamp.now())
            ?: throw IllegalStateException("No auth key available")
        val staticAuthData = StaticAuthDataParser(authKey.issuerProvidedData).parse()
        val mergedIssuerNamespaces = MdocUtil.mergeIssuerNamesSpaces(
            request, credential.nameSpacedData, staticAuthData
        )
        val keyUnlockData = AndroidKeystoreSecureArea.KeyUnlockData(authKey.alias)
        try {
            val generator =
                DocumentGenerator(disclosedDocument.docType, staticAuthData.issuerAuth, transcript)
                    .setIssuerNamespaces(mergedIssuerNamespaces)
            generator.setDeviceNamespacesSignature(
                NameSpacedData.Builder().build(),
                authKey.secureArea,
                authKey.alias,
                keyUnlockData,
                SecureArea.ALGORITHM_ES256
            )
            val data = generator.generate()
            responseGenerator.addDocument(data)
        } catch (lockedException: SecureArea.KeyLockedException) {
            Log.e(TAG, "error", lockedException)
            return AddDocumentToResponse.UserAuthRequired(keyUnlockData)
        }
        return AddDocumentToResponse.Success
    }

    private fun createRequestedDocumentData(
        requestedFields: Map<String, Map<String, List<String>>>,
        readerAuth: ReaderAuth?
    ): RequestedDocumentData {
        val requestedDocuments = mutableListOf<RequestDocument>()
        requestedFields.forEach { document ->
            // create doc item
            val docItems = mutableListOf<DocItem>()
            document.value.forEach { (namespace, elementIds) ->
                elementIds.forEach { elementId ->
                    docItems.add(DocItem(namespace, elementId))
                }
            }
            val docType = document.key

            requestedDocuments.addAll(
                documentsResolver.resolveDocuments(
                    DocRequest(
                        docType,
                        docItems,
                        readerAuth
                    )
                )
            )
        }
        return RequestedDocumentData(requestedDocuments)
    }

    class Builder(context: Context) {
        private val _context = context.applicationContext
        var documentsResolver: DocumentsResolver? = null
        var readerTrustStore: ReaderTrustStore? = null

        /**
         * Reader trust store that will be used to validate the certificate chain of the mdoc verifier
         *
         * @param readerTrustStore
         */
        fun readerTrustStore(readerTrustStore: ReaderTrustStore) =
            apply { this.readerTrustStore = readerTrustStore }

        fun build(): OpenId4VpCBORResponseGeneratorImpl {
            return documentsResolver?.let { documentsResolver ->
                OpenId4VpCBORResponseGeneratorImpl(
                    documentsResolver,
                    storageEngine,
                    androidSecureArea
                ).apply {
                    readerTrustStore?.let { setReaderTrustStore(it) }
                }
            } ?: throw IllegalArgumentException("documentResolver not set")
        }

        private val storageEngine: StorageEngine
            get() = AndroidStorageEngine.Builder(_context, _context.noBackupFilesDir)
                .setUseEncryption(true)
                .build()
        private val androidSecureArea: AndroidKeystoreSecureArea
            get() = AndroidKeystoreSecureArea(_context, storageEngine)
    }

    private sealed interface AddDocumentToResponse {
        object Success : AddDocumentToResponse
        data class UserAuthRequired(val keyUnlockData: AndroidKeystoreSecureArea.KeyUnlockData) :
            AddDocumentToResponse
    }
}