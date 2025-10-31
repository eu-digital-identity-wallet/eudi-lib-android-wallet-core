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

import android.content.Intent
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import org.bouncycastle.util.encoders.Hex
import org.json.JSONObject
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.Algorithm
import org.multipaz.util.fromBase64Url
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate

/**
 * Processes a DCAPI request by generating a response based on the provided device request and credential options.
 *
 * This implementation follows the protocol `org-iso-mdoc` as defined in the ISO/IEC TS 18013-7:2025 Annex C.
 *
 * @property processedDeviceRequest The processed device request containing the device response.
 * @property providerGetCredentialRequest The provider get credential request containing the credential options.
 * @property logger Optional logger for logging events.
 * @param origin The origin of the request.
 */

class ProcessedDCPAPIRequest(
    private val processedDeviceRequest: ProcessedDeviceRequest,
    private val providerGetCredentialRequest: ProviderGetCredentialRequest,
    private val logger: Logger? = null,
    val origin: String,
    requestedDocuments: RequestedDocuments
    ): RequestProcessor.ProcessedRequest.Success(requestedDocuments) {

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?
    ): ResponseResult {
        try {
            val option =
                providerGetCredentialRequest.credentialOptions[0] as GetDigitalCredentialOption
            val json = JSONObject(option.requestJson)
            val firstRequest = json.getJSONArray(REQUESTS).getJSONObject(0)

            val protocol = firstRequest[PROTOCOL] as String
            require(protocol == DC_API_PROTOCOL_ORG_ISO_MDOC) { "Unsupported protocol: $protocol" }

            val data = firstRequest[DATA] as JSONObject
            val request = JSONObject(data.toString())
            val encryptionInfoBase64 = request.getString(ENCRYPTION_INFO)

            val encryptionInfo = CBORObject.DecodeFromBytes(encryptionInfoBase64.fromBase64Url())
            if (encryptionInfo.type != CBORType.Array) {
                logger?.e(TAG, "EncryptionInfo should be an array: $encryptionInfo")
                throw DCAPIException("EncryptionInfo should be an array but was: ${encryptionInfo.type}")
            }
            val recipientPublicKey =
                Cbor.decode(
                    encryptionInfo[1][RECIPIENT_PUBLIC_KEY].EncodeToBytes()
                ).asCoseKey.ecPublicKey

            val deviceResponse = processedDeviceRequest.generateResponse(
                disclosedDocuments,
                signatureAlgorithm
            ).getOrThrow() as DeviceResponse

            logger?.d(
                TAG,
                "Device response: ${Hex.toHexString(deviceResponse.deviceResponseBytes)}"
            )

            // Encrypt the device response using HPKE
            val (cipherText, encapsulatedPublicKey) = Crypto.hpkeEncrypt(
                cipherSuite = Algorithm.HPKE_BASE_P256_SHA256_AES128GCM,
                receiverPublicKey = recipientPublicKey,
                plainText = deviceResponse.deviceResponseBytes,
                aad = deviceResponse.sessionTranscriptBytes
            )

            val enc =
                (encapsulatedPublicKey as EcPublicKeyDoubleCoordinate).asUncompressedPointEncoding
            val encryptedResponse = CBORObject.NewArray().apply {
                Add(DCAPI)
                Add(CBORObject.NewMap().apply {
                    Add(ENC, enc)
                    Add(CIPHER_TEXT, cipherText)
                })
            }.EncodeToBytes()

            val responseJson = JSONObject()
            responseJson.put(RESPONSE, encryptedResponse.toBase64())
            val response = responseJson.toString()
            logger?.d(TAG, "Response JSON: $response")

            return ResponseResult.Success(
                DCAPIResponse(
                    deviceResponseBytes = deviceResponse.deviceResponseBytes,
                    intent = createResponseIntent(response),
                    documentIds = deviceResponse.documentIds
                )
            )
        } catch (e: Exception) {
            logger?.e(TAG, "Error generating response: ${e.message}", e)
            return ResponseResult.Failure(
                DCAPIException(
                    message = "Error generating response: ${e.message}",
                    cause = e
                )
            )
        }
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun createResponseIntent(response: String): Intent {
        val resultData = Intent()
        PendingIntentHandler.setGetCredentialResponse(
            resultData,
            GetCredentialResponse(
                DigitalCredential(response)
            )
        )
        return resultData
    }

    companion object {
        private const val TAG = "ProcessedDCPAPIRequest"
        private const val ENC = "enc"
        private const val CIPHER_TEXT = "cipherText"
        private const val RECIPIENT_PUBLIC_KEY = "recipientPublicKey"
        private const val RESPONSE = "response"
    }
}