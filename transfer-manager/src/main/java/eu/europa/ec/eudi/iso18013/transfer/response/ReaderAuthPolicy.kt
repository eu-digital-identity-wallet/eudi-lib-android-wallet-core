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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore

/**
 * Policy for how reader authentication results are enforced during response generation.
 *
 * Each variant carries its own [ReaderTrustStore] so that enforcement intent and
 * enforcement means are always bundled together. This prevents misconfiguration where
 * a policy is set without a trust store, or vice versa.
 *
 * The [readerTrustStore] is used both for trust evaluation (determining whether a
 * verifier's certificate chain is trusted) and as a prerequisite for enforcement
 * (the policy cannot enforce what it cannot evaluate).
 */
sealed interface ReaderAuthPolicy {

    /**
     * The trust store used to validate verifier certificate chains.
     * Null when no trust evaluation is configured.
     */
    val readerTrustStore: ReaderTrustStore?

    /**
     * Do not enforce reader authentication results.
     * Documents are always included in the response regardless of trust status.
     *
     * An optional [readerTrustStore] may be provided for informational trust evaluation
     * (e.g. displaying trust status on the consent UI) without gating disclosure.
     */
    data class DoNotEnforce(
        override val readerTrustStore: ReaderTrustStore? = null
    ) : ReaderAuthPolicy

    /**
     * Enforce reader authentication when present.
     * Documents are skipped when a verifier certificate chain is present but
     * fails trust validation against the [readerTrustStore].
     * Requests with no certificate chain (e.g. unsigned OpenID4VP requests)
     * are still allowed through.
     *
     * Requires a non-null [readerTrustStore] — enforcement cannot proceed
     * without the means to evaluate trust.
     */
    data class EnforceIfPresent(
        override val readerTrustStore: ReaderTrustStore
    ) : ReaderAuthPolicy

    /**
     * Always require verified reader authentication.
     * Documents are skipped unless the verifier's certificate chain is present
     * and successfully validated against the [readerTrustStore].
     *
     * Requires a non-null [readerTrustStore] — enforcement cannot proceed
     * without the means to evaluate trust.
     */
    data class AlwaysRequire(
        override val readerTrustStore: ReaderTrustStore
    ) : ReaderAuthPolicy
}
