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

package eu.europa.ec.eudi.wallet.statium

import eu.europa.ec.eudi.statium.GetStatus
import eu.europa.ec.eudi.statium.GetStatusListToken
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.statium.VerifyStatusListTokenSignature
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import io.ktor.client.HttpClient
import kotlinx.datetime.Clock

/**
 * Interface for resolving the status of a document
 */
interface DocumentStatusResolver {

    /**
     * Resolves the status of the given document
     *
     * @param document the document whose status needs to be resolved
     * @return a [Result] containing the status of the document
     */
    suspend fun resolveStatus(document: IssuedDocument): Result<Status>

    companion object {

        /**
         * Creates an instance of [DocumentStatusResolver]
         *
         * @param ktorHttpClientFactory a factory function to create an [HttpClient]
         * @param verifySignature a function to verify the status list token signature
         */
        operator fun invoke(
            verifySignature: VerifyStatusListTokenSignature = VerifyStatusListTokenSignature.Ignore,
            ktorHttpClientFactory: () -> HttpClient = { HttpClient() },
        ): DocumentStatusResolver {
            return DocumentStatusResolverImpl(verifySignature, ktorHttpClientFactory)
        }
    }
}

/**
 * Default implementation of [DocumentStatusResolver]
 *
 * @param verifySignature a function to verify the status list token signature
 * @param ktorHttpClientFactory a factory function to create an [HttpClient]
 * @param extractor an instance of [StatusReferenceExtractor] to extract the status reference from the document
 */
class DocumentStatusResolverImpl(
    private val verifySignature: VerifyStatusListTokenSignature,
    private val ktorHttpClientFactory: () -> HttpClient,
    private val extractor: StatusReferenceExtractor = DefaultStatusReferenceExtractor,
) : DocumentStatusResolver {


    override suspend fun resolveStatus(document: IssuedDocument): Result<Status> = runCatching {
        val statusReference = extractor.extractStatusReference(document).getOrThrow()

        val getStatusListToken = GetStatusListToken.usingJwt(
            clock = Clock.System,
            httpClientFactory = ktorHttpClientFactory,
            verifyStatusListTokenSignature = verifySignature,
        )
        with(GetStatus(getStatusListToken)) {
            statusReference.currentStatus().getOrThrow()
        }

    }
}