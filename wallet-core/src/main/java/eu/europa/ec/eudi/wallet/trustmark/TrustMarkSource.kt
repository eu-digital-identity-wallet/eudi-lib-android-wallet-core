/*
 * Copyright (c) 2025-2026 European Commission
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

package eu.europa.ec.eudi.wallet.trustmark

/**
 * Sealed hierarchy representing how Trust Mark information is sourced.
 *
 * Matches the two delivery mechanisms described in EC TS01 v1.2:
 * - **Pre-distribution**: data compiled into the Wallet Solution at build time ([Static]).
 * - **On-demand**: data fetched from a Wallet Provider backend at runtime ([Dynamic]).
 *
 * Pass an instance to [EudiWallet.invoke][eu.europa.ec.eudi.wallet.EudiWallet.Companion.invoke]
 * or [EudiWallet.Builder][eu.europa.ec.eudi.wallet.EudiWallet.Builder] to configure
 * the wallet's Trust Mark support.
 */
sealed interface TrustMarkSource {

    /**
     * Static (pre-distribution) source. The [information] is known at build time and
     * returned directly without any network call.
     */
    data class Static(val information: TrustMarkInformation) : TrustMarkSource

    /**
     * Dynamic (on-demand) source. The [provider] is called at runtime to fetch
     * Trust Mark information from a Wallet Provider backend.
     */
    data class Dynamic(val provider: TrustMarkProvider) : TrustMarkSource
}
