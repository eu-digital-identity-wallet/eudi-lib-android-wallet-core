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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor

/**
 * Dispatches OpenID4VP request processing to the appropriate processor based on the presentation query type.
 *
 * This class acts as a router for OpenID4VP requests, delegating processing to either a Presentation Definition
 * processor or a Digital Credentials Query processor, depending on the type of query in the resolved request object.
 * It also manages the reader trust store for verifying reader certificates.
 *
 * @property presentationDefinitionProcessor Processor for Presentation Definition queries.
 * @property digitalCredentialsQueryProcessor Processor for Digital Credentials Query (DCQL) queries.
 * @property openId4VpReaderTrust Trust anchor and store for reader certificate validation.
 */
class RequestProcessorDispatcher(
    val presentationDefinitionProcessor: RequestProcessor,
    val digitalCredentialsQueryProcessor: RequestProcessor,
    var openId4VpReaderTrust: OpenId4VpReaderTrust
) : RequestProcessor, ReaderTrustStoreAware {

    /**
     * The trust store used for verifying reader certificates.
     */
    override var readerTrustStore: ReaderTrustStore?
        get() = openId4VpReaderTrust.readerTrustStore
        set(value) {
            openId4VpReaderTrust.readerTrustStore = value
        }

    /**
     * Processes an [OpenId4VpRequest] by dispatching to the appropriate processor based on the query type.
     *
     * @param request The request to process. Must be an [OpenId4VpRequest] with a resolved [ResolvedRequestObject.OpenId4VPAuthorization].
     * @return The processed request result.
     * @throws IllegalArgumentException if the request is not valid or supported.
     */
    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        // Validate request type and structure
        require(request is OpenId4VpRequest) { "Request must be an OpenId4VpRequest" }
        require(request.resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) {
            "Request must have be a OpenId4VPAuthorization"
        }
        // Dispatch to the correct processor based on the presentation query type
        return when (request.resolvedRequestObject.presentationQuery) {
            is PresentationQuery.ByPresentationDefinition -> presentationDefinitionProcessor
            is PresentationQuery.ByDigitalCredentialsQuery -> digitalCredentialsQueryProcessor
        }.process(request)
    }

    companion object {
        operator fun invoke(
            documentManager: DocumentManager,
            readerTrustStore: ReaderTrustStore?
        ): RequestProcessorDispatcher {
            val openId4VpReaderTrust = OpenId4VpReaderTrustImpl(
                readerTrustStore = readerTrustStore
            )
            val requestProcessor = RequestProcessorDispatcher(
                presentationDefinitionProcessor = OpenId4VpRequestProcessor(
                    documentManager,
                    openId4VpReaderTrust
                ),
                digitalCredentialsQueryProcessor = DcqlRequestProcessor(
                    documentManager,
                    openId4VpReaderTrust
                ),
                openId4VpReaderTrust = openId4VpReaderTrust
            )

            return requestProcessor
        }
    }
}