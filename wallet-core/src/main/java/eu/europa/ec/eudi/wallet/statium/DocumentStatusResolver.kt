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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.statium.GetStatus
import eu.europa.ec.eudi.statium.GetStatusListToken
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.statium.VerifyStatusListTokenCwtSignature
import eu.europa.ec.eudi.statium.VerifyStatusListTokenJwtSignature
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.trust.StatusListTrustConfig
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Interface for resolving the status of a document
 */
@Suppress("kotlin:S6517")
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
         * @param verifySignature a function to verify the JWT status list token signature
         * @param ktorHttpClientFactory a factory function to create an [HttpClient]
         * @param allowedClockSkew the allowed clock skew for the verification
         */
        operator fun invoke(
            verifySignature: VerifyStatusListTokenJwtSignature = VerifyStatusListTokenJwtSignature.x5c,
            ktorHttpClientFactory: () -> HttpClient = { HttpClient() },
            allowedClockSkew: Duration = Duration.ZERO,
        ): DocumentStatusResolver {
            return DocumentStatusResolverImpl(
                verifyJwtSignature = verifySignature,
                verifyCwtSignature = VerifyStatusListTokenCwtSignature.x5c,
                allowedClockSkew = allowedClockSkew,
                ktorHttpClientFactory = ktorHttpClientFactory,
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
     * @property verifyJwtSignature a function to verify the JWT status list token signature; default is [VerifyStatusListTokenJwtSignature.x5c]
     * @property verifyCwtSignature a function to verify the CWT status list token signature; default is [VerifyStatusListTokenCwtSignature.x5c]
     * @property ktorHttpClientFactory a factory function to create an [HttpClient]; default is [HttpClient]
     * @property allowedClockSkew the allowed clock skew for the verification; default is [Duration.ZERO]
     * @property extractor an instance of [StatusReferenceExtractor] to extract the status reference from the document; default is [DefaultStatusReferenceExtractor]
     * @property statusListTrustConfig optional ETSI trust configuration for status list token signers
     * @property logger optional logger for logging events
     */
    class Builder {

        var verifyJwtSignature: VerifyStatusListTokenJwtSignature = VerifyStatusListTokenJwtSignature.x5c
        var verifyCwtSignature: VerifyStatusListTokenCwtSignature = VerifyStatusListTokenCwtSignature.x5c
        var ktorHttpClientFactory: () -> HttpClient = { HttpClient() }
        var allowedClockSkew: Duration = Duration.ZERO
        var extractor: StatusReferenceExtractor = DefaultStatusReferenceExtractor
        var statusListTrustConfig: StatusListTrustConfig? = null
        var logger: Logger? = null

        /**
         * Sets the function to verify the JWT status list token signature
         * @param verifySignature a function to verify the JWT status list token signature
         * @return the builder instance
         */
        fun withVerifyJwtSignature(verifySignature: VerifyStatusListTokenJwtSignature) = apply {
            this.verifyJwtSignature = verifySignature
        }

        /**
         * Sets the function to verify the CWT status list token signature
         * @param verifySignature a function to verify the CWT status list token signature
         * @return the builder instance
         */
        fun withVerifyCwtSignature(verifySignature: VerifyStatusListTokenCwtSignature) = apply {
            this.verifyCwtSignature = verifySignature
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
         * Sets the ETSI trust configuration for status list token signers
         * @param config the trust configuration
         * @return the builder instance
         */
        fun withStatusListTrustConfig(config: StatusListTrustConfig) = apply {
            this.statusListTrustConfig = config
        }

        /**
         * Sets the logger for logging events
         * @param logger the logger
         * @return the builder instance
         */
        fun withLogger(logger: Logger?) = apply {
            this.logger = logger
        }

        /**
         * Builds the [DocumentStatusResolver] instance
         */
        fun build(): DocumentStatusResolver {
            return DocumentStatusResolverImpl(
                verifyJwtSignature = verifyJwtSignature,
                verifyCwtSignature = verifyCwtSignature,
                allowedClockSkew = allowedClockSkew,
                ktorHttpClientFactory = ktorHttpClientFactory,
                extractor = extractor,
                statusListTrustConfig = statusListTrustConfig,
                logger = logger,
            )
        }
    }
}

/**
 * Default implementation of [DocumentStatusResolver]
 *
 * Selects JWT or CWT token format based on the document format:
 * - [SdJwtVcFormat] documents use JWT status list tokens
 * - [MsoMdocFormat] documents use CWT status list tokens
 *
 * When a [StatusListTrustConfig] is provided, the signature verifiers are wrapped
 * with trust-evaluating decorators that validate the signer's certificate chain
 * against ETSI trusted lists.
 *
 * @param verifyJwtSignature a function to verify the JWT status list token signature
 * @param verifyCwtSignature a function to verify the CWT status list token signature
 * @param allowedClockSkew the allowed clock skew for the verification
 * @param ktorHttpClientFactory a factory function to create an [HttpClient]
 * @param extractor an instance of [StatusReferenceExtractor] to extract the status reference from the document
 * @param statusListTrustConfig optional ETSI trust configuration for status list token signers
 */
class DocumentStatusResolverImpl(
    internal val verifyJwtSignature: VerifyStatusListTokenJwtSignature,
    internal val verifyCwtSignature: VerifyStatusListTokenCwtSignature,
    internal val allowedClockSkew: Duration,
    internal val ktorHttpClientFactory: () -> HttpClient,
    internal val extractor: StatusReferenceExtractor = DefaultStatusReferenceExtractor,
    internal val statusListTrustConfig: StatusListTrustConfig? = null,
    private val logger: Logger? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DocumentStatusResolver {

    override suspend fun resolveStatus(document: IssuedDocument): Result<Status> = runCatching {
        withContext(ioDispatcher) {
            logger?.d(TAG, "resolveStatus: format=${document.format}, trustConfig=${if (statusListTrustConfig != null) "present" else "null"}")
            val statusReference = extractor.extractStatusReference(document).getOrThrow()
            logger?.d(TAG, "resolveStatus: statusReference uri=${statusReference.uri}, index=${statusReference.index}")

            val getStatusListToken = when (document.format) {
                is MsoMdocFormat -> {
                    logger?.d(TAG, "resolveStatus: using CWT path for MsoMdoc")
                    val verifier = withCwtVerifier(
                        verifyCwtSignature,
                        document.format as MsoMdocFormat,
                    )
                    GetStatusListToken.usingCwt(
                        clock = Clock.System,
                        httpClient = ktorHttpClientFactory(),
                        verifyStatusListTokenSignature = verifier,
                        allowedClockSkew = allowedClockSkew,
                    )
                }

                is SdJwtVcFormat -> {
                    logger?.d(TAG, "resolveStatus: using JWT path for SdJwtVc")
                    val verifier = withJwtVerifier(
                        verifyJwtSignature,
                        document.format as SdJwtVcFormat,
                    )
                    GetStatusListToken.usingJwt(
                        clock = Clock.System,
                        httpClient = ktorHttpClientFactory(),
                        verifyStatusListTokenSignature = verifier,
                        allowedClockSkew = allowedClockSkew,
                    )
                }
            }

            val status = with(GetStatus(getStatusListToken)) {
                statusReference.currentStatus().getOrThrow()
            }
            logger?.d(TAG, "resolveStatus: result=$status")
            status
        }
    }

        private fun withJwtVerifier(
        verifier: VerifyStatusListTokenJwtSignature,
        format: SdJwtVcFormat,
    ): VerifyStatusListTokenJwtSignature {
        val trustConfig = statusListTrustConfig ?: return verifier
        val attestationIdentifier = AttestationIdentifier.SDJwtVc(format.vct)
        return TrustEvaluatingJwtSignatureVerifier(verifier, trustConfig, attestationIdentifier, logger)
    }

    private fun withCwtVerifier(
        verifier: VerifyStatusListTokenCwtSignature,
        format: MsoMdocFormat,
    ): VerifyStatusListTokenCwtSignature {
        val trustConfig = statusListTrustConfig ?: return verifier
        val attestationIdentifier = AttestationIdentifier.MDoc(format.docType)
        return TrustEvaluatingCwtSignatureVerifier(verifier, trustConfig, attestationIdentifier, logger)
    }

    companion object {
        private const val TAG = "StatusList"
    }
}