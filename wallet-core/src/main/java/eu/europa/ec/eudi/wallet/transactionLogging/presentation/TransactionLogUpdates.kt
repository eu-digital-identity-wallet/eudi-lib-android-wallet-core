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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant
import java.util.Base64

/**
 * Updates the transaction log with the request information.
 *
 * It determines the type of request and updates the transaction log accordingly.
 *
 * @receiver The current transaction log.
 * @param request The request to be added to the transaction log.
 * @return A new transaction log with the request information added.
 */
internal fun TransactionLog.withRequest(request: Request): TransactionLog {
    if (type != TransactionLog.Type.Presentation) return this
    return when (request) {
        is DeviceRequest -> copy(
            timestamp = Instant.now().toEpochMilli(),
            rawRequest = request.deviceRequestBytes,
        )

        is OpenId4VpRequest -> {
            val resolvedRequestObject = request.resolvedRequestObject
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val presentationQuery = resolvedRequestObject.presentationQuery
            require(presentationQuery is PresentationQuery.ByPresentationDefinition)
            copy(
                timestamp = Instant.now().toEpochMilli(),
                rawRequest = Json.encodeToString(presentationQuery.value).toByteArray(),
            )
        }

        else -> throw IllegalArgumentException(
            "Unsupported request type: ${request::class.simpleName}"
        )
    }
}

/**
 * Updates the transaction log with the relying party information.
 *
 * @receiver The current transaction log.
 * @param processedRequest The processed request to be added to the transaction log.
 */
internal fun TransactionLog.withRelyingParty(processedRequest: RequestProcessor.ProcessedRequest): TransactionLog {
    if (type != TransactionLog.Type.Presentation) return this
    val requestedDocuments = processedRequest.getOrNull()?.requestedDocuments ?: return this
    val readerAuth = requestedDocuments.firstNotNullOf { it.readerAuth }

    val name = readerAuth.readerCommonName
    val certificateChain = readerAuth.readerCertificateChain
        .map { Base64.getEncoder().encodeToString(it.encoded) }
    val isVerified = readerAuth.isVerified
    val readerAuthBytes = readerAuth.readerAuth
    return copy(
        timestamp = Instant.now().toEpochMilli(),
        relyingParty = TransactionLog.RelyingParty(
            name = name,
            certificateChain = certificateChain,
            isVerified = isVerified,
            readerAuth = readerAuthBytes.let { Base64.getEncoder().encodeToString(it) }
        )
    )
}

/**
 * Updates the transaction log with the response information.
 *
 * It determines the type of response and updates the transaction log accordingly.
 * Three types of responses are supported:
 *  - DeviceResponse
 *  - OpenId4VpResponse.DeviceResponse
 *  - OpenId4VpResponse.GenericResponse
 *
 *  The metadataResolver function is used to resolve the metadata for the response.
 *
 *  If an exception occurs during the response processing, it is recorded in the transaction log
 *  with the status set to Error.
 *
 * @receiver The current transaction log.
 * @param response The response to be added to the transaction log.
 * @param metadataResolver A function that resolves the metadata for the response.
 * @param e An optional exception that occurred during the response processing.
 * @return A new transaction log with the response information added.
 */
fun TransactionLog.withResponse(
    response: Response,
    metadataResolver: (List<DocumentId>) -> List<String?>,
    e: Throwable? = null
): TransactionLog {
    if (type != TransactionLog.Type.Presentation) return this
    return when (response) {
        is DeviceResponse -> {
            this.copy(
                rawResponse = response.deviceResponseBytes,
                dataFormat = TransactionLog.DataFormat.Cbor,
                metadata = metadataResolver(response.documentIds),
                sessionTranscript = response.sessionTranscriptBytes,
            )
        }

        is OpenId4VpResponse.DeviceResponse -> {
            this.copy(
                rawResponse = response.responseBytes,
                dataFormat = TransactionLog.DataFormat.Cbor,
                metadata = metadataResolver(response.documentIds),
                sessionTranscript = response.sessionTranscript,
            )
        }

        is OpenId4VpResponse.GenericResponse -> {
            val presentationExchange = response.consensus.vpContent
            require(presentationExchange is VpContent.PresentationExchange) {
                "Only PresentationExchange is supported"
            }
            this.copy(

                rawResponse = Json.encodeToString(
                    mapOf(
                        "verifiable_presentations" to Json.encodeToJsonElement(
                            presentationExchange.verifiablePresentations
                                .filterIsInstance<VerifiablePresentation.Generic>()
                                .map { Json.encodeToJsonElement(it.value) }),
                        "presentation_submission" to Json.encodeToJsonElement(presentationExchange.presentationSubmission)
                    )
                ).toByteArray(),
                dataFormat = TransactionLog.DataFormat.Json,
                metadata = metadataResolver(response.documentIds)
            )
        }

        else -> throw IllegalArgumentException(
            "Unsupported response type: ${response::class.simpleName}"
        )
    }.copy(
        timestamp = Instant.now().toEpochMilli(),
        status = if (e == null) TransactionLog.Status.Completed else TransactionLog.Status.Error
    )

}
