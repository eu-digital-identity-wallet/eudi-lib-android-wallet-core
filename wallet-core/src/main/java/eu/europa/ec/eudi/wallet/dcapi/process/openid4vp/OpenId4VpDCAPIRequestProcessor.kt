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

import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.dcapi.internal.*
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.ProviderGetCredentialRequest
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.internal.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.internal.wrappedWithLogging
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.ProcessedDcqlRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Processes OpenID4VP requests for the Digital Credential API (DCAPI), for the
 * `openid4vp-v1-unsigned` and `openid4vp-v1-signed` protocols.
 *
 * It resolves the request through the OpenID4VP library's Digital Credential API channel and reuses
 * [DcqlRequestProcessor] for credential matching, since the resolved request carries a DCQL query.
 *
 * @param openId4VpConfig the wallet's OpenID4VP configuration.
 * @param dcqlRequestProcessor the processor that matches the DCQL query against stored documents.
 * @param privilegedAllowlist allowlist of privileged callers permitted to set the request origin.
 * @param supportedProtocols the OpenID4VP protocols this processor will accept.
 * @param ktorHttpClientFactory optional factory for the HTTP client used during request resolution.
 * @param logger optional logger.
 */
class OpenId4VpDCAPIRequestProcessor(
    private val openId4VpConfig: OpenId4VpConfig,
    private val dcqlRequestProcessor: DcqlRequestProcessor,
    private val privilegedAllowlist: String,
    private val supportedProtocols: List<DCAPIProtocol>,
    private val ktorHttpClientFactory: (() -> HttpClient)? = null,
    private var logger: Logger? = null
) : RequestProcessor, ReaderTrustStoreAware {

    override var readerTrustStore: ReaderTrustStore?
        get() = dcqlRequestProcessor.readerTrustStore
        set(value) {
            dcqlRequestProcessor.readerTrustStore = value
        }

    private val openId4Vp: OpenId4Vp.OverDcAPI by lazy {
        OpenId4Vp.overDcApi(
            openId4VPConfig = makeOpenId4VPConfig(
                openId4VpConfig,
                dcqlRequestProcessor.openid4VpX509CertificateTrust
            ),
            httpClient = (ktorHttpClientFactory ?: DefaultHttpClientFactory)
                .wrappedWithLogging(logger)
                .wrappedWithContentNegotiation()
                .invoke()
        )
    }

    override suspend fun process(request: Request): RequestProcessor.ProcessedRequest {
        require(request is DCAPIRequest) { "Request must be a DCAPIRequest" }
        logger?.d(TAG, "Processing OpenID4VP DC API request")

        val credRequest = request.providerGetCredentialRequest
        val origin = credRequest.resolveOrigin(privilegedAllowlist)
        val (protocol, requestData) = credRequest.toOpenId4VpRequestData()
        logger?.d(TAG, "Resolved origin=$origin, protocol=$protocol")

        return when (val resolution = openId4Vp.resolveRequestObject(protocol, origin, requestData)) {
            is Resolution.Invalid -> {
                logger?.e(TAG, "Invalid OpenID4VP DC API request: ${resolution.error}")
                RequestProcessor.ProcessedRequest.Failure(resolution.error.asException())
            }

            is Resolution.Success -> {
                try {
                    openId4VpConfig.encryptionPolicy
                        .enforce(resolution.requestObject.responseMode)
                } catch (e: IllegalArgumentException) {
                    logger?.e(TAG, "EncryptionPolicy rejected DC-API request", e)
                    return RequestProcessor.ProcessedRequest.Failure(e)
                }

                logger?.d(TAG, "Resolved OpenID4VP DC API request (protocol=$protocol); delegating to DCQL processor")
                // The resolved request carries a DCQL query — reuse the shared DCQL processor.
                val processed =
                    dcqlRequestProcessor.process(OpenId4VpRequest(resolution.requestObject))
                val dcql = processed as? ProcessedDcqlRequest ?: return processed

                val selectedIds = credRequest.selectedDocumentIds()
                val scoped = when {
                    selectedIds.isEmpty() -> {
                        logger?.d(TAG, "No OS picker selection; using all DCQL matches")
                        dcql
                    }

                    else -> {
                        val filtered = dcql.presentmentData.filterByCredentialIds(selectedIds)
                        if (filtered.credentialSets.isEmpty()) {
                            logger?.e(TAG, "OS-picker selection $selectedIds matched no DCQL candidate; using all matches")
                            dcql
                        } else {
                            logger?.d(TAG, "Scoped DCQL presentment to picker selection: $selectedIds")
                            dcql.withPresentmentData(filtered)
                        }
                    }
                }

                ProcessedOpenId4VpDCAPIRequest(
                    inner = scoped,
                    openId4Vp = openId4Vp,
                    origin = origin,
                    protocol = protocol,
                    logger = logger,
                )
            }
        }
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun ProviderGetCredentialRequest.toOpenId4VpRequestData(): Pair<String, JsonObject> {
        val (protocol, index) = resolveDcApiRequest(supportedProtocols)
        val option = credentialOptions[0] as GetDigitalCredentialOption
        val requestJson = Json.parseToJsonElement(option.requestJson).jsonObject
        val entry = requestJson.getValue(REQUESTS).jsonArray[index].jsonObject
        val data = entry.getValue(DATA).jsonObject
        return protocol to data
    }

    companion object {
        private const val TAG = "OpenId4VpDCAPIRequestProcessor"

        private val DefaultHttpClientFactory: () -> HttpClient = {
            HttpClient {
                install(ContentNegotiation) { json() }
                expectSuccess = true
            }
        }
    }
}
