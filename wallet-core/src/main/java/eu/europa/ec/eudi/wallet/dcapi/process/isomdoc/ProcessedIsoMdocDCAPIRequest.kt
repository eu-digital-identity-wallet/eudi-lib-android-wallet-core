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

import eu.europa.ec.eudi.wallet.dcapi.DCAPIException
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.IsoMdocDCAPIResponse
import eu.europa.ec.eudi.wallet.dcapi.internal.*

import android.content.Intent
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.logging.Logger
import org.bouncycastle.util.encoders.Hex
import org.json.JSONObject
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.Hpke
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.request.Requester
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.util.fromBase64Url

/**
 * Generates the response for an ISO mdoc Digital Credential API request, following the
 * `org-iso-mdoc` protocol of ISO/IEC TS 18013-7:2025 Annex C.
 *
 * It delegates device-response generation to the wrapped [ProcessedDeviceRequest] and then encrypts
 * the result for the verifier. The exposed [presentmentData] is already narrowed to the selected
 * credential(s), so consent only covers what the user chose.
 *
 * @param processedDeviceRequest the underlying ISO 18013-5 request, which generates the signed
 *   device response.
 * @param providerGetCredentialRequest the original request, used to read the verifier's encryption
 *   key and to build the response intent.
 * @param origin the resolved verifier origin.
 * @param presentmentData the credentials offered for consent, narrowed to the user's selection.
 * @param requester information about the verifier making the request.
 * @param trustMetadata trust information about the verifier, when available.
 * @param logger optional logger.
 */
class ProcessedIsoMdocDCAPIRequest(
    private val processedDeviceRequest: ProcessedDeviceRequest,
    private val providerGetCredentialRequest: ProviderGetCredentialRequest,
    val origin: String,
    presentmentData: CredentialPresentmentData,
    requester: Requester,
    trustMetadata: TrustMetadata?,
    private val logger: Logger? = null,
) : RequestProcessor.ProcessedRequest.Success(
    presentmentData = presentmentData,
    requester = requester,
    trustMetadata = trustMetadata
) {

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override suspend fun generateResponse(
        selection: CredentialPresentmentSelection,
        keyUnlockData: Map<String, KeyUnlockData>
    ): ResponseResult {
        return try {
            val (protocol, index) = providerGetCredentialRequest.resolveDcApiRequest(
                listOf(DCAPIProtocol.ISO_MDOC)
            )
            require(protocol == DCAPIProtocol.ISO_MDOC.identifier) { "Unsupported protocol: $protocol" }
            logger?.i(TAG, "Generating ISO mdoc DC API response (protocol=$protocol)")

            val option =
                providerGetCredentialRequest.credentialOptions[0] as GetDigitalCredentialOption
            val json = JSONObject(option.requestJson)
            val request = json.getJSONArray(REQUESTS).getJSONObject(index)

            val data = request[DATA] as JSONObject
            val encryptionInfoBase64 = data.getString(ENCRYPTION_INFO)

            val encryptionInfo = CBORObject.DecodeFromBytes(encryptionInfoBase64.fromBase64Url())
            if (encryptionInfo.type != CBORType.Array) {
                logger?.e(TAG, "EncryptionInfo should be an array: $encryptionInfo")
                throw DCAPIException("EncryptionInfo should be an array but was: ${encryptionInfo.type}")
            }
            val recipientPublicKey = Cbor.decode(
                encryptionInfo[1][RECIPIENT_PUBLIC_KEY].EncodeToBytes(),
            ).asCoseKey.ecPublicKey

            val deviceResponse = processedDeviceRequest.generateResponse(
                selection = selection,
                keyUnlockData = keyUnlockData
            ).getOrThrow() as DeviceResponse

            logger?.d(
                TAG,
                "Device response: ${Hex.toHexString(deviceResponse.deviceResponseBytes)}",
            )

            // HPKE-encrypt the device response per ISO/IEC TS 18013-7:2025 Annex C.
            val encrypter = Hpke.getEncrypter(
                cipherSuite = Hpke.CipherSuite.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM,
                receiverPublicKey = recipientPublicKey,
                info = deviceResponse.sessionTranscriptBytes
            )
            val cipherText = encrypter.encrypt(
                plaintext = deviceResponse.deviceResponseBytes,
                aad = ByteArray(0)
            )
            val encapsulatedPublicKey = encrypter.encapsulatedKey.toByteArray()

            val encryptedResponse = CBORObject.NewArray().apply {
                Add(DCAPI)
                Add(
                    CBORObject.NewMap().apply {
                        Add(ENC, encapsulatedPublicKey)
                        Add(CIPHER_TEXT, cipherText)
                    }
                )
            }.EncodeToBytes()

            val response = JSONObject().put(RESPONSE, encryptedResponse.toBase64())
            logger?.d(TAG, "Response JSON: $response")

            logger?.i(TAG, "ISO mdoc DC API response ready")
            ResponseResult.Success(
                IsoMdocDCAPIResponse(
                    deviceResponseBytes = deviceResponse.deviceResponseBytes,
                    documentIds = selection.matches.map { it.credential.document.identifier }.distinct(),
                    intent = createResponseIntent(protocol = protocol, data = response),
                )
            )
        } catch (e: Exception) {
            logger?.e(TAG, "Error generating response: ${e.message}", e)
            ResponseResult.Failure(
                DCAPIException(
                    message = "Error generating response: ${e.message}",
                    cause = e
                )
            )
        }
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun createResponseIntent(protocol: String, data: JSONObject): Intent {
        val credentialJson = JSONObject().apply {
            put(PROTOCOL, protocol)
            put(DATA, data)
        }
        logger?.d(TAG, "Credential JSON: $credentialJson")
        val resultData = Intent()
        PendingIntentHandler.setGetCredentialResponse(
            resultData,
            GetCredentialResponse(DigitalCredential(credentialJson.toString()))
        )
        return resultData
    }

    companion object {
        private const val TAG = "ProcessedIsoMdocDCAPIRequest"
        private const val ENC = "enc"
        private const val CIPHER_TEXT = "cipherText"
        private const val RECIPIENT_PUBLIC_KEY = "recipientPublicKey"
        private const val RESPONSE = "response"
    }
}