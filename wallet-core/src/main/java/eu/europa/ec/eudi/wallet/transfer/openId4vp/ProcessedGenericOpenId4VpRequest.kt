/*
 * Copyright (c) 2025 European Commission
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
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VpContent
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForMsoMdoc
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForSdJwtVc
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Algorithm
import java.util.UUID

class ProcessedGenericOpenId4VpRequest(
    private val documentManager: DocumentManager,
    private val resolvedRequestObject: ResolvedRequestObject,
    private val inputDescriptorMap: Map<InputDescriptorId, List<DocumentId>>,
    requestedDocuments: RequestedDocuments,
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(requestedDocuments) {

    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        return runBlocking {
            try {
                require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
                val presentationQuery = resolvedRequestObject.presentationQuery
                require(presentationQuery is PresentationQuery.ByPresentationDefinition) {
                    "Currently only PresentationDefinition is supported"
                }

                val signatureAlgorithm = signatureAlgorithm ?: Algorithm.ESP256
                val presentationDefinition = presentationQuery.value
                val verifiablePresentations = disclosedDocuments
                    .filter { it.disclosedItems.isNotEmpty() } // remove empty disclosed documents
                    .map { disclosedDocument ->
                        val document = documentManager.getValidIssuedDocumentById(
                            documentId = disclosedDocument.documentId
                        )
                        val verifiablePresentation = when (document.format) {
                            is SdJwtVcFormat -> verifiablePresentationForSdJwtVc(
                                resolvedRequestObject = resolvedRequestObject,
                                document = document,
                                disclosedDocument = disclosedDocument,
                                signatureAlgorithm = signatureAlgorithm,
                            )

                            is MsoMdocFormat -> verifiablePresentationForMsoMdoc(
                                documentManager = documentManager,
                                sessionTranscript = resolvedRequestObject
                                    .getSessionTranscriptBytes(msoMdocNonce),
                                disclosedDocument = disclosedDocument,
                                requestedDocuments = requestedDocuments,
                                signatureAlgorithm = signatureAlgorithm
                            )
                        }
                        Pair(document, verifiablePresentation)
                    }

                val (descriptorMaps, documentIds) = constructDescriptorsMap(
                    inputDescriptorMap = inputDescriptorMap,
                    verifiablePresentations = verifiablePresentations
                )

                val presentationSubmission = PresentationSubmission(
                    id = Id(UUID.randomUUID().toString()),
                    definitionId = presentationDefinition.id,
                    descriptorMaps = descriptorMaps,
                )
                val vpContent = VpContent.PresentationExchange(
                    verifiablePresentations = verifiablePresentations.map { it.second }.toList(),
                    presentationSubmission = presentationSubmission,
                )
                val consensus = Consensus.PositiveConsensus.VPTokenConsensus(vpContent)

                ResponseResult.Success(
                    OpenId4VpResponse.GenericResponse(
                        resolvedRequestObject = resolvedRequestObject,
                        consensus = consensus,
                        msoMdocNonce = msoMdocNonce,
                        response = verifiablePresentations
                            .map { it.second.toString() }
                            .toList(),
                        documentIds = documentIds
                    )
                )
            } catch (e: Throwable) {
                ResponseResult.Failure(e)
            }
        }
    }
}

/**
 * Constructs descriptor maps and collects document IDs based on the input descriptor map
 * and the provided verifiable presentations.
 *
 * This function maps each verifiable presentation to its corresponding input descriptor ID,
 * determines the format of the document, and creates a JSON path for the document within
 * the presentation. The resulting descriptor maps and document IDs are returned as a pair.
 *
 * @param inputDescriptorMap A map where keys are input descriptor IDs and values are lists of document IDs.
 * @param verifiablePresentations A list of pairs containing issued documents and their corresponding verifiable presentations.
 * @return A pair containing:
 *         - A list of `DescriptorMap` objects, each representing the mapping of a document to its descriptor.
 *         - A list of `DocumentId` objects representing the IDs of the documents included in the descriptor maps.
 * @throws IllegalArgumentException If no input descriptor is found for a document.
 * @throws IllegalStateException If the JSON path creation fails.
 */
internal fun constructDescriptorsMap(
    inputDescriptorMap: Map<InputDescriptorId, List<DocumentId>>,
    verifiablePresentations: List<Pair<IssuedDocument, VerifiablePresentation.Generic>>,
): Pair<List<DescriptorMap>, List<DocumentId>> {

    val documentIds = mutableListOf<DocumentId>()
    val descriptorMaps = verifiablePresentations.mapIndexed { index, (document, _) ->
        // get the input descriptor id for the document
        // that is in the verifiable presentation
        val inputDescriptorId = inputDescriptorMap.entries
            .firstOrNull { (_, documentIds) ->
                documentIds.contains(document.id)
            }?.key
            ?: throw IllegalArgumentException("No input descriptor found for document")
        // determine the format of the document
        val format = when (document.format) {
            is MsoMdocFormat -> FORMAT_MSO_MDOC
            is SdJwtVcFormat -> FORMAT_SD_JWT_VC
        }
        // create the json path for the document
        // if there are multiple verifiable presentations
        // the json path will be an array
        // e.g. $[0], $[1]
        // otherwise it will be just $
        val jsonPath = JsonPath.jsonPath(
            if (verifiablePresentations.size > 1) {
                "$[$index]"
            } else {
                "$"
            }
        ) ?: throw IllegalStateException("Failed to create JsonPath")
        documentIds.add(document.id)
        // create the descriptor map
        DescriptorMap(
            id = inputDescriptorId,
            format = format,
            path = jsonPath
        )
    }

    return Pair(descriptorMaps, documentIds.toList())
}


