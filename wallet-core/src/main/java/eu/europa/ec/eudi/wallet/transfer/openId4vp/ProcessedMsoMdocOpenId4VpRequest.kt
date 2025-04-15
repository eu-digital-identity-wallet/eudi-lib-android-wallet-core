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

import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VpContent
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import org.multipaz.crypto.Algorithm
import java.util.Base64
import java.util.UUID

/**
 * Processes and handles OpenID4VP requests specifically for MSO_MDOC document format.
 *
 * This class extends [RequestProcessor.ProcessedRequest.Success] and specializes in handling
 * OpenID for Verifiable Presentation (OpenID4VP) requests that exclusively target MSO_MDOC
 * (Mobile Security Object Mobile Driving License) document formats. It transforms the processed
 * device request into an OpenID4VP response that conforms to the Presentation Exchange protocol.
 *
 * The class is responsible for:
 * - Converting device responses to OpenID4VP verifiable presentations
 * - Creating presentation submissions based on input descriptors
 * - Generating Base64-encoded responses suitable for OpenID4VP communication
 *
 * @param processedDeviceRequest The device request that has been processed and is ready for response generation
 * @param resolvedRequestObject The resolved OpenID4VP request object containing presentation query information
 * @param msoMdocNonce A nonce value used for the MSO_MDOC protocol to prevent replay attacks
 */
class ProcessedMsoMdocOpenId4VpRequest(
    private val processedDeviceRequest: ProcessedDeviceRequest,
    private val resolvedRequestObject: ResolvedRequestObject,
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(processedDeviceRequest.requestedDocuments) {

    /**
     * Generates an OpenID4VP response from the disclosed documents.
     *
     * This method converts the disclosed documents into an OpenID4VP response by:
     * 1. Validating the OpenID4VP request object and presentation definition
     * 2. Generating a device response using the processed request
     * 3. Encoding the device response as a verifiable presentation
     * 4. Creating a presentation submission that maps input descriptors to the presentation
     * 5. Wrapping everything in an OpenID4VP response object
     *
     * @param disclosedDocuments Documents that the user has agreed to disclose
     * @param signatureAlgorithm Optional algorithm to use for signing the response
     * @return A [ResponseResult] object containing either a successful [OpenId4VpResponse.DeviceResponse]
     *         or a [ResponseResult.Failure] with the error that occurred during processing
     * @throws IllegalArgumentException if the request object is not a valid OpenId4VPAuthorization
     *         or if the presentation query is not a PresentationDefinition
     */
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        return try {
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val presentationQuery = resolvedRequestObject.presentationQuery
            require(presentationQuery is PresentationQuery.ByPresentationDefinition) {
                "Currently only PresentationDefinition is supported"
            }

            val presentationDefinition = presentationQuery.value

            val deviceResponse = processedDeviceRequest.generateResponse(
                disclosedDocuments,
                signatureAlgorithm
            ).getOrThrow() as DeviceResponse

            val vpContent = VpContent.PresentationExchange(
                verifiablePresentations = listOf(
                    VerifiablePresentation.Generic(
                        Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(deviceResponse.deviceResponseBytes)
                    )
                ),
                presentationSubmission = PresentationSubmission(
                    id = Id(UUID.randomUUID().toString()),
                    definitionId = presentationDefinition.id,
                    descriptorMaps = presentationDefinition.inputDescriptors.map { inputDescriptor ->
                        DescriptorMap(
                            id = inputDescriptor.id,
                            format = FORMAT_MSO_MDOC,
                            path = JsonPath.Companion.jsonPath("$")
                                ?: throw IllegalStateException("Failed to create JsonPath")
                        )
                    }
                )
            )
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(vpContent)

            ResponseResult.Success(
                OpenId4VpResponse.DeviceResponse(
                    resolvedRequestObject = resolvedRequestObject,
                    consensus = consensus,
                    msoMdocNonce = msoMdocNonce,
                    sessionTranscript = deviceResponse.sessionTranscriptBytes,
                    responseBytes = deviceResponse.deviceResponseBytes,
                    documentIds = deviceResponse.documentIds
                )
            )
        } catch (e: Throwable) {
            ResponseResult.Failure(e)
        }
    }
}

