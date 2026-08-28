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

/**
 * DSL builder for configuring reader trust and authentication policy when using
 * the centralized ETSI trust source from
 * [configureEtsiTrust][eu.europa.ec.eudi.wallet.EudiWalletConfig.configureEtsiTrust].
 *
 * Because the ETSI trust store is created at build time (not config time), this builder
 * tracks the enforcement *intent* rather than a fully-constructed
 * [eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy]. The wallet builder
 * combines the enforcement kind with the resolved trust store to produce the final policy.
 *
 * Example:
 * ```
 * configureReaderTrustStore {
 *     alwaysRequire()
 * }
 * ```
 */
class EtsiReaderTrustConfigBuilder {

    /**
     * Tracks the enforcement intent for the reader authentication policy.
     * Combined with the resolved trust store at build time to produce the
     * final [eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy].
     */
    internal enum class EnforcementKind {
        /** Reader authentication is evaluated but never blocks disclosure. */
        DoNotEnforce,
        /** Documents are excluded when reader auth is present but fails (default). */
        EnforceIfPresent,
        /** Documents are excluded unless reader auth is present and verified. */
        AlwaysRequire
    }

    internal var enforcementKind: EnforcementKind = EnforcementKind.EnforceIfPresent
        private set

    /**
     * Reader authentication is evaluated but never blocks document disclosure.
     *
     * Trust evaluation still occurs (for displaying trust status on the consent UI),
     * but an untrusted or missing certificate chain does not prevent disclosure.
     */
    fun doNotEnforce() {
        this.enforcementKind = EnforcementKind.DoNotEnforce
    }

    /**
     * Documents are excluded from the response when reader authentication is present
     * but fails trust verification. Requests with no certificate chain (e.g. unsigned
     * OpenID4VP requests) are still allowed through.
     *
     * This is the default enforcement policy.
     */
    fun enforceIfPresent() {
        this.enforcementKind = EnforcementKind.EnforceIfPresent
    }

    /**
     * Documents are excluded unless reader authentication is present and successfully
     * verified against the trust store.
     */
    fun alwaysRequire() {
        this.enforcementKind = EnforcementKind.AlwaysRequire
    }
}
