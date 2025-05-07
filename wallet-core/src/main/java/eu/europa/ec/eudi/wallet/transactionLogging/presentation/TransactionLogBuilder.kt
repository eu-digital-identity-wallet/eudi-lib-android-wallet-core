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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation

import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VpContent
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant
import java.util.Base64

/**
 * Builder class for creating and updating TransactionLog objects
 * Makes log updates more testable and readable
 */
class TransactionLogBuilder(
    private val metadataResolver: (List<DocumentId>) -> List<String?>
) {
    /**
     * Creates an initial empty transaction log for presentation
     */
    fun createEmptyPresentationLog(): TransactionLog = TransactionLog(
        timestamp = Instant.now().toEpochMilli(),
        status = TransactionLog.Status.Incomplete,
        type = TransactionLog.Type.Presentation,
        relyingParty = null,
        rawRequest = null,
        rawResponse = null,
        dataFormat = null,
        sessionTranscript = null,
        metadata = null
    )
    
    /**
     * Updates the transaction log with request information
     * 
     * @param log The current transaction log
     * @param request The request to process
     * @return Updated transaction log with request data
     */
    fun withRequest(log: TransactionLog, request: Request): TransactionLog {
        if (log.type != TransactionLog.Type.Presentation) return log
        
        return when (request) {
            is DeviceRequest -> log.copy(
                timestamp = Instant.now().toEpochMilli(),
                rawRequest = request.deviceRequestBytes
            )
            
            is OpenId4VpRequest -> {
                val resolvedRequestObject = request.resolvedRequestObject
                require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) { 
                    "Only OpenId4VPAuthorization is supported" 
                }
                
                val presentationQuery = resolvedRequestObject.presentationQuery
                require(presentationQuery is PresentationQuery.ByPresentationDefinition) {
                    "Only PresentationQuery.ByPresentationDefinition is supported"
                }
                
                log.copy(
                    timestamp = Instant.now().toEpochMilli(),
                    rawRequest = Json.encodeToString(presentationQuery.value).toByteArray()
                )
            }
            
            else -> log.copy(
                timestamp = Instant.now().toEpochMilli(),
                status = TransactionLog.Status.Error
            )
        }
    }
    
    /**
     * Updates the transaction log with relying party information
     * 
     * @param log The current transaction log
     * @param processedRequest The processed request
     * @return Updated transaction log with relying party data
     */
    fun withRelyingParty(
        log: TransactionLog,
        processedRequest: RequestProcessor.ProcessedRequest
    ): TransactionLog {
        if (log.type != TransactionLog.Type.Presentation) return log
        
        val requestedDocuments = processedRequest.getOrNull()?.requestedDocuments ?: return log
        val readerAuth = requestedDocuments.firstNotNullOfOrNull { it.readerAuth }

        val name = readerAuth?.readerCommonName ?: "Unidentified Relying Party"
        val certificateChain = readerAuth
            ?.readerCertificateChain
            ?.map { Base64.getEncoder().encodeToString(it.encoded) }
            ?: emptyList()
        val isVerified = readerAuth?.isVerified == true
        val readerAuthBytes = readerAuth?.readerAuth
        
        return log.copy(
            timestamp = Instant.now().toEpochMilli(),
            relyingParty = TransactionLog.RelyingParty(
                name = name,
                certificateChain = certificateChain,
                isVerified = isVerified,
                readerAuth = readerAuthBytes?.let { Base64.getEncoder().encodeToString(it) }
            )
        )
    }
    
    /**
     * Updates the transaction log with response information
     * 
     * @param log The current transaction log
     * @param response The response to process
     * @param error Optional error that occurred during response processing
     * @return Updated transaction log with response data
     */
    fun withResponse(
        log: TransactionLog,
        response: Response,
        error: Throwable? = null
    ): TransactionLog {
        if (log.type != TransactionLog.Type.Presentation) return log
        
        val updatedLog = when (response) {
            is DeviceResponse -> {
                log.copy(
                    rawResponse = response.deviceResponseBytes,
                    dataFormat = TransactionLog.DataFormat.Cbor,
                    metadata = metadataResolver(response.documentIds),
                    sessionTranscript = response.sessionTranscriptBytes
                )
            }
            
            is OpenId4VpResponse.DeviceResponse -> {
                log.copy(
                    rawResponse = response.responseBytes,
                    dataFormat = TransactionLog.DataFormat.Cbor,
                    metadata = metadataResolver(response.documentIds),
                    sessionTranscript = response.sessionTranscript
                )
            }
            
            is OpenId4VpResponse.GenericResponse -> {
                val presentationExchange = response.consensus.vpContent
                require(presentationExchange is VpContent.PresentationExchange) {
                    "Only PresentationExchange is supported"
                }
                
                log.copy(
                    rawResponse = Json.encodeToString(
                        mapOf(
                            "verifiable_presentations" to Json.encodeToJsonElement(
                                presentationExchange.verifiablePresentations
                                    .filterIsInstance<VerifiablePresentation.Generic>()
                                    .map { Json.encodeToJsonElement(it.value) }
                            ),
                            "presentation_submission" to Json.encodeToJsonElement(
                                presentationExchange.presentationSubmission
                            )
                        )
                    ).toByteArray(),
                    dataFormat = TransactionLog.DataFormat.Json,
                    metadata = metadataResolver(response.documentIds)
                )
            }
            
            else -> throw IllegalArgumentException(
                "Unsupported response type: ${response::class.simpleName}"
            )
        }
        
        return updatedLog.copy(
            timestamp = Instant.now().toEpochMilli(),
            status = if (error == null) TransactionLog.Status.Completed else TransactionLog.Status.Error
        )
    }
    
    /**
     * Updates the transaction log with error status
     * 
     * @param log The current transaction log
     * @return Updated transaction log with error status
     */
    fun withError(log: TransactionLog): TransactionLog {
        return log.copy(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Error
        )
    }
}
