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
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import org.json.JSONObject
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember

/**
 * Processes requests for the Digital Credential API (DCAPI) by delegating to the
 * ISO 18013-5 [DeviceRequestProcessor] and narrowing its result to the credential the OS
 * picker has already selected (via [ProviderGetCredentialRequest.selectedEntryId]).
 *
 * The exposed [RequestProcessor.ProcessedRequest.Success.presentmentData] tree is filtered
 * to that single credential, so the wallet UI cannot inadvertently surface other matching
 * documents during consent.
 *
 * Follows protocol `org-iso-mdoc` per ISO/IEC TS 18013-7:2025 Annex C.
 */

private const val TAG = "DCAPIRequestProcessor"

internal class DCAPIRequestProcessor(
    private val documentManager: DocumentManager,
    override var readerTrustStore: ReaderTrustStore?,
    private val readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
    private val privilegedAllowlist: String,
    private var zkSystemRepository: ZkSystemRepository?,
    private var logger: Logger? = null,
) : RequestProcessor, ReaderTrustStoreAware {

    override suspend fun process(request: Request): RequestProcessor.ProcessedRequest {
        require(request is DCAPIRequest) { "Request must be an DCAPIRequest" }
        logger?.d(TAG, "Processing DCAPI request")

        val credRequest = request.providerGetCredentialRequest
        val (deviceRequest, origin) = credRequest.toDeviceRequest()
        val processedDeviceRequest = DeviceRequestProcessor(
            documentManager = documentManager,
            readerTrustStore = readerTrustStore,
            readerAuthPolicy = readerAuthPolicy,
            zkSystemRepository = zkSystemRepository
        ).process(deviceRequest) as? ProcessedDeviceRequest
            ?: return RequestProcessor.ProcessedRequest.Failure(
                DCAPIException("DeviceRequestProcessor failed to produce a ProcessedDeviceRequest"),
            )

        val credentialId = credRequest.selectedEntryId
            ?: return RequestProcessor.ProcessedRequest.Failure(
                DCAPIException("No selected credential ID in DCAPI request"),
            )
        logger?.d(TAG, "Filtering presentment data for credential ID: $credentialId")

        val filteredPresentmentData = processedDeviceRequest.presentmentData
            .filterByCredentialId(credentialId)
        if (filteredPresentmentData.credentialSets.isEmpty()) {
            logger?.e(TAG, "No requested document found for credential ID: $credentialId")
            return RequestProcessor.ProcessedRequest.Failure(
                DCAPIException("No requested document found for credential ID: $credentialId"),
            )
        }

        return ProcessedDCPAPIRequest(
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
        // Resolve the origin according to:
        // https://developer.android.com/identity/digital-credentials/credential-holder/credential-holder#check-verifier-origin
        //
        // Privileged callers, such as trusted browsers, may act on behalf of another verifier
        // by setting an origin. CallingAppInfo.getOrigin() returns this origin only when the
        // caller's package name and signing certificate match the provided allowlist.
        //
        // If a trusted origin is returned, use it in the response.
        //
        // If origin is empty, the request is from an Android native app,
        // and we derive the origin from the caller's signing certificate in the form:
        // 'android:apk-key-hash:<encoded SHA 256 fingerprint>'
        val callingOrigin = this.callingAppInfo.getOrigin(privilegedAllowlist)
            ?: getAppOrigin(callingAppInfo.signingInfoCompat.signingCertificateHistory[0].toByteArray())
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

/**
 * Walk a [CredentialPresentmentData] tree and keep only matches whose underlying
 * `Credential.document.identifier` equals [credentialId].
 */
private fun CredentialPresentmentData.filterByCredentialId(
    credentialId: String,
): CredentialPresentmentData {
    val sets = credentialSets.mapNotNull { set ->
        val options = set.options.mapNotNull { option ->
            val members = option.members.mapNotNull { member ->
                val matches = member.matches.filter {
                    it.credential.document.identifier == credentialId
                }
                if (matches.isEmpty()) null
                else CredentialPresentmentSetOptionMember(matches = matches)
            }
            if (members.isEmpty()) null
            else CredentialPresentmentSetOption(members = members)
        }
        if (options.isEmpty()) null
        else CredentialPresentmentSet(optional = set.optional, options = options)
    }
    return CredentialPresentmentData(sets)
}