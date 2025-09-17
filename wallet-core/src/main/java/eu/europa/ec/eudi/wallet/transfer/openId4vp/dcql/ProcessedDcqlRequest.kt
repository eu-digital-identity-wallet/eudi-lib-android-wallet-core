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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifiablePresentations
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForMsoMdoc
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForSdJwtVc
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_MSO_MDOC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_SD_JWT_VC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Algorithm

/**
 * Represents a processed DCQL (Digital Credentials Query Language) request for OpenID4VP flows.
 *
 * After a DCQL request has been processed by the [DcqlRequestProcessor], this class holds the results
 * and is responsible for generating appropriate responses when the user selects which documents
 * to disclose. It supports multiple document formats and ensures proper response formatting.
 *
 * This class:
 * 1. Organizes requested documents by query ID and document format
 * 2. Generates verifiable presentations for selected documents
 * 3. Ensures only one document per query is disclosed (per protocol requirements)
 * 4. Constructs properly formatted OpenID4VP responses
 *
 * @property resolvedRequestObject The parsed OpenID4VP authorization request with presentation query details
 * @property documentManager Manager to access and handle wallet documents
 * @property queryMap Mapping from query IDs to requested documents, organized by format
 * @property msoMdocNonce Random nonce used for MSO mdoc format presentations for security
 */
class ProcessedDcqlRequest(
    val resolvedRequestObject: ResolvedRequestObject.OpenId4VPAuthorization,
    private val documentManager: DocumentManager,
    private val queryMap: RequestedDocumentsByQueryId,
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(RequestedDocuments(queryMap.flatMap { it.value.requestedDocuments })) {
    /**
     * Generates an OpenID4VP response with verifiable presentations for the selected documents.
     *
     * This method creates appropriate verifiable presentations based on the document format:
     * - For MSO mdoc format documents, it generates ISO 18013-5 compatible presentations
     * - For SD-JWT VC format documents, it creates SD-JWT format verifiable presentations
     *
     * Important aspects:
     * - Only one document per query ID is disclosed (if multiple are selected, only the first is used)
     * - Each document is wrapped in its appropriate format-specific verifiable presentation
     * - Documents are tracked with proper metadata for later transaction logging
     *
     * @param disclosedDocuments Documents selected by the user to disclose
     * @param signatureAlgorithm Algorithm to use for signing the presentations
     * @return [ResponseResult] with prepared response or error information
     */
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        val result = try {
            // Set to track all the documents that will be included in the response
            val respondedDocumentsMap =
                mutableMapOf<QueryId, List<OpenId4VpResponse.RespondedDocument>>()
            val verifiablePresentationsMap = mutableMapOf<QueryId, List<VerifiablePresentation>>()
            queryMap.forEach { (queryId, requestedDocumentsByFormat) ->
                val (format, requestedDocuments) = requestedDocumentsByFormat
                val respondedDocuments = mutableListOf<OpenId4VpResponse.RespondedDocument>()
                val verifiablePresentationsForQueryId = mutableListOf<VerifiablePresentation>()
                disclosedDocuments
                    .filter { disclosedDocument ->
                        disclosedDocument.documentId in requestedDocuments.map { it.documentId }
                    }.map { disclosedDocument ->
                        val respondedDocument = OpenId4VpResponse.RespondedDocument(
                            documentId = disclosedDocument.documentId,
                            format = format,
                        )
                        respondedDocuments.add(respondedDocument)
                        val verifiablePresentation = runBlocking {
                            vpFromRequestedDocuments(
                                format = format,
                                requestedDocuments = requestedDocuments,
                                disclosedDocument = disclosedDocument,
                                signatureAlgorithm = signatureAlgorithm ?: Algorithm.ESP256
                            )
                        }
                        verifiablePresentationsForQueryId.add(verifiablePresentation)
                    }
                respondedDocumentsMap.put(queryId, respondedDocuments)
                verifiablePresentationsMap.put(queryId, verifiablePresentationsForQueryId)
            }
            val verifiablePresentations = VerifiablePresentations(verifiablePresentationsMap)

            val vpToken = Consensus.PositiveConsensus.VPTokenConsensus(verifiablePresentations)

            // Construct the complete response object
            val response = OpenId4VpResponse(
                resolvedRequestObject = resolvedRequestObject,
                vpToken = vpToken,
                msoMdocNonce = msoMdocNonce,
                respondedDocuments = respondedDocumentsMap.toMap()
            )
            ResponseResult.Success(response)
        } catch (e: Exception) {
            // Propagate any errors that occur during response generation
            ResponseResult.Failure(e)
        }

        return result
    }

    /**
     * Creates a format-specific verifiable presentation for a disclosed document.
     *
     * This method handles the conversion of different document formats into their
     * corresponding verifiable presentation formats as defined by OpenID4VP specifications.
     *
     * @param format Document format identifier (MSO_MDOC or SD_JWT_VC)
     * @param requestedDocuments The collection of all requested documents for this query
     * @param disclosedDocument The specific document selected to disclose
     * @param signatureAlgorithm The algorithm to use for signing the presentation
     * @return A generic verifiable presentation containing the requested document data
     * @throws IllegalArgumentException If document format doesn't match expected format or is unsupported
     */
    private suspend fun vpFromRequestedDocuments(
        format: String,
        requestedDocuments: RequestedDocuments,
        disclosedDocument: DisclosedDocument,
        signatureAlgorithm: Algorithm
    ): VerifiablePresentation.Generic {
        val documentId = disclosedDocument.documentId
        // Retrieve the full document from the document manager
        val document = documentManager.getDocumentById(documentId) as? IssuedDocument
        requireNotNull(document) { "Document with id $documentId not found" }

        // Determine the document format and ensure it matches expected format
        val documentFormat = when (document.format) {
            is MsoMdocFormat -> FORMAT_MSO_MDOC
            is SdJwtVcFormat -> FORMAT_SD_JWT_VC
        }
        require(format == documentFormat) {
            "Document with id $documentId is not of format $format"
        }

        // Generate the appropriate verifiable presentation based on format
        val vp = when (format) {
            FORMAT_MSO_MDOC -> {
                // For MSO mdoc, include session transcript and handle devic engagement
                verifiablePresentationForMsoMdoc(
                    documentManager = documentManager,
                    sessionTranscript = resolvedRequestObject
                        .getSessionTranscriptBytes(msoMdocNonce),
                    disclosedDocument = disclosedDocument,
                    requestedDocuments = requestedDocuments,
                    signatureAlgorithm = signatureAlgorithm
                )
            }

            FORMAT_SD_JWT_VC -> {
                // For SD-JWT, create presentation according to SD-JWT VC format
                verifiablePresentationForSdJwtVc(
                    resolvedRequestObject = resolvedRequestObject,
                    document = document,
                    disclosedDocument = disclosedDocument,
                    signatureAlgorithm = signatureAlgorithm
                )
            }

            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
        return vp
    }
}
