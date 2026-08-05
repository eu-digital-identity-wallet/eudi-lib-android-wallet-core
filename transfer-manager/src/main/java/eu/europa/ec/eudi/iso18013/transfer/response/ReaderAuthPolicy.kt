/*
 * Copyright (c) 2026 European Commission
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

/**
 * Policy for how reader authentication results are enforced during response generation.
 *
 * Controls whether [ProcessedDeviceRequest.generateResponse] includes documents in the
 * device response based on the [ReaderAuth] result of the corresponding [RequestedDocument].
 */
sealed interface ReaderAuthPolicy {

    /**
     * Do not enforce reader authentication results.
     * Documents are always included in the response regardless of [ReaderAuth] status.
     */
    data object DoNotEnforce : ReaderAuthPolicy

    /**
     * Enforce reader authentication when present.
     * Documents are skipped when [ReaderAuth] is present but [ReaderAuth.isVerified] is `false`.
     * Documents with no reader authentication (null [ReaderAuth]) are still included.
     *
     * This is the default policy.
     */
    data object EnforceIfPresent : ReaderAuthPolicy

    /**
     * Always require verified reader authentication.
     * Documents are skipped when [ReaderAuth] is null or [ReaderAuth.isVerified] is `false`.
     * Only documents with verified reader authentication are included in the response.
     */
    data object AlwaysRequire : ReaderAuthPolicy
}
