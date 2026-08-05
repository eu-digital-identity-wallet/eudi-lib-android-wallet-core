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
import eu.europa.ec.eudi.openid4vci.CredentialReusePolicy
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vci.TxCode
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat

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
     * @property batchCredentialIssuanceSize batch credential issuance size
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
         * The credential reuse policy from the issuer's credential metadata, if present.
         * Returns [CredentialReusePolicy.None] when the issuer does not advertise a policy.
         */
        val credentialReusePolicy: CredentialReusePolicy
            get() = configuration.credentialMetadata?.credentialReusePolicy
                ?: CredentialReusePolicy.None

        /**
         * Returns the effective batch credential issuance size.
         *
         * Per ETSI TS 119 472-3: when `credential_reuse_policy` is present,
         * the `batch_credential_issuance` metadata property MUST be ignored and
         * the batch size is determined by the reuse policy instead.
         *
         * If no reuse policy is present, falls back to the issuer's
         * `batch_credential_issuance` metadata, or 1 if not supported.
         */
        val batchCredentialIssuanceSize: Int
            get() = when (val policy = credentialReusePolicy) {
                is CredentialReusePolicy.EUDI -> {
                    policy.options.firstNotNullOfOrNull { it.batchSize } ?: 1
                }
                CredentialReusePolicy.None -> {
                    (offer.issuerMetadata.batchCredentialIssuance
                        as? BatchCredentialIssuance.Supported)?.batchSize ?: 1
                }
            }
    }
}