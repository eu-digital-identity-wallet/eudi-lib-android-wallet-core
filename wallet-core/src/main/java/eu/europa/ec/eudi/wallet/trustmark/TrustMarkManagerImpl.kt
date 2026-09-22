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

import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Default implementation of [TrustMarkManager].
 *
 * Resolves [TrustMarkInformation] from either a static source or a dynamic
 * [TrustMarkProvider], and fetches the [TrustMarkResource] from the EC-hosted URL.
 */
internal class TrustMarkManagerImpl(
    private val source: TrustMarkSource,
    private val ktorHttpClientFactory: () -> HttpClient,
    private val logger: Logger? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrustMarkManager {

    override suspend fun getTrustMark(): Result<TrustMark> = runCatching {
        withContext(ioDispatcher) {
            val info = resolveInformation()
            logger?.d(TAG, "getTrustMark: fetching resource from ${info.trustMarkResourceURL}")
            val resource = ktorHttpClientFactory().use { client ->
                val json = client.get(info.trustMarkResourceURL).bodyAsText()
                Json.decodeFromString<TrustMarkResource>(json)
            }
            TrustMark(information = info, resource = resource)
        }
    }.also { result ->
        result.onFailure { error ->
            logger?.e(TAG, "getTrustMark: failed", error)
        }
    }

    private suspend fun resolveInformation(): TrustMarkInformation {
        return when (source) {
            is TrustMarkSource.Static -> {
                logger?.d(TAG, "resolveInformation: returning static configuration")
                source.information
            }

            is TrustMarkSource.Dynamic -> {
                logger?.d(TAG, "resolveInformation: delegating to provider")
                source.provider.getTrustMarkInformation().getOrThrow()
            }
        }
    }

    private companion object {
        private const val TAG = "TrustMark"
    }
}
