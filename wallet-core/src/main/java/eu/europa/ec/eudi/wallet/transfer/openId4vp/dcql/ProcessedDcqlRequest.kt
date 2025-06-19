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
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VpContent
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForMsoMdoc
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForSdJwtVc
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Algorithm

/**
 * Represents a processed DCQL (Digital Credentials Query Language) request for OpenID4VP flows.
 *
 * This class holds the resolved request object, document manager, and a mapping of query IDs to
 * requested documents. It is responsible for generating the response to a credential presentation
 * request, ensuring that only one document per query is disclosed, and constructing the appropriate
 * verifiable presentations for each credential format (MSO mdoc or SD-JWT VC).
 *
 * @property resolvedRequestObject The resolved OpenID4VP authorization request.
 * @property documentManager The document manager used to retrieve and manage wallet documents.
 * @property queryMap Mapping from query IDs to the requested documents for each credential.
 * @property msoMdocNonce Nonce used for MSO mdoc presentations.
 */
class ProcessedDcqlRequest(
    val resolvedRequestObject: ResolvedRequestObject.OpenId4VPAuthorization,
    private val documentManager: DocumentManager,
    private val queryMap: Map<QueryId, Pair<String, RequestedDocuments>>,
    requestedDocuments: RequestedDocuments = queryMap.values.flatMap { it.second }
        .let { RequestedDocuments(it) },
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(requestedDocuments) {
    /**
     * Generates a response for the disclosed documents, ensuring only one document per query ID is present.
     *
     * @param disclosedDocuments The documents disclosed by the user.
     * @param signatureAlgorithm The algorithm to use for signing the response, if applicable.
     * @return [ResponseResult] containing the constructed verifiable presentations or an error.
     * @throws IllegalArgumentException if multiple documents are disclosed for the same query ID.
     */
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        val result = try {

            val respondedDocuments = mutableSetOf<OpenId4VpResponse.RespondedDocument>()
            val verifiablePresentations = queryMap
                .mapNotNull { (queryId, requestedDocumentsFormatPair) ->
                    val (format, requestedDocuments) = requestedDocumentsFormatPair
                    // if multiple documents are disclosed for the same queryId, only one should be used
                    // and the rest should be ignored. We pick the first document in the list.
                    val requestedDocument = requestedDocuments.first()
                    val disclosedDocument = disclosedDocuments.firstOrNull {
                        it.documentId == requestedDocument.documentId
                    }
                    disclosedDocument
                        ?.let { disclosedDocument ->
                            respondedDocuments.add(
                                OpenId4VpResponse.RespondedDocument.QueryBased(
                                    documentId = requestedDocument.documentId,
                                    format = format,
                                    queryId = queryId.value,
                                )
                            )
                            queryId to runBlocking {
                                vpFromRequestedDocuments(
                                    requestedDocument = requestedDocument,
                                    disclosedDocument = disclosedDocument,
                                    signatureAlgorithm = signatureAlgorithm ?: Algorithm.ESP256
                                )
                            }
                        }
                }.toMap()

            val vpContent = VpContent.DCQL(
                verifiablePresentations = verifiablePresentations
            )
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(vpContent)

            val response = OpenId4VpResponse.DcqlResponse(
                resolvedRequestObject = resolvedRequestObject,
                consensus = consensus,
                msoMdocNonce = msoMdocNonce,
                response = verifiablePresentations.mapValues { it.toString() },
                respondedDocuments = respondedDocuments.toList()
            )
            ResponseResult.Success(response)
        } catch (e: Exception) {
            ResponseResult.Failure(e)
        }

        return result
    }

    private suspend fun vpFromRequestedDocuments(
        requestedDocument: RequestedDocument,
        disclosedDocument: DisclosedDocument,
        signatureAlgorithm: Algorithm
    ): VerifiablePresentation.Generic {
        val documentId = disclosedDocument.documentId
        val document = documentManager.getDocumentById(documentId) as? IssuedDocument
        requireNotNull(document) { "Document with id $documentId not found" }
        val requestedItems = requestedDocument.requestedItems
        val vp = when (document.format) {
            is MsoMdocFormat -> {
                // if document format is mso_mdoc then requestedItems should be of type MsoMdocItem
                require(requestedItems.all { it.key is MsoMdocItem })
                verifiablePresentationForMsoMdoc(
                    documentManager = documentManager,
                    sessionTranscript = resolvedRequestObject
                        .getSessionTranscriptBytes(msoMdocNonce),
                    disclosedDocument = disclosedDocument,
                    requestedDocuments = requestedDocuments,
                    signatureAlgorithm = signatureAlgorithm
                )
            }

            is SdJwtVcFormat -> {
                // if document format is dc+sd-jwt then requestedItems should be of type SdJwtVcItem
                require(requestedItems.all { it.key is SdJwtVcItem })
                verifiablePresentationForSdJwtVc(
                    resolvedRequestObject = resolvedRequestObject,
                    document = document,
                    disclosedDocument = disclosedDocument,
                    signatureAlgorithm = signatureAlgorithm
                )
            }
        }
        return vp
    }
}


