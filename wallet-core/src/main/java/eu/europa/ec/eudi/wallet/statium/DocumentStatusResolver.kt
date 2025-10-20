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
import eu.europa.ec.eudi.statium.VerifyStatusListTokenJwtSignature
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration

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
         * @param allowedClockSkew the allowed clock skew for the verification
         */
        operator fun invoke(
            verifySignature: VerifyStatusListTokenJwtSignature = VerifyStatusListTokenJwtSignature.x5c,
            ktorHttpClientFactory: () -> HttpClient = { HttpClient() },
            allowedClockSkew: Duration = Duration.ZERO,
        ): DocumentStatusResolver {
            return DocumentStatusResolverImpl(
                verifySignature,
                allowedClockSkew,
                ktorHttpClientFactory
            )
        }

        /**
         * Creates an instance of [DocumentStatusResolver] using a builder
         *
         * @param block a lambda function with a [Builder] as receiver to configure the resolver
         * @return a [DocumentStatusResolver] instance
         */
        operator fun invoke(block: Builder.() -> Unit): DocumentStatusResolver {
            return Builder().apply(block).build()
        }
    }

    /**
     * Builder for [DocumentStatusResolver]
     * It allows to set the parameters for the resolver it builds a [DocumentStatusResolverImpl]
     *
     * @property verifySignature a function to verify the status list token signature; default is [VerifyStatusListTokenJwtSignature.x5c]
     * @property ktorHttpClientFactory a factory function to create an [HttpClient]; default is [HttpClient]
     * @property allowedClockSkew the allowed clock skew for the verification; default is [Duration.ZERO]
     * @property extractor an instance of [StatusReferenceExtractor] to extract the status reference from the document; default is [DefaultStatusReferenceExtractor]
     */
    class Builder {

        var verifySignature: VerifyStatusListTokenJwtSignature = VerifyStatusListTokenJwtSignature.x5c
        var ktorHttpClientFactory: () -> HttpClient = { HttpClient() }
        var allowedClockSkew: Duration = Duration.ZERO
        var extractor: StatusReferenceExtractor = DefaultStatusReferenceExtractor

        /**
         * Sets the function to verify the status list token signature
         * @param verifySignature a function to verify the status list token signature
         * @return the builder instance
         */
        fun withVerifySignature(verifySignature: VerifyStatusListTokenJwtSignature) = apply {
            this.verifySignature = verifySignature
        }

        /**
         * Sets the factory function to create an [HttpClient]
         * @param ktorHttpClientFactory a factory function to create an [HttpClient]
         * @return the builder instance
         */
        fun withKtorHttpClientFactory(ktorHttpClientFactory: () -> HttpClient) = apply {
            this.ktorHttpClientFactory = ktorHttpClientFactory
        }

        /**
         * Sets the allowed clock skew for the verification
         * @param allowedClockSkew the allowed clock skew for the verification
         * @return the builder instance
         */
        fun withAllowedClockSkew(allowedClockSkew: Duration) = apply {
            this.allowedClockSkew = allowedClockSkew
        }

        /**
         * Sets the instance of [StatusReferenceExtractor] to extract the status reference from the document
         * @param extractor an instance of [StatusReferenceExtractor]
         * @return the builder instance
         */
        fun withExtractor(extractor: StatusReferenceExtractor) = apply {
            this.extractor = extractor
        }

        /**
         * Builds the [DocumentStatusResolver] instance
         */
        fun build(): DocumentStatusResolver {
            return DocumentStatusResolverImpl(
                verifySignature,
                allowedClockSkew,
                ktorHttpClientFactory,
                extractor
            )
        }
    }
}

/**
 * Default implementation of [DocumentStatusResolver]
 *
 * @param verifySignature a function to verify the status list token signature
 * @param allowedClockSkew the allowed clock skew for the verification
 * @param ktorHttpClientFactory a factory function to create an [HttpClient]
 * @param extractor an instance of [StatusReferenceExtractor] to extract the status reference from the document
 */
class DocumentStatusResolverImpl(
    internal val verifySignature: VerifyStatusListTokenJwtSignature,
    internal val allowedClockSkew: Duration,
    internal val ktorHttpClientFactory: () -> HttpClient,
    internal val extractor: StatusReferenceExtractor = DefaultStatusReferenceExtractor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DocumentStatusResolver {


    override suspend fun resolveStatus(document: IssuedDocument): Result<Status> = runCatching {
        withContext(ioDispatcher) {
            val statusReference = extractor.extractStatusReference(document).getOrThrow()

            val getStatusListToken = GetStatusListToken.usingJwt(
                clock = Clock.System,
                httpClient = ktorHttpClientFactory(),
                verifyStatusListTokenSignature = verifySignature,
                allowedClockSkew = allowedClockSkew,
            )
            with(GetStatus(getStatusListToken)) {
                statusReference.currentStatus().getOrThrow()
            }
        }
    }
}