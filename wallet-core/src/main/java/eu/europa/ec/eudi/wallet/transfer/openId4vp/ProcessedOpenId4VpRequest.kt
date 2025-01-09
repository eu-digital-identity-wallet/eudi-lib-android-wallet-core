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

import com.android.identity.crypto.Algorithm
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VpToken
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.Companion.present
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.Companion.serialize
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import java.util.Base64
import java.util.UUID

class ProcessedMsoMdocOpenId4VpRequest(
    private val processedDeviceRequest: ProcessedDeviceRequest,
    private val resolvedRequestObject: ResolvedRequestObject,
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(processedDeviceRequest.requestedDocuments) {
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        return try {
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val deviceResponse = processedDeviceRequest.generateResponse(
                disclosedDocuments,
                signatureAlgorithm
            ).getOrThrow() as DeviceResponse
            val vpToken = VpToken.MsoMdoc(
                Base64URL.encode(msoMdocNonce),
                Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(deviceResponse.deviceResponseBytes)
            )

            val presentationDefinition = resolvedRequestObject.presentationDefinition
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(
                vpToken = vpToken,
                presentationSubmission = PresentationSubmission(
                    id = Id(UUID.randomUUID().toString()),
                    definitionId = presentationDefinition.id,
                    descriptorMaps = presentationDefinition.inputDescriptors.map { inputDescriptor ->
                        DescriptorMap(
                            id = inputDescriptor.id,
                            format = "mso_mdoc",
                            path = JsonPath.Companion.jsonPath("$")!!
                        )
                    }
                )
            )
            ResponseResult.Success(
                OpenId4VpResponse.DeviceResponse(
                    resolvedRequestObject = resolvedRequestObject,
                    consensus = consensus,
                    vpToken = vpToken,
                    responseBytes = deviceResponse.deviceResponseBytes,
                    msoMdocNonce = msoMdocNonce
                )
            )
        } catch (e: Throwable) {
            ResponseResult.Failure(e)
        }
    }
}

class ProcessedGenericOpenId4VpRequest(
    private val documentManager: DocumentManager,
    private val resolvedRequestObject: ResolvedRequestObject,
    private val inputDescriptorMap : Map<InputDescriptorId, List<DocumentId>>,
    requestedDocuments: RequestedDocuments,
) : RequestProcessor.ProcessedRequest.Success(requestedDocuments) {
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?
    ): ResponseResult {
        return try {
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val sdJwtVCsForPresentation = disclosedDocuments.map {
                val document = documentManager.getValidIssuedSdJwtVcDocumentById(it.documentId)
                require(document.format is SdJwtVcFormat)
                // TODO: support SD JWT Vc && mDoc format in one response
                // TODO: Key Binding
                val issuedSdJwt =
                    SdJwt.unverifiedIssuanceFrom(String(document.issuerProvidedData)).getOrThrow()
                document.id to (issuedSdJwt.present(it.disclosedItems.map { disclosedItem ->
                    ClaimPath.claim(disclosedItem.elementIdentifier)
                }.toSet())?.serialize()
                    ?: return ResponseResult.Failure(IllegalArgumentException("Failed to create SD JWT VC presentation")))
            }.toList()
            val vpToken = VpToken.Generic(*(sdJwtVCsForPresentation.map { it.second }).toTypedArray())
            val presentationDefinition = resolvedRequestObject.presentationDefinition
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(
                vpToken = vpToken,
                presentationSubmission = PresentationSubmission(
                    id = Id(UUID.randomUUID().toString()),
                    definitionId = presentationDefinition.id,
                    descriptorMaps = inputDescriptorMap.entries.flatMap { inputDescriptor ->
                        sdJwtVCsForPresentation.mapIndexed { index, sdjwt ->
                            inputDescriptor.value.takeIf { it.contains(sdjwt.first) }?.let {
                                DescriptorMap(
                                    id = inputDescriptor.key,
                                    format = "vc+sd-jwt",
                                    path = JsonPath.jsonPath(if(sdJwtVCsForPresentation.size > 1) "$[$index]" else "$")!!
                                )
                            }
                        }
                    }.filterNotNull()
                ))
            ResponseResult.Success(
                OpenId4VpResponse.GenericResponse(
                    resolvedRequestObject = resolvedRequestObject,
                    consensus = consensus,
                    vpToken = vpToken,
                    response = sdJwtVCsForPresentation.map { it.second }.toList()
                )
            )
        } catch (e: Throwable) {
            ResponseResult.Failure(e)
        }
    }
}