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

package eu.europa.ec.eudi.iso18013.transfer.response

import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.request.Requester
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.trustmanagement.TrustMetadata

/**
 * Interface for request processor. A request processor processes the raw request and returns a processed request.
 * The processed request can be either a success or a failure.
 *
 * On success, the processor exposes the matched credentials [CredentialPresentmentData],
 * the verified [Requester] (cert chain, app id, origin) and any resolved [TrustMetadata].
 * The caller then drives consent UI to produce a [CredentialPresentmentSelection], which is passed
 * back into [Success.generateResponse] to produce the wire response.
 */
fun interface RequestProcessor {

    /**
     * Processes the request.
     *
     * @param request the request
     * @return the processed request
     */
    suspend fun process(request: Request): ProcessedRequest

    /**
     * Represents the result of a processed request.
     */
    sealed interface ProcessedRequest {
        /**
         * The request processing was successful.
         *
         * @property presentmentData candidate credentials and claims that
         *   satisfy the request — used by the wallet UI to drive consent and selection.
         * @property requester the entity that issued the request (cert chain, optional appId/origin).
         * @property trustMetadata metadata about the requester when its identity was successfully
         *   validated against the configured trust store; `null` for untrusted or unverified requesters.
         */
        abstract class Success(
            val presentmentData: CredentialPresentmentData,
            val requester: Requester,
            val trustMetadata: TrustMetadata?
        ) : ProcessedRequest {

            /**
             * The list of options the user can choose from in the consent UI. Each entry
             * groups the credentials that would be shared if the user picks it; pass the
             * chosen one to [generateResponse].
             *
             * The default contains every valid choice that satisfies the request.
             * Subclasses may produce a different list (e.g. a single grouped option). An
             * empty list means the wallet cannot satisfy the request.
             */
            open val presentmentSelections: List<CredentialPresentmentSelection>
                get() = presentmentData.getAllSelections()

            /**
             * Generates the response for the user-confirmed selection.
             *
             * @param selection the user's [CredentialPresentmentSelection] of credentials/claims
             *   to disclose. The selection's matches must originate from this [Success]'s
             *   [presentmentData] tree.
             * @param keyUnlockData per-credential unlock data, keyed by the
             *   `Credential.identifier`. Empty by default.
             * @return the response result containing the wire response or an error.
             */
            abstract suspend fun generateResponse(
                selection: CredentialPresentmentSelection,
                keyUnlockData: Map<String, KeyUnlockData> = emptyMap()
            ): ResponseResult
        }

        /**
         * The request processing failed.
         * @property error the error
         */
        data class Failure(val error: Throwable) : ProcessedRequest

        /**
         * Returns the processed request or throws the error.
         * @throws Throwable the error
         * @return the processed request
         */
        fun getOrThrow(): Success = when (this) {
            is Success -> this
            is Failure -> throw error
        }

        /**
         * Returns the processed request or null.
         * @return the processed request or null
         */
        fun getOrNull(): Success? = this as? Success
    }
}