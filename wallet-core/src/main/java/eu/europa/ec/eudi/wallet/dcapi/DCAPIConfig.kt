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

@file:JvmMultifileClass
package eu.europa.ec.eudi.wallet.dcapi

/**
 * Configuration for the Digital Credential API (DCAPI).
 *
 * @property enabled whether the DCAPI is enabled, defaults to false.
 * @property privilegedAllowlist the privileged allowlist for the DCAPI, if not set, a default value
 * will be used, see file in assets/privilegedUserAgents.json
 * @property supportedProtocols the DC API protocols this wallet will process; mandatory (non-empty)
 * when [enabled].
 */
class DCAPIConfig private constructor(private val builder: Builder) {

    val enabled: Boolean
        get() = builder.enabled

    val privilegedAllowlist: String?
        get() = builder.privilegedAllowlist

    /**
     * The Digital Credential API protocols this wallet will process. A request for any protocol
     * that is not listed here is rejected. At least one protocol is required when the DCAPI is
     * [enabled].
     */
    val supportedProtocols: Set<DCAPIProtocol>
        get() = builder.supportedProtocols

    /**
     * Builder for [DCAPIConfig].
     * @property enabled whether the DCAPI is enabled, defaults to false
     * @property privilegedAllowlist the privileged allowlist for the DCAPI
     */
    class Builder {

        var enabled: Boolean = false
            private set

        /**
         * Sets whether the DCAPI is enabled.
         * @param enabled true if the DCAPI is enabled, false otherwise
         */
        fun withEnabled(enabled: Boolean) = apply {
            this.enabled = enabled
        }

        var privilegedAllowlist: String? = null
            private set

        /**
         * Sets the privileged allowlist for the DCAPI.
         * @param allowlist the privileged allowlist as a string
         */
        fun withPrivilegedAllowlist(allowlist: String) = apply {
            this.privilegedAllowlist = allowlist
        }

        var supportedProtocols: Set<DCAPIProtocol> = emptySet()
            private set

        /**
         * Sets the DC API protocols this wallet will process. At least one is required when the
         * DCAPI is [enabled].
         * @param protocols the supported [DCAPIProtocol]s
         */
        fun withSupportedProtocols(vararg protocols: DCAPIProtocol) = apply {
            this.supportedProtocols = protocols.toSet()
        }

        fun build(): DCAPIConfig {
            require(!enabled || supportedProtocols.isNotEmpty()) {
                "DCAPIConfig: when enabled, at least one supported protocol must be set via withSupportedProtocols(...)"
            }
            return DCAPIConfig(this)
        }
    }
}