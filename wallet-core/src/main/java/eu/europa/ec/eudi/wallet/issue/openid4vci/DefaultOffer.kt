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
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential

/**
 * Default implementation of [Offer].
 * @property issuerName issuer name
 * @property offeredDocuments offered documents
 *
 * @constructor Creates a new [DefaultOffer] instance.
 * @param credentialOffer [CredentialOffer] instance
 * @param credentialConfigurationFilter [CredentialConfigurationFilter] instance
 * @see Offer
 */
internal data class DefaultOffer(
    @JvmSynthetic val credentialOffer: CredentialOffer,
    @JvmSynthetic val credentialConfigurationFilter: CredentialConfigurationFilter = CredentialConfigurationFilter.SdJwtOrMsoMdocFormatFilter
) : Offer {

    private val issuerMetadata: CredentialIssuerMetadata = credentialOffer.credentialIssuerMetadata

    override val issuerName: String = issuerMetadata.credentialIssuerIdentifier.value.value.host
    override val offeredDocuments: List<Offer.OfferedDocument> = issuerMetadata.credentialConfigurationsSupported
        .filterKeys { it in credentialOffer.credentialConfigurationIdentifiers }
        .filterValues { credentialConfigurationFilter(it) }
        .map { (id, conf) -> Offer.OfferedDocument(conf.name, conf.docType, id, conf) }
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
        is SdJwtVcCredential -> "eu.europa.ec.eudi.pid_jwt_vc_json"
        else -> "unknown"
    }