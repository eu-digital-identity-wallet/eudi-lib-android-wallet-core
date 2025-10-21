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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialOfferRequestResolver
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import io.ktor.client.HttpClient
import org.jetbrains.annotations.VisibleForTesting

internal class OfferResolver(
    private val ktorHttpClientFactory: () -> HttpClient,
) {
    val resolver by lazy {
        CredentialOfferRequestResolver(
            httpClient = ktorHttpClientFactory(),
            issuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned
        )
    }

    @VisibleForTesting
    val cache = mutableMapOf<String, Offer>()

    suspend fun resolve(offerUri: String, useCache: Boolean = true): Result<Offer> {
        return if (useCache) {
            cache[offerUri]?.let { Result.success(it) } ?: resolveAndCache(offerUri)
        } else resolveAndCache(offerUri)
    }

    private suspend fun resolveAndCache(offerUri: String): Result<Offer> {
        return resolver.resolve(offerUri).map {
            Offer(it)
        }.also { result ->
            result
                .onSuccess { cache[offerUri] = it }
                .onFailure { cache.remove(offerUri) }
        }
    }
}
