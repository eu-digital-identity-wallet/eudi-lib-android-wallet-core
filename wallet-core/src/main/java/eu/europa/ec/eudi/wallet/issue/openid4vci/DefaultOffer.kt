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

import eu.europa.ec.eudi.openid4vci.*

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
    @JvmSynthetic val credentialConfigurationFilter: CredentialConfigurationFilter = CredentialConfigurationFilter.MsoMdocFormatFilter,
) : Offer {

    private val issuerMetadata: CredentialIssuerMetadata
        get() = credentialOffer.credentialIssuerMetadata

    override val issuerName: String
        get() = issuerMetadata.credentialIssuerIdentifier.value.value.host

    override val offeredDocuments: List<Offer.OfferedDocument>
        get() = issuerMetadata.credentialConfigurationsSupported
            .filterKeys { it in credentialOffer.credentialConfigurationIdentifiers }
            .filterValues { credentialConfigurationFilter(it) }
            .map { (id, conf) -> DefaultOfferedDocument(id, conf) }

    override val txCodeSpec: Offer.TxCodeSpec?
        get() = credentialOffer.txCodeSpec

    override fun toString(): String {
        return "Offer(issuerName='$issuerName', offeredDocuments=$offeredDocuments, txCodeSpec=$txCodeSpec)"
    }
}

/**
 * Default implementation of [Offer.OfferedDocument].
 * @property configurationIdentifier credential configuration identifier
 * @property configuration credential configuration
 * @constructor Creates a new [DefaultOfferedDocument] instance.
 * @param configurationIdentifier [CredentialConfigurationIdentifier] instance
 * @param configuration [CredentialConfiguration] instance
 */
internal data class DefaultOfferedDocument(
    @JvmSynthetic internal val configurationIdentifier: CredentialConfigurationIdentifier,
    @JvmSynthetic internal val configuration: CredentialConfiguration,
) : Offer.OfferedDocument {
    override val name: String = configuration.name
    override val docType: String = configuration.docType

    /**
     *
     */
    override fun toString(): String {
        return "OfferedDocument(name='$name', docType='$docType')"
    }
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

internal val CredentialOffer.txCodeSpec: Offer.TxCodeSpec?
    @JvmSynthetic get() = grants?.preAuthorizedCode()?.txCode.let {
        it?.let { txCode ->
            Offer.TxCodeSpec(
                inputMode = when (txCode.inputMode) {
                    TxCodeInputMode.NUMERIC -> Offer.TxCodeSpec.InputMode.NUMERIC
                    TxCodeInputMode.TEXT -> Offer.TxCodeSpec.InputMode.TEXT
                },
                length = txCode.length,
                description = txCode.description
            )
        }
    }