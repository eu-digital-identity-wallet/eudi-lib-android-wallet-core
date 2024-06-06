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

import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential

internal data class DefaultOffer(
    @JvmSynthetic val credentialOffer: CredentialOffer,
    @JvmSynthetic val credentialConfigurationFilter: CredentialConfigurationFilter = CredentialConfigurationFilter.MsoMdocFormatFilter
) : Offer {

    private val issuerMetadata = credentialOffer.credentialIssuerMetadata

    override val issuerName: String = issuerMetadata.credentialIssuerIdentifier.value.value.host
    override val offeredDocuments: List<Offer.OfferedDocument> = issuerMetadata.credentialConfigurationsSupported
        .filterKeys { it in credentialOffer.credentialConfigurationIdentifiers }
        .filterValues { credentialConfigurationFilter(it) }
        .map { (id, conf) -> Offer.OfferedDocument(conf.name, conf.docType, id, conf) }
}

internal val CredentialConfiguration.name: String
    @JvmSynthetic get() = this.display.takeUnless { it.isEmpty() }?.get(0)?.name ?: docType

internal val CredentialConfiguration.docType: String
    @JvmSynthetic get() = when (this) {
        is MsoMdocCredential -> docType
        else -> "unknown"
    }