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

import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.HttpClient

/**
 * Manager for EUDI Wallet Trust Mark operations.
 *
 * Provides access to Trust Mark information and resources as defined in
 * EC TS01 v1.2 (2026-06) — Specification of EUDI Wallet Trust Mark.
 *
 * The manager supports two configuration modes matching the specification's
 * delivery mechanisms:
 *
 * - **Static (pre-distribution)**: Trust Mark information compiled into the app
 *   at build time via [TrustMarkSource.Static].
 *
 * - **Dynamic (on-demand)**: Trust Mark information fetched at runtime from
 *   a Wallet Provider backend via [TrustMarkSource.Dynamic].
 */
fun interface TrustMarkManager {

    /**
     * Retrieves the complete [TrustMark] for this wallet solution.
     *
     * This resolves the [TrustMarkInformation] (from static configuration or dynamic
     * provider) and then fetches the [TrustMarkResource] (logo and localised text) from
     * the [TrustMarkInformation.trustMarkResourceURL] via an HTTP GET to the EC-hosted
     * resource endpoint.
     *
     * @return A [Result] containing the [TrustMark] (information + resource), or a
     *   failure if the information could not be obtained or the resource could not be
     *   fetched or parsed.
     */
    suspend fun getTrustMark(): Result<TrustMark>

    companion object {

        /**
         * Creates a [TrustMarkManager] from the given [TrustMarkSource].
         *
         * @param source The trust mark delivery configuration ([TrustMarkSource.Static]
         *   or [TrustMarkSource.Dynamic]).
         * @param ktorHttpClientFactory A factory for creating [HttpClient] instances used
         *   to fetch the [TrustMarkResource]. Defaults to a plain [HttpClient].
         * @param logger Optional logger for diagnostic output.
         * @return A [TrustMarkManager] instance.
         */
        operator fun invoke(
            source: TrustMarkSource,
            ktorHttpClientFactory: () -> HttpClient = { HttpClient() },
            logger: Logger? = null,
        ): TrustMarkManager = TrustMarkManagerImpl(
            source = source,
            ktorHttpClientFactory = ktorHttpClientFactory,
            logger = logger,
        )
    }
}
