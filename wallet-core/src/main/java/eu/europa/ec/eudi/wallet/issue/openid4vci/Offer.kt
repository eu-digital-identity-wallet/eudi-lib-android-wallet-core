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

@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential

/**
 * An offer of credentials to be issued.
 * @property issuerName the name of the issuer
 * @property offeredDocuments the items to be issued
 */
interface Offer {

    val issuerName: String
    val offeredDocuments: List<OfferedDocument>

    /**
     * An item to be issued.
     * @property name the name of the item
     * @property docType the document type of the item
     */
    data class OfferedDocument internal constructor(
        val name: String,
        val docType: String,
        @JvmSynthetic internal val configurationIdentifier: CredentialConfigurationIdentifier,
        @JvmSynthetic internal val configuration: CredentialConfiguration
    ) {
        /**
         * Converts this item to a pair of name and document type.
         */
        fun asPair() = Pair(name, docType)
    }
}

data class DefaultOffer(
    @JvmSynthetic internal val credentialOffer: CredentialOffer,
    private val filterConfigurations: List<CredentialConfigurationFilter> = listOf(FormatFilter, ProofTypeFilter)
) : Offer {

    override val issuerName: String
        get() = credentialOffer.credentialIssuerMetadata.credentialIssuerIdentifier.value.value.host
    override val offeredDocuments: List<Offer.OfferedDocument>
        get() = credentialOffer.credentialIssuerMetadata.credentialConfigurationsSupported.filter { (id, _) ->
            id in credentialOffer.credentialConfigurationIdentifiers
        }.filterValues { conf ->
            filterConfigurations.all { filter -> filter(conf) }
        }.map { (id, conf) ->
            Offer.OfferedDocument(conf.name, conf.docType, id, conf)
        }
}

internal val CredentialConfiguration.name: String
    @JvmSynthetic get() = display[0].name

internal val CredentialConfiguration.docType: String
    @JvmSynthetic get() = when (this) {
        is MsoMdocCredential -> docType
        else -> "unknown"
    }


