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
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.prex.InputDescriptor
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
        get() = Openid4VpX509CertificateTrust(readerTrustStore)

    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        require(request is OpenId4VpRequest) { "Request must be an OpenId4VpRequest" }
        require(request.resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) {
            "Request must have be a OpenId4VPAuthorization"
        }
        // Currently, we only support mso_mdoc format
        // TODO In the future, we will need to change this to support also sd-jwt-vc format also
        val requestedDocuments: RequestedDocuments =
            request.resolvedRequestObject.presentationDefinition.inputDescriptors
                // filter non mso_mdoc descriptors
                .filter { true == it.format?.jsonObject()?.containsKey("mso_mdoc") }
                .map { descriptor -> descriptor.toParsedRequestedDocument(request) }
                .let { helper.getRequestedDocuments(it) }
        val msoMdocNonce = generateMdocGeneratedNonce()
        val sessionTranscriptBytes =
            request.resolvedRequestObject.getSessionTranscriptBytes(msoMdocNonce)

        return ProcessedOpenId4VpRequest(
            resolvedRequestObject = request.resolvedRequestObject,
            processedDeviceRequest = ProcessedDeviceRequest(
                documentManager = documentManager,
                requestedDocuments = requestedDocuments,
                sessionTranscript = sessionTranscriptBytes
            ),
            msoMdocNonce = msoMdocNonce
        )
    }

    private fun InputDescriptor.toParsedRequestedDocument(request: OpenId4VpRequest): DeviceRequestProcessor.RequestedMdocDocument {
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


}