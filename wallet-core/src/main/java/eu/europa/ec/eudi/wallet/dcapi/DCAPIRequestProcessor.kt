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

package eu.europa.ec.eudi.wallet.dcapi

import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.selectedEntryId
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import org.json.JSONObject

/**
 * Processes requests for the Digital Credential API (DCAPI) by converting them into device requests
 * and processing them using the [DeviceRequestProcessor].
 *
 * This implementation follows the protocol `org-iso-mdoc` as defined in the ISO/IEC TS 18013-7:2025 Annex C.
 *
 * @property documentManager The [DocumentManager] instance used to manage documents.
 * @property readerTrustStore The [ReaderTrustStore] the reader trust store.
 * @property privilegedAllowlist The allowlist for privileged browsers/apps that are trusted.
 * @property logger Optional logger for logging events.
 *
 */

private const val TAG = "DCAPIRequestProcessor"

internal class DCAPIRequestProcessor(
    private val documentManager: DocumentManager,
    override var readerTrustStore: ReaderTrustStore?,
    private val privilegedAllowlist: String,
    private var logger: Logger? = null
    ): RequestProcessor, ReaderTrustStoreAware {

    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        require(request is DCAPIRequest) { "Request must be an DCAPIRequest" }
        logger?.d(TAG, "Processing DCAPI request")

        val credRequest = request.providerGetCredentialRequest
        val (deviceRequest, origin) = credRequest.toDeviceRequest()
        val processedDeviceRequest = DeviceRequestProcessor(
            documentManager = documentManager,
            readerTrustStore = readerTrustStore
        ).process(deviceRequest) as ProcessedDeviceRequest

        val credentialId = credRequest.selectedEntryId
        logger?.d(TAG, "Selected credential ID: $credentialId")

        // Filter the requested documents and ProcessedDeviceRequest
        // based on the selected credential ID provided by the credential request.
        logger?.d(TAG, "Filtering requested documents for credential ID: $credentialId")
        val filteredRequestedDocuments = processedDeviceRequest.requestedDocuments.filter {
            it.documentId == credentialId
        }
        if (filteredRequestedDocuments.isEmpty()) {
            logger?.e(TAG, "No requested document found for credential ID: $credentialId")
            return RequestProcessor.ProcessedRequest.Failure(
                DCAPIException("No requested document found for credential ID: $credentialId")
            )
        }
        val filteredProcessedDeviceRequest = ProcessedDeviceRequest(
            documentManager = documentManager,
            sessionTranscript = deviceRequest.sessionTranscriptBytes,
            requestedDocuments = RequestedDocuments(filteredRequestedDocuments)
        )
        return ProcessedDCPAPIRequest(
            processedDeviceRequest = filteredProcessedDeviceRequest,
            providerGetCredentialRequest = request.providerGetCredentialRequest,
            origin = origin,
            requestedDocuments = RequestedDocuments(filteredRequestedDocuments),
            logger = logger
        )
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun ProviderGetCredentialRequest.toDeviceRequest(): Pair<DeviceRequest, String> {
        // If the mdoc does not receive the origin from the API, it shall abort the transaction.
        val callingOrigin = this.callingAppInfo.getOrigin(privilegedAllowlist)
        requireNotNull(callingOrigin) { "Calling origin must not be null." }
        logger?.d(TAG, "Origin: $callingOrigin")

        val option = this.credentialOptions[0] as GetDigitalCredentialOption
        val requestJson = JSONObject(option.requestJson)
        val firstRequest = requestJson.getJSONArray(REQUESTS).getJSONObject(0)
        val protocol = firstRequest[PROTOCOL] as String

        require(protocol == DC_API_PROTOCOL_ORG_ISO_MDOC) { "Unsupported protocol: $protocol" }

        val data = firstRequest[DATA] as JSONObject
        val deviceRequestBase64 = data[DEVICE_REQUEST] as String
        val encryptionInfoBase64 = data.getString(ENCRYPTION_INFO)
        val deviceRequestBytes = deviceRequestBase64.fromBase64()
        val sessionTranscriptBytes =
            getDCAPIIsoMdocSessionTranscript(encryptionInfoBase64, callingOrigin)

        logger?.apply {
            d(TAG, "Processing DCAPI request for protocol: $protocol")
            d(TAG, "Device request Base64: $deviceRequestBase64")
            d(TAG, "Encryption info Base64: $encryptionInfoBase64")
        }

        return DeviceRequest(
            deviceRequestBytes = deviceRequestBytes,
            sessionTranscriptBytes = sessionTranscriptBytes
        ) to callingOrigin
    }
}