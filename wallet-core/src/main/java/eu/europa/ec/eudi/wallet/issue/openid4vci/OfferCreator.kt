/*
 * Copyright (c) 2024 European Commission
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

import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.DocTypeFilter
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class OfferCreator(
    val config: OpenId4VciManager.Config,
    val ktorHttpClientFactory: () -> HttpClient,
) {

    suspend fun createOffer(docType: String): Offer {
        return withContext(Dispatchers.IO) {
            val credentialIssuerId = CredentialIssuerId(config.issuerUrl).getOrThrow()
            val (credentialIssuerMetadata, authorizationServerMetadata) = ktorHttpClientFactory()
                .use { client ->
                    Issuer.metaData(client, credentialIssuerId)
                }

            val credentialConfigurationFilter = DocTypeFilter(docType)

            val credentialConfigurationId =
                credentialIssuerMetadata.credentialConfigurationsSupported.filterValues { conf ->
                    credentialConfigurationFilter(conf)
                }.keys.firstOrNull() ?: throw IllegalStateException("No suitable configuration found")

            val credentialOffer = CredentialOffer(
                credentialIssuerIdentifier = credentialIssuerId,
                credentialIssuerMetadata = credentialIssuerMetadata,
                authorizationServerMetadata = authorizationServerMetadata.first(),
                credentialConfigurationIdentifiers = listOf(credentialConfigurationId)
            )
            Offer(credentialOffer)
        }
    }
}