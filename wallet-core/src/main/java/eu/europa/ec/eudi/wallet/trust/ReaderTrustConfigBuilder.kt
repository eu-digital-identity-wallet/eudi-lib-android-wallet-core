/*
 * Copyright (c) 2023-2026 European Commission
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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy

/**
 * DSL builder for configuring reader trust and authentication policy when using
 * the centralized ETSI trust source from
 * [configureEtsiTrust][eu.europa.ec.eudi.wallet.EudiWalletConfig.configureEtsiTrust].
 *
 * Example:
 * ```
 * configureReaderTrustStore {
 *     readerAuthPolicy(ReaderAuthPolicy.AlwaysRequire)
 * }
 * ```
 */
class ReaderTrustConfigBuilder {

    internal var readerAuthPolicy: ReaderAuthPolicy? = null
        private set

    /**
     * Sets the reader authentication enforcement policy.
     *
     * This controls how reader authentication results affect document disclosure
     * during proximity and DCAPI presentations.
     *
     * - [ReaderAuthPolicy.DoNotEnforce]: Reader authentication is evaluated but never blocks disclosure.
     * - [ReaderAuthPolicy.EnforceIfPresent]: Documents are excluded when reader auth is present but fails (default).
     * - [ReaderAuthPolicy.AlwaysRequire]: Documents are excluded unless reader auth is present and verified.
     *
     * @param policy the reader authentication enforcement policy
     * @see ReaderAuthPolicy
     */
    fun readerAuthPolicy(policy: ReaderAuthPolicy) {
        this.readerAuthPolicy = policy
    }
}
