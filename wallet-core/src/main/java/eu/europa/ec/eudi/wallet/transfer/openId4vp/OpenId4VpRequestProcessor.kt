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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.prex.InputDescriptor
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.Openid4VpX509CertificateTrust
import eu.europa.ec.eudi.wallet.internal.generateMdocGeneratedNonce
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes

class OpenId4VpRequestProcessor(
    private val documentManager: DocumentManager,
    override var readerTrustStore: ReaderTrustStore?,
) : RequestProcessor, ReaderTrustStoreAware {

    private val helper: DeviceRequestProcessor.Helper by lazy {
        DeviceRequestProcessor.Helper(documentManager)
    }

    internal val openid4VpX509CertificateTrustStore: Openid4VpX509CertificateTrust
            by lazy { Openid4VpX509CertificateTrust(readerTrustStore) }

    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        require(request is OpenId4VpRequest) { "Request must be an OpenId4VpRequest" }
        require(request.resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) {
            "Request must have be a OpenId4VPAuthorization"
        }
        val presentationQuery = request.resolvedRequestObject.presentationQuery
        require(presentationQuery is PresentationQuery.ByPresentationDefinition) {
            "Currently only PresentationDefinition is supported"
        }

        val presentationDefinition = presentationQuery.value

        val requestedFormats = presentationDefinition.inputDescriptors
            .flatMap { it.format?.jsonObject()?.keys ?: emptySet() }
            .toSet()
        require(requestedFormats.isNotEmpty()) { "No format is requested" }
        val msoMdocNonce: String = generateMdocGeneratedNonce()
        val requestedOnlyMsoMdoc =
            requestedFormats.size == 1 && requestedFormats.first() == FORMAT_MSO_MDOC

        return if (requestedOnlyMsoMdoc) {
            val requestedDocuments: RequestedDocuments =
                presentationDefinition.inputDescriptors
                    .map { descriptor -> parseDescriptorForMsoMdocDocument(descriptor, request) }
                    .let { helper.getRequestedDocuments(it) }

            val msoMdocNonce = generateMdocGeneratedNonce()
            val sessionTranscriptBytes =
                request.resolvedRequestObject.getSessionTranscriptBytes(msoMdocNonce)

            ProcessedMsoMdocOpenId4VpRequest(
                resolvedRequestObject = request.resolvedRequestObject,
                processedDeviceRequest = ProcessedDeviceRequest(
                    documentManager = documentManager,
                    requestedDocuments = requestedDocuments,
                    sessionTranscript = sessionTranscriptBytes
                ),
                msoMdocNonce = msoMdocNonce
            )
        } else {
            val inputDescriptorMap: MutableMap<InputDescriptorId, List<DocumentId>> = mutableMapOf()

            val requestedDocuments = RequestedDocuments(
                presentationDefinition.inputDescriptors
                    // NOTE: mso_mdoc and dc+sd-jwt are supported, other formats are ignored
                    .filter {
                        it.format?.jsonObject()?.keys?.first() in listOf(
                            FORMAT_MSO_MDOC, FORMAT_SD_JWT_VC
                        )
                    }
                    .flatMap { inputDescriptor ->
                        // NOTE: one format is supported per input descriptor
                        when (inputDescriptor.format?.jsonObject()?.keys?.first()) {
                            FORMAT_MSO_MDOC -> {
                                parseDescriptorForMsoMdocDocument(inputDescriptor, request)
                                    .let { helper.getRequestedDocuments(listOf(it)) }
                                    .also { requestedDoc ->
                                        inputDescriptorMap[inputDescriptor.id] =
                                            requestedDoc.map { it.documentId }.toList()
                                    }
                            }

                            FORMAT_SD_JWT_VC -> {
                                parseDescriptorForSdJwtVcDocument(inputDescriptor, request)
                                    .also { requestedDoc ->
                                        inputDescriptorMap[inputDescriptor.id] =
                                            requestedDoc.map { it.documentId }.toList()
                                    }
                            }

                            else -> throw IllegalArgumentException("Unsupported format ${inputDescriptor.format}")
                        }
                    }.toList()
            )
            ProcessedGenericOpenId4VpRequest(
                documentManager = documentManager,
                resolvedRequestObject = request.resolvedRequestObject,
                inputDescriptorMap = inputDescriptorMap,
                requestedDocuments = requestedDocuments,
                msoMdocNonce = msoMdocNonce
            )
        }
    }

    private fun parseDescriptorForMsoMdocDocument(
        descriptor: InputDescriptor,
        request: OpenId4VpRequest,
    ): DeviceRequestProcessor.RequestedMdocDocument {
        val requested = descriptor.constraints.fields()
            .mapNotNull { fields ->
                val path = fields.paths.first().value
                Regex("\\\$\\['(.*?)']\\['(.*?)']").find(path)
                    ?.destructured
                    ?.takeIf { (nameSpace, elementIdentifier) -> nameSpace.isNotBlank() && elementIdentifier.isNotBlank() }
                    ?.let { (nameSpace, elementIdentifier) -> nameSpace to elementIdentifier }

            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, elementIdentifiers) -> elementIdentifiers.associateWith { false } }

        return DeviceRequestProcessor.RequestedMdocDocument(
            docType = descriptor.id.value.trim(),
            requested = requested,
            readerAuthentication = {
                openid4VpX509CertificateTrustStore.getTrustResult()
                    ?.let { (chain, isTrusted) ->
                        val readerCommonName =
                            request.resolvedRequestObject.client.legalName() ?: ""
                        ReaderAuth(
                            readerAuth = byteArrayOf(0),
                            readerSignIsValid = true,
                            readerCertificatedIsTrusted = isTrusted,
                            readerCertificateChain = chain,
                            readerCommonName = readerCommonName
                        )
                    }
            }
        )
    }

    private fun parseDescriptorForSdJwtVcDocument(
        descriptor: InputDescriptor,
        request: OpenId4VpRequest,
    ): RequestedDocuments {
        val vct = descriptor.constraints.fields().find { field ->
            field.paths.first().value == "$.vct"
        }?.filter?.jsonObject()?.getValue("const")?.toString()
            ?.removeSurrounding("\"")
            ?: throw IllegalArgumentException("vct not found")

        val requestedClaims = descriptor.constraints.fields()
            .filter { it.paths.first().value != "$.vct" }
            .mapNotNull { fieldConstraint ->
                val path = fieldConstraint.paths.first().value
                // NOTE: currently we only support simple paths e.g. $.claim or $.claim.subclaim, not $.claim[0], $.claim.*
                Regex("""^\$\.(\w+(?:\.\w+)*)$""").matchEntire(path)
                    ?.groupValues?.get(1)?.split(".")?.let {
                        SdJwtVcItem(it) to (fieldConstraint.intentToRetain ?: false)
                    }
            }.toMap()

        return RequestedDocuments(documentManager.getValidIssuedSdJwtVcDocuments(vct)
            .map { doc ->
                RequestedDocument(
                    documentId = doc.id,
                    requestedItems = requestedClaims,
                    readerAuth =
                    openid4VpX509CertificateTrustStore.getTrustResult()
                        ?.let { (chain, isTrusted) ->
                            val readerCommonName =
                                request.resolvedRequestObject.client.legalName() ?: ""
                            ReaderAuth(
                                readerAuth = byteArrayOf(0),
                                readerSignIsValid = true,
                                readerCertificatedIsTrusted = isTrusted,
                                readerCertificateChain = chain,
                                readerCommonName = readerCommonName
                            )
                        }
                )
            }
        )
    }
}