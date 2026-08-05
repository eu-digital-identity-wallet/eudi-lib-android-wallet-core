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

package eu.europa.ec.eudi.wallet.dcapi.process.isomdoc

import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.dcapi.DCAPIException
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.internal.*

import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.ProviderGetCredentialRequest
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import org.json.JSONObject
import org.multipaz.mdoc.zkp.ZkSystemRepository

/**
 * Processes ISO mdoc requests for the Digital Credential API (DCAPI), following the `org-iso-mdoc`
 * protocol of ISO/IEC TS 18013-7:2025 Annex C.
 *
 * It delegates to the ISO 18013-5 [DeviceRequestProcessor] and narrows the result to the
 * document(s) the OS credential picker selected, so the wallet only discloses what the user chose.
 *
 * @param documentManager provides the issued documents to match against the request.
 * @param readerTrustStore trust store used to verify the reader's authentication.
 * @param readerAuthPolicy how reader authentication results affect document disclosure.
 * @param privilegedAllowlist allowlist of privileged callers permitted to set the request origin.
 * @param zkSystemRepository optional Zero-Knowledge Proof system repository.
 * @param logger optional logger.
 */
class IsoMdocDCAPIRequestProcessor(
    private val documentManager: DocumentManager,
    override var readerTrustStore: ReaderTrustStore?,
    private val readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
    private val privilegedAllowlist: String,
    private var zkSystemRepository: ZkSystemRepository?,
    private val zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.Strict,
    private var logger: Logger? = null,
) : RequestProcessor, ReaderTrustStoreAware {

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override suspend fun process(request: Request): RequestProcessor.ProcessedRequest {
        require(request is DCAPIRequest) { "Request must be an DCAPIRequest" }
        logger?.d(TAG, "Processing DCAPI request")

        val credRequest = request.providerGetCredentialRequest
        val (deviceRequest, origin) = credRequest.toDeviceRequest()
        val processed = DeviceRequestProcessor(
            documentManager = documentManager,
            readerTrustStore = readerTrustStore,
            readerAuthPolicy = readerAuthPolicy,
            zkSystemRepository = zkSystemRepository,
            zkResponsePolicy = zkResponsePolicy
        ).process(deviceRequest)
        val processedDeviceRequest = processed as? ProcessedDeviceRequest
            ?: run {
                logger?.e(TAG, "DeviceRequestProcessor did not return ProcessedDeviceRequest: $processed")
                return RequestProcessor.ProcessedRequest.Failure(
                    DCAPIException("DeviceRequestProcessor failed: $processed"),
                )
            }

        val selectedIds = credRequest.selectedDocumentIds()
        if (selectedIds.isEmpty()) {
            logger?.e(TAG, "No credential selected by the OS picker for the DCAPI request")
            return RequestProcessor.ProcessedRequest.Failure(
                DCAPIException("No selected credential in DCAPI request"),
            )
        }
        logger?.d(TAG, "Filtering presentment data for credential IDs: $selectedIds")

        val filteredPresentmentData = processedDeviceRequest.presentmentData
            .filterByCredentialIds(selectedIds)
        if (filteredPresentmentData.credentialSets.isEmpty()) {
            logger?.e(TAG, "No requested document found for credential IDs: $selectedIds")
            return RequestProcessor.ProcessedRequest.Failure(
                DCAPIException("No requested document found for credential IDs: $selectedIds"),
            )
        }

        return ProcessedIsoMdocDCAPIRequest(
            processedDeviceRequest = processedDeviceRequest,
            providerGetCredentialRequest = request.providerGetCredentialRequest,
            origin = origin,
            presentmentData = filteredPresentmentData,
            requester = processedDeviceRequest.requester,
            trustMetadata = processedDeviceRequest.trustMetadata,
            logger = logger
        )
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun ProviderGetCredentialRequest.toDeviceRequest(): Pair<DeviceRequest, String> {
        val callingOrigin = resolveOrigin(privilegedAllowlist)
        logger?.d(TAG, "Origin: $callingOrigin")

        val (protocol, index) = resolveDcApiRequest(listOf(DCAPIProtocol.ISO_MDOC))
        require(protocol == DCAPIProtocol.ISO_MDOC.identifier) { "Unsupported protocol: $protocol" }

        val option = this.credentialOptions[0] as GetDigitalCredentialOption
        val requestJson = JSONObject(option.requestJson)
        val request = requestJson.getJSONArray(REQUESTS).getJSONObject(index)
        val data = request[DATA] as JSONObject
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

    companion object {
        private const val TAG = "IsoMdocDCAPIRequestProcessor"
        private const val DEVICE_REQUEST = "deviceRequest"
    }
}