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
 */
class DCAPIConfig private constructor(private val builder: Builder) {

    val enabled: Boolean
        get() = builder.enabled

    val privilegedAllowlist: String?
        get() = builder.privilegedAllowlist

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

        fun build(): DCAPIConfig {
            return DCAPIConfig(this)
        }
    }
}