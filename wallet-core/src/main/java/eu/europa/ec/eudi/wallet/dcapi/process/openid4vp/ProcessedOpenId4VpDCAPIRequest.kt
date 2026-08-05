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

package eu.europa.ec.eudi.wallet.dcapi.process.openid4vp

import eu.europa.ec.eudi.wallet.dcapi.DCAPIException
import eu.europa.ec.eudi.wallet.dcapi.OpenId4VpDCAPIResponse
import eu.europa.ec.eudi.wallet.dcapi.internal.*

import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.ProcessedDcqlRequest
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.securearea.KeyUnlockData

/**
 * Generates the response for an OpenID4VP request over the Digital Credential API.
 *
 * It wraps a [ProcessedDcqlRequest], which builds the VP token, and then assembles the final
 * response through the OpenID4VP library's Digital Credential API channel. For the `dc_api.jwt`
 * response mode the library encrypts the response itself. The result is wrapped into a response
 * intent for the system. Consent and credential selection are delegated to the wrapped request.
 *
 * @param inner the underlying DCQL request that builds the VP token.
 * @param openId4Vp the OpenID4VP Digital Credential API channel used to assemble the response.
 * @param origin the resolved verifier origin, used to bind the response to this request.
 * @param protocol the resolved protocol identifier, echoed back in the response.
 * @param logger optional logger.
 */
class ProcessedOpenId4VpDCAPIRequest(
    private val inner: ProcessedDcqlRequest,
    private val openId4Vp: OpenId4Vp.OverDcAPI,
    private val origin: String,
    private val protocol: String,
    private val logger: Logger? = null,
) : RequestProcessor.ProcessedRequest.Success(
    presentmentData = inner.presentmentData,
    requester = inner.requester,
    trustMetadata = inner.trustMetadata,
) {

    override val presentmentSelections: List<CredentialPresentmentSelection>
        get() = inner.presentmentSelections

    override suspend fun generateResponse(
        selection: CredentialPresentmentSelection,
        keyUnlockData: Map<String, KeyUnlockData>
    ): ResponseResult {
        return try {
            logger?.i(TAG, "Generating OpenID4VP DC API response (protocol=$protocol, origin=$origin)")
            val response = inner.generateResponse(
                selection = selection,
                keyUnlockData = keyUnlockData,
                sessionTranscriptProvider = { it.getSessionTranscriptBytes(origin) },
                sdJwtAudience = "origin:$origin"
            ).getOrThrow() as? OpenId4VpResponse
                ?: return ResponseResult.Failure(
                    DCAPIException("Expected OpenId4VpResponse from DCQL processor")
                )
            logger?.d(TAG, "VP token built; encrypted(dc_api.jwt)=${response.encryptionParameters != null}")
            logger?.d(TAG, "vp_token: ${response.vpToken}")

            // The library decides dc_api vs dc_api.jwt from the response mode and encrypts
            // internally for dc_api.jwt — the wallet must not encrypt here.
            val data = openId4Vp.assembleResponse(
                request = response.resolvedRequestObject,
                consensus = response.vpToken,
                encryptionParameters = response.encryptionParameters,
            )
            logger?.d(TAG, "Assembled OpenID4VP DC API response: $data")

            logger?.i(TAG, "OpenID4VP DC API response ready (encrypted=${response.encryptionParameters != null})")
            ResponseResult.Success(
                OpenId4VpDCAPIResponse(
                    vpToken = response.vpToken,
                    respondedDocuments = response.respondedDocuments,
                    intent = createDcApiResponseIntent(protocol, data),
                )
            )
        } catch (e: Exception) {
            logger?.e(TAG, "Error generating OpenID4VP DC API response: ${e.message}", e)
            ResponseResult.Failure(
                DCAPIException("Error generating response: ${e.message}", e)
            )
        }
    }

    companion object {
        private const val TAG = "ProcessedOpenId4VpDCAPIRequest"
    }
}
