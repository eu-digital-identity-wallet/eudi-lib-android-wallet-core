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
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.dcapi.DCAPIResponse
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Base64

/**
 * Builder class for creating and updating [TransactionLog] objects for presentation events.
 *
 * This class encapsulates the logic for constructing and modifying transaction logs
 * based on different stages and types of presentation data (e.g., requests, responses, errors).
 * It aims to make the process of log updates more testable and readable.
 *
 * @param metadataResolver A function that resolves a list of [DocumentId]s to their corresponding
 * metadata strings (e.g., JSON representations).
 */
class TransactionLogBuilder(
    private val metadataResolver: (Response) -> List<String>?,
) {
    /**
     * Creates an initial, empty [TransactionLog] for a presentation.
     *
     * The created log has a status of [TransactionLog.Status.Incomplete] and
     * type [TransactionLog.Type.Presentation].
     *
     * @return A new [TransactionLog] instance.
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
     * Updates the provided [TransactionLog] with information from a [Request].
     *
     * If the log's type is not [TransactionLog.Type.Presentation], it returns the log unchanged.
     * It handles different types of requests:
     * - [DeviceRequest]: Stores the raw request bytes.
     * - [OpenId4VpRequest]: Extracts and stores the presentation definition or digital credentials query from the resolved request object.
     *   Requires the resolved request to be [ResolvedRequestObject] and
     * - Other request types: Marks the log status as [TransactionLog.Status.Error].
     *
     * The timestamp of the log is updated to the current time.
     *
     * @param log The current transaction log to update.
     * @param request The request object containing data to add to the log.
     * @return An updated [TransactionLog] instance.
     * @throws IllegalArgumentException if an [OpenId4VpRequest] does not conform to expected subtypes.
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
                val rawRequest = Json.encodeToString(resolvedRequestObject.query).toByteArray()

                log.copy(
                    timestamp = Instant.now().toEpochMilli(),
                    rawRequest = rawRequest
                )
            }

            is DCAPIRequest -> {
                // TODO log dc api request
                log.copy(
                    timestamp = Instant.now().toEpochMilli(),
                )
            }

            else -> log.copy(
                timestamp = Instant.now().toEpochMilli(),
                status = TransactionLog.Status.Error
            )
        }
    }

    /**
     * Updates the provided [TransactionLog] with relying party information extracted
     * from a [RequestProcessor.ProcessedRequest].
     *
     * If the log's type is not [TransactionLog.Type.Presentation], it returns the log unchanged.
     * It extracts the relying party's name, certificate chain (Base64 encoded),
     * verification status, and reader authentication data (Base64 encoded) if available.
     * If reader authentication details are not found, a default name is used.
     *
     * The timestamp of the log is updated to the current time.
     *
     * @param log The current transaction log to update.
     * @param processedRequest The processed request containing relying party details.
     * @return An updated [TransactionLog] instance.
     */
    fun withRelyingParty(
        log: TransactionLog,
        processedRequest: RequestProcessor.ProcessedRequest,
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
     * Updates the provided [TransactionLog] with information from a [Response] and an optional [Throwable].
     *
     * If the log's type is not [TransactionLog.Type.Presentation], it returns the log unchanged.
     * It handles different types of responses:
     * - [DeviceResponse]: Stores raw response bytes, document metadata, session transcript, and sets format to CBOR.
     * - [OpenId4VpResponse]: Stores the VP token as a JSON string, document metadata, and sets format to JSON.
     * - [DCAPIResponse]: Stores raw response bytes, document metadata, and sets format to CBOR.
     * - Other response types: Throws an [IllegalArgumentException].
     *
     * The log status is set to [TransactionLog.Status.Completed] if `error` is null,
     * otherwise to [TransactionLog.Status.Error].
     * The timestamp of the log is updated to the current time.
     *
     * @param log The current transaction log to update.
     * @param response The response object.
     * @param error An optional error that occurred during response processing.
     * @return An updated [TransactionLog] instance.
     * @throws IllegalArgumentException if an unsupported response type is provided
     */
    fun withResponse(
        log: TransactionLog,
        response: Response,
        error: Throwable? = null,
    ): TransactionLog {
        if (log.type != TransactionLog.Type.Presentation) return log

        val updatedLog = when (response) {
            is DeviceResponse -> {
                log.copy(
                    rawResponse = response.deviceResponseBytes,
                    dataFormat = TransactionLog.DataFormat.Cbor,
                    metadata = metadataResolver(response),
                    sessionTranscript = response.sessionTranscriptBytes
                )
            }

            is OpenId4VpResponse -> {
                log.copy(
                    rawResponse = VPTokenConsensusJson.encodeToString(response.vpToken)
                        .toByteArray(),
                    dataFormat = TransactionLog.DataFormat.Json,
                    metadata = metadataResolver(response),
                )
            }

            is DCAPIResponse -> {
                log.copy(
                    rawResponse = response.deviceResponseBytes,
                    dataFormat = TransactionLog.DataFormat.Cbor,
                    metadata = metadataResolver(response),
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
     * Updates the provided [TransactionLog] to indicate an error occurred.
     *
     * Sets the log status to [TransactionLog.Status.Error] and updates the timestamp.
     *
     * @param log The current transaction log to update.
     * @return An updated [TransactionLog] instance with error status.
     */
    fun withError(log: TransactionLog): TransactionLog {
        return log.copy(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Error
        )
    }

    companion object
}
