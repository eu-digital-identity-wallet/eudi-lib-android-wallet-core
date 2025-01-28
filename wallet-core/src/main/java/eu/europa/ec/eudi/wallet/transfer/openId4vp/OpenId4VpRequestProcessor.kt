/*
 * Copyright (c) 2024 European Commission
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
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.prex.InputDescriptor
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.OpenId4VpUtils.generateMdocGeneratedNonce
import eu.europa.ec.eudi.wallet.internal.OpenId4VpUtils.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.internal.Openid4VpX509CertificateTrust

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
        val requestedFormats = request.resolvedRequestObject.presentationDefinition.inputDescriptors
            .flatMap { it.format?.jsonObject()?.keys ?: emptySet() }
            .toSet()
        require(requestedFormats.isNotEmpty()) { "No format is requested" }

        return if (requestedFormats.size == 1 && requestedFormats.first() == "mso_mdoc") {
            val requestedDocuments: RequestedDocuments =
                request.resolvedRequestObject.presentationDefinition.inputDescriptors
                    .map { descriptor -> descriptor.toParsedRequestedMsoMdocDocument(request) }
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
            var mdocGeneratedNonce: String? = null
            val requestedDocuments = RequestedDocuments(
                request.resolvedRequestObject.presentationDefinition.inputDescriptors
                    // NOTE: mso_mdoc and vc+sd-jwt are supported, other formats are ignored
                    .filter {
                        it.format?.jsonObject()?.keys?.first() in listOf(
                            "mso_mdoc",
                            "vc+sd-jwt"
                        )
                    }
                    .flatMap { inputDescriptor ->
                        // NOTE: one format is supported per input descriptor
                        when (inputDescriptor.format?.jsonObject()?.keys?.first()) {
                            "mso_mdoc" -> {
                                inputDescriptor.toParsedRequestedMsoMdocDocument(request)
                                    .let { helper.getRequestedDocuments(listOf(it)) }
                                    .also {
                                        mdocGeneratedNonce =
                                            mdocGeneratedNonce ?: generateMdocGeneratedNonce()
                                    }.also { requestedDoc ->
                                        inputDescriptorMap[inputDescriptor.id] =
                                            requestedDoc.map { it.documentId }.toList()
                                    }
                            }

                            "vc+sd-jwt" -> {
                                inputDescriptor.toParsedRequestedSdJwtVcDocument(request)
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
                documentManager,
                request.resolvedRequestObject,
                inputDescriptorMap,
                requestedDocuments,
                mdocGeneratedNonce
            )
        }
    }

    private fun InputDescriptor.toParsedRequestedMsoMdocDocument(request: OpenId4VpRequest): DeviceRequestProcessor.RequestedMdocDocument {
        val requested = constraints.fields()
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
            docType = id.value.trim(),
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

    private fun InputDescriptor.toParsedRequestedSdJwtVcDocument(request: OpenId4VpRequest): RequestedDocuments {
        val vct = constraints.fields().find { field ->
            field.paths.first().value == "$.vct"
        }?.filter?.jsonObject()?.getValue("const")?.toString()?.removeSurrounding("\"")
            ?: throw IllegalArgumentException("vct not found")

        val requestedClaims = this.constraints.fields()
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