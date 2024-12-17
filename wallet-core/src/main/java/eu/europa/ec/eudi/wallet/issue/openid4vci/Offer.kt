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
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.TxCode

/**
 * Default implementation of [Offer].
 * @property issuerName issuer name
 * @property issuerMetadata issuer metadata
 * @property offeredDocuments offered documents
 * @property txCodeSpec offered documents
 *
 * @constructor Creates a new [DefaultOffer] instance.
 * @param credentialOffer [CredentialOffer] instance
 * @see Offer
 */

data class Offer(
    val credentialOffer: CredentialOffer,
) {

    val issuerName: String
        get() = issuerMetadata.credentialIssuerIdentifier.value.value.host

    val issuerMetadata: CredentialIssuerMetadata
        get() = credentialOffer.credentialIssuerMetadata

    val offeredDocuments: List<OfferedDocument>
        get() = issuerMetadata.credentialConfigurationsSupported
            .filterKeys { it in credentialOffer.credentialConfigurationIdentifiers }
            .map { (id, conf) -> OfferedDocument(this@Offer, id, conf) }

    val txCodeSpec: TxCode?
        get() = credentialOffer.grants?.preAuthorizedCode()?.txCode

    override fun toString(): String {
        return "Offer(issuerName='$issuerName', offeredDocuments=$offeredDocuments, txCodeSpec=$txCodeSpec)"
    }

}

/**
 * Default implementation of [Offer.OfferedDocument].
 * @property configurationIdentifier credential configuration identifier
 * @property configuration credential configuration
 * @constructor Creates a new [OfferedDocument] instance.
 * @param configurationIdentifier [CredentialConfigurationIdentifier] instance
 * @param configuration [CredentialConfiguration] instance
 */
data class OfferedDocument(
    val offer: Offer,
    val configurationIdentifier: CredentialConfigurationIdentifier,
    val configuration: CredentialConfiguration,
) {
    val name
        get() = configuration.name

    val docType
        get() = configuration.docType
}

/**
 * Credential name based on the display name or the document type.
 * If the display name is empty, the docType is used.
 * @receiver [CredentialConfiguration] instance
 */
internal val CredentialConfiguration.name: String
    @JvmSynthetic get() = this.display.takeUnless { it.isEmpty() }?.get(0)?.name ?: docType

/**
 * Document type based on the credential configuration.
 * DocType is currently only supported for [MsoMdocCredential].
 * @receiver [CredentialConfiguration] instance
 */
internal val CredentialConfiguration.docType: String
    @JvmSynthetic get() = when (this) {
        is MsoMdocCredential -> docType
        else -> "unknown"
    }