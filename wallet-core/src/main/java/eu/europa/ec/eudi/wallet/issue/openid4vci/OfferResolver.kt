/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialOfferRequestResolver
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.Compose
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.MsoMdocFormatFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.ProofTypeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

internal class OfferResolver(private val config: OpenId4VciManager.Config) {
    private val resolver by lazy {
        CredentialOfferRequestResolver(
            config.ktorHttpClientFactoryWithLogging
        )
    }
    private val credentialConfigurationFilter by lazy {
        Compose(MsoMdocFormatFilter, ProofTypeFilter(config.proofTypes))
    }

    @VisibleForTesting
    val cache = mutableMapOf<String, Offer>()
    suspend fun resolve(offerUri: String): Result<Offer> {
        if (cache.containsKey(offerUri)) {
            return Result.success(cache[offerUri]!!)
        }
        return resolveOnceWithoutCache(offerUri)
    }

    suspend fun resolveOnceWithoutCache(offerUri: String): Result<Offer> {
        return withContext(Dispatchers.IO) {
            resolver.resolve(offerUri).map { credentialOffer ->
                DefaultOffer(credentialOffer, credentialConfigurationFilter)
            }.onSuccess {
                cache[offerUri] = it
            }.onFailure {
                cache.remove(offerUri)
            }
        }
    }
}
