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

import eu.europa.ec.eudi.openid4vci.BatchCredentialIssuance
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vci.TxCode
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy.OneTimeUse
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy.RotateUse
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import kotlin.math.min

/**
 * Represents an offer of credentials from an issuer.
 * @property credentialOffer credential offer
 * @property issuerMetadata issuer metadata
 * @property offeredDocuments offered documents
 * @property txCodeSpec offered documents
 */

data class Offer(
    val credentialOffer: CredentialOffer,
) {

    val issuerMetadata: CredentialIssuerMetadata
        get() = credentialOffer.credentialIssuerMetadata

    val offeredDocuments: List<OfferedDocument>
        get() = issuerMetadata.credentialConfigurationsSupported
            .filterKeys { it in credentialOffer.credentialConfigurationIdentifiers }
            .map { (id, conf) -> OfferedDocument(this@Offer, id, conf) }

    val txCodeSpec: TxCode?
        get() = credentialOffer.grants?.preAuthorizedCode()?.txCode


    /**
     * Represents an offered document part of an [Offer].
     * @property offer [Offer] instance
     * @property configurationIdentifier credential configuration identifier
     * @property configuration credential configuration
     * @property documentFormat document format
     * @property credentialPolicy credential policy by the issuer
     * @property numberOfCredentials number of credentials by the issuer
     */
    data class OfferedDocument(
        val offer: Offer,
        val configurationIdentifier: CredentialConfigurationIdentifier,
        val configuration: CredentialConfiguration,
    ) {
        /**
         * Returns the document format based on the credential configuration.
         */
        val documentFormat: DocumentFormat?
            get() = when (configuration) {
                is MsoMdocCredential -> MsoMdocFormat(docType = configuration.docType)
                is SdJwtVcCredential -> SdJwtVcFormat(vct = configuration.type)
                else -> null
            }

        /**
         * Returns the credential policy based on the issuer metadata.
         */
        val credentialPolicy: CreateDocumentSettings.CredentialPolicy
            get() = if (policy?.oneTimeUse == true) OneTimeUse else RotateUse

        /**
         * Returns the number of credentials based on the issuer metadata.
         */
        val numberOfCredentials: Int
            get() = min(policy?.batchSize ?: 1, batchCredentialIssuanceSize)


        private val batchCredentialIssuanceSize =
            (offer.issuerMetadata.batchCredentialIssuance as? BatchCredentialIssuance.Supported)
                ?.batchSize ?: 1

        private val policy = (configuration as? MsoMdocCredential)?.isoPolicy
    }
}